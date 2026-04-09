package lofar.system.service;
 
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
/**
 * NohupFileService
 *
 * Reads nohup.out from disk.
 *
 *  - getFullContent()      → returns the entire file as a String (for the
 *                            initial load or an explicit "refresh all" request)
 *  - getNewContent()       → returns only the bytes added since the last call
 *                            (tail-like behaviour for incremental WebSocket pushes)
 */
@Service
public class NohupFileService {
 
    private static final Logger logger = LoggerFactory.getLogger(NohupFileService.class);
 
    /**
     * Path to nohup.out.
     * Override in application.properties:  nohup.file.path=/absolute/path/nohup.out
     * Default: nohup.out in the working directory (project root when running with Gradle).
     */
    @Value("${nohup.file.path:nohup.out}")
    private String nohupFilePath;
 
    /** Byte offset of the last read — used by getNewContent() */
    private volatile long lastReadPosition = 0L;
 
    // ---------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------
 
    /**
     * Returns the entire content of nohup.out as a UTF-8 String.
     * Resets the incremental-read pointer to the end of the file.
     */
    public String getFullContent() {
        Path path = resolvePath();
        if (path == null) return "[nohup.out not found at: " + nohupFilePath + "]";
 
        try {
            byte[] bytes = Files.readAllBytes(path);
            lastReadPosition = bytes.length;   // reset pointer
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            logger.error("Failed to read nohup.out: {}", e.getMessage());
            return "[Error reading nohup.out: " + e.getMessage() + "]";
        }
    }
 
    /**
     * Returns only the content added to nohup.out since the last call.
     * Returns an empty String if nothing new has been written.
     */
    public String getNewContent() {
        Path path = resolvePath();
        if (path == null) return "";
 
        try {
            long fileLength = Files.size(path);
 
            if (fileLength <= lastReadPosition) {
                // File hasn't grown (or was truncated — reset in that case)
                if (fileLength < lastReadPosition) {
                    logger.warn("nohup.out shrank — resetting read pointer");
                    lastReadPosition = 0;
                }
                return "";
            }
 
            long bytesToRead = fileLength - lastReadPosition;
            byte[] buffer = new byte[(int) Math.min(bytesToRead, 10 * 1024 * 1024)]; // max 10 MB per poll
 
            try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
                raf.seek(lastReadPosition);
                int read = raf.read(buffer);
                lastReadPosition += read;
                return new String(buffer, 0, read, StandardCharsets.UTF_8);
            }
 
        } catch (IOException e) {
            logger.error("Failed to tail nohup.out: {}", e.getMessage());
            return "";
        }
    }
 
    /** Returns true if the file exists and is readable. */
    public boolean fileExists() {
        Path path = resolvePath();
        return path != null && Files.isReadable(path);
    }
 
    /** Resets the incremental-read pointer to 0 (re-read from the start). */
    public void resetPosition() {
        lastReadPosition = 0;
    }
 
    // ---------------------------------------------------------------
    //  Helpers
    // ---------------------------------------------------------------
 
    private Path resolvePath() {
        try {
            Path p = Paths.get(nohupFilePath);
            if (!p.isAbsolute()) {
                p = Paths.get(System.getProperty("user.dir")).resolve(p);
            }
            return p;
        } catch (Exception e) {
            logger.error("Invalid nohup file path '{}': {}", nohupFilePath, e.getMessage());
            return null;
        }
    }
}