package lofar.system.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForumPostDTO {
    private Long id;
    private String title;
    private String content;
    private LocalDateTime createdAt;
    private String authorUsername;
    private String googleCalendarEventId;
}