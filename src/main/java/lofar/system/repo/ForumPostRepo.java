package lofar.system.repo;
 
import java.time.LocalDateTime;
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
 
import lofar.system.model.ForumPost;
 
@Repository
public interface ForumPostRepo extends JpaRepository<ForumPost, Long> {
 
    /** All posts, newest first (used by admin) */
    List<ForumPost> findAllByOrderByCreatedAtDesc();
 
    /** Posts by a specific author, newest first */
    List<ForumPost> findByAuthorUsernameOrderByCreatedAtDesc(String username);
 
    /**
     * Conflict detection: does any existing post's scheduled window overlap
     * with [newStart, newEnd)?
     *
     * Two intervals [a_start, a_end) and [b_start, b_end) overlap when:
     *   a_start < b_end  AND  a_end > b_start
     *
     * We exclude a given post ID so an admin "edit" doesn't conflict with itself.
     */
    @Query("""
        SELECT COUNT(p) > 0 FROM ForumPost p
        WHERE (:excludeId IS NULL OR p.id <> :excludeId)
          AND (
                COALESCE(p.scheduledDateTime, p.createdAt) < :newEnd
            AND (
                  COALESCE(p.scheduledDateTime, p.createdAt)
                  + (COALESCE(p.durationSeconds, 3600) / 86400.0)
                ) > :newStart
          )
        """)
    boolean existsConflict(
        @Param("newStart") LocalDateTime newStart,
        @Param("newEnd")   LocalDateTime newEnd,
        @Param("excludeId") Long excludeId
    );
 
    /**
     * Simpler, database-portable conflict check (works with MySQL which
     * doesn't support arithmetic on LocalDateTime in JPQL as above).
     * Fetches overlapping posts so the service can do the check in Java.
     */
    @Query("""
        SELECT p FROM ForumPost p
        WHERE (:excludeId IS NULL OR p.id <> :excludeId)
          AND COALESCE(p.scheduledDateTime, p.createdAt) < :newEnd
          AND COALESCE(p.scheduledDateTime, p.createdAt) >= :rangeStart
        """)
    List<ForumPost> findPostsNear(
        @Param("rangeStart") LocalDateTime rangeStart,
        @Param("newEnd")     LocalDateTime newEnd,
        @Param("excludeId")  Long excludeId
    );
}