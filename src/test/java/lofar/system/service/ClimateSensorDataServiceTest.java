package lofar.system.service;
 
import lofar.system.model.ClimateSensorData;
import lofar.system.repo.ClimateSensorDataRepo;
import lofar.system.service.impl.ClimateSensorDataServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
 
import java.time.LocalDateTime;
import java.util.*;
 
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
 
@ExtendWith(MockitoExtension.class)
class ClimateSensorDataServiceTest {
 
    @Mock private ClimateSensorDataRepo repo;
 
    @InjectMocks private ClimateSensorDataServiceImpl service;
 
    // ── getAll ────────────────────────────────────────────────────────
 
    @Test
    void getAll_ReturnsAllSensors() {
        ClimateSensorData s1 = sensor(21.5, 45.0);
        ClimateSensorData s2 = sensor(18.2, 55.0);
        when(repo.findAll()).thenReturn(List.of(s1, s2));
 
        List<ClimateSensorData> result = service.getAll();
 
        assertThat(result).hasSize(2);
        verify(repo).findAll();
    }
 
    @Test
    void getAll_EmptyDatabase_ReturnsEmptyList() {
        when(repo.findAll()).thenReturn(Collections.emptyList());
 
        assertThat(service.getAll()).isEmpty();
    }
 
    // ── getById ───────────────────────────────────────────────────────
 
    @Test
    void getById_ExistingId_ReturnsSensor() {
        ClimateSensorData s = sensor(21.5, 45.0);
        when(repo.findById(1L)).thenReturn(Optional.of(s));
 
        ClimateSensorData result = service.getById(1L);
 
        assertThat(result.getTemperature()).isEqualTo(21.5);
    }
 
    @Test
    void getById_MissingId_ThrowsRuntimeException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> service.getById(99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("99");
    }
 
    // ── getLatest10 ───────────────────────────────────────────────────
 
    @Test
    void getLatest10_ReturnsUpTo10Records() {
        List<ClimateSensorData> tenSensors = new ArrayList<>();
        for (int i = 0; i < 10; i++) tenSensors.add(sensor(20.0 + i, 50.0));
 
        Page<ClimateSensorData> page = new PageImpl<>(tenSensors);
        when(repo.findAll(any(PageRequest.class))).thenReturn(page);
 
        List<ClimateSensorData> result = service.getLatest10();
 
        assertThat(result).hasSize(10);
 
        // Verify it uses DESC sort on sensorDateTime
        ArgumentCaptor<PageRequest> captor = ArgumentCaptor.forClass(PageRequest.class);
        verify(repo).findAll(captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("sensorDateTime");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }
 
    @Test
    void getLatest10_FewerThan10Records_ReturnsAll() {
        List<ClimateSensorData> three = List.of(sensor(21.0, 44.0), sensor(22.0, 55.0), sensor(23.0, 66.0));
        when(repo.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(three));
 
        assertThat(service.getLatest10()).hasSize(3);
    }
 
    // ── save ──────────────────────────────────────────────────────────
 
    @Test
    void save_ValidData_PersistsAndReturns() {
        ClimateSensorData input  = sensor(25.1, 60.3);
        ClimateSensorData saved  = sensor(25.1, 60.3);
        when(repo.save(input)).thenReturn(saved);
 
        ClimateSensorData result = service.save(input);
 
        assertThat(result.getTemperature()).isEqualTo(25.1);
        verify(repo).save(input);
    }
 
    // ── update ────────────────────────────────────────────────────────
 
    @Test
    void update_ExistingId_UpdatesFields() {
        ClimateSensorData existing = sensor(20.0, 50.0);
        ClimateSensorData updated  = sensor(30.0, 70.0);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenReturn(updated);
 
        ClimateSensorData result = service.update(1L, updated);
 
        assertThat(result.getTemperature()).isEqualTo(30.0);
        assertThat(result.getHumidity()).isEqualTo(70.0);
        verify(repo).save(existing);
    }
 
    @Test
    void update_MissingId_ThrowsRuntimeException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());
 
        assertThatThrownBy(() -> service.update(99L, sensor(20.0, 50.0)))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("99");
    }
 
    @Test
    void update_UpdatesAllThreeFields() {
        ClimateSensorData existing = sensor(10.0, 20.0);
        LocalDateTime newTime = LocalDateTime.of(2026, 1, 15, 10, 30);
        ClimateSensorData patch = new ClimateSensorData(35.5, 88.8, newTime);
 
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
 
        ClimateSensorData result = service.update(1L, patch);
 
        assertThat(result.getTemperature()).isEqualTo(35.5);
        assertThat(result.getHumidity()).isEqualTo(88.8);
        assertThat(result.getSensorDateTime()).isEqualTo(newTime);
    }
 
    // ── delete ────────────────────────────────────────────────────────
 
    @Test
    void delete_ExistingId_DeletesRecord() {
        when(repo.existsById(1L)).thenReturn(true);
 
        service.delete(1L);
 
        verify(repo).deleteById(1L);
    }
 
    @Test
    void delete_MissingId_ThrowsRuntimeException() {
        when(repo.existsById(99L)).thenReturn(false);
 
        assertThatThrownBy(() -> service.delete(99L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("99");
 
        verify(repo, never()).deleteById(any());
    }
 
    // ── helper ────────────────────────────────────────────────────────
 
    private ClimateSensorData sensor(double temp, double humidity) {
        return new ClimateSensorData(temp, humidity, LocalDateTime.now());
    }
}
