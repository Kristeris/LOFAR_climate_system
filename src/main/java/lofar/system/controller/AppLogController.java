package lofar.system.controller;
 
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
 
/**
 * AppLogController
 *
 * Serves the application log file (logs/lofar-system.log) to admin users
 * so the admin panel can display a live-updating log view.
 *
 * Endpoints:
 *   GET /api/admin/logs/app           — last N lines of the application log
 *   GET /api/admin/logs/errors        — last N lines of the error log
 *   GET /api/admin/logs/app/download  — download full app log as .txt
 *
 * The admin panel uses polling (every 5 s) to fetch new lines.
 * The endpoint accepts ?lines=200 to limit the response size.
 *
 * Security: restricted to ADMIN via SecurityConfig (/api/admin/** → ADMIN).
 */
@RestController
@RequestMapping("/api/admin/logs")
@CrossOrigin(origins = "http://localhost:4200")
public class AppLogController {
 
    /** Path to the rolling application log — matches logback-spring.xml */
    @Value("${app.log.path:logs/lofar-system.log}")
    private String appLogPath;
 
    /** Path to the error-only log */
    @Value("${app.log.error.path:logs/lofar-system-error.log}")
    private String errorLogPath;
 
    // Default number of lines to return (limits payload size)
    private static final int DEFAULT_LINES = 200;
    private static final int MAX_LINES     = 2000;
 
    // ---------------------------------------------------------------
    //  Application log (DEBUG+)
    // ---------------------------------------------------------------
 
    @GetMapping("/app")
    public ResponseEntity<?> getAppLog(
            @RequestParam(defaultValue = "200") int lines) {
        return readLastLines(appLogPath, lines);
    }
 
    // ---------------------------------------------------------------
    //  Error log (ERROR only)
    // ---------------------------------------------------------------
 
    @GetMapping("/errors")
    public ResponseEntity<?> getErrorLog(
            @RequestParam(defaultValue = "200") int lines) {
        return readLastLines(errorLogPath, lines);
    }
 
    // ---------------------------------------------------------------
    //  Download full app log
    // ---------------------------------------------------------------
 
    @GetMapping("/app/download")
    public ResponseEntity<byte[]> downloadAppLog() {
        Path path = resolvePath(appLogPath);
        if (path == null || !Files.isReadable(path)) {
            return ResponseEntity.notFound().build();
        }
        try {
            byte[] bytes = Files.readAllBytes(path);
            return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"lofar-system.log\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
 
    // ---------------------------------------------------------------
    //  Shared helper — read last N lines
    // ---------------------------------------------------------------
 
    private ResponseEntity<?> readLastLines(String filePath, int requestedLines) {
        int limit = Math.min(Math.max(1, requestedLines), MAX_LINES);
 
        Path path = resolvePath(filePath);
        if (path == null || !Files.isReadable(path)) {
            return ResponseEntity.ok(Map.of(
                "lines", new String[0],
                "message", "Log file not found: " + filePath
            ));
        }
 
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String[] allLines = content.split("\\r?\\n");
 
            // Take the last `limit` lines
            int start = Math.max(0, allLines.length - limit);
            String[] result = new String[allLines.length - start];
            System.arraycopy(allLines, start, result, 0, result.length);
 
            return ResponseEntity.ok(Map.of(
                "lines",     result,
                "totalLines", allLines.length,
                "returned",   result.length,
                "filePath",   path.toString()
            ));
 
        } catch (IOException e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to read log: " + e.getMessage()));
        }
    }
 
    private Path resolvePath(String filePath) {
        try {
            Path p = Paths.get(filePath);
            if (!p.isAbsolute()) {
                p = Paths.get(System.getProperty("user.dir")).resolve(p);
            }
            return p;
        } catch (Exception e) {
            return null;
        }
    }
}