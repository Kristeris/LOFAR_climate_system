package lofar.system.model;
 
import java.time.LocalDateTime;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
/**
 * ForumCommentDTO
 *
 * Lightweight DTO sent to the Angular frontend when listing or creating comments.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumCommentDTO {
    private Long          id;
    private Long          postId;
    private String        authorUsername;
    private String        content;
    private LocalDateTime createdAt;
}