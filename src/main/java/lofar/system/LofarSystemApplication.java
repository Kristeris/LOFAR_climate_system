package lofar.system;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import lofar.system.model.ClimateSensorData;
import lofar.system.repo.ClimateSensorDataRepo;

@SpringBootApplication
@EnableScheduling  // Enable scheduled tasks
public class LofarSystemApplication {

	public static void main(String[] args) {
		SpringApplication.run(LofarSystemApplication.class, args);
	}
	
	@Bean
	public CommandLineRunner testModel(ClimateSensorDataRepo repo) {
		return new CommandLineRunner() {
			@Override
			public void run(String... args) throws Exception {
				ClimateSensorData S1 = new ClimateSensorData(21.5, 45.0, LocalDateTime.of(2025, 11, 16, 9, 20));
				ClimateSensorData S2 = new ClimateSensorData(18.2, 55.0, LocalDateTime.of(2025, 12, 17, 9, 20));
				ClimateSensorData S3 = new ClimateSensorData(25.1, 60.3, LocalDateTime.of(2025, 10, 20, 9, 20));
				ClimateSensorData S4 = new ClimateSensorData(12.8, 70.0, LocalDateTime.of(2025, 9, 26, 9, 20));

				repo.saveAll(Arrays.asList(S1, S2, S3, S4));
			}
		};
	}
}