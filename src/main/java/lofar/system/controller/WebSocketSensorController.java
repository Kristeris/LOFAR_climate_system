package lofar.system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lofar.system.model.ClimateSensorData;
import lofar.system.service.ClimateSensorDataService;

//WebSocketSensorController - Manages WebSocket endpoints

@Controller
public class WebSocketSensorController {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private ClimateSensorDataService sensorService;

    /**
     * Handles client requests for the latest sensor data
     */
    @MessageMapping("/sensor/request")
    @SendTo("/topic/sensor-data")
    public ClimateSensorData requestLatestData() {
        List<ClimateSensorData> latest = sensorService.getLatest10();
        return latest.isEmpty() ? null : latest.get(0);
    }

    /**
     * Allows server to push data to all connected clients
     */
    public void sendSensorUpdate(ClimateSensorData data) {
        messagingTemplate.convertAndSend("/topic/sensor-data", data);
    }

    /**
     * Send all historical data
     */
    @MessageMapping("/sensor/history")
    @SendTo("/topic/sensor-history")
    public List<ClimateSensorData> requestHistoricalData() {
        return sensorService.getAll();
    }
}

/**
 * REST endpoint to get WebSocket connection status
 */
@RestController
@RequestMapping("/api/websocket")
class WebSocketStatusController {
    
    @GetMapping("/status")
    public String getWebSocketStatus() {
        return "WebSocket endpoint available at /ws-sensor";
    }
}