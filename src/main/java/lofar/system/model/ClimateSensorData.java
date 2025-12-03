package lofar.system.model;

import java.time.LocalDate;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString

public class ClimateSensorData {
	private long sensorId;
	private double temperature;
	private double humidity;
	private LocalDate sensorDateTime;
	
	
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
	
	
	
	public ClimateSensorData(Double temperature, Double humidity, LocalDate sensorDateTime) {
		this.temperature = temperature;
		this.humidity = humidity;
		this.sensorDateTime = sensorDateTime;
		
}
	
	
	
}
