package lofar.system.controller;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import lofar.system.model.ForumPostDTO;
import lofar.system.service.ForumService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
 
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
 
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import tools.jackson.databind.ObjectMapper;
 
@ExtendWith(MockitoExtension.class)
class ForumControllerTest {
 
    private MockMvc mockMvc;
 
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(tools.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
 
    @Mock
    private ForumService forumService;
 
    @InjectMocks
    private ForumController forumController;
 
    private ForumPostDTO sampleDTO;
 
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(forumController).build();
 
        sampleDTO = new ForumPostDTO(
                1L, "Test Observation", "Commands here",
                LocalDateTime.now().minusDays(1), "2026-04-09T14:35:00Z",
                "testuser", "cal-event-123",
                LocalDateTime.now().plusDays(1), 3600,
                1.0472, 0.5236, "J2000",
                "FUTURE", null, 0
        );
    }
 
    // ── GET /api/forum/{id} ───────────────────────────────────────────
 
    @Test
    void getPost_ExistingId_Returns200() throws Exception {
        when(forumService.getPost(1L)).thenReturn(sampleDTO);
 
        mockMvc.perform(get("/api/forum/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Observation"))
                .andExpect(jsonPath("$.status").value("FUTURE"));
    }
 
    @Test
    void getPost_MissingId_Returns404() throws Exception {
        when(forumService.getPost(99L))
                .thenThrow(new IllegalArgumentException("Post not found"));
 
        mockMvc.perform(get("/api/forum/99"))
                .andExpect(status().isNotFound());
    }
 
    // ── POST /api/forum ───────────────────────────────────────────────
 
    @Test
    void createPost_MissingTitle_Returns400() throws Exception {
        Map<String, String> body = Map.of("content", "some content");
 
        mockMvc.perform(post("/api/forum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Title is required"));
    }
 
    @Test
    void createPost_MissingContent_Returns400() throws Exception {
        Map<String, String> body = Map.of("title", "A Title");
 
        mockMvc.perform(post("/api/forum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Content is required"));
    }
 
    @Test
    void createPost_InvalidScheduledDateTime_Returns400() throws Exception {
        Map<String, String> body = Map.of(
                "title", "My Post",
                "content", "Content",
                "scheduledDateTime", "not-a-date"
        );
 
        mockMvc.perform(post("/api/forum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
 
    @Test
    void createPost_TimeSlotConflict_Returns409() throws Exception {
        Map<String, String> body = Map.of("title", "Conflict Post", "content", "content");
        when(forumService.createPost(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("Time slot conflict"));
 
        mockMvc.perform(post("/api/forum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isConflict());
    }
 
    @Test
    void createPost_ValidBody_Returns200() throws Exception {
        when(forumService.createPost(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleDTO);
 
        Map<String, String> body = Map.of(
                "title", "New Observation",
                "content", "beamctl commands..."
        );
 
        mockMvc.perform(post("/api/forum")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.googleCalendarEventId").value("cal-event-123"));
    }
 
    // ── DELETE /api/forum/{id} ────────────────────────────────────────
 
    @Test
    void deletePost_ExistingId_Returns200() throws Exception {
        doNothing().when(forumService).deletePost(eq(1L), any());
 
        mockMvc.perform(delete("/api/forum/1")
                        .param("reason", "Test deletion"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Post deleted"));
 
        verify(forumService).deletePost(eq(1L), eq("Test deletion"));
    }
 
    @Test
    void deletePost_MissingId_Returns404() throws Exception {
        doThrow(new IllegalArgumentException("Post not found"))
                .when(forumService).deletePost(eq(99L), any());
 
        mockMvc.perform(delete("/api/forum/99"))
                .andExpect(status().isNotFound());
    }
 
    // ── PUT /api/forum/{id}/time ──────────────────────────────────────
 
    @Test
    void updateTime_ValidBody_AdminUser_Returns200() throws Exception {
        // Set up admin authentication in SecurityContext
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ADMIN"))
        );
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(adminAuth);
 
        LocalDateTime newStart = LocalDateTime.now().plusDays(3);
        Map<String, String> body = Map.of("scheduledDateTime", newStart.toString());
        when(forumService.adminUpdateTime(eq(1L), any(), anyInt())).thenReturn(sampleDTO);
 
        mockMvc.perform(put("/api/forum/1/time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
 
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
 
    @Test
    void updateTime_NonAdmin_Returns403() throws Exception {
        UsernamePasswordAuthenticationToken userAuth = new UsernamePasswordAuthenticationToken(
                "regularuser", null,
                List.of(new SimpleGrantedAuthority("USER"))
        );
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(userAuth);
 
        Map<String, String> body = Map.of(
                "scheduledDateTime", LocalDateTime.now().plusDays(1).toString()
        );
 
        mockMvc.perform(put("/api/forum/1/time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isForbidden());
 
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
 
    @Test
    void updateTime_MissingDateField_Returns400() throws Exception {
        UsernamePasswordAuthenticationToken adminAuth = new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ADMIN"))
        );
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(adminAuth);
 
        Map<String, String> body = Map.of();
 
        mockMvc.perform(put("/api/forum/1/time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
 
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
 
    // ── GET /api/forum/check-path ─────────────────────────────────────
 
    @Test
    void checkPath_ExistingPath_ReturnsExistsTrue() throws Exception {
        String existingPath = System.getProperty("java.io.tmpdir");
 
        mockMvc.perform(get("/api/forum/check-path")
                        .param("path", existingPath))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(true));
    }
 
    @Test
    void checkPath_NonexistentPath_ReturnsExistsFalse() throws Exception {
        mockMvc.perform(get("/api/forum/check-path")
                        .param("path", "/this/path/does/not/exist/12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exists").value(false));
    }
}