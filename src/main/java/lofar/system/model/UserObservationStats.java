package lofar.system.model;
 
import jakarta.persistence.*;
import lombok.*;
 
/**
 * Tracks how many observation hours a user has accumulated in a given month.
 *
 * Each time a user publishes a LOFAR forum post, 1 hour (or the actual
 * durationSeconds) is added to their total for the current year+month.
 *
 * Admins can view this data in the admin panel.
 */
@Entity
@Table(
    name = "user_observation_stats",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "obs_year_month"})
)
@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserObservationStats {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    /** The user this record belongs to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private MyUser user;
 
    /**
     * Year+month key, e.g. "2026-05".
     * Using a plain String keeps things simple and avoids composite key complexity.
     */
    @Column(name = "obs_year_month", nullable = false, length = 7)
    private String yearMonth;
 
    /** Total observation time in seconds accumulated this month */
    @Column(name = "total_seconds", nullable = false)
    private long totalSeconds = 0L;
 
    public UserObservationStats(MyUser user, String yearMonth) {
        this.user      = user;
        this.yearMonth = yearMonth;
    }
 
    /** Convenience: add duration and return this (for fluent use in service) */
    public UserObservationStats addSeconds(long seconds) {
        this.totalSeconds += seconds;
        return this;
    }
 
    /** Total hours (rounded to 2 dp) */
    public double totalHours() {
        return Math.round((totalSeconds / 3600.0) * 100.0) / 100.0;
    }
}