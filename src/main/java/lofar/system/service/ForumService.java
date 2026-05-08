package lofar.system.service;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import lofar.system.model.ForumPost;
import lofar.system.model.ForumPostDTO;
import lofar.system.model.MyUser;
import lofar.system.repo.ForumPostRepo;
import lofar.system.repo.IMyUserRepo;
 
@Service
public class ForumService {
 
    private static final Logger logger = LoggerFactory.getLogger(ForumService.class);
 
    @Autowired private ForumPostRepo         forumRepo;
    @Autowired private IMyUserRepo           userRepo;
    @Autowired private GoogleCalendarService calendarService;
    @Autowired private EmailService          emailService;
    @Autowired private UserStatsService      userStatsService;   // ← NEW
 
    // ---------------------------------------------------------------
    //  Create
    // ---------------------------------------------------------------
 
    /**
     * Creates a new forum / observation post.
     *
     * 1. Look up author.
     * 2. Conflict-check: reject if time window overlaps an existing post.
     * 3. Create Google Calendar event (with correct duration and creator name).
     * 4. Persist post.
     * 5. Record hours in user stats.
     * 6. Send confirmation e-mail.
     */
    public ForumPostDTO createPost(
            String title,
            String content,
            String username,
            LocalDateTime scheduledDateTime,
            Integer durationSeconds,
            Double anadirX,
            Double anadirY,
            String anadirSystem) {
 
        MyUser author = userRepo.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
 
        ForumPost post = new ForumPost(title, content, author);
        post.setScheduledDateTime(scheduledDateTime);
 
        int safeDuration = (durationSeconds != null && durationSeconds > 0 && durationSeconds <= 3600)
                           ? durationSeconds : 3600;
        post.setDurationSeconds(safeDuration);
 
        post.setAnadirX(anadirX);
        post.setAnadirY(anadirY);
        post.setAnadirSystem(anadirSystem);
 
        // ── Conflict check ──────────────────────────────────────────
        LocalDateTime newStart = post.effectiveStartTime();
        LocalDateTime newEnd   = post.effectiveEndTime();
        checkForConflict(newStart, newEnd, null);
 
        // ── Google Calendar ─────────────────────────────────────────
        String calendarEventId = calendarService.createCalendarEvent(
            title, content, newStart, safeDuration, username
        );
        post.setGoogleCalendarEventId(calendarEventId);
 
        ForumPost saved = forumRepo.save(post);
        logger.info("Forum post created: '{}' by {} (calendarId={}, start={}, duration={}s)",
            title, username, calendarEventId, newStart, safeDuration);
 
        // ── Track user observation hours ────────────────────────────
        userStatsService.recordObservation(author, safeDuration);
 
        // ── Confirmation e-mail ─────────────────────────────────────
        emailService.sendForumPostConfirmation(
            author.getEmail(), author.getUsername(), title, calendarEventId
        );
 
        return toDTO(saved);
    }
 
    /** Backwards-compatible overload (no anadir / duration). */
    public ForumPostDTO createPost(String title, String content, String username,
                                   LocalDateTime scheduledDateTime) {
        return createPost(title, content, username, scheduledDateTime, null, null, null, null);
    }
 
    // ---------------------------------------------------------------
    //  Read — role-scoped
    // ---------------------------------------------------------------
 
    /**
     * Admin sees all posts; regular users see only their own.
     */
    public List<ForumPostDTO> getPostsForUser(String username, boolean isAdmin) {
        List<ForumPost> posts = isAdmin
            ? forumRepo.findAllByOrderByCreatedAtDesc()
            : forumRepo.findByAuthorUsernameOrderByCreatedAtDesc(username);
        return posts.stream().map(this::toDTO).collect(Collectors.toList());
    }
 
    public List<ForumPostDTO> getAllPosts() {
        return forumRepo.findAllByOrderByCreatedAtDesc()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }
 
    public ForumPostDTO getPost(Long id) {
        ForumPost post = forumRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
        return toDTO(post);
    }
 
