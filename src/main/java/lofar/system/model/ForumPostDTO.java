package lofar.system.model;
 
import java.time.LocalDateTime;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
/**
 Added:
 *   - outcomeStatus  — "SUCCESS" / "FAILURE" / "UNKNOWN" / null (no outcome yet)
 *   - commentCount   — number of comments on the post
 
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostDTO {
    private Long          id;
    private String        title;
    private String        content;
    private LocalDateTime createdAt;
    private String        timeUtc;
    private String        authorUsername;
    private String        googleCalendarEventId;
 
    /** Scheduled observation start time (may be null) */
    private LocalDateTime scheduledDateTime;
 
    /** Observation duration in seconds (1–3600) */
    private Integer durationSeconds;
 
    /** ANADIR X (RA, radians) */
    private Double anadirX;
 
    /** ANADIR Y (Dec, radians) */
    private Double anadirY;
 
    /** ANADIR coordinate system */
    private String anadirSystem;
 
    /**
     Temporal status: "PAST" | "CURRENT" | "FUTURE"
     */
    private String status;
 
    // ── NEW ──────────────────────────────────────────────────────────
 
    /**
     Observation outcome for PAST events.
     null    → no outcome recorded yet
     "SUCCESS"
     "FAILURE"
     "UNKNOWN"
    
     Frontend uses this to colour cards: green (SUCCESS), red (FAILURE),
     grey (UNKNOWN/null).
     */
    private String outcomeStatus;
 
    /**
     * Total number of comments on this post.
     * Shown as a badge on the post card so users know discussion exists.
     */
    private int commentCount;
}