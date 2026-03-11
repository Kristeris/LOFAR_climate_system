package lofar.system.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lofar.system.model.MyAuthority;
import lofar.system.model.MyUser;
import lofar.system.model.RegisterRequest;
import lofar.system.repo.IMyAuthorityRepo;
import lofar.system.repo.IMyUserRepo;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);

    @Autowired
    private IMyUserRepo userRepo;

    @Autowired
    private IMyAuthorityRepo authorityRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    /**
     * Registers a new user with the USER role.
     * Throws IllegalArgumentException on duplicate username / e-mail.
     */
    public MyUser register(RegisterRequest req) {
        if (userRepo.existsByUsername(req.getUsername())) {
            throw new IllegalArgumentException("Username '" + req.getUsername() + "' is already taken.");
        }
        if (userRepo.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("E-mail '" + req.getEmail() + "' is already registered.");
        }

        // Find or fail the USER authority
        MyAuthority userRole = authorityRepo.findAll().stream()
            .filter(a -> "USER".equals(a.getTitle()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("USER authority not found — is the database seeded?"));

        MyUser newUser = new MyUser(
            req.getUsername(),
            passwordEncoder.encode(req.getPassword()),
            req.getEmail(),
            userRole
        );

        MyUser saved = userRepo.save(newUser);
        logger.info("New user registered: username={}, email={}", saved.getUsername(), saved.getEmail());

        // Send welcome e-mail (async-style fire-and-forget)
        emailService.sendWelcomeEmail(saved.getEmail(), saved.getUsername());

        return saved;
    }
}