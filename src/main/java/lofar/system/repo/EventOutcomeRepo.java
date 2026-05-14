package lofar.system.repo;
 
import java.util.Optional;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import lofar.system.model.EventOutcome;
 
/**
 * EventOutcomeRepo
 *
 * Spring Data JPA repository for EventOutcome entities.
 */
@Repository
public interface EventOutcomeRepo extends JpaRepository<EventOutcome, Long> {
 
    /** Find the outcome for a specific post */
    Optional<EventOutcome> findByPostId(Long postId);
 
    /** Check whether an outcome already exists for a post */
    boolean existsByPostId(Long postId);
}
 