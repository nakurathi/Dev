package com.navya.gcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the GCP + Spring Boot integration showcase.
 * Demonstrates: Pub/Sub, Cloud Spanner, Secret Manager, GCS.
 */
@SpringBootApplication
public class GcpApplication {

    public static void main(String[] args) {
        SpringApplication.run(GcpApplication.class, args);
    }
}
