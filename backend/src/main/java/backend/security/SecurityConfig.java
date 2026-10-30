package backend.security;

import backend.repository.UserRepository;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;


@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Password Encryption
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    // Main Security Configuration
    @Bean
    public SecurityFilterChain securityFilterChain(

            HttpSecurity http,

            JwtUtil jwtUtil,

            UserRepository userRepository,

            JsonSecurityHandlers handlers,

            @Qualifier("corsConfigurationSource")
            CorsConfigurationSource corsSource,

            @Value("${app.trust-proxy:false}")
            boolean trustProxy

    ) throws Exception {

        http

                // CSRF disabled for stateless JWT API
                .csrf(AbstractHttpConfigurer::disable)

                // CORS Configuration
                .cors(cors ->
                        cors.configurationSource(corsSource)
                )

                // Stateless Session Management
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                // Disable Default Authentication
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                // Exception Handling
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(handlers)
                                .accessDeniedHandler(handlers)
                )

                // Authorization Rules
                .authorizeHttpRequests(auth -> auth

                        // Allow CORS preflight requests
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // Public Health Endpoints
                        .requestMatchers(
                                "/error",
                                "/actuator/health",
                                "/actuator/health/**"
                        ).permitAll()

                        // WebSocket Handshake
                        .requestMatchers(
                                "/ws/**"
                        ).permitAll()

                        // Public Authentication APIs
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/forgot-password",
                                "/api/auth/reset-password"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/auth/verify-email"
                        ).permitAll()

                        // Public Vendor and Media APIs
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/vendors/**",
                                "/api/media/**"
                        ).permitAll()

                        // Vendor APIs
                        .requestMatchers(
                                "/api/vendor/**"
                        ).hasRole("VENDOR")

                        // Admin APIs
                        .requestMatchers(
                                "/api/admin/**"
                        ).hasRole("ADMIN")

                        // Customer Order APIs
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders",
                                "/api/demand/signals"
                        ).hasRole("CUSTOMER")

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/orders/mine"
                        ).hasRole("CUSTOMER")

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/orders/*/cancel",
                                "/api/orders/*/review"
                        ).hasRole("CUSTOMER")

                        // All Remaining API Requests Require Authentication
                        .requestMatchers(
                                "/api/**"
                        ).authenticated()

                        // Deny Everything Else
                        .anyRequest().denyAll()
                )

                // Rate Limiting Filter
                .addFilterBefore(
                        new RateLimitFilter(trustProxy),
                        UsernamePasswordAuthenticationFilter.class
                )

                // JWT Authentication Filter
                .addFilterBefore(
                        new JwtAuthFilter(jwtUtil, userRepository),
                        UsernamePasswordAuthenticationFilter.class
                );


        return http.build();
    }


    // CORS Configuration Bean
    @Bean
    public CorsConfigurationSource corsConfigurationSource(

            @Value("${app.cors.allowed-origins:}")
            String allowedOrigins

    ) {

        CorsConfiguration config = new CorsConfiguration();

        // Allowed Origins
        List<String> origins = Arrays.stream(
                        allowedOrigins.split(",")
                )
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();

        config.setAllowedOrigins(origins);

        // Allowed HTTP Methods
        config.setAllowedMethods(
                List.of(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
        );

        // Allowed Headers
        config.setAllowedHeaders(
                List.of(
                        "Authorization",
                        "Content-Type",
                        "Accept"
                )
        );

        // Credentials
        config.setAllowCredentials(false);

        // Cache Preflight Response
        config.setMaxAge(3600L);

        // Register CORS Configuration
        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration(
                "/**",
                config
        );

        return source;
    }

}