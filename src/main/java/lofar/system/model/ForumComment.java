package lofar.system.model;
 
import java.time.LocalDateTime;
 
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
 
/**
 * ForumComment
 *
 * A comment left by a user on their own (or any visible) forum post.
 * Users can comment on their own posts; admins can comment on any post.
 *
 * Comments are displayed in the expanded post view on the frontend.
 */
@Entity
@Table(name = "forum_comment")
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ForumComment {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    /** The post this comment belongs to */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id", nullable = false)
    private ForumPost post;
 
    /** Who wrote the comment */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_uid", nullable = false)
    private MyUser author;
 
    /** Comment body — max 2000 characters */
    @NotBlank
    @Size(max = 2000)
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
 
    /** When the comment was posted */
    @Column(nullable = false)
    private LocalDateTime createdAt;
 
    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
 
    public ForumComment(ForumPost post, MyUser author, String content) {
        this.post    = post;
        this.author  = author;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}