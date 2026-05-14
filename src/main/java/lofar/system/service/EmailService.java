package lofar.system.service;
 
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
 
/**
 * EmailService
 *
 * Sends:
 *   1. Temperature warning when sensor reading exceeds 30 °C
 *   2. Forum-post confirmation after a Google Calendar event is created
 *   3. Time-changed notification when admin reschedules a post
 *   4. Welcome e-mail on registration
 */
@Service
public class EmailService {
 
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter DISPLAY_FMT =
        DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm");
 
    @Autowired
    private JavaMailSender mailSender;
 
    @Value("${spring.mail.from:noreply@lofar-system.local}")
    private String fromAddress;
 
    // ---------------------------------------------------------------
    //  Temperature warning
    // ---------------------------------------------------------------
 
    public void sendTemperatureWarning(String toEmail, String username, double temperature) {
        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("Temperature warning skipped — no e-mail for user '{}'", username);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject(" LOFAR Temperature Alert — High Reading Detected");
            msg.setText(
                "Hello " + username + ",\n\n" +
                "A temperature reading of " + temperature + " °C has been recorded, " +
                "which exceeds the warning threshold of 30 °C.\n\n" +
                "Please check the LOFAR Climate Monitoring System for details:\n" +
                "http://localhost:4200/sensors\n\n" +
                "— LOFAR Climate System"
            );
            mailSender.send(msg);
            logger.info("Temperature warning sent to {} for reading {}°C", toEmail, temperature);
        } catch (Exception e) {
            logger.error("Failed to send temperature warning to {}: {}", toEmail, e.getMessage(), e);
        }
    }
 
    // ---------------------------------------------------------------
    //  Forum-post / Calendar confirmation
    // ---------------------------------------------------------------
 
    public void sendForumPostConfirmation(
            String toEmail,
            String username,
            String postTitle,
            String calendarEventId) {
 
        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("Forum confirmation skipped — no e-mail for user '{}'", username);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject("📡 LOFAR Forum Post Created — " + postTitle);
            msg.setText(
                "Hello " + username + ",\n\n" +
                "Your forum post \"" + postTitle + "\" has been published successfully " +
                "and a Google Calendar event has been created for it.\n\n" +
                "Calendar Event ID: " + calendarEventId + "\n\n" +
                "You can view your post at:\n" +
                "http://localhost:4200/forum\n\n" +
                "— LOFAR Climate System"
            );
            mailSender.send(msg);
            logger.info("Forum confirmation sent to {} for post '{}'", toEmail, postTitle);
        } catch (Exception e) {
            logger.error("Failed to send forum confirmation to {}: {}", toEmail, e.getMessage(), e);
        }
    }
 
    // ---------------------------------------------------------------
    //  NEW: Admin changed the scheduled time of a post
    // ---------------------------------------------------------------
 
    /**
     * Notifies the post author that an admin has changed the scheduled
     * observation time for their post.
     *
     * @param toEmail         author's e-mail
     * @param username        author's display name
     * @param postTitle       post title
     * @param newStart        new observation start time
     * @param durationSeconds new observation duration in seconds
     */
    public void sendTimeChangedNotification(
            String toEmail,
            String username,
            String postTitle,
            LocalDateTime newStart,
            int durationSeconds) {
 
        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("Time-change notification skipped — no e-mail for user '{}'", username);
            return;
        }
        try {
            LocalDateTime newEnd = newStart.plusSeconds(durationSeconds);
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject(" LOFAR Observation Rescheduled — " + postTitle);
            msg.setText(
                "Hello " + username + ",\n\n" +
                "An administrator has updated the scheduled time for your observation post " +
                "\"" + postTitle + "\".\n\n" +
                "New start time : " + newStart.format(DISPLAY_FMT) + "\n" +
                "New end time   : " + newEnd.format(DISPLAY_FMT) + "\n" +
                "Duration       : " + durationSeconds + " seconds\n\n" +
                "The Google Calendar event has been updated accordingly.\n\n" +
                "You can view your post at:\n" +
                "http://localhost:4200/forum\n\n" +
                "— LOFAR Climate System"
            );
            mailSender.send(msg);
            logger.info("Time-change notification sent to {} for post '{}'", toEmail, postTitle);
        } catch (Exception e) {
            logger.error("Failed to send time-change notification to {}: {}", toEmail, e.getMessage(), e);
        }
    }


    /**
     * Sent to the user whose post creation was rejected because their requested
     * time window overlaps with an already-booked observation.
     */
    public void sendConflictRejectionEmail(
            String toEmail,
            String username,
            String requestedTitle,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            LocalDateTime conflictStart,
            LocalDateTime conflictEnd,
            String conflictOwner) {
 
        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("Conflict rejection e-mail skipped — no e-mail for '{}'", username);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject("LOFAR Observation Rejected — Time Slot Conflict");
            msg.setText(
                "Hello " + username + ",\n\n" +
                "Unfortunately your observation request could not be accepted\n" +
                "because the requested time slot is already taken by another user.\n\n" +
                "Your request\n" +
                "  Title : " + (requestedTitle != null ? requestedTitle : "—") + "\n" +
                "  Start : " + requestedStart.format(DISPLAY_FMT) + "\n" +
                "  End   : " + requestedEnd.format(DISPLAY_FMT) + "\n\n" +
                "Conflicting observation\n" +
                "  Booked by : " + conflictOwner + "\n" +
                "  Start     : " + conflictStart.format(DISPLAY_FMT) + "\n" +
                "  End       : " + conflictEnd.format(DISPLAY_FMT) + "\n\n" +
                "Please choose a different time slot and try again:\n" +
                "http://localhost:4200/forum\n\n" +
                "— LOFAR Climate System"
            );
            mailSender.send(msg);
            logger.info("Conflict rejection e-mail sent to {} for '{}'", toEmail, requestedTitle);
        } catch (Exception e) {
            logger.error("Failed to send conflict rejection e-mail to {}: {}", toEmail, e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    //  Post deleted by admin
    // ---------------------------------------------------------------

    /**
     * Notifies a post author that an admin has deleted their forum post.
     */
    public void sendPostDeletedNotification(
            String toEmail,
            String username,
            String postTitle,
            LocalDateTime scheduledStart,
            String adminReason) {

        if (toEmail == null || toEmail.isBlank()) {
            logger.warn("Post-deleted notification skipped — no e-mail for user '{}'", username);
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject("LOFAR Observation Deleted — " + postTitle);

            StringBuilder body = new StringBuilder();
            body.append("Hello ").append(username).append(",\n\n");
            body.append("An administrator has deleted your observation post:\n\n");
            body.append("  Title : ").append(postTitle).append("\n");

            if (scheduledStart != null) {
                body.append("  Scheduled start : ")
                    .append(scheduledStart.format(DISPLAY_FMT)).append("\n");
            }

            if (adminReason != null && !adminReason.isBlank()) {
                body.append("\nAdmin comment:\n  \"").append(adminReason.trim()).append("\"\n");
            }

            body.append("\nIf you have questions, please contact your administrator.\n\n");
            body.append("You can schedule a new observation at:\n");
            body.append("http://localhost:4200/forum\n\n");
            body.append("— LOFAR Climate System");

            msg.setText(body.toString());
            mailSender.send(msg);
            logger.info("Post-deleted notification sent to {} for post '{}'", toEmail, postTitle);
        } catch (Exception e) {
            logger.error("Failed to send post-deleted notification to {}: {}",
                toEmail, e.getMessage(), e);
        }
    }
    // ---------------------------------------------------------------
    //  Welcome / Registration
    // ---------------------------------------------------------------
 
    public void sendWelcomeEmail(String toEmail, String username) {
        if (toEmail == null || toEmail.isBlank()) return;
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject("Welcome to LOFAR Climate System");
            msg.setText(
                "Hello " + username + ",\n\n" +
                "Your account has been created successfully.\n\n" +
                "You can now log in at http://localhost:4200/login\n\n" +
                "— LOFAR Climate System"
            );
            mailSender.send(msg);
            logger.info("Welcome e-mail sent to {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send welcome e-mail to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}