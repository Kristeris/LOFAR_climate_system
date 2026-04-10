package lofar.system.controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lofar.system.model.ForumPostDTO;
import lofar.system.service.ForumService;

/**
 * ForumController
 *
 * GET    /api/forum          — list all posts (newest first)
 * GET    /api/forum/{id}     — single post
 * POST   /api/forum          — create new post (authenticated)
 * DELETE /api/forum/{id}     — delete post (ADMIN or owner)
 */
@RestController
@RequestMapping("/api/forum")
@CrossOrigin(origins = "http://localhost:4200")
public class ForumController {

    @Autowired
    private ForumService forumService;

    @GetMapping
    public ResponseEntity<List<ForumPostDTO>> getAllPosts() {
        return ResponseEntity.ok(forumService.getAllPosts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ForumPostDTO> getPost(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(forumService.getPost(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

@PostMapping
public ResponseEntity<?> createPost(
        @RequestBody Map<String, String> body,
        Principal principal) {

    if (principal == null) {
        return ResponseEntity.status(401).body(Map.of("error", "Not authenticated"));
    }

    String title   = body.get("title");
    String content = body.get("content");

    if (title == null || title.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Title is required"));
    }
    if (content == null || content.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "Content is required"));
    }

    // Parse the optional scheduled datetime from the frontend (e.g. "2026-04-11T14:00:00")
    LocalDateTime scheduledDateTime = null;
    String rawDt = body.get("scheduledDateTime");
    if (rawDt != null && !rawDt.isBlank()) {
        try {
            scheduledDateTime = LocalDateTime.parse(rawDt);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid scheduledDateTime. Use format: 2026-04-11T14:00:00"));
        }
    }

    try {
        ForumPostDTO created = forumService.createPost(
            title, content, principal.getName(), scheduledDateTime
        );
        return ResponseEntity.ok(created);
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
    }
}

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(@PathVariable Long id) {
        try {
            forumService.deletePost(id);
            return ResponseEntity.ok(Map.of("message", "Post deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}