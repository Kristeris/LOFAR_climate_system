package lofar.system.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lofar.system.model.ClimateSensorData;
import lofar.system.repo.ClimateSensorDataRepo;

@Service
public class SensorDataParserService {

    @Autowired
    private ClimateSensorDataRepo repo;

    public ClimateSensorData parseAndSave(String rawOutput) {
        try {
            // Parse the datetime
            Pattern datePattern = Pattern.compile("^(\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\d{4})");
            Matcher dateMatcher = datePattern.matcher(rawOutput);
            
            LocalDateTime dateTime = null;
            if (dateMatcher.find()) {
                String dateStr = dateMatcher.group(1);
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
                dateTime = LocalDateTime.parse(dateStr, formatter);
            }

            // Parse temperature
            Pattern tempPattern = Pattern.compile("temperature = ([\\d.]+)");
            Matcher tempMatcher = tempPattern.matcher(rawOutput);
            double temperature = 0.0;
            if (tempMatcher.find()) {
                temperature = Double.parseDouble(tempMatcher.group(1));
            }

            // Parse humidity
            Pattern humidityPattern = Pattern.compile("humidity = ([\\d.]+)");
            Matcher humidityMatcher = humidityPattern.matcher(rawOutput);
            double humidity = 0.0;
            if (humidityMatcher.find()) {
                humidity = Double.parseDouble(humidityMatcher.group(1));
            }

            // Create and save the sensor data
            ClimateSensorData sensorData = new ClimateSensorData(temperature, humidity, dateTime);
            return repo.save(sensorData);

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse sensor data: " + e.getMessage(), e);
        }
    }
}