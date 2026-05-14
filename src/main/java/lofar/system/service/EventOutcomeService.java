package lofar.system.service;
 
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
 
import lofar.system.model.EventOutcome;
import lofar.system.model.EventOutcome.Status;
import lofar.system.model.EventOutcomeDTO;
import lofar.system.model.ForumPost;
import lofar.system.repo.EventOutcomeRepo;
import lofar.system.repo.ForumPostRepo;
 
/**
 * EventOutcomeService
 *
 * Manages the pass/fail status of LOFAR observation sessions.
 *
 * An outcome can be set:
 *  1. Manually — admin sends POST /api/forum/{id}/outcome
 *  2. Automatically — every 2 minutes this service checks whether any
 *     past event still has no outcome and tries to derive one from
 *     the nohup.out log snapshot captured by NohupFileService.
 *
 * Failure criteria (derived from real nohup.out patterns):
 *  - dropped-by-kernel count > 0
 *  - missed-packet percentage > 35 %
 *
 * Success: observation completed with ≤ 35 % missed and 0 kernel drops.
 */
@Service
public class EventOutcomeService {
 
    private static final Logger logger = LoggerFactory.getLogger(EventOutcomeService.class);
 
    /** Threshold above which a session is considered failed */
    private static final double MISSED_FAILURE_THRESHOLD = 35.0;
 
    @Autowired private EventOutcomeRepo eventOutcomeRepo;
    @Autowired private ForumPostRepo    forumPostRepo;
    @Autowired private NohupFileService nohupFileService;
 
    // ---------------------------------------------------------------
    //  Manual set — called from the REST controller
    // ---------------------------------------------------------------
 
    /**
     * Manually records (or overwrites) the outcome for a post.
     *
     * @param postId   target post ID
     * @param status   "SUCCESS", "FAILURE", or "UNKNOWN"
     * @param summary  optional human-readable summary
     * @param logSnap  optional nohup.out snippet
     */
    public EventOutcomeDTO setOutcome(Long postId, String status, String summary, String logSnap) {
        ForumPost post = forumPostRepo.findById(postId)
            .orElseThrow(() -> new IllegalArgumentException("Post not found: " + postId));
 
        Status s;
        try {
            s = Status.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + status
                + " — must be SUCCESS, FAILURE, or UNKNOWN");
        }
 
        EventOutcome outcome = eventOutcomeRepo.findByPostId(postId)
            .orElseGet(() -> new EventOutcome());
 
        outcome.setPost(post);
        outcome.setStatus(s);
        outcome.setSummary(summary);
        outcome.setLogSnapshot(logSnap);
        outcome.setRecordedAt(LocalDateTime.now());
 
        // Try to parse stats from the log snapshot if provided
        if (logSnap != null && !logSnap.isBlank()) {
            parseAndApplyStats(outcome, logSnap);
        }
 
