package lofar.system.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String timeUtc;
    private String authorUsername;
    private String googleCalendarEventId;

    // ── New fields ──────────────────────────────────────────────────
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
     * Temporal status of the post relative to "now":
     *   "PAST"    — observation has already ended
     *   "CURRENT" — observation is happening right now
     *   "FUTURE"  — observation has not started yet
     */
    private String status;
}