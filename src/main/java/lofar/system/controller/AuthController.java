package lofar.system.controller;

import java.security.Principal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * AuthController
 *
 * Replaces Spring's built-in form login with JSON-friendly endpoints
 * that Angular's HttpClient can actually use.
 *
 * CORS is handled globally in SecurityConfig — no @CrossOrigin needed here.
 *
 * POST /api/auth/login  — authenticates and stores the session
 * GET  /api/auth/me     — returns current user info (session restore on refresh)
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * POST /api/auth/login
     *
     * Accepts form-encoded body: username=xxx&password=yyy
     * (matches what auth.service.ts sends as URLSearchParams)
     *
     * On success: stores auth in the HTTP session (same as Spring's form login
     * does internally) and returns { username, role }.
     * On failure: returns 401 with an error message.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletRequest request) {

        try {
            // Authenticate credentials against the database
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
            );

            // Store in SecurityContext
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            SecurityContextHolder.setContext(context);

            // Persist to the HTTP session — this is what makes the session
            // cookie work on subsequent requests (same as Spring's form login)
            HttpSession session = request.getSession(true);
            session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
            );

            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("USER");

            return ResponseEntity.ok(Map.of(
                "username", auth.getName(),
                "role",     role
            ));

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body(Map.of("error", "Authentication error: " + e.getMessage()));
        }
    }

    /**
     * GET /api/auth/me
     *
     * Called by auth.service.ts on every app startup to restore session state.
     * Returns 401 if the session has expired or the user is not logged in —
     * the Angular interceptor will then redirect to /login.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401)
                .body(Map.of("error", "Not authenticated"));
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String role = auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .findFirst()
            .orElse("USER");

        return ResponseEntity.ok(Map.of(
            "username", principal.getName(),
            "role",     role
        ));
    }
}