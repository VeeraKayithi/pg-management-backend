package com.pgcompliance.config;

import com.pgcompliance.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http) throws Exception {

                http
                                .cors(cors -> {
                                })

                                .csrf(csrf -> csrf.disable())

                                .sessionManagement(session -> session.sessionCreationPolicy(
                                                SessionCreationPolicy.STATELESS))

                                .authorizeHttpRequests(auth -> auth

                                                /*
                                                 * CORS preflight
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.OPTIONS,
                                                                "/**")
                                                .permitAll()

                                                /*
                                                 * Public login
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/auth/login")
                                                .permitAll()

                                                /*
                                                 * Public Tenant account activation
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/auth/activation/validate",
                                                                "/api/v1/auth/activation/complete",
                                                                "/api/v1/auth/activation/resend")
                                                .permitAll()

                                                /*
                                                 * Health check
                                                 */
                                                .requestMatchers(
                                                                "/actuator/health")
                                                .permitAll()

                                                /*
                                                 * Tenant self-profile
                                                 *
                                                 * Keep this BEFORE /api/v1/tenants/**
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/tenants/me")
                                                .hasRole("TENANT")

                                                /*
                                                 * Notifications
                                                 *
                                                 * ADMIN
                                                 * BUILDING_ADMIN
                                                 * SUPERVISOR
                                                 * TENANT
                                                 *
                                                 * Service layer still determines which
                                                 * notifications belong to the user.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/notifications/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN",
                                                                "SUPERVISOR",
                                                                "TENANT")

                                                /*
                                                 * Communication Center
                                                 *
                                                 * Keep Super Admin only for now.
                                                 * We can define building-specific
                                                 * communication permissions later.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/admin/announcements/**")
                                                .hasRole("ADMIN")

                                                /*
                                                 * Tenant portal-account creation
                                                 *
                                                 * Super Admin only.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/users/tenant-account")
                                                .hasRole("ADMIN")

                                                /*
                                                 * =================================================
                                                 * BUILDINGS
                                                 * =================================================
                                                 */

                                                /*
                                                 * Create building
                                                 *
                                                 * Super Admin only.
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/buildings",
                                                                "/api/v1/buildings/")
                                                .hasRole("ADMIN")

                                                /*
                                                 * Delete building
                                                 *
                                                 * Super Admin only.
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/v1/buildings/**")
                                                .hasRole("ADMIN")

                                                /*
                                                 * Update building
                                                 *
                                                 * ADMIN:
                                                 * any building
                                                 *
                                                 * BUILDING_ADMIN:
                                                 * assigned building only
                                                 *
                                                 * BuildingAccessService performs the
                                                 * actual building ownership check.
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/v1/buildings/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/v1/buildings/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                /*
                                                 * Read buildings
                                                 *
                                                 * ADMIN:
                                                 * all buildings
                                                 *
                                                 * BUILDING_ADMIN:
                                                 * assigned building
                                                 *
                                                 * SUPERVISOR:
                                                 * assigned building
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/buildings/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN",
                                                                "SUPERVISOR")

                                                /*
                                                 * =================================================
                                                 * ROOMS
                                                 * =================================================
                                                 */

                                                /*
                                                 * Room writes
                                                 *
                                                 * ADMIN + BUILDING_ADMIN
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/rooms/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/v1/rooms/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/v1/rooms/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/v1/rooms/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                /*
                                                 * Room reads
                                                 *
                                                 * ADMIN
                                                 * BUILDING_ADMIN
                                                 * SUPERVISOR
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/rooms/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN",
                                                                "SUPERVISOR")

                                                /*
                                                 * =================================================
                                                 * TENANTS
                                                 * =================================================
                                                 */

                                                /*
                                                 * Tenant writes
                                                 *
                                                 * ADMIN + BUILDING_ADMIN
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/v1/tenants/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/v1/tenants/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PATCH,
                                                                "/api/v1/tenants/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/v1/tenants/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN")

                                                /*
                                                 * Tenant reads
                                                 *
                                                 * ADMIN
                                                 * BUILDING_ADMIN
                                                 * SUPERVISOR
                                                 */
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/v1/tenants/**")
                                                .hasAnyRole(
                                                                "ADMIN",
                                                                "BUILDING_ADMIN",
                                                                "SUPERVISOR")

                                                /*
                                                 * =================================================
                                                 * USER MANAGEMENT
                                                 * =================================================
                                                 *
                                                 * Super Admin only.
                                                 */
                                                .requestMatchers(
                                                                "/api/v1/users/**")
                                                .hasRole("ADMIN")

                                                /*
                                                 * Everything else requires authentication.
                                                 */
                                                .anyRequest()
                                                .authenticated())

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}