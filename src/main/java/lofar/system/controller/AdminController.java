package lofar.system.controller;

import lofar.system.service.ScheduledSensorUpdateService;
import lofar.system.service.WebSocketNotificationService;
import lofar.system.model.ClimateSensorData;
import lofar.system.service.SensorDataParserService;
import lofar.system.service.SensorScriptExecutionService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private ScheduledSensorUpdateService schedulerService;

    @Autowired
    private SensorScriptExecutionService scriptService;

    @Autowired
    private SensorDataParserService parserService;

    @Autowired
    private WebSocketNotificationService wsService;

    /**
     * GET /api/admin/scheduler/status
     * Returns the current scheduler enabled/disabled state.
     */
    @GetMapping("/scheduler/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        boolean enabled = schedulerService.isSchedulerEnabled();
        return ResponseEntity.ok(Map.of(
            "schedulerEnabled", enabled,
            "message", enabled ? "Scheduler is running" : "Scheduler is stopped"
        ));
    }

    /**
     * POST /api/admin/scheduler/stop
     * Disables the automatic sensor data scheduler.
     */
    @PostMapping("/scheduler/stop")
    public ResponseEntity<Map<String, String>> stopScheduler() {
        schedulerService.disableScheduler();
        return ResponseEntity.ok(Map.of(
            "status", "stopped",
            "message", "Scheduler has been stopped. No new data will be collected automatically."
        ));
    }

    /**
     * POST /api/admin/scheduler/start
     * Re-enables the automatic sensor data scheduler.
     */
    @PostMapping("/scheduler/start")
    public ResponseEntity<Map<String, String>> startScheduler() {
        schedulerService.enableScheduler();
        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "Scheduler has been started. Data will be collected every 10 minutes."
        ));
    }

    @PostMapping("/scheduler/interval")
    public ResponseEntity<Map<String, Object>> setInterval(@RequestBody Map<String, Long> body) {
        long minutes = body.getOrDefault("minutes", 10L);
        if (minutes < 1 || minutes > 1440) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Intervālam jābūt 1-1440 minūšu robežās"
            ));
        }
        schedulerService.setInterval(minutes);
            return ResponseEntity.ok(Map.of(
                "status", "updated",
                "intervalMinutes", minutes
            ));
    }

    @GetMapping("/scheduler/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(Map.of(
            "schedulerEnabled", schedulerService.isSchedulerEnabled(),
            "intervalMinutes", schedulerService.getIntervalMinutes()
        ));
    }

    /**
     * GET /api/admin/sensor/trigger
     * Manually triggers the Python sensor script immediately,
     * saves the result, and broadcasts it via WebSocket.
     */
    @GetMapping("/sensor/trigger")
    public ResponseEntity<?> manualTrigger() {
        try {
            String rawOutput = scriptService.runPythonScript();
            ClimateSensorData saved = parserService.parseAndSave(rawOutput);
            wsService.notifyClients(saved);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Sensor data collected and broadcast successfully.",
                "data", saved
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "status", "error",
                "message", "Failed to trigger sensor update: " + e.getMessage()
            ));
        }
    }
}