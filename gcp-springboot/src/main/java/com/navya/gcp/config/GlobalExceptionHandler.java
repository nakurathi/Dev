package com.navya.gcp.config;

import com.navya.gcp.secretmanager.SecretManagerService.SecretAccessException;
import com.navya.gcp.storage.GcsStorageService.GcsObjectNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;

/**
 * Global exception handler for the GCP integration application.
 * Returns RFC 7807 ProblemDetail responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GcsObjectNotFoundException.class)
    public ProblemDetail handleGcsNotFound(GcsObjectNotFoundException ex) {
        log.warn("GCS object not found: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setType(URI.create("/errors/gcs-not-found"));
        return pd;
    }

    @ExceptionHandler(SecretAccessException.class)
    public ProblemDetail handleSecretAccess(SecretAccessException ex) {
        log.error("Secret Manager error: {}", ex.getMessage());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to access secret. Check GCP permissions.");
        pd.setType(URI.create("/errors/secret-access"));
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        pd.setType(URI.create("/errors/bad-request"));
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred.");
        pd.setType(URI.create("/errors/internal"));
        return pd;
    }
}
