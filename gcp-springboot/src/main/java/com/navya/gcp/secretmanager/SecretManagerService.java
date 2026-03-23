package com.navya.gcp.secretmanager;

import com.google.cloud.secretmanager.v1.*;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Service for interacting with GCP Secret Manager.
 *
 * <p>Provides:
 * <ul>
 *   <li>Fetching a secret value by name</li>
 *   <li>Creating a new secret with an initial version</li>
 *   <li>Adding a new version to an existing secret</li>
 *   <li>Disabling (soft-deleting) a secret version</li>
 * </ul>
 *
 * <p><b>Note:</b> For Spring Boot property injection via Secret Manager,
 * prefix property names with {@code sm://} in {@code application.yml}:
 * <pre>
 *   my.db.password: ${sm://projects/MY_PROJECT/secrets/db-password/versions/latest}
 * </pre>
 */
@Slf4j
@Service
public class SecretManagerService {

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    /**
     * Fetches the latest version of a secret's payload as a String.
     *
     * @param secretName the secret's resource name (without project prefix)
     * @return the secret value as a UTF-8 string
     */
    public String getSecret(String secretName) {
        String resourceName = String.format(
                "projects/%s/secrets/%s/versions/latest", projectId, secretName);

        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            AccessSecretVersionResponse response = client.accessSecretVersion(resourceName);
            String value = response.getPayload().getData().toStringUtf8();
            log.debug("Fetched secret: {}", secretName);
            return value;
        } catch (IOException e) {
            log.error("Failed to fetch secret={}: {}", secretName, e.getMessage(), e);
            throw new SecretAccessException("Could not retrieve secret: " + secretName, e);
        }
    }

    /**
     * Creates a new secret and stores its first version.
     *
     * @param secretName  unique ID for the secret within the project
     * @param secretValue the secret payload
     */
    public void createSecret(String secretName, String secretValue) {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            ProjectName projectRef = ProjectName.of(projectId);

            Secret secret = Secret.newBuilder()
                    .setReplication(Replication.newBuilder()
                            .setAutomatic(Replication.Automatic.newBuilder().build())
                            .build())
                    .build();

            Secret createdSecret = client.createSecret(projectRef, secretName, secret);
            log.info("Created secret: {}", createdSecret.getName());

            addSecretVersion(client, createdSecret.getName(), secretValue);

        } catch (IOException e) {
            log.error("Failed to create secret={}: {}", secretName, e.getMessage(), e);
            throw new SecretAccessException("Could not create secret: " + secretName, e);
        }
    }

    /**
     * Adds a new version to an existing secret.
     *
     * @param secretResourceName full resource name of the secret
     * @param newValue           new payload to store
     */
    public void addSecretVersion(String secretResourceName, String newValue) {
        try (SecretManagerServiceClient client = SecretManagerServiceClient.create()) {
            addSecretVersion(client, secretResourceName, newValue);
        } catch (IOException e) {
            throw new SecretAccessException("Could not add secret version: " + secretResourceName, e);
        }
    }

    // ── Internal helpers ──────────────────────────────────────────────────────

    private void addSecretVersion(SecretManagerServiceClient client,
                                   String secretName,
                                   String value) {
        SecretPayload payload = SecretPayload.newBuilder()
                .setData(ByteString.copyFromUtf8(value))
                .build();
        SecretVersion version = client.addSecretVersion(secretName, payload);
        log.info("Added secret version: {}", version.getName());
    }

    /** Unchecked wrapper for secret access failures. */
    public static class SecretAccessException extends RuntimeException {
        public SecretAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
