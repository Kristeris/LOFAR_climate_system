package lofar.system.service;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
 
/**
 * NohupBroadcastService
 *
 * Every 5 seconds this service checks whether nohup.out has grown.
 * If new content is found it is pushed to all WebSocket subscribers
 * on the topic  /topic/nohup-log.
 *
 * The frontend can also request a full reload via STOMP:
 *   destination: /app/nohup/full
 * which calls NohupWebSocketController.sendFullLog().
 */
@Service
public class NohupBroadcastService {
 
    private static final Logger logger = LoggerFactory.getLogger(NohupBroadcastService.class);
 
    /** WebSocket topic that the Angular frontend subscribes to */
    public static final String TOPIC_NOHUP = "/topic/nohup-log";
 
    @Autowired private SimpMessagingTemplate messagingTemplate;
    @Autowired private NohupFileService      nohupFileService;
 
    // ---------------------------------------------------------------
    //  Scheduled tail — runs every 5 seconds
    // ---------------------------------------------------------------
 
    @Scheduled(fixedDelay = 5000)
    public void pollAndBroadcast() {
        if (!nohupFileService.fileExists()) {
            return;   // file not present yet — silently skip
        }
 
        String newContent = nohupFileService.getNewContent();
        if (!newContent.isEmpty()) {
            logger.debug("nohup.out grew by {} chars — broadcasting", newContent.length());
            messagingTemplate.convertAndSend(TOPIC_NOHUP, newContent);
        }
    }
 
    // ---------------------------------------------------------------
    //  Manual broadcast of the full file (called by the WS controller)
    // ---------------------------------------------------------------
 
    public void broadcastFullLog() {
        String full = nohupFileService.getFullContent();
        messagingTemplate.convertAndSend(TOPIC_NOHUP, full);
        logger.info("Full nohup.out broadcast ({} chars)", full.length());
    }
}