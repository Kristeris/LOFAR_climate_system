package lofar.system.service;
 
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
 
    @Autowired private ForumPostRepo  forumRepo;
    @Autowired private IMyUserRepo    userRepo;
    @Autowired private GoogleCalendarService calendarService;
    @Autowired private EmailService   emailService;
 
    // ---------------------------------------------------------------
    //  Create
    // ---------------------------------------------------------------
 
    public ForumPostDTO createPost(String title, String content, String username) {
        MyUser author = userRepo.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
 
        ForumPost post = new ForumPost(title, content, author);
 
        String calendarEventId = calendarService.createCalendarEvent(
            title, content, post.getCreatedAt()
        );
        post.setGoogleCalendarEventId(calendarEventId);
 
        ForumPost saved = forumRepo.save(post);
        logger.info("Forum post created: '{}' by {} (calendarId={}, timeUtc={})",
            title, username, calendarEventId, saved.getTimeUtc());
 
        emailService.sendForumPostConfirmation(
            author.getEmail(), author.getUsername(), title, calendarEventId
        );
 
        return toDTO(saved);
    }
 
    // ---------------------------------------------------------------
    //  Read
    // ---------------------------------------------------------------
 
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
 
    public void deletePost(Long id) {
        if (!forumRepo.existsById(id)) {
            throw new IllegalArgumentException("Post not found: " + id);
        }
        forumRepo.deleteById(id);
    }
 
    // ---------------------------------------------------------------
    //  Mapper
    // ---------------------------------------------------------------
 
    private ForumPostDTO toDTO(ForumPost p) {
        return new ForumPostDTO(
            p.getId(),
            p.getTitle(),
            p.getContent(),
            p.getCreatedAt(),
            p.getTimeUtc(),          // ← new field
            p.getAuthor().getUsername(),
            p.getGoogleCalendarEventId()
        );
    }
}