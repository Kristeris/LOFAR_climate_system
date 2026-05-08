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
    @Autowired private UserStatsService      userStatsService;
 
    // ---------------------------------------------------------------
    //  Create
    // ---------------------------------------------------------------
 
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
 
        LocalDateTime newStart = post.effectiveStartTime();
        LocalDateTime newEnd   = post.effectiveEndTime();
 
        // Conflict check — e-mails the rejected user if slot is taken
        checkForConflict(newStart, newEnd, null, author, title);
 
        String calendarEventId = calendarService.createCalendarEvent(
            title, content, newStart, safeDuration, username
        );
        post.setGoogleCalendarEventId(calendarEventId);
 
        ForumPost saved = forumRepo.save(post);
        logger.info("Forum post created: '{}' by {} (calendarId={}, start={}, duration={}s)",
            title, username, calendarEventId, newStart, safeDuration);
 
        userStatsService.recordObservation(author, safeDuration);
 
        emailService.sendForumPostConfirmation(
            author.getEmail(), author.getUsername(), title, calendarEventId
        );
 
        return toDTO(saved);
    }
 
    public ForumPostDTO createPost(String title, String content, String username,
                                   LocalDateTime scheduledDateTime) {
        return createPost(title, content, username, scheduledDateTime, null, null, null, null);
    }
 
    // ---------------------------------------------------------------
    //  Read
    // ---------------------------------------------------------------
 
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
    //  Delete
    // ---------------------------------------------------------------
 
    public void deletePost(Long id, String reason) {
        ForumPost post = forumRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
 
        logger.info("Deleting post {} ('{}') — reason: '{}', calendarId: {}",
            id, post.getTitle(), reason != null ? reason : "not provided",
            post.getGoogleCalendarEventId());
 
        calendarService.deleteCalendarEvent(post.getGoogleCalendarEventId());
        forumRepo.deleteById(id);
        logger.info("Post {} deleted successfully", id);
    }
 
    public void deletePost(Long id) {
        deletePost(id, null);
    }
 
    // ---------------------------------------------------------------
    //  Admin: reschedule
    // ---------------------------------------------------------------
 
    public ForumPostDTO adminUpdateTime(Long id, LocalDateTime newStart, int durationSeconds) {
        ForumPost post = forumRepo.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + id));
 
        int safeDuration = Math.max(1, Math.min(durationSeconds, 3600));
        LocalDateTime newEnd = newStart.plusSeconds(safeDuration);
 
        // null author = no rejection e-mail on admin reschedule
        checkForConflict(newStart, newEnd, id, null, null);
 
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
    //  Conflict check — sends rejection e-mail to the declined user
    // ---------------------------------------------------------------
 
    /**
     * Throws IllegalStateException if [newStart, newEnd) overlaps any existing post.
     *
     * When a real user is being rejected (rejectedUser != null), an e-mail is
     * sent to them BEFORE throwing so they immediately know what happened and
     * which time window was already taken.
     */
    private void checkForConflict(
            LocalDateTime newStart,
            LocalDateTime newEnd,
            Long excludeId,
            MyUser rejectedUser,
            String requestedTitle) {
 
        LocalDateTime rangeStart = newStart.minusHours(1);
        List<ForumPost> candidates = forumRepo.findPostsNear(rangeStart, newEnd, excludeId);
 
        for (ForumPost p : candidates) {
            LocalDateTime pStart = p.effectiveStartTime();
            LocalDateTime pEnd   = p.effectiveEndTime();
 
            if (pStart.isBefore(newEnd) && pEnd.isAfter(newStart)) {
                String conflictingOwner = p.getAuthor() != null
                    ? p.getAuthor().getUsername() : "another user";
 
                logger.warn(
                    "Conflict: '{}' requested [{} – {}] but '{}' already holds [{} – {}]",
                    rejectedUser != null ? rejectedUser.getUsername() : "admin",
                    newStart, newEnd, conflictingOwner, pStart, pEnd
                );
 
                // E-mail the user whose request is being rejected
                if (rejectedUser != null
                        && rejectedUser.getEmail() != null
                        && !rejectedUser.getEmail().isBlank()) {
 
                    emailService.sendConflictRejectionEmail(
                        rejectedUser.getEmail(),
                        rejectedUser.getUsername(),
                        requestedTitle,
                        newStart,
                        newEnd,
                        pStart,
                        pEnd,
                        conflictingOwner
                    );
                }
 
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
        if (end.isBefore(now))    status = "PAST";
        else if (start.isAfter(now)) status = "FUTURE";
        else                         status = "CURRENT";
 
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