package com.cloud.drive.config;

import com.cloud.drive.security.admin.AdminJwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.config.Customizer;
import com.cloud.drive.security.JsonAccessDeniedHandler;
import com.cloud.drive.security.JsonAuthenticationEntryPoint;

@Configuration
@Order(1)
public class AdminSecurityConfig {

    private final AdminJwtAuthFilter adminJwtAuthFilter;
    private final JsonAuthenticationEntryPoint authenticationEntryPoint;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    public AdminSecurityConfig(AdminJwtAuthFilter adminJwtAuthFilter,
                               JsonAuthenticationEntryPoint authenticationEntryPoint,
                               JsonAccessDeniedHandler accessDeniedHandler) {
        this.adminJwtAuthFilter = adminJwtAuthFilter;
        this.authenticationEntryPoint = authenticationEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    @Bean
    @Order(1)
    public SecurityFilterChain adminFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/admin/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/auth/login").permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .addFilterBefore(adminJwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
