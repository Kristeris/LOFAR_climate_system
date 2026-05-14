package lofar.system.repo;
 
import java.util.List;
 
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
 
import lofar.system.model.ForumComment;
 
/**
 * ForumCommentRepo
 *
 * Spring Data JPA repository for forum comments.
 */
@Repository
public interface ForumCommentRepo extends JpaRepository<ForumComment, Long> {
 
    /** All comments for a given post, oldest first */
    List<ForumComment> findByPostIdOrderByCreatedAtAsc(Long postId);
 
    /** All comments written by a specific user */
    List<ForumComment> findByAuthorUsernameOrderByCreatedAtDesc(String username);
 
    /** Count of comments on a post */
    long countByPostId(Long postId);
}