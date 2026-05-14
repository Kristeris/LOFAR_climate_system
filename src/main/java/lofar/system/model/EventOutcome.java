package lofar.system.model;
 
import java.time.LocalDateTime;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
 
/**
 * EventOutcome
 *
 * Stores the result of an observation after it has finished.
 * One row per ForumPost (one-to-one relationship).
 *
 * The frontend uses this to:
 *  - Colour past-event cards green (SUCCESS) or red (FAILURE)
 *  - Show static graphs in the past-event detail view
 *  - Offer a "Download logs" button with the captured log snippet
 *
 * Who sets this?
 *  - An admin manually via POST /api/forum/{id}/outcome
 *  - OR automatically by a future integration that reads nohup.out
 *    after the scheduled end time (see EventOutcomeScheduler).
 */
@Entity
@Table(name = "event_outcome")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class EventOutcome {
 
    public enum Status { SUCCESS, FAILURE, UNKNOWN }
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    /** The observation post this outcome belongs to */
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false, unique = true)
    private ForumPost post;
 
    /** Whether the observation succeeded or failed */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.UNKNOWN;
 
    /**
     * Short human-readable summary, e.g.
     *   "Packet loss 22.7 % — threshold exceeded"
     * or
     *   "Observation completed: 59.5 GB recorded, 0 % dropped"
     */
    @Column(columnDefinition = "TEXT")
    private String summary;
 
    /**
     * Snapshot of the relevant nohup.out section captured at the end
     * of the observation window.  Stored as plain text so the frontend
     * can display it and offer it as a download.
     */
    @Column(name = "log_snapshot", columnDefinition = "LONGTEXT")
    private String logSnapshot;
 
    /**
     * Total data volume recorded (GB), parsed from nohup.out.
     * Used for the statistics graphs in the expanded past-event view.
     */
    @Column(name = "total_gb")
    private Double totalGb;
 
    /**
     * Packet miss rate (%), parsed from nohup.out.
     * Used for the graphs.
     */
    @Column(name = "missed_percent")
    private Double missedPercent;
 
    /**
     * Packets dropped by kernel, parsed from nohup.out.
     */
    @Column(name = "dropped_by_kernel")
    private Long droppedByKernel;
 
    /** When the outcome record was created */
    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
 
    @PrePersist
    public void prePersist() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }
 
    public EventOutcome(ForumPost post, Status status, String summary) {
        this.post      = post;
        this.status    = status;
        this.summary   = summary;
        this.recordedAt = LocalDateTime.now();
    }
}