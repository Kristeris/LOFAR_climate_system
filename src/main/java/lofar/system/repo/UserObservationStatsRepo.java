package lofar.system.repo;
 
import java.util.List;
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import lofar.system.model.UserObservationStats;
 
@Repository
public interface UserObservationStatsRepo extends JpaRepository<UserObservationStats, Long> {
 
    /** Find a specific user's stats for a given month (e.g. "2026-05") */
    Optional<UserObservationStats> findByUserUsernameAndYearMonth(String username, String yearMonth);
 
    /** All stats rows for a given user (all months) */
    List<UserObservationStats> findByUserUsernameOrderByYearMonthDesc(String username);
 
    /** All stats for a given month (used by admin overview) */
    List<UserObservationStats> findByYearMonthOrderByTotalSecondsDesc(String yearMonth);
}