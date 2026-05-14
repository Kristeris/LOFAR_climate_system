package lofar.system.model;
 
import java.time.LocalDateTime;
 
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
 
/**
 * EventOutcomeDTO
 *
 * Sent to the Angular frontend for rendering past-event cards and detail views.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventOutcomeDTO {
    private Long          id;
    private Long          postId;
    /** "SUCCESS", "FAILURE", or "UNKNOWN" */
    private String        status;
    private String        summary;
    /** Plain-text nohup.out snapshot for display and download */
    private String        logSnapshot;
    /** Parsed statistics for graphs */
    private Double        totalGb;
    private Double        missedPercent;
    private Long          droppedByKernel;
    private LocalDateTime recordedAt;
}