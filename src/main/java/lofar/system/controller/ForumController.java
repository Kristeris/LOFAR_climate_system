package lofar.system.controller;
 
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import lofar.system.model.ForumPostDTO;
import lofar.system.service.ForumService;
 
/**
 * ForumController
 *
 * GET    /api/forum                  — list posts visible to caller
 *                                      (admin sees all; user sees own)
 * GET    /api/forum/{id}             — single post
 * POST   /api/forum                  — create post (authenticated)
 * DELETE /api/forum/{id}?reason=...  — delete post + Google Calendar event (admin)
 * PUT    /api/forum/{id}/time        — admin changes scheduled time (admin only)
 */
@RestController
@RequestMapping("/api/forum")
@CrossOrigin(origins = "http://localhost:4200")
public class ForumController {
 
    @Autowired
    private ForumService forumService;
 
    // ---------------------------------------------------------------
    //  GET list
    // ---------------------------------------------------------------
 
    @GetMapping
    public ResponseEntity<List<ForumPostDTO>> getAllPosts(
            Principal principal,
            Authentication auth) {
 
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
 
        boolean isAdmin = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ADMIN"::equals);
 
        List<ForumPostDTO> posts = forumService.getPostsForUser(principal.getName(), isAdmin);
        return ResponseEntity.ok(posts);
    }
 


//     @GetMapping
// public ResponseEntity<List<ForumPostDTO>> getAllPosts(Authentication authentication) {
//     if (authentication == null) {
//         return ResponseEntity.status(401).build();
//     }

//     logger.info("User '{}' authorities: {}", authentication.getName(), authentication.getAuthorities());

//     boolean isAdmin = authentication.getAuthorities().stream()
//             .map(GrantedAuthority::getAuthority)
//             .anyMatch(auth -> auth.equals("ADMIN") || auth.equals("ROLE_ADMIN"));

//     logger.info("isAdmin = {} for user '{}'", isAdmin, authentication.getName());

//     List<ForumPostDTO> posts = forumService.getPostsForUser(authentication.getName(), isAdmin);
//     return ResponseEntity.ok(posts);
// }



            //my old version
    //     @GetMapping
    // public ResponseEntity<List<ForumPostDTO>> getAllPosts(Authentication authentication) {
    //     if (authentication == null) {
    //         return ResponseEntity.status(401).build();
    //     }
        
    //     logger.info("User '{}' authorities: {}", authentication.getName(), authentication.getAuthorities());
        
    //     boolean isAdmin = authentication.getAuthorities().stream()
    //             .anyMatch(auth -> {
    //                 String authStr = auth.getAuthority();
    //                 return authStr.equals("ADMIN") || authStr.equals("ROLE_ADMIN");
    //             });
        
    //     logger.info("isAdmin = {} for user '{}'", isAdmin, authentication.getName());
        
    //     if (isAdmin) {
    //         return ResponseEntity.ok(forumService.getAllPosts());
    //     }
    //     return ResponseEntity.ok(forumService.getPostsByUser(authentication.getName()));
    // }




    // ---------------------------------------------------------------
    //  GET single
    // ---------------------------------------------------------------
 
    @GetMapping("/{id}")
    public ResponseEntity<ForumPostDTO> getPost(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(forumService.getPost(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
 
    // ---------------------------------------------------------------
    //  POST create
    // ---------------------------------------------------------------
 
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
 
        // Optional scheduled datetime
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
 
        // Optional duration (seconds, max 3600)
        Integer durationSeconds = null;
        String rawDur = body.get("durationSeconds");
        if (rawDur != null && !rawDur.isBlank()) {
            try {
                int d = Integer.parseInt(rawDur);
                if (d < 1 || d > 3600) {
                    return ResponseEntity.badRequest()
                        .body(Map.of("error", "durationSeconds must be between 1 and 3600"));
                }
                durationSeconds = d;
            } catch (NumberFormatException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid durationSeconds"));
            }
        }
 
        // Optional ANADIR fields
        Double anadirX = parseDoubleOrNull(body.get("anadirX"));
        Double anadirY = parseDoubleOrNull(body.get("anadirY"));
        String anadirSystem = body.get("anadirSystem");
 
        try {
            ForumPostDTO created = forumService.createPost(
                title, content, principal.getName(),
                scheduledDateTime, durationSeconds,
                anadirX, anadirY, anadirSystem
            );
            return ResponseEntity.ok(created);
        } catch (IllegalStateException e) {
            // Time-slot conflict
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
 
    // ---------------------------------------------------------------
    //  DELETE — with optional reason (admin only; reason not sent to user)
    // ---------------------------------------------------------------
 
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePost(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            forumService.deletePost(id, reason);
            return ResponseEntity.ok(Map.of("message", "Post deleted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
 
    // ---------------------------------------------------------------
    //  PUT /api/forum/{id}/time — admin changes scheduled time
    // ---------------------------------------------------------------
 
    @PutMapping("/{id}/time")
    public ResponseEntity<?> updateTime(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            Authentication auth) {
 
        boolean isAdmin = auth != null && auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ADMIN"::equals);
 
        if (!isAdmin) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin only"));
        }
 
        String rawDt = body.get("scheduledDateTime");
        if (rawDt == null || rawDt.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "scheduledDateTime is required"));
        }
 
        LocalDateTime newStart;
        try {
            newStart = LocalDateTime.parse(rawDt);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid scheduledDateTime. Use format: 2026-04-11T14:00:00"));
        }
 
        int durationSeconds = 3600;
        String rawDur = body.get("durationSeconds");
        if (rawDur != null && !rawDur.isBlank()) {
            try {
                durationSeconds = Integer.parseInt(rawDur);
            } catch (NumberFormatException ignored) { /* keep default */ }
        }
 
        try {
            ForumPostDTO updated = forumService.adminUpdateTime(id, newStart, durationSeconds);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
 
    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------
 
    private Double parseDoubleOrNull(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return null; }
    }
}