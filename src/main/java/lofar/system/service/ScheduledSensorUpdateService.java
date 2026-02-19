package lofar.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lofar.system.model.ClimateSensorData;

/**
 * ScheduledSensorUpdateService
 *
 * Runs the Python sensor script on a fixed interval.
 * The scheduler can be paused/resumed at runtime via the Admin Panel
 * without requiring an application restart.
 */
@Service
public class ScheduledSensorUpdateService {

    private static final Logger logger =
            LoggerFactory.getLogger(ScheduledSensorUpdateService.class);

    /** Runtime flag — true = scheduler will execute on each tick */
    private volatile boolean schedulerEnabled = true;

    @Autowired
    private SensorScriptExecutionService scriptService;

    @Autowired
    private SensorDataParserService parserService;

    @Autowired
    private WebSocketNotificationService webSocketService;

    // ----------------------------------------------------------------
    //  Scheduled task — runs every 10 minutes (100 000 ms placeholder)
    // ----------------------------------------------------------------

    @Scheduled(fixedRate = 600000) // 600 000 ms = 10 minutes
    public void updateSensorDataAutomatically() {
        if (!schedulerEnabled) {
            logger.info("Scheduler is disabled — skipping sensor update tick.");
            return;
        }

        logger.info("Scheduled sensor update triggered");

        try {
            String rawOutput = scriptService.runPythonScript();
            ClimateSensorData savedData = parserService.parseAndSave(rawOutput);
            webSocketService.notifyClients(savedData);
            logger.info("Scheduled sensor update successful: {}", savedData);
        } catch (Exception e) {
            logger.error("Scheduled update failed", e);
        }
    }

    // ----------------------------------------------------------------
    //  Admin controls
    // ----------------------------------------------------------------

    /** Stop automatic data collection. */
    public void disableScheduler() {
        schedulerEnabled = false;
        logger.info("Sensor scheduler DISABLED by admin.");
    }

    /** Resume automatic data collection. */
    public void enableScheduler() {
        schedulerEnabled = true;
        logger.info("Sensor scheduler ENABLED by admin.");
    }

    /** Returns current scheduler state. */
    public boolean isSchedulerEnabled() {
        return schedulerEnabled;
    }
}