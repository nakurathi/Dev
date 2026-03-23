package com.navya.gcp.secretmanager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Demonstrates loading application secrets from GCP Secret Manager
 * at startup via Spring's property injection.
 *
 * <p>To enable this pattern, add to {@code application.yml}:
 * <pre>
 * spring:
 *   config:
 *     import: "sm://"   # activates Spring Cloud GCP Secret Manager property source
 *
 * db:
 *   password: ${sm://projects/MY_PROJECT/secrets/db-password}
 *   api-key:  ${sm://projects/MY_PROJECT/secrets/api-key/versions/latest}
 * </pre>
 *
 * Secrets are resolved at application startup, so any missing or
 * inaccessible secret will prevent the app from starting — which is
 * intentional (fail-fast on misconfiguration).
 */
@Slf4j
@Configuration
public class SecretManagerConfig {

    /**
     * Example: injected directly from GCP Secret Manager via sm:// URI.
     * In practice, replace the default with your actual secret path.
     */
    @Value("${db.password:PLACEHOLDER_REPLACE_IN_PROD}")
    private String dbPassword;

    @Value("${db.api-key:PLACEHOLDER_REPLACE_IN_PROD}")
    private String apiKey;

    @PostConstruct
    public void logSecretStatus() {
        // Never log the actual secret value — only confirm presence
        log.info("Secret Manager config loaded: dbPassword={}, apiKey={}",
                dbPassword.isEmpty() ? "MISSING" : "***loaded***",
                apiKey.isEmpty() ? "MISSING" : "***loaded***");
    }
}
