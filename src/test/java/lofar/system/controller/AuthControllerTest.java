package lofar.system.controller;
 

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
 
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
 
    private MockMvc mockMvc;
 
    @Mock
    private AuthenticationManager authenticationManager;
 
    @InjectMocks
    private AuthController authController;
 
    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }
 
    // ── POST /api/auth/login ──────────────────────────────────────────
 
    @Test
    void login_ValidUserCredentials_Returns200WithUsernameAndRole() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", null,
                List.of(new SimpleGrantedAuthority("USER"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(auth);
 
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "testuser")
                        .param("password", "secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }
 
    @Test
    void login_ValidAdminCredentials_ReturnsAdminRole() throws Exception {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "adminuser", null,
                List.of(new SimpleGrantedAuthority("ADMIN"))
        );
        when(authenticationManager.authenticate(any())).thenReturn(auth);
 
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "adminuser")
                        .param("password", "adminpass"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }
 
    @Test
    void login_BadCredentials_Returns401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));
 
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "wrong")
                        .param("password", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid username or password"));
    }
 
    @Test
    void login_AuthManagerThrowsGenericException_Returns500() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new RuntimeException("DB connection lost"));
 
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "user")
                        .param("password", "pass"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }
 
    // ── GET /api/auth/me ──────────────────────────────────────────────
 
    @Test
    void getMe_WithPrincipal_Returns200WithDetails() throws Exception {
        // Set up a SecurityContext with an authenticated user
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "testuser", null,
                List.of(new SimpleGrantedAuthority("USER"))
        );
        org.springframework.security.core.context.SecurityContextHolder
                .getContext().setAuthentication(auth);
 
        mockMvc.perform(get("/api/auth/me")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
 
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
 
    @Test
    void getMe_NoPrincipal_Returns401() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}