    // ---------------------------------------------------------------
    //  Delete — removes Google Calendar event too
    // ---------------------------------------------------------------
 
    /**
     * Deletes a forum post and its Google Calendar event.
     * The deletion reason is logged server-side but NOT sent to the user.
     */
    public void deletePost(Long id, String reason) {
        ForumPost post = forumRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
 
        String calId = post.getGoogleCalendarEventId();
        logger.info("Deleting post {} ('{}') — reason: '{}', calendarId: {}",
            id, post.getTitle(), reason != null ? reason : "not provided", calId);
 
        calendarService.deleteCalendarEvent(calId);
        forumRepo.deleteById(id);
        logger.info("Post {} deleted successfully", id);
    }
 
    public void deletePost(Long id) {
        deletePost(id, null);
    }
 
    // ---------------------------------------------------------------
    //  Admin: change scheduled time
    // ---------------------------------------------------------------
 
    /**
     * Admin reschedules an existing post.
     * - Conflict-checks (excluding itself).
     * - Updates Google Calendar event.
     * - Notifies the post author via e-mail.
     */
    public ForumPostDTO adminUpdateTime(Long id, LocalDateTime newStart, int durationSeconds) {
        ForumPost post = forumRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
 
        int safeDuration = Math.max(1, Math.min(durationSeconds, 3600));
        LocalDateTime newEnd = newStart.plusSeconds(safeDuration);
 
        checkForConflict(newStart, newEnd, id);
 
        calendarService.updateCalendarEventTime(
            post.getGoogleCalendarEventId(), newStart, safeDuration
        );
 
        post.setScheduledDateTime(newStart);
        post.setDurationSeconds(safeDuration);
        ForumPost saved = forumRepo.save(post);
 
        logger.info("Admin updated time for post {} — new start: {}, duration: {}s",
            id, newStart, safeDuration);
 
        MyUser author = post.getAuthor();
        if (author != null && author.getEmail() != null && !author.getEmail().isBlank()) {
            emailService.sendTimeChangedNotification(
                author.getEmail(), author.getUsername(),
                post.getTitle(), newStart, safeDuration
            );
        }
 
        return toDTO(saved);
    }
 
    // ---------------------------------------------------------------
    //  Conflict check
    // ---------------------------------------------------------------
 
    private void checkForConflict(LocalDateTime newStart, LocalDateTime newEnd, Long excludeId) {
        LocalDateTime rangeStart = newStart.minusHours(1);
        List<ForumPost> candidates = forumRepo.findPostsNear(rangeStart, newEnd, excludeId);
 
        for (ForumPost p : candidates) {
            LocalDateTime pStart = p.effectiveStartTime();
            LocalDateTime pEnd   = p.effectiveEndTime();
            if (pStart.isBefore(newEnd) && pEnd.isAfter(newStart)) {
                throw new IllegalStateException(
                    "Time slot conflict: another observation is already scheduled between "
                    + pStart + " and " + pEnd + "."
                );
            }
        }
    }
 
    // ---------------------------------------------------------------
    //  Mapper
    // ---------------------------------------------------------------
 
    private ForumPostDTO toDTO(ForumPost p) {
        LocalDateTime now   = LocalDateTime.now();
        LocalDateTime start = p.effectiveStartTime();
        LocalDateTime end   = p.effectiveEndTime();
 
        String status;
        if (end.isBefore(now)) {
            status = "PAST";
        } else if (start.isAfter(now)) {
            status = "FUTURE";
        } else {
            status = "CURRENT";
        }
 
        return new ForumPostDTO(
            p.getId(),
            p.getTitle(),
            p.getContent(),
            p.getCreatedAt(),
            p.getTimeUtc(),
            p.getAuthor().getUsername(),
            p.getGoogleCalendarEventId(),
            p.getScheduledDateTime(),
            p.getDurationSeconds(),
            p.getAnadirX(),
            p.getAnadirY(),
            p.getAnadirSystem(),
            status
        );
    }
}