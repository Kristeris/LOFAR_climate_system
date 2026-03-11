package lofar.system;

import java.time.LocalDateTime;
import java.util.Arrays;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.password.PasswordEncoder;

import lofar.system.model.ClimateSensorData;
import lofar.system.model.MyAuthority;
import lofar.system.model.MyUser;
import lofar.system.repo.ClimateSensorDataRepo;
import lofar.system.repo.IMyAuthorityRepo;
import lofar.system.repo.IMyUserRepo;

@SpringBootApplication
@EnableScheduling
public class LofarSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(LofarSystemApplication.class, args);
    }

    @Bean
    public CommandLineRunner initData(
            ClimateSensorDataRepo sensorRepo,
            IMyAuthorityRepo authRepo,
            IMyUserRepo userRepo,
            PasswordEncoder encoder
    ) {
        return args -> {

            // ========== SENSOR DATA ==========
            ClimateSensorData s1 = new ClimateSensorData(21.5, 45.0, LocalDateTime.of(2025, 11, 16, 9, 20));
            ClimateSensorData s2 = new ClimateSensorData(18.2, 55.0, LocalDateTime.of(2025, 12, 17, 9, 20));
            ClimateSensorData s3 = new ClimateSensorData(25.1, 60.3, LocalDateTime.of(2025, 10, 20, 9, 20));
            ClimateSensorData s4 = new ClimateSensorData(12.8, 70.0, LocalDateTime.of(2025, 9,  26, 9, 20));
            sensorRepo.saveAll(Arrays.asList(s1, s2, s3, s4));

            // ========== AUTORIZĀCIJA ==========
            MyAuthority userRole  = new MyAuthority("USER");
            MyAuthority adminRole = new MyAuthority("ADMIN");
            authRepo.save(userRole);
            authRepo.save(adminRole);

            // Replace these with real addresses to test e-mail alerts.
            MyUser admin   = new MyUser("admins",  encoder.encode("1234"),
                                        "admin@lofar-system.local", adminRole);
            MyUser regular = new MyUser("kristers", encoder.encode("4321"),
                                        "lofar0749@gmail.com",  userRole);
            userRepo.save(admin);
            userRepo.save(regular);
        };
    }
}