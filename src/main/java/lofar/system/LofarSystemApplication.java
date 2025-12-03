package lofar.system;


import java.time.LocalDate;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import lofar.system.model.ClimateSensorData;


@SpringBootApplication
public class LofarSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LofarSystemApplication.class, args);
	}
	
	
	@Bean
	public CommandLineRunner testModel(ClimateSensorDataRepository repo) {
		
		return new CommandLineRunner() {
			
			@Override
			public void run(String... args) throws Exception {
				
				ClimateSensorData S1 = new ClimateSensorData(21.5,45.0,LocalDate.of(2025, 10, 15));
	            ClimateSensorData S2 = new ClimateSensorData(18.2,55.0,LocalDate.of(2025, 11, 16));
	            ClimateSensorData S3 = new ClimateSensorData(25.1,60.3,LocalDate.of(2025, 12, 17));
	            ClimateSensorData S4 = new ClimateSensorData(12.8,70.0,LocalDate.of(2025, 9, 12));

	            repo.saveAll(Arrays.asList(S1, S2, S3, S4));
				
			}
		};
		
	}
	
	

}
