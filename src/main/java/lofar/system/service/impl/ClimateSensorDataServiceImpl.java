package lofar.system.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lofar.system.model.ClimateSensorData;
import lofar.system.repo.ClimateSensorDataRepo;
import lofar.system.service.ClimateSensorDataService;

@Service
public class ClimateSensorDataServiceImpl implements ClimateSensorDataService {

    @Autowired
    private ClimateSensorDataRepo repo;

    @Override
    public List<ClimateSensorData> getAll() {
        return repo.findAll();
    }

    @Override
    public ClimateSensorData getById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found with id, " + id));
    }

    @Override
    public List<ClimateSensorData> getLatest10() {
        PageRequest page = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "sensorDateTime"));
        return repo.findAll(page).getContent();
    }

    @Override
    public ClimateSensorData save(ClimateSensorData data) {
        return repo.save(data);
    }

    @Override
    public ClimateSensorData update(Long id, ClimateSensorData data) {
        ClimateSensorData existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Sensor not found with id, " + id));

        existing.setTemperature(data.getTemperature());
        existing.setHumidity(data.getHumidity());
        existing.setSensorDateTime(data.getSensorDateTime());

        return repo.save(existing);
    }

    @Override
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new RuntimeException("Sensor not found with id, " + id);
        }
        repo.deleteById(id);
    }
}