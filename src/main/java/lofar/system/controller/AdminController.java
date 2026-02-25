package lofar.system.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lofar.system.model.ClimateSensorData;
import lofar.system.service.DynamicSchedulerService;
import lofar.system.service.SensorDataParserService;
import lofar.system.service.SensorScriptExecutionService;
import lofar.system.service.WebSocketNotificationService;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    // DynamicSchedulerService handles start/stop/interval at runtime
    @Autowired
    private DynamicSchedulerService schedulerService;

    @Autowired
    private SensorScriptExecutionService scriptService;

    @Autowired
    private SensorDataParserService parserService;

    @Autowired
    private WebSocketNotificationService wsService;

    /**
     * GET /api/admin/scheduler/status
     * Returns current scheduler state and interval.
     */
    @GetMapping("/scheduler/status")
    public ResponseEntity<Map<String, Object>> getSchedulerStatus() {
        return ResponseEntity.ok(Map.of(
            "schedulerEnabled", schedulerService.isSchedulerEnabled(),
            "intervalMinutes", schedulerService.getIntervalMinutes(),
            "message", schedulerService.isSchedulerEnabled() ? "Scheduler is running" : "Scheduler is stopped"
        ));
    }

    /**
     * POST /api/admin/scheduler/stop
     * Pauses automatic data collection.
     */
    @PostMapping("/scheduler/stop")
    public ResponseEntity<Map<String, String>> stopScheduler() {
        schedulerService.disableScheduler();
        return ResponseEntity.ok(Map.of(
            "status", "stopped",
            "message", "Scheduler stopped. No new data will be collected automatically."
        ));
    }

    /**
     * POST /api/admin/scheduler/start
     * Resumes automatic data collection.
     */
    @PostMapping("/scheduler/start")
    public ResponseEntity<Map<String, String>> startScheduler() {
        schedulerService.enableScheduler();
        return ResponseEntity.ok(Map.of(
            "status", "started",
            "message", "Scheduler started. Data will be collected every " + schedulerService.getIntervalMinutes() + " minute(s)."
        ));
    }

    /**
     * POST /api/admin/scheduler/interval
     * Changes how often the sensor script runs.
     * Body: { "minutes": 5 }
     */
    @PostMapping("/scheduler/interval")
    public ResponseEntity<Map<String, Object>> setInterval(@RequestBody Map<String, Long> body) {
        long minutes = body.getOrDefault("minutes", 10L);
        if (minutes < 1 || minutes > 1440) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Interval must be between 1 and 1440 minutes."
            ));
        }
        schedulerService.setInterval(minutes);
        return ResponseEntity.ok(Map.of(
            "status", "updated",
            "intervalMinutes", minutes,
            "message", "Scheduler interval updated to " + minutes + " minute(s)."
        ));
    }

    /**
     * GET /api/admin/sensor/trigger
     * Manually runs the Python script once and broadcasts the result.
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