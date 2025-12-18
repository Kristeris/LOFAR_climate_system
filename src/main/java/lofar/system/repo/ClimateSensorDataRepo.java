package lofar.system.repo;

import lofar.system.model.ClimateSensorData;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClimateSensorDataRepo extends JpaRepository<ClimateSensorData, Long> {
    
	
}