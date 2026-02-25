package lofar.system.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lofar.system.service.MyUserDetailsService;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private MyUserDetailsService myUserDetailsService;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(myUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes AuthenticationManager so AuthController can inject and use it
     * to programmatically authenticate username/password credentials.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS must be configured here inside Spring Security, not only in
     * WebMvcConfigurer — Security's filter chain runs before MVC and will
     * block preflight OPTIONS requests otherwise.
     * allowCredentials(true) is required so the browser sends the session cookie.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);          // required for cookies
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Use the CORS config defined above — this replaces CorsConfig.java
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // Return 401 JSON instead of redirecting to a login page
            // when an unauthenticated request hits a protected endpoint.
            // Without this, Spring redirects to /login (HTML), which breaks
            // Angular's HttpClient interceptor.
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(401);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
            )

            .authenticationProvider(authenticationProvider())

            // Disable CSRF for all API and WebSocket paths.
            // Angular does not send CSRF tokens by default, and session cookies
            // are HttpOnly so JS cannot read them anyway.
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    "/api/**",
                    "/sensors/**",
                    "/ws-sensor/**",
                    "/logout"
                )
            )

            .authorizeHttpRequests(auth -> auth
                // Angular calls these before the user is logged in
                .requestMatchers("/api/auth/login", "/api/auth/me").permitAll()

                // Admin-only REST endpoints
                .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                // Authenticated users (any role)
                .requestMatchers("/api/sensors/**").authenticated()
                .requestMatchers("/ws-sensor/**").authenticated()

                // Everything else also requires authentication
                .anyRequest().authenticated()
            )

            // NO .formLogin() — Angular handles login itself via /api/auth/login.
            // Keeping formLogin() here would register Spring's own POST /login
            // handler which conflicts and causes redirect loops.

            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                // Return JSON 200 instead of a redirect — Angular expects JSON
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(200);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"message\":\"Logged out\"}");
                })
                .permitAll()
            )

            .sessionManagement(session -> session
                .maximumSessions(1)
            );

        return http.build();
    }
}