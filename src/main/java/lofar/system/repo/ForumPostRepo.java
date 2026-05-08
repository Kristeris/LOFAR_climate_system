package lofar.system.repo;
 
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import lofar.system.model.ForumPost;
 
@Repository
public interface ForumPostRepo extends JpaRepository<ForumPost, Long> {
 
    /** All posts, newest first (used by admin) */
    List<ForumPost> findAllByOrderByCreatedAtDesc();
 
    /** Posts by a specific author, newest first */
    List<ForumPost> findByAuthorUsernameOrderByCreatedAtDesc(String username);
 
    
    
    @Query("SELECT p FROM ForumPost p WHERE p.author.username = :username ORDER BY p.createdAt DESC")
    List<ForumPost> findPostsByAuthorUsername(@Param("username") String username);
    
    
    /**
     * Conflict detection: does any existing post's scheduled window overlap
     * with [newStart, newEnd)?
     *
     * Two intervals [a_start, a_end) and [b_start, b_end) overlap when:
     *   a_start < b_end  AND  a_end > b_start
     *
     * We exclude a given post ID so an admin "edit" doesn't conflict with itself.
     */
@Query(value = """
    SELECT COUNT(*) > 0
    FROM forum_post p
    WHERE (:excludeId IS NULL OR p.id <> :excludeId)
      AND COALESCE(p.scheduled_date_time, p.created_at) < :newEnd
      AND DATE_ADD(
            COALESCE(p.scheduled_date_time, p.created_at),
            INTERVAL COALESCE(p.duration_seconds, 3600) SECOND
          ) > :newStart
    """, nativeQuery = true)
boolean existsOverlapping(
    @Param("excludeId") Long excludeId,
    @Param("newStart") LocalDateTime newStart,
    @Param("newEnd") LocalDateTime newEnd
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