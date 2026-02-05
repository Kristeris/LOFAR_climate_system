package lofar.system.service;

import java.util.List;
import lofar.system.model.ClimateSensorData;

public interface ClimateSensorDataService {

    List<ClimateSensorData> getAll();
    ClimateSensorData getById(Long id);
    List<ClimateSensorData> getLatest10();
    ClimateSensorData save(ClimateSensorData data);
    ClimateSensorData update(Long id, ClimateSensorData data);
    void delete(Long id);
}