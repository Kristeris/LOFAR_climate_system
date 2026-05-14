package lofar.system.controller;
 
import java.security.Principal;
import java.util.List;
import java.util.Map;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import lofar.system.model.ForumCommentDTO;
import lofar.system.service.ForumCommentService;
 
/**
 * ForumCommentController
 *
 * GET    /api/forum/{postId}/comments          — list comments on a post
 * POST   /api/forum/{postId}/comments          — add a comment (authenticated)
 * DELETE /api/forum/{postId}/comments/{id}     — delete a comment (owner or admin)
 */
@RestController
@RequestMapping("/api/forum")
@CrossOrigin(origins = "http://localhost:4200")
public class ForumCommentController {
 
    @Autowired
    private ForumCommentService commentService;
 
    // ---------------------------------------------------------------
    //  GET comments for a post
    // ---------------------------------------------------------------
 
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<ForumCommentDTO>> getComments(
            @PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsForPost(postId));
    }
 
    // ---------------------------------------------------------------
    //  POST — add comment
    // ---------------------------------------------------------------
 
    @PostMapping("/{postId}/comments")
    public ResponseEntity<?> addComment(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body,
            Principal principal) {
 
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
 
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
        }
 
        try {
            ForumCommentDTO created = commentService.addComment(postId, principal.getName(), content);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
 
    // ---------------------------------------------------------------
    //  DELETE — remove comment (owner or admin)
    // ---------------------------------------------------------------
 
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            Principal principal,
            Authentication auth) {
 
        if (principal == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
        }
 
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ADMIN"::equals);
 
        try {
            commentService.deleteComment(commentId, principal.getName(), isAdmin);
            return ResponseEntity.ok(Map.of("message", "Comment deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).body(Map.of("error", e.getMessage()));
        }
    }
}