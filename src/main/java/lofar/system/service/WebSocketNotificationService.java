package lofar.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lofar.system.model.ClimateSensorData;

//WebSocketNotificationService - Handles sending updates to clients

@Service
public class WebSocketNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketNotificationService.class);

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    
    //Notify all connected WebSocket clients with new sensor data
    
    public void notifyClients(ClimateSensorData sensorData) {
        try {
            logger.info("Sending sensor data update to WebSocket clients: {}", sensorData);
            messagingTemplate.convertAndSend("/topic/sensor-data", sensorData);
        } catch (Exception e) {
            logger.error("Error sending WebSocket notification: {}", e.getMessage(), e);
        }
    }

    
    //Send custom notification message
     
    public void sendNotification(String message) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications", message);
        } catch (Exception e) {
            logger.error("Error sending notification: {}", e.getMessage(), e);
        }
    }
}