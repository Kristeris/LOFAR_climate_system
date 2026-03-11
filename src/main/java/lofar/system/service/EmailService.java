package lofar.system.service;

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
 */
@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.from:noreply@lofar-system.local}")
    private String fromAddress;

    // ---------------------------------------------------------------
    //  Temperature warning
    // ---------------------------------------------------------------

    /**
     * Sends a temperature-threshold warning to the given e-mail address.
     *
     * @param toEmail     recipient's e-mail
     * @param username    recipient's display name
     * @param temperature the measured temperature that exceeded the threshold
     */
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

    /**
     * Notifies the user that their forum post has been published and
     * a Google Calendar event has been created.
     *
     * @param toEmail        recipient's e-mail
     * @param username       recipient's display name
     * @param postTitle      title of the forum post
     * @param calendarEventId Google Calendar event ID (for reference)
     */
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
            msg.setSubject(" LOFAR Forum Post Created — " + postTitle);
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
    //  Generic / Registration welcome
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