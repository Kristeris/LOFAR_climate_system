package lofar.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lofar.system.model.ClimateSensorData;


//ScheduledSensorUpdateService - Runs the Python script every 10 minutes automatically

@Service
public class ScheduledSensorUpdateService {

    private static final Logger logger =
            LoggerFactory.getLogger(ScheduledSensorUpdateService.class);

    @Autowired
    private SensorScriptExecutionService scriptService;

    @Autowired
    private SensorDataParserService parserService;

    @Autowired
    private WebSocketNotificationService webSocketService;

    @Scheduled(fixedRate = 600000)
    public void updateSensorDataAutomatically() {
        logger.info("Scheduled sensor update triggered");

        try {
            String rawOutput = scriptService.runPythonScript();

            ClimateSensorData savedData =
                    parserService.parseAndSave(rawOutput);

            webSocketService.notifyClients(savedData);

            logger.info("Sensor update successful");

        } catch (Exception e) {
            logger.error("Scheduled update failed", e);
        }
    }

    @Scheduled(initialDelay = 5000, fixedRate = Long.MAX_VALUE)
    public void initialSensorUpdate() {
        updateSensorDataAutomatically();
    }
}