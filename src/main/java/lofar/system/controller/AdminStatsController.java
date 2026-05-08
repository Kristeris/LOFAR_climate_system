package lofar.system.controller;
 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
 
import lofar.system.service.UserStatsService;
import lofar.system.service.UserStatsService.UserMonthlyStats;
 
/**
 * AdminStatsController
 *
 * GET /api/admin/stats/user/{username}   — all monthly stats for one user
 * GET /api/admin/stats/month/{yearMonth} — stats for all users in a month
 * GET /api/admin/stats/month/current     — stats for the current month
 *
 * All endpoints are restricted to ADMIN via SecurityConfig.
 */
@RestController
@RequestMapping("/api/admin/stats")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminStatsController {
 
    @Autowired
    private UserStatsService userStatsService;
 
    private static final DateTimeFormatter MONTH_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM");
 
    // ---------------------------------------------------------------
    //  Per-user stats (all months)
    // ---------------------------------------------------------------
 
    @GetMapping("/user/{username}")
    public ResponseEntity<List<UserMonthlyStats>> getUserStats(
            @PathVariable String username) {
        return ResponseEntity.ok(userStatsService.getStatsForUser(username));
    }
 
    // ---------------------------------------------------------------
    //  All users, specific month
    // ---------------------------------------------------------------
 
    @GetMapping("/month/{yearMonth}")
    public ResponseEntity<List<UserMonthlyStats>> getMonthStats(
            @PathVariable String yearMonth) {
        return ResponseEntity.ok(userStatsService.getStatsForMonth(yearMonth));
    }
 
    // ---------------------------------------------------------------
    //  All users, current month (convenience)
    // ---------------------------------------------------------------
 
    @GetMapping("/month/current")
    public ResponseEntity<List<UserMonthlyStats>> getCurrentMonthStats() {
        String current = LocalDateTime.now().format(MONTH_FMT);
        return ResponseEntity.ok(userStatsService.getStatsForMonth(current));
    }
}