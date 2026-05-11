package com.internship.tool.config;

import com.internship.tool.security.JwtAuthenticationFilter;
import com.internship.tool.security.PiiMaskingFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.http.HttpHeaders;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PiiMaskingFilter piiMaskingFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/").permitAll()
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger", "/swagger/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(piiMaskingFilter, UsernamePasswordAuthenticationFilter.class)
                .headers(headers -> headers
                        // Prevent clickjacking
                        .frameOptions(frameOptions -> frameOptions.deny())
                        // Strict Content Security Policy - no unsafe-inline, no unsafe-eval
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; " +
                                "script-src 'self'; " +
                                "style-src 'self'; " +
                                "img-src 'self' data:; " +
                                "font-src 'self'; " +
                                "connect-src 'self'; " +
                                "frame-ancestors 'none'; " +
                                "form-action 'self'; " +
                                "base-uri 'self'; " +
                                "object-src 'none'"
                        ))
                        // Cache control - prevent caching of sensitive data
                        .cacheControl(cache -> cache.disable())
                        // Referrer Policy
                        .referrerPolicy(referrer -> referrer.policy(
                                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
                        ))
                        // Custom headers for additional protection
                        .addHeaderWriter(new org.springframework.security.web.header.writers.StaticHeadersWriter(
                                "X-Content-Type-Options", "nosniff",
                                "X-XSS-Protection", "1; mode=block",
                                "X-Frame-Options", "DENY",
                                "Permissions-Policy", "geolocation=(), microphone=(), camera=(), payment=(), usb=(), magnetometer=(), gyroscope=(), accelerometer=()",
                                "Cache-Control", "no-store, no-cache, must-revalidate, max-age=0, private",
                                "Pragma", "no-cache",
                                "Expires", "0"
                        ))
                )
                // Cookie Security Configuration
                .rememberMe(remember -> remember.disable());

        return http.build();
    }
}
