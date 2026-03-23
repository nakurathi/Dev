package com.navya.gcp.storage;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

/**
 * Service for Google Cloud Storage (GCS) operations.
 *
 * <p>Capabilities:
 * <ul>
 *   <li>Upload files (from MultipartFile or byte array)</li>
 *   <li>Download file content as byte array</li>
 *   <li>Generate short-lived signed URLs for direct browser access</li>
 *   <li>List all objects in a bucket (with optional prefix filter)</li>
 *   <li>Delete individual objects</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GcsStorageService {

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    // ── Upload ────────────────────────────────────────────────────────────────

    /**
     * Uploads a Spring {@link MultipartFile} to GCS.
     *
     * @param file       the incoming file from an HTTP multipart request
     * @param objectName the target GCS object key (e.g., "uploads/report.pdf")
     * @return the full GCS resource path gs://bucket/objectName
     */
    public String upload(MultipartFile file, String objectName) throws IOException {
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(file.getContentType())
                .build();

        Blob blob = storage.createFrom(blobInfo, file.getInputStream());
        String gcsPath = String.format("gs://%s/%s", bucketName, objectName);
        log.info("Uploaded file to GCS: {} ({} bytes)", gcsPath, blob.getSize());
        return gcsPath;
    }

    /**
     * Uploads raw bytes to GCS with an explicit content type.
     */
    public String uploadBytes(byte[] content, String objectName, String contentType) {
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        storage.create(blobInfo, content);
        String gcsPath = String.format("gs://%s/%s", bucketName, objectName);
        log.info("Uploaded bytes to GCS: {}", gcsPath);
        return gcsPath;
    }

    // ── Download ──────────────────────────────────────────────────────────────

    /**
     * Downloads a GCS object as a byte array.
     *
     * @param objectName the GCS object key
     * @return file bytes, or empty array if not found
     */
    public byte[] download(String objectName) {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) {
            log.warn("GCS object not found: {}/{}", bucketName, objectName);
            return new byte[0];
        }
        log.debug("Downloading GCS object: {}", objectName);
        return blob.getContent();
    }

    /**
     * Returns an InputStream for streaming large GCS objects.
     */
    public InputStream stream(String objectName) {
        Blob blob = storage.get(BlobId.of(bucketName, objectName));
        if (blob == null) throw new GcsObjectNotFoundException(bucketName, objectName);
        return blob.reader();
    }

    // ── Signed URL ────────────────────────────────────────────────────────────

    /**
     * Generates a V4 signed URL valid for {@code expiryMinutes}.
     * Use for temporary client-side access without exposing credentials.
     *
     * @param objectName     the GCS object key
     * @param expiryMinutes  how long the URL remains valid (max 7 days = 10080 min)
     * @return HTTPS signed URL
     */
    public URL generateSignedUrl(String objectName, int expiryMinutes) {
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, objectName)).build();
        URL url = storage.signUrl(blobInfo, expiryMinutes, TimeUnit.MINUTES,
                Storage.SignUrlOption.withV4Signature());
        log.info("Generated signed URL for {} (expires in {} min)", objectName, expiryMinutes);
        return url;
    }

    // ── List ──────────────────────────────────────────────────────────────────

    /**
     * Lists all object names in the configured bucket.
     */
    public List<String> listObjects() {
        return listObjects("");
    }

    /**
     * Lists object names in the bucket filtered by a prefix (folder path).
     *
     * @param prefix e.g., "uploads/" or "reports/2024/"
     */
    public List<String> listObjects(String prefix) {
        Storage.BlobListOption option = prefix.isBlank()
                ? Storage.BlobListOption.pageSize(100)
                : Storage.BlobListOption.prefix(prefix);

        return StreamSupport.stream(
                        storage.list(bucketName, option).iterateAll().spliterator(), false)
                .map(BlobInfo::getName)
                .toList();
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    /**
     * Deletes a GCS object. Returns true if deleted, false if not found.
     */
    public boolean delete(String objectName) {
        boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
        log.info("GCS delete objectName={} result={}", objectName, deleted);
        return deleted;
    }

    // ── Exceptions ────────────────────────────────────────────────────────────

    public static class GcsObjectNotFoundException extends RuntimeException {
        public GcsObjectNotFoundException(String bucket, String object) {
            super("GCS object not found: gs://" + bucket + "/" + object);
        }
    }
}
