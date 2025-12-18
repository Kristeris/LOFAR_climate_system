package lofar.system.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import lofar.system.model.ClimateSensorData;
import lofar.system.service.ClimateSensorDataService;

@Controller
@RestController
@RequestMapping("/api/sensors")
public class ClimateSensorDataController {

    @Autowired
    private ClimateSensorDataService service;

    @GetMapping
    public List<ClimateSensorData> getAllSensorData() {
        return service.getAll();
    }

    @GetMapping("/{id}")
    public ClimateSensorData getSensorDataById(@PathVariable Long id) {
        return service.getById(id);
    }

    @GetMapping("/latest")
    public List<ClimateSensorData> getLatest10Sensors() {
        return service.getLatest10();
    }

    @PostMapping
    public ClimateSensorData createSensorData(@RequestBody ClimateSensorData sensorData) {
        return service.save(sensorData);
    }

    @PutMapping("/{id}")
    public ClimateSensorData updateSensorData(@PathVariable Long id, @RequestBody ClimateSensorData sensorData) {
        return service.update(id, sensorData);
    }

    @DeleteMapping("/{id}")
    public void deleteSensorData(@PathVariable Long id) {
        service.delete(id);
    }
}