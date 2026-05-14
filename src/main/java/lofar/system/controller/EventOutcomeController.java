package lofar.system.controller;
 
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import lofar.system.model.EventOutcomeDTO;
import lofar.system.service.EventOutcomeService;
 
/**
 * EventOutcomeController
 *
 * GET  /api/forum/{postId}/outcome              — get the outcome for a past event
 * POST /api/forum/{postId}/outcome              — admin sets/updates the outcome
 * GET  /api/forum/{postId}/outcome/download-log — download the log snapshot as .txt
 */
@RestController
@RequestMapping("/api/forum")
@CrossOrigin(origins = "http://localhost:4200")
public class EventOutcomeController {
 
    @Autowired
    private EventOutcomeService outcomeService;
 
    // ---------------------------------------------------------------
    //  GET outcome
    // ---------------------------------------------------------------
 
    @GetMapping("/{postId}/outcome")
    public ResponseEntity<?> getOutcome(@PathVariable Long postId) {
        Optional<EventOutcomeDTO> outcome = outcomeService.getOutcome(postId);
        if (outcome.isPresent()) {
            return ResponseEntity.ok(outcome.get());
        }
        // No outcome yet — return a neutral placeholder so the frontend
        // can distinguish "no result yet" from "fetch error"
        return ResponseEntity.ok(Map.of(
            "postId", postId,
            "status", "UNKNOWN",
            "summary", "Outcome not yet recorded"
        ));
    }
 
    // ---------------------------------------------------------------
    //  POST outcome (admin manual set)
    // ---------------------------------------------------------------
 
    /**
     * Body JSON:
     * {
     *   "status":      "SUCCESS" | "FAILURE" | "UNKNOWN",
     *   "summary":     "optional human-readable text",
     *   "logSnapshot": "optional nohup.out excerpt"
     * }
     */
    @PostMapping("/{postId}/outcome")
    public ResponseEntity<?> setOutcome(
            @PathVariable Long postId,
            @RequestBody Map<String, String> body) {
 
        String status  = body.getOrDefault("status", "UNKNOWN");
        String summary = body.get("summary");
        String logSnap = body.get("logSnapshot");
 
        try {
            EventOutcomeDTO saved = outcomeService.setOutcome(postId, status, summary, logSnap);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
 
    // ---------------------------------------------------------------
    //  GET — download log snapshot as plain-text file
    // ---------------------------------------------------------------
 
    /**
     * Returns the stored log snapshot as a downloadable .txt file.
     * Used by the "Download logs" button in the past-event detail view.
     */
    @GetMapping("/{postId}/outcome/download-log")
    public ResponseEntity<byte[]> downloadLogSnapshot(@PathVariable Long postId) {
        Optional<EventOutcomeDTO> outcome = outcomeService.getOutcome(postId);
 
        if (outcome.isEmpty() || outcome.get().getLogSnapshot() == null) {
            return ResponseEntity.notFound().build();
        }
 
        String content  = outcome.get().getLogSnapshot();
        byte[] bytes    = content.getBytes(StandardCharsets.UTF_8);
        String filename = "observation_log_post_" + postId + ".txt";
 
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + "\"")
            .contentType(MediaType.TEXT_PLAIN)
            .contentLength(bytes.length)
            .body(bytes);
    }
}