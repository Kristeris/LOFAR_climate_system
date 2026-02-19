package lofar.system.service;

import java.time.Duration;
import java.util.concurrent.ScheduledFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lofar.system.model.ClimateSensorData;

@Service
public class DynamicSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicSchedulerService.class);

    @Autowired
    private SensorScriptExecutionService scriptService;
    @Autowired
    private SensorDataParserService parserService;
    @Autowired
    private WebSocketNotificationService webSocketService;

    private ThreadPoolTaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledTask;
    private volatile boolean schedulerEnabled = true;
    private volatile long intervalMs = 600_000; // 10 minūtes pēc noklusējuma

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(1);
        taskScheduler.initialize();
        scheduleTask();
    }

    private void scheduleTask() {
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
        scheduledTask = taskScheduler.scheduleAtFixedRate(this::runSensorUpdate, Duration.ofMillis(intervalMs));
    }

    private void runSensorUpdate() {
        if (!schedulerEnabled) {
            logger.info("Scheduler disabled — skipping tick");
            return;
        }
        logger.info("Scheduled sensor update triggered (interval: {}ms)", intervalMs);
        try {
            String rawOutput = scriptService.runPythonScript();
            ClimateSensorData saved = parserService.parseAndSave(rawOutput);
            webSocketService.notifyClients(saved);
        } catch (Exception e) {
            logger.error("Scheduled update failed", e);
        }
    }

    public void setInterval(long minutes) {
        this.intervalMs = minutes * 60_000;
        scheduleTask(); // restartē ar jaunu intervālu
        logger.info("Scheduler interval updated to {} minutes", minutes);
    }

    public void enableScheduler() {
        schedulerEnabled = true;
        logger.info("Scheduler ENABLED");
    }

    public void disableScheduler() {
        schedulerEnabled = false;
        logger.info("Scheduler DISABLED");
    }

    public boolean isSchedulerEnabled() { return schedulerEnabled; }
    public long getIntervalMinutes() { return intervalMs / 60_000; }

    @PreDestroy
    public void shutdown() {
        if (scheduledTask != null) scheduledTask.cancel(true);
        taskScheduler.shutdown();
    }
}