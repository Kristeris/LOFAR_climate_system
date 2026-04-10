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
 
    /** Google Calendar event ID — populated after successful calendar creation */
    @Column(name = "google_calendar_event_id")
    private String googleCalendarEventId;
 
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_uid", nullable = false)
    private MyUser author;
 
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
    //  Helper
    // ---------------------------------------------------------------
 
    /**
     * Converts a LocalDateTime (treated as UTC) to an ISO-8601 UTC string,
     * e.g. "2026-04-09T14:35:00Z".
     *
     * If your JVM runs in a non-UTC timezone and createdAt stores local time,
     * replace ZoneOffset.UTC with ZoneId.systemDefault() in the conversion.
     */
    private static String buildUtcString(LocalDateTime ldt) {
        return ldt.toInstant(ZoneOffset.UTC).toString();  // "2026-04-09T14:35:00Z"
    }
}
