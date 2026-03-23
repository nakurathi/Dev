package com.navya.gcp.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * REST controller for GCS file operations.
 *
 * <p>Endpoints:
 * <pre>
 *   POST   /api/storage/upload              — upload file
 *   GET    /api/storage/download/{name}     — download file bytes
 *   GET    /api/storage/signed-url/{name}   — get temporary signed URL
 *   GET    /api/storage/list                — list all objects
 *   DELETE /api/storage/{name}              — delete object
 * </pre>
 */
@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class GcsController {

    private final GcsStorageService storageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "uploads") String folder) throws IOException {

        String objectName = folder + "/" + file.getOriginalFilename();
        String gcsPath = storageService.upload(file, objectName);
        return ResponseEntity.ok(Map.of("gcsPath", gcsPath, "objectName", objectName));
    }

    @GetMapping("/download/{objectName}")
    public ResponseEntity<byte[]> download(@PathVariable String objectName) {
        byte[] content = storageService.download(objectName);
        if (content.length == 0) return ResponseEntity.notFound().build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + objectName + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(content);
    }

    @GetMapping("/signed-url/{objectName}")
    public ResponseEntity<Map<String, String>> signedUrl(
            @PathVariable String objectName,
            @RequestParam(defaultValue = "15") int expiryMinutes) {

        URL url = storageService.generateSignedUrl(objectName, expiryMinutes);
        return ResponseEntity.ok(Map.of("signedUrl", url.toString(), "expiryMinutes", String.valueOf(expiryMinutes)));
    }

    @GetMapping("/list")
    public List<String> listObjects(
            @RequestParam(defaultValue = "") String prefix) {
        return storageService.listObjects(prefix);
    }

    @DeleteMapping("/{objectName}")
    public ResponseEntity<Map<String, Boolean>> delete(@PathVariable String objectName) {
        boolean deleted = storageService.delete(objectName);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}
