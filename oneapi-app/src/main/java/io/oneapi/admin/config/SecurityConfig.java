package io.oneapi.admin.config;

import io.oneapi.admin.security.security.AuthoritiesConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
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
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

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
     * Configures the security filter chain with JWT authentication.
     *
     * Security Rules:
     * - /api/authenticate - Public (login endpoint to get JWT)
     * - /api/register - Public (user registration)
     * - /actuator/** - Public (health checks, metrics)
     * - /h2-console/** - Public (development only)
     * - /swagger-ui/** - Public (API documentation)
     * - /api-docs/** - Public (OpenAPI spec)
     * - /graphiql - Public (GraphQL IDE)
     * - /api/admin/** - Requires ADMIN role
     * - All other /api/** endpoints require authentication
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, HandlerMappingIntrospector introspector,
                                                    JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
        MvcRequestMatcher.Builder mvcMatcherBuilder = new MvcRequestMatcher.Builder(introspector);

        http
            .cors(Customizer.withDefaults())
            .csrf(csrf -> csrf
                // Disable CSRF for H2 console and API endpoints (using JWT tokens)
                .ignoringRequestMatchers(
                    mvcMatcherBuilder.pattern("/h2-console/**"),
                    mvcMatcherBuilder.pattern("/api/**"),
                    mvcMatcherBuilder.pattern("/graphql/**")
                )
            )
            .authorizeHttpRequests(authz -> authz
                // Public endpoints
                .requestMatchers(
                    mvcMatcherBuilder.pattern("/api/authenticate"),
                    mvcMatcherBuilder.pattern("/api/register"),
                    mvcMatcherBuilder.pattern("/api/account/reset-password/init"),
                    mvcMatcherBuilder.pattern("/api/account/reset-password/finish")
                ).permitAll()
                .requestMatchers(
                    mvcMatcherBuilder.pattern("/actuator/**"),
                    mvcMatcherBuilder.pattern("/h2-console/**"),
                    mvcMatcherBuilder.pattern("/swagger-ui/**"),
                    mvcMatcherBuilder.pattern("/swagger-ui.html"),
                    mvcMatcherBuilder.pattern("/api-docs/**"),
                    mvcMatcherBuilder.pattern("/v3/api-docs/**"),
                    mvcMatcherBuilder.pattern("/graphiql"),
                    mvcMatcherBuilder.pattern("/graphiql/**"),
                    mvcMatcherBuilder.pattern("/error")
                ).permitAll()

                // Admin endpoints - require ADMIN role
                .requestMatchers(mvcMatcherBuilder.pattern("/api/admin/**")).hasAuthority(AuthoritiesConstants.ADMIN)

                // User management - require ADMIN role
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.POST, "/api/users")).hasAuthority(AuthoritiesConstants.ADMIN)
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.PUT, "/api/users/**")).hasAuthority(AuthoritiesConstants.ADMIN)
                .requestMatchers(mvcMatcherBuilder.pattern(HttpMethod.DELETE, "/api/users/**")).hasAuthority(AuthoritiesConstants.ADMIN)

                // Permission management - require ADMIN role
                .requestMatchers(mvcMatcherBuilder.pattern("/api/permissions/**")).hasAuthority(AuthoritiesConstants.ADMIN)

                // All other API endpoints require authentication
                .requestMatchers(mvcMatcherBuilder.pattern("/api/**")).authenticated()
                .requestMatchers(mvcMatcherBuilder.pattern("/graphql")).authenticated()
            )
            // Stateless session management (JWT)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            // JWT authentication with custom authority extraction
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
            )
            // Exception handling
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
            )
            // Frame options for H2 Console
            .headers(headers -> headers
                .frameOptions(frameOptions -> frameOptions.sameOrigin())
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
}
