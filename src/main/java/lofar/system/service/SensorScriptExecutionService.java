package lofar.system.service;

import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Paths;

@Service
public class SensorScriptExecutionService {
    
    public String runPythonScript() throws Exception {
        // Get the project root directory
        String projectRoot = System.getProperty("user.dir");
        String scriptPath = Paths.get(projectRoot, "src", "main", "java", "python", "SensorOutput.py").toString();
        
        ProcessBuilder pb = new ProcessBuilder("python", scriptPath);
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        StringBuilder errorOutput = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
             BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            System.err.println("Python script error output: " + errorOutput.toString());
            throw new RuntimeException("Python exited with code " + exitCode + ". Error: " + errorOutput.toString());
        }
        
        return output.toString();
    }
}