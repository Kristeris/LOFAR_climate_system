package lofar.system.service;
 
import java.util.List;
import java.util.stream.Collectors;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import lofar.system.model.ForumComment;
import lofar.system.model.ForumCommentDTO;
import lofar.system.model.ForumPost;
import lofar.system.model.MyUser;
import lofar.system.repo.ForumCommentRepo;
import lofar.system.repo.ForumPostRepo;
import lofar.system.repo.IMyUserRepo;
 
/**
 * ForumCommentService
 *
 * Business logic for forum post comments.
 *
 * Rules:
 *  - Any authenticated user may comment on ANY visible post
 *    (not only their own, so teams can discuss each other's observations).
 *  - Comment content max 2 000 characters.
 *  - Only the comment author or an ADMIN may delete a comment.
 */
@Service
public class ForumCommentService {
 
    private static final Logger logger = LoggerFactory.getLogger(ForumCommentService.class);
 
    @Autowired private ForumCommentRepo commentRepo;
    @Autowired private ForumPostRepo    postRepo;
    @Autowired private IMyUserRepo      userRepo;
 
    // ---------------------------------------------------------------
    //  Create
    // ---------------------------------------------------------------
 
    /**
     * Adds a comment to the specified post.
     *
     * @param postId   ID of the ForumPost being commented on
     * @param username username of the commenter (from Principal)
     * @param content  comment body (1–2000 chars)
     * @return DTO of the saved comment
     */
    public ForumCommentDTO addComment(Long postId, String username, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Comment content must not be empty");
        }
        if (content.length() > 2000) {
            throw new IllegalArgumentException("Comment must be 2000 characters or fewer");
        }
 
        ForumPost post = postRepo.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
 
        MyUser author = userRepo.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
 
        ForumComment comment = new ForumComment(post, author, content.trim());
        ForumComment saved   = commentRepo.save(comment);
 
        logger.info("Comment #{} added to post {} by '{}'", saved.getId(), postId, username);
        return toDTO(saved);
    }
 
    // ---------------------------------------------------------------
    //  Read
    // ---------------------------------------------------------------
 
    /**
     * Returns all comments for a post, ordered oldest-first.
     */
    public List<ForumCommentDTO> getCommentsForPost(Long postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }
 
    // ---------------------------------------------------------------
    //  Delete
    // ---------------------------------------------------------------
 
    /**
     * Deletes a comment.
     *
     * @param commentId ID of the comment to delete
     * @param username  username of the requester
     * @param isAdmin   true if requester is an ADMIN (can delete any comment)
     */
    public void deleteComment(Long commentId, String username, boolean isAdmin) {
        ForumComment comment = commentRepo.findById(commentId)
            .orElseThrow(() -> new IllegalArgumentException("Comment not found: " + commentId));
 
        boolean isOwner = comment.getAuthor().getUsername().equals(username);
        if (!isOwner && !isAdmin) {
            throw new SecurityException("Not authorised to delete comment " + commentId);
        }
 
        commentRepo.deleteById(commentId);
        logger.info("Comment #{} deleted by '{}' (admin={})", commentId, username, isAdmin);
    }
 
    // ---------------------------------------------------------------
    //  Mapper
    // ---------------------------------------------------------------
 
    private ForumCommentDTO toDTO(ForumComment c) {
        return new ForumCommentDTO(
            c.getId(),
            c.getPost().getId(),
            c.getAuthor().getUsername(),
            c.getContent(),
            c.getCreatedAt()
        );
    }
}