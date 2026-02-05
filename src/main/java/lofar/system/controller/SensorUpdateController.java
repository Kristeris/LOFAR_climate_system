package lofar.system.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import lofar.system.model.ClimateSensorData;
import lofar.system.model.SensorOutputDTO;
import lofar.system.service.SensorDataParserService;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@RestController
@RequestMapping("/sensors")
//@CrossOrigin(origins = "*")
public class SensorUpdateController {

    @Autowired
    private SensorDataParserService parserService;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Endpoint to manually trigger the script and update database
    @GetMapping("/update")
    public ResponseEntity<?> updateSensorData() {
        try {
            // For Windows, use .bat script
            String scriptPath = "./src/main/java/scripts/RunSensorOutput.bat";
            ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", scriptPath);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Read the output
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream())
            );
            
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
            
            int exitCode = process.waitFor();
            
            if (exitCode == 0) {
                // Parse and save the output
                ClimateSensorData savedData = parserService.parseAndSave(output.toString());
                
                // Send data via WebSocket
                messagingTemplate.convertAndSend("/topic/sensor-data", savedData);
                
                return ResponseEntity.ok(savedData);
            } else {
                return ResponseEntity.status(500)
                    .body("Script execution failed with exit code: " + exitCode);
            }
            
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Error executing script: " + e.getMessage());
        }
    }

    // Alternative endpoint to receive parsed data directly from external source
    @PostMapping("/receive")
    public ResponseEntity<?> receiveSensorData(@RequestBody SensorOutputDTO sensorOutput) {
        try {
            ClimateSensorData savedData = parserService.parseAndSave(sensorOutput.getRawOutput());
            
            // Send data via WebSocket to all connected clients
            messagingTemplate.convertAndSend("/topic/sensor-data", savedData);
            
            return ResponseEntity.ok(savedData);
        } catch (Exception e) {
            return ResponseEntity.status(400)
                .body("Error parsing sensor data: " + e.getMessage());
        }
    }
}