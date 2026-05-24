package lofar.system.controller;
 
import com.fasterxml.jackson.databind.ObjectMapper;
import lofar.system.model.ClimateSensorData;
import lofar.system.service.DynamicSchedulerService;
import lofar.system.service.SensorDataParserService;
import lofar.system.service.SensorScriptExecutionService;
import lofar.system.service.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
 
import java.time.LocalDateTime;
import java.util.Map;
 
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
 
@ExtendWith(MockitoExtension.class)
class AdminControllerTest {
 
    private MockMvc mockMvc;
 
    private final ObjectMapper objectMapper = new ObjectMapper();
 
    @Mock private DynamicSchedulerService      schedulerService;
    @Mock private SensorScriptExecutionService scriptService;
    @Mock private SensorDataParserService      parserService;
    @Mock private WebSocketNotificationService wsService;
 
    @InjectMocks
    private AdminController adminController;
 
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminController).build();
    }
 
    // ── GET /api/admin/scheduler/status ──────────────────────────────
 
    @Test
    void getSchedulerStatus_Running_ReturnsCorrectState() throws Exception {
        when(schedulerService.isSchedulerEnabled()).thenReturn(true);
        when(schedulerService.getIntervalMinutes()).thenReturn(10L);
 
        mockMvc.perform(get("/api/admin/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedulerEnabled").value(true))
                .andExpect(jsonPath("$.intervalMinutes").value(10))
                .andExpect(jsonPath("$.message").value("Scheduler is running"));
    }
 
    @Test
    void getSchedulerStatus_Stopped_ReturnsStoppedMessage() throws Exception {
        when(schedulerService.isSchedulerEnabled()).thenReturn(false);
        when(schedulerService.getIntervalMinutes()).thenReturn(10L);
 
        mockMvc.perform(get("/api/admin/scheduler/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedulerEnabled").value(false))
                .andExpect(jsonPath("$.message").value("Scheduler is stopped"));
    }
 
    // ── POST /api/admin/scheduler/stop ────────────────────────────────
 
    @Test
    void stopScheduler_Returns200WithStoppedStatus() throws Exception {
        mockMvc.perform(post("/api/admin/scheduler/stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("stopped"));
 
        verify(schedulerService).disableScheduler();
    }
 
    // ── POST /api/admin/scheduler/start ───────────────────────────────
 
    @Test
    void startScheduler_Returns200WithStartedStatus() throws Exception {
        when(schedulerService.getIntervalMinutes()).thenReturn(5L);
 
        mockMvc.perform(post("/api/admin/scheduler/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("started"));
 
        verify(schedulerService).enableScheduler();
    }
 
    // ── POST /api/admin/scheduler/interval ────────────────────────────
 
    @Test
    void setInterval_ValidMinutes_Returns200() throws Exception {
        Map<String, Long> body = Map.of("minutes", 15L);
 
        mockMvc.perform(post("/api/admin/scheduler/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("updated"))
                .andExpect(jsonPath("$.intervalMinutes").value(15));
 
        verify(schedulerService).setInterval(15L);
    }
 
    @Test
    void setInterval_ZeroMinutes_Returns400() throws Exception {
        Map<String, Long> body = Map.of("minutes", 0L);
 
        mockMvc.perform(post("/api/admin/scheduler/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
 
        verifyNoInteractions(schedulerService);
    }
 
    @Test
    void setInterval_Above1440Minutes_Returns400() throws Exception {
        Map<String, Long> body = Map.of("minutes", 1441L);
 
        mockMvc.perform(post("/api/admin/scheduler/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
 
    @Test
    void setInterval_Exactly1440Minutes_Returns200() throws Exception {
        Map<String, Long> body = Map.of("minutes", 1440L);
 
        mockMvc.perform(post("/api/admin/scheduler/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
 
        verify(schedulerService).setInterval(1440L);
    }
 
    @Test
    void setInterval_Exactly1Minute_Returns200() throws Exception {
        Map<String, Long> body = Map.of("minutes", 1L);
 
        mockMvc.perform(post("/api/admin/scheduler/interval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
 
        verify(schedulerService).setInterval(1L);
    }
 
    // ── GET /api/admin/sensor/trigger ─────────────────────────────────
 
    @Test
    void manualTrigger_Success_Returns200WithSavedData() throws Exception {
        ClimateSensorData data = new ClimateSensorData(22.5, 50.0, LocalDateTime.now());
        when(scriptService.runPythonScript()).thenReturn("raw output");
        when(parserService.parseAndSave("raw output")).thenReturn(data);
 
        mockMvc.perform(get("/api/admin/sensor/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));
 
        verify(wsService).notifyClients(data);
    }
 
    @Test
    void manualTrigger_ScriptFails_Returns500() throws Exception {
        when(scriptService.runPythonScript())
                .thenThrow(new RuntimeException("Python not found"));
 
        mockMvc.perform(get("/api/admin/sensor/trigger"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").exists());
    }
 
    @Test
    void manualTrigger_ParseFails_Returns500() throws Exception {
        when(scriptService.runPythonScript()).thenReturn("raw");
        when(parserService.parseAndSave("raw"))
                .thenThrow(new RuntimeException("Parse error"));
 
        mockMvc.perform(get("/api/admin/sensor/trigger"))
                .andExpect(status().isInternalServerError());
    }
}
 