package lofar.system.model;
 
import java.time.LocalDateTime;
import java.time.ZoneOffset;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
 
@Entity
@Table(name = "forum_post")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ForumPost {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;
 
    @NotBlank
    @Size(max = 10000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
 
    /** Local server time — kept for backwards compatibility */
    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
 
    /**
     * Creation time stored as UTC.
     * Example value: "2026-04-09T14:35:00Z"
     */
    @Column(name = "time_utc", nullable = false)
    private String timeUtc;
 
    /**
     * The scheduled observation start time chosen by the user.
     * Used for Google Calendar event creation and conflict detection.
     * NULL means "use createdAt".
     */
    @Column(name = "scheduled_date_time")
    private LocalDateTime scheduledDateTime;
 
    /**
     * Observation duration in seconds (max 3600 = 1 hour).
     * Used to set the Google Calendar event end time.
     */
    @Column(name = "duration_seconds")
    private Integer durationSeconds;
 
    /**
     * ANADIR direction X component (RA in radians for coordinate systems,
     * or 0 for planet/body systems).
     */
    @Column(name = "anadir_x")
    private Double anadirX;
 
    /**
     * ANADIR direction Y component (Dec in radians for coordinate systems,
     * or 0 for planet/body systems).
     */
    @Column(name = "anadir_y")
    private Double anadirY;
 
    /**
     * ANADIR coordinate system (e.g. J2000, JUPITER, SUN …)
     */
    @Column(name = "anadir_system", length = 20)
    private String anadirSystem;
 
    /** Google Calendar event ID — populated after successful calendar creation */
    @Column(name = "google_calendar_event_id")
    private String googleCalendarEventId;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_uid", nullable = false)
    private MyUser author;
 
    // ---------------------------------------------------------------
    //  Constructors
    // ---------------------------------------------------------------
 
    public ForumPost(String title, String content, MyUser author) {
        this.title   = title;
        this.content = content;
        this.author  = author;
        this.createdAt = LocalDateTime.now();
        this.timeUtc   = buildUtcString(this.createdAt);
    }
 
    /**
     * JPA lifecycle callback — ensures timeUtc is always populated
     * even if the two-arg / three-arg constructor is not used.
     */
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.timeUtc == null || this.timeUtc.isBlank()) {
            this.timeUtc = buildUtcString(this.createdAt);
        }
    }
 
    // ---------------------------------------------------------------
    //  Computed helpers
    // ---------------------------------------------------------------
 
    /**
     * Returns the effective start time for calendar/conflict purposes.
     * Prefers scheduledDateTime; falls back to createdAt.
     */
    public LocalDateTime effectiveStartTime() {
        return scheduledDateTime != null ? scheduledDateTime : createdAt;
    }
 
    /**
     * Returns the effective end time = start + durationSeconds (or +1 hour default).
     */
    public LocalDateTime effectiveEndTime() {
        int secs = (durationSeconds != null && durationSeconds > 0 && durationSeconds <= 3600)
                   ? durationSeconds : 3600;
        return effectiveStartTime().plusSeconds(secs);
    }
 
    // ---------------------------------------------------------------
    //  Helper
    // ---------------------------------------------------------------
 
    private static String buildUtcString(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.UTC).toString();
    }
}