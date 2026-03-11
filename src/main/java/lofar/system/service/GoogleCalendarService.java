package lofar.system.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * GoogleCalendarService
 *
 * Creates Google Calendar events via the Google Calendar REST API
 * using a pre-generated OAuth2 Bearer access token.
 *
 * Required application.properties keys:
 *   google.calendar.api.key   — OAuth2 Bearer token
 *   google.calendar.id        — Calendar ID (e.g. "primary" or owner e-mail)
 *   google.calendar.timezone  — e.g. "Europe/Riga"
 */
@Service
public class GoogleCalendarService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCalendarService.class);

    private static final String CALENDAR_API_BASE =
            "https://www.googleapis.com/calendar/v3/calendars/";

    @Value("${google.calendar.api.key:}")
    private String apiKey;

    @Value("${google.calendar.id:primary}")
    private String calendarId;

    @Value("${google.calendar.timezone:Europe/Riga}")
    private String timezone;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Creates a one-hour Google Calendar event for the given forum post.
     *
     * @param title       event / post title
     * @param description event description (post content summary)
     * @param startTime   event start time
     * @return Google Calendar event ID, or a placeholder if API is not configured
     */
    public String createCalendarEvent(String title, String description, LocalDateTime startTime) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Google Calendar API key not configured — returning placeholder event ID");
            return "LOCAL-" + System.currentTimeMillis();
        }

        try {
            ZonedDateTime start = startTime.atZone(ZoneId.of(timezone));
            ZonedDateTime end   = start.plusHours(1);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

            // Build payload using plain Maps — no Jackson import needed.
            // RestTemplate serialises the Map to JSON automatically via
            // MappingJackson2HttpMessageConverter, which is already on the
            // classpath through spring-boot-starter-web.
            Map<String, String> startNode = Map.of(
                    "dateTime", start.format(fmt),
                    "timeZone", timezone
            );
            Map<String, String> endNode = Map.of(
                    "dateTime", end.format(fmt),
                    "timeZone", timezone
            );

            String truncatedDesc = (description != null && description.length() > 500)
                    ? description.substring(0, 500) + "\u2026"
                    : description;

            Map<String, Object> event = Map.of(
                    "summary",     "\uD83D\uDCCB LOFAR Forum: " + title,
                    "description", truncatedDesc != null ? truncatedDesc : "",
                    "start",       startNode,
                    "end",         endNode
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(event, headers);

            String url = CALENDAR_API_BASE + calendarId + "/events";

            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Object eventId = response.getBody().get("id");
                String id = eventId != null ? eventId.toString() : "UNKNOWN";
                logger.info("Google Calendar event created: '{}' — id: {}", title, id);
                return id;
            } else {
                logger.warn("Google Calendar returned non-2xx: {}", response.getStatusCode());
                return "API-ERROR-" + response.getStatusCode().value();
            }

        } catch (Exception e) {
            logger.error("Failed to create Google Calendar event: {}", e.getMessage(), e);
            return "ERROR-" + System.currentTimeMillis();
        }
    }
}