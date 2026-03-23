package com.navya.gcp.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 6 configuration — stateless JWT resource server.
 *
 * <p>Integrates with GCP Identity Platform or any OIDC provider.
 * JWT tokens issued by GCP have the JWKS at:
 * {@code https://www.googleapis.com/oauth2/v3/certs}
 *
 * <p>Access rules:
 * <ul>
 *   <li>Actuator health/info — public (for load balancer probes)</li>
 *   <li>GET reads          — require authenticated user</li>
 *   <li>POST/PATCH/DELETE  — require {@code ROLE_CLINICIAN} or {@code ROLE_ADMIN}</li>
 *   <li>Billing endpoints  — require {@code ROLE_BILLING} or {@code ROLE_ADMIN}</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Stateless — no session, no CSRF needed
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            .authorizeHttpRequests(auth -> auth
                // Public — actuator probes (GKE liveness/readiness)
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/prometheus").permitAll()

                // Read-only: any authenticated user
                .requestMatchers(HttpMethod.GET, "/api/v1/**").authenticated()

                // Write ops: clinician or admin only
                .requestMatchers(HttpMethod.POST,   "/api/v1/appointments/**").hasAnyRole("CLINICIAN", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,  "/api/v1/appointments/**").hasAnyRole("CLINICIAN", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/appointments/**").hasAnyRole("CLINICIAN", "ADMIN")

                // Billing: finance role or admin
                .requestMatchers("/api/v1/billing/**").hasAnyRole("BILLING", "ADMIN")

                // Pub/Sub + storage management: admin only
                .requestMatchers("/api/pubsub/**", "/api/storage/**").hasRole("ADMIN")

                .anyRequest().authenticated()
            )

            // JWT resource server — validates Bearer tokens
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                    .decoder(jwtDecoder())
                    .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
            );

        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }

    /**
     * Maps JWT {@code roles} claim to Spring Security {@code ROLE_} authorities.
     * GCP Identity Platform encodes roles in the {@code roles} claim.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authConverter = new JwtGrantedAuthoritiesConverter();
        authConverter.setAuthoritiesClaimName("roles");
        authConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authConverter);
        return converter;
    }
}
