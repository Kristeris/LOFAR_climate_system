package lofar.system.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import lofar.system.model.ClimateSensorData;
import lofar.system.model.MyUser;
import lofar.system.repo.IMyUserRepo;

/**
 * WebSocketNotificationService
 *
 * Pushes sensor updates to connected WebSocket clients and
 * triggers e-mail temperature warnings when readings exceed 30 °C.
 */
@Service
public class WebSocketNotificationService {

    private static final Logger logger =
            LoggerFactory.getLogger(WebSocketNotificationService.class);

    private static final double TEMPERATURE_THRESHOLD = 30.0;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IMyUserRepo userRepo;

    // ---------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------

    /**
     * Sends the sensor reading to all WebSocket subscribers and, if the
     * temperature exceeds 30 °C, e-mails every registered user who has
     * an e-mail address on file.
     */
    public void notifyClients(ClimateSensorData sensorData) {
        try {
            logger.info("Sending sensor data update to WebSocket clients: {}", sensorData);
            messagingTemplate.convertAndSend("/topic/sensor-data", sensorData);
        } catch (Exception e) {
            logger.error("Error sending WebSocket notification: {}", e.getMessage(), e);
        }

        // Temperature threshold check
        if (sensorData.getTemperature() > TEMPERATURE_THRESHOLD) {
            logger.warn("Temperature {} °C exceeds threshold — sending warning e-mails",
                sensorData.getTemperature());
            sendTemperatureWarnings(sensorData.getTemperature());
        }
    }

    /**
     * Sends a generic text notification to all WebSocket subscribers.
     */
    public void sendNotification(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", message);
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    //  Private helpers
    // ---------------------------------------------------------------

    private void sendTemperatureWarnings(double temperature) {
        try {
            List<MyUser> users = userRepo.findAll();
            for (MyUser user : users) {
                if (user.getEmail() != null && !user.getEmail().isBlank()) {
                    emailService.sendTemperatureWarning(
                        user.getEmail(), user.getUsername(), temperature
                    );
                }
            }
        } catch (Exception e) {
            logger.error("Failed to send temperature warning e-mails: {}", e.getMessage(), e);
        }
    }
}