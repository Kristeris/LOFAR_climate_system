package lofar.system.service;
 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
 
import lofar.system.model.MyUser;
import lofar.system.model.UserObservationStats;
import lofar.system.repo.UserObservationStatsRepo;
 
/**
 * UserStatsService
 *
 * Handles tracking of per-user observation hours on a monthly basis.
 *
 * Called by ForumService whenever a new post is created so that
 * the duration is credited to the author's monthly total.
 *
 * The admin panel calls the read methods to display usage statistics.
 */
@Service
public class UserStatsService {
 
    private static final Logger logger = LoggerFactory.getLogger(UserStatsService.class);
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
 
    @Autowired
    private UserObservationStatsRepo statsRepo;
 
    // ---------------------------------------------------------------
    //  Write (called when a post is created)
    // ---------------------------------------------------------------
 
    /**
     * Adds the given number of seconds to the author's stats for the
     * current calendar month.
     *
     * @param author          the post author
     * @param durationSeconds observation duration in seconds (1–3600)
     */
    public void recordObservation(MyUser author, int durationSeconds) {
        String month = LocalDateTime.now().format(MONTH_FMT);
        UserObservationStats stats = statsRepo
            .findByUserUsernameAndYearMonth(author.getUsername(), month)
            .orElseGet(() -> new UserObservationStats(author, month));
 
        stats.addSeconds(durationSeconds);
        stats.incrementEventCount();
        statsRepo.save(stats);
 
        logger.info("Recorded {}s observation for '{}' in {} (total: {}s)",
            durationSeconds, author.getUsername(), month, stats.getTotalSeconds());
    }
 
    // ---------------------------------------------------------------
    //  Read (admin panel)
    // ---------------------------------------------------------------
 
    /**
     * Returns a summary DTO for every month the given user has posted in.
     */
    public List<UserMonthlyStats> getStatsForUser(String username) {
        return statsRepo.findByUserUsernameOrderByYearMonthDesc(username)
            .stream()
            .map(s -> new UserMonthlyStats(
                s.getUser().getUsername(),
                s.getYearMonth(),
                s.getTotalSeconds(),
                s.totalHours(),
                s.getEventCount()))
            .collect(Collectors.toList());
    }

    /**
     * Returns stats for ALL users for the given month (e.g. "2026-05").
     */
    public List<UserMonthlyStats> getStatsForMonth(String yearMonth) {
        return statsRepo.findByYearMonthOrderByTotalSecondsDesc(yearMonth)
            .stream()
            .map(s -> new UserMonthlyStats(
                s.getUser().getUsername(),
                s.getYearMonth(),
                s.getTotalSeconds(),
                s.totalHours(),
                s.getEventCount()))
            .collect(Collectors.toList());
    }
 
    // ---------------------------------------------------------------
    //  Lightweight DTO (no Lombok so no extra file needed)
    // ---------------------------------------------------------------
 
    public static class UserMonthlyStats {
        public final String username;
        public final String yearMonth;
        public final long   totalSeconds;
        public final double totalHours;
        public final int    eventCount;

        public UserMonthlyStats(String username, String yearMonth,
                                long totalSeconds, double totalHours,
                                int eventCount) {
            this.username     = username;
            this.yearMonth    = yearMonth;
            this.totalSeconds = totalSeconds;
            this.totalHours   = totalHours;
            this.eventCount   = eventCount;
        }
    }
}