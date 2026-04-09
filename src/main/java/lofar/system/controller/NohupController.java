package lofar.system.controller;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
 
import lofar.system.service.NohupBroadcastService;
import lofar.system.service.NohupFileService;
 
/**
 * NohupController
 *
 * REST endpoints (for convenience / debugging):
 *   GET  /api/nohup/content        — return full nohup.out as plain text
 *   POST /api/nohup/reset          — reset read pointer (re-read from start)
 *
 * WebSocket endpoint (STOMP):
 *   /app/nohup/full                — triggers a full-file broadcast to /topic/nohup-log
 *
 * The ongoing tail is handled automatically by NohupBroadcastService (every 5 s).
 */
@Controller
public class NohupController {
 
    @Autowired private NohupFileService      nohupFileService;
    @Autowired private NohupBroadcastService nohupBroadcastService;
 
    // ---------------------------------------------------------------
    //  REST endpoints
    // ---------------------------------------------------------------
 
    @RestController
    @RequestMapping("/api/nohup")
    @CrossOrigin(origins = "http://localhost:4200")
    class NohupRestController {
 
        @GetMapping("/content")
        public ResponseEntity<String> getFullContent() {
            return ResponseEntity.ok()
                .header("Content-Type", "text/plain; charset=UTF-8")
                .body(nohupFileService.getFullContent());
        }
 
        @PostMapping("/reset")
        public ResponseEntity<String> resetPosition() {
            nohupFileService.resetPosition();
            return ResponseEntity.ok("Read pointer reset to 0");
        }
    }
 
    // ---------------------------------------------------------------
    //  WebSocket endpoint
    //  Client sends:   stompClient.publish({ destination: '/app/nohup/full' })
    //  Server replies to /topic/nohup-log with the full file content
    // ---------------------------------------------------------------
 
    @MessageMapping("/nohup/full")
    @SendTo("/topic/nohup-log")
    public String sendFullLog() {
        return nohupFileService.getFullContent();
    }
}