        EventOutcome saved = eventOutcomeRepo.save(outcome);
        logger.info("Outcome set for post {}: {}", postId, s);
        return toDTO(saved);
    }
 
    // ---------------------------------------------------------------
    //  Read
    // ---------------------------------------------------------------
 
    public Optional<EventOutcomeDTO> getOutcome(Long postId) {
        return eventOutcomeRepo.findByPostId(postId).map(this::toDTO);
    }
 
    // ---------------------------------------------------------------
    //  Automatic derivation — runs every 2 minutes
    // ---------------------------------------------------------------
 
    /**
     * Checks all past posts that have no outcome yet and tries to assign
     * SUCCESS/FAILURE by scanning the current nohup.out content.
     */
    @Scheduled(fixedDelay = 120_000) // every 2 minutes
    public void autoDerivePastOutcomes() {
        LocalDateTime now = LocalDateTime.now();
 
        forumPostRepo.findAllByOrderByCreatedAtDesc().forEach(post -> {
            // Only process past events
            if (post.effectiveEndTime().isAfter(now)) return;
            // Skip if already has an outcome
            if (eventOutcomeRepo.existsByPostId(post.getId())) return;
 
            tryDeriveOutcome(post);
        });
    }
 
    // ---------------------------------------------------------------
    //  Internal derivation logic
    // ---------------------------------------------------------------
 
    private void tryDeriveOutcome(ForumPost post) {
        String logContent = nohupFileService.getFullContent();
        if (logContent == null || logContent.isBlank()) return;
 
        // Find the last "total per socket" summary block in the log
        // which represents the most recent completed observation session.
        String lastSummaryBlock = extractLastSummaryBlock(logContent);
        if (lastSummaryBlock == null) {
            logger.debug("No summary block found in nohup.out for post {}", post.getId());
            return;
        }
 
        EventOutcome outcome = new EventOutcome();
        outcome.setPost(post);
        outcome.setLogSnapshot(lastSummaryBlock);
        outcome.setRecordedAt(LocalDateTime.now());
 
        parseAndApplyStats(outcome, lastSummaryBlock);
 
        // Determine status
        boolean kernelDropped = outcome.getDroppedByKernel() != null
                                && outcome.getDroppedByKernel() > 0;
        boolean highMiss      = outcome.getMissedPercent() != null
                                && outcome.getMissedPercent() > MISSED_FAILURE_THRESHOLD;
 
        if (kernelDropped || highMiss) {
            outcome.setStatus(Status.FAILURE);
            outcome.setSummary(String.format(
                "Observation ended with issues: %.2f%% missed, %d dropped by kernel",
                outcome.getMissedPercent() != null ? outcome.getMissedPercent() : 0.0,
                outcome.getDroppedByKernel() != null ? outcome.getDroppedByKernel() : 0L));
        } else {
            outcome.setStatus(Status.SUCCESS);
            outcome.setSummary(String.format(
                "Observation completed: %.2f GB recorded, %.2f%% missed",
                outcome.getTotalGb()      != null ? outcome.getTotalGb()      : 0.0,
                outcome.getMissedPercent() != null ? outcome.getMissedPercent() : 0.0));
        }
 
        eventOutcomeRepo.save(outcome);
        logger.info("Auto-derived outcome for post {}: {} — {}",
            post.getId(), outcome.getStatus(), outcome.getSummary());
    }
 
    // ---------------------------------------------------------------
    //  Log parsing helpers
    // ---------------------------------------------------------------
 
    /**
     * Extracts the last "total per socket" summary block from the nohup.out content.
     * This block appears at the end of each dump_udp_ow_17 session.
     */
    private String extractLastSummaryBlock(String logContent) {
        // Find the last occurrence of the summary marker
        int lastIdx = logContent.lastIndexOf("total per socket:");
        if (lastIdx < 0) return null;
 
        // Take a reasonable window (500 chars) following the marker
        int end = Math.min(lastIdx + 600, logContent.length());
        return logContent.substring(lastIdx, end);
    }
 
    /**
     * Parses numeric statistics from a nohup.out text block and
     * populates the EventOutcome entity fields.
     */
    private void parseAndApplyStats(EventOutcome outcome, String text) {
        // Total GB — e.g. "volume     59.540 GB"
        Matcher gbMatcher = Pattern.compile("volume\\s+([0-9.]+)\\s+GB", Pattern.CASE_INSENSITIVE)
            .matcher(text);
        if (gbMatcher.find()) {
            outcome.setTotalGb(Double.parseDouble(gbMatcher.group(1)));
        }
 
        // Missed % — e.g. "missed packets   2478663    23.274524 % of exp"
        Matcher missMatcher = Pattern.compile("missed packets\\s+\\d+\\s+([0-9.]+)\\s+%")
            .matcher(text);
        if (missMatcher.find()) {
            outcome.setMissedPercent(Double.parseDouble(missMatcher.group(1)));
        }
 
        // Dropped by kernel — e.g. "dropped by kernel       636"
        Matcher kernelMatcher = Pattern.compile("dropped by kernel\\s+(\\d+)")
            .matcher(text);
        if (kernelMatcher.find()) {
            outcome.setDroppedByKernel(Long.parseLong(kernelMatcher.group(1)));
        }
    }
 
    // ---------------------------------------------------------------
    //  Mapper
    // ---------------------------------------------------------------
 
    private EventOutcomeDTO toDTO(EventOutcome o) {
        return new EventOutcomeDTO(
            o.getId(),
            o.getPost().getId(),
            o.getStatus().name(),
            o.getSummary(),
            o.getLogSnapshot(),
            o.getTotalGb(),
            o.getMissedPercent(),
            o.getDroppedByKernel(),
            o.getRecordedAt()
        );
    }
}