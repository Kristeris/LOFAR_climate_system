package lofar.system.service;
 
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
 
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
 
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
 
@Service
public class GoogleCalendarService {
 
    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);
 
    /** Maximum allowed duration in seconds (1 hour) */
    private static final int MAX_DURATION_SECONDS = 3600;
 
    @Value("${google.calendar.service-account-path}")
    private String serviceAccountPath;
 
    @Value("${google.calendar.id:primary}")
    private String calendarId;
 
    @Value("${google.calendar.timezone:Europe/Riga}")
    private String timezone;
 
    // ---------------------------------------------------------------
    //  Create event
    // ---------------------------------------------------------------
 
    /**
     * Creates a Google Calendar event.
     *
     * @param title           event summary
     * @param description     event description (truncated to 500 chars)
     * @param startTime       observation start time
     * @param durationSeconds observation duration in seconds (max 3600 = 1 hour)
     * @param createdByUsername username of the person who made the post
     * @return Google Calendar event ID, or a LOCAL-/ERROR- placeholder
     */
    public String createCalendarEvent(
            String title,
            String description,
            LocalDateTime startTime,
            int durationSeconds,
            String createdByUsername) {
 
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            logger.warn("Google Calendar service account not configured — returning placeholder");
            return "LOCAL-" + System.currentTimeMillis();
        }
 
        try {
            Calendar calendarClient = buildCalendarClient();
            if (calendarClient == null) return "LOCAL-" + System.currentTimeMillis();
 
            // Clamp duration to max 3600 s
            int safeDuration = Math.max(1, Math.min(durationSeconds, MAX_DURATION_SECONDS));
 
            ZonedDateTime start = startTime.atZone(ZoneId.of(timezone));
            ZonedDateTime end   = start.plusSeconds(safeDuration);
 
            // Embed who created the event in the description
            String fullDescription = "Created by: " + createdByUsername + "\n\n"
                + (description != null && description.length() > 480
                    ? description.substring(0, 480) + "…"
                    : description);
 
            Event event = new Event()
                .setSummary("📡 LOFAR: " + title)
                .setDescription(fullDescription);
 
            event.setStart(new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                    java.util.Date.from(start.toInstant())))
                .setTimeZone(timezone));
 
            event.setEnd(new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                    java.util.Date.from(end.toInstant())))
                .setTimeZone(timezone));
 
            Event created = calendarClient.events()
                .insert(calendarId, event)
                .execute();
 
            logger.info("Google Calendar event created: '{}' by {} — id: {}, duration: {}s",
                title, createdByUsername, created.getId(), safeDuration);
            return created.getId();
 
        } catch (Exception e) {
            logger.error("Failed to create Google Calendar event: {}", e.getMessage(), e);
            return "ERROR-" + System.currentTimeMillis();
        }
    }
 
    /**
     * Backwards-compatible overload (no duration / username) — defaults to 1 hour.
     */
    public String createCalendarEvent(String title, String description, LocalDateTime startTime) {
        return createCalendarEvent(title, description, startTime, MAX_DURATION_SECONDS, "system");
    }
 
    // ---------------------------------------------------------------
    //  Delete event
    // ---------------------------------------------------------------
 
    /**
     * Deletes a Google Calendar event by its event ID.
     * Logs an error but does NOT throw if the deletion fails (e.g. event already gone).
     *
     * @param eventId the Google Calendar event ID stored in ForumPost.googleCalendarEventId
     */
    public void deleteCalendarEvent(String eventId) {
        if (eventId == null || eventId.isBlank()
                || eventId.startsWith("LOCAL-") || eventId.startsWith("ERROR-")) {
            logger.info("Skipping calendar delete — event ID is a placeholder: {}", eventId);
            return;
        }
 
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            logger.warn("Google Calendar not configured — cannot delete event {}", eventId);
            return;
        }
 
        try {
            Calendar calendarClient = buildCalendarClient();
            if (calendarClient == null) return;
 
            calendarClient.events().delete(calendarId, eventId).execute();
            logger.info("Google Calendar event deleted: {}", eventId);
 
        } catch (Exception e) {
            logger.error("Failed to delete Google Calendar event {}: {}", eventId, e.getMessage(), e);
        }
    }
 
    // ---------------------------------------------------------------
    //  Update event time / duration
    // ---------------------------------------------------------------
 
    /**
     * Updates the start/end time of an existing Google Calendar event.
     * Used when an admin changes the scheduled time of a forum post.
     *
     * @param eventId         the existing Google Calendar event ID
     * @param newStart        new start time
     * @param durationSeconds new duration in seconds (max 3600)
     */
    public void updateCalendarEventTime(String eventId, LocalDateTime newStart, int durationSeconds) {
        if (eventId == null || eventId.isBlank()
                || eventId.startsWith("LOCAL-") || eventId.startsWith("ERROR-")) {
            logger.info("Skipping calendar update — placeholder event ID: {}", eventId);
            return;
        }
 
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            logger.warn("Google Calendar not configured — cannot update event {}", eventId);
            return;
        }
 
        try {
            Calendar calendarClient = buildCalendarClient();
            if (calendarClient == null) return;
 
            int safeDuration = Math.max(1, Math.min(durationSeconds, MAX_DURATION_SECONDS));
            ZonedDateTime start = newStart.atZone(ZoneId.of(timezone));
            ZonedDateTime end   = start.plusSeconds(safeDuration);
 
            // Fetch existing event so we keep summary/description
            Event event = calendarClient.events().get(calendarId, eventId).execute();
 
            event.setStart(new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                    java.util.Date.from(start.toInstant())))
                .setTimeZone(timezone));
 
            event.setEnd(new EventDateTime()
                .setDateTime(new com.google.api.client.util.DateTime(
                    java.util.Date.from(end.toInstant())))
                .setTimeZone(timezone));
 
            calendarClient.events().update(calendarId, eventId, event).execute();
            logger.info("Google Calendar event {} updated: start={}, duration={}s",
                eventId, newStart, safeDuration);
 
        } catch (Exception e) {
            logger.error("Failed to update Google Calendar event {}: {}", eventId, e.getMessage(), e);
        }
    }
 
    // ---------------------------------------------------------------
    //  Private helper
    // ---------------------------------------------------------------
 
    private Calendar buildCalendarClient() {
        try {
            InputStream credStream = getClass().getClassLoader()
                .getResourceAsStream(serviceAccountPath);
 
            if (credStream == null) {
                logger.warn("service-account.json not found in classpath at: {}", serviceAccountPath);
                return null;
            }
 
            GoogleCredentials credentials = GoogleCredentials
                .fromStream(credStream)
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));
 
            return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("LOFAR Climate System")
                .build();
 
        } catch (Exception e) {
            logger.error("Failed to build Google Calendar client: {}", e.getMessage(), e);
            return null;
        }
    }
}