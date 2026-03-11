package lofar.system.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import lofar.system.model.MyUser;
import lofar.system.model.RegisterRequest;
import lofar.system.service.RegistrationService;

/**
 * RegistrationController
 *
 * POST /api/auth/register — publicly accessible endpoint for new account creation.
 * The registered user always receives the USER role.
 */
@RestController
@RequestMapping("/api/auth")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            MyUser saved = registrationService.register(request);
            return ResponseEntity.ok(Map.of(
                "message", "Registration successful",
                "username", saved.getUsername(),
                "email",    saved.getEmail()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "Registration failed: " + e.getMessage()));
        }
    }
}