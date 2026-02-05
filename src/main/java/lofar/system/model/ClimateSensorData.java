package lofar.system.model;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "climate_sensor_data")
@Getter
@Setter
@ToString
@NoArgsConstructor

public class ClimateSensorData {
	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	private long sensorId;
	private double temperature;
	private double humidity;
	private LocalDateTime sensorDateTime;
	
	public double getTemperature() {
		return temperature;
	}
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}
	public double getHumidity() {
		return humidity;
	}
	public void setHumidity(double humidity) {
		this.humidity = humidity;
	}
	
	
	
	public ClimateSensorData(Double temperature, Double humidity, LocalDateTime sensorDateTime) {
		this.temperature = temperature;
		this.humidity = humidity;
		this.sensorDateTime = sensorDateTime;
		
	}
	
	
	
}
