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

    @Value("${google.calendar.service-account-path}")
    private String serviceAccountPath;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${google.calendar.timezone:Europe/Riga}")
    private String timezone;

    public String createCalendarEvent(String title, String description, LocalDateTime startTime) {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            logger.warn("Google Calendar service account not configured — returning placeholder");
            return "LOCAL-" + System.currentTimeMillis();
        }

        try {
            // Load service account credentials from classpath
            InputStream credStream = getClass().getClassLoader()
                .getResourceAsStream(serviceAccountPath);

            if (credStream == null) {
                logger.warn("service-account.json not found in classpath — returning placeholder");
                return "LOCAL-" + System.currentTimeMillis();
            }

            GoogleCredentials credentials = GoogleCredentials
                .fromStream(credStream)
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

            Calendar calendarClient = new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("LOFAR Climate System")
                .build();

            // Build event times
            ZonedDateTime start = startTime.atZone(ZoneId.of(timezone));
            ZonedDateTime end = start.plusHours(1);

            Event event = new Event()
                .setSummary(" LOFAR Forum: " + title)
                .setDescription(description != null && description.length() > 500
                    ? description.substring(0, 500) + "…"
                    : description);

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

            logger.info("Google Calendar event created: '{}' — id: {}", title, created.getId());
            return created.getId();

        } catch (Exception e) {
            logger.error("Failed to create Google Calendar event: {}", e.getMessage(), e);
            return "ERROR-" + System.currentTimeMillis();
        }
    }
}