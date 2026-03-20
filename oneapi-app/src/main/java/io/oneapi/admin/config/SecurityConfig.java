package io.oneapi.admin.config;

import io.oneapi.admin.security.security.AuthoritiesConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for OneAPI Application with JWT authentication.
 *
 * This configuration provides:
 * - JWT-based authentication (stateless)
 * - Database-backed user management
 * - Role-based access control (RBAC)
 * - Database, Table, and Column level permissions
 * - Public access to development tools (H2, Swagger, GraphiQL)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    /**
     * Security filter chain for PUBLIC endpoints (no JWT required).
     * This chain handles /api/authenticate and other public endpoints WITHOUT OAuth2 Resource Server.
     * Order = 1 means this is checked FIRST.
     */
    @Bean
    @org.springframework.core.annotation.Order(1)
    public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/api/authenticate",
                "/api/register",
                "/api/account/reset-password/init",
                "/api/account/reset-password/finish",
                "/actuator/**",
                "/h2-console/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/api-docs/**",
                "/v3/api-docs/**",
                "/graphiql",
                "/graphiql/**",
                "/error"
            )
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }

    /**
     * Security filter chain for PROTECTED endpoints (JWT required).
     * This chain handles all other /api/** endpoints WITH OAuth2 Resource Server.
     * Order = 2 means this is checked AFTER the public chain.
     */
    @Bean
    @org.springframework.core.annotation.Order(2)
    public SecurityFilterChain protectedSecurityFilterChain(HttpSecurity http,
                                                             JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        http
            .securityMatcher(request -> {
                String uri = request.getRequestURI();
                // Match /api/** but exclude public endpoints
                if (uri.equals("/api/authenticate") ||
                    uri.equals("/api/register") ||
                    uri.startsWith("/api/account/reset-password")) {
                    return false;
                }
                return uri.startsWith("/api") || uri.equals("/graphql");
            })
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(authz -> authz
                // Admin endpoints
                .requestMatchers("/api/admin/**").hasAuthority(AuthoritiesConstants.ADMIN)

                // User management
                .requestMatchers(HttpMethod.POST, "/api/users").hasAuthority(AuthoritiesConstants.ADMIN)
                .requestMatchers(HttpMethod.PUT, "/api/users/**").hasAuthority(AuthoritiesConstants.ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/api/users/**").hasAuthority(AuthoritiesConstants.ADMIN)

                // Permission management
                .requestMatchers("/api/permissions/**").hasAuthority(AuthoritiesConstants.ADMIN)

                // All other API endpoints require authentication
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            );

        return http.build();
    }

    /**
     * Password encoder using BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * AuthenticationManager bean required for username/password authentication.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
