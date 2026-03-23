package com.navya.gcp;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.navya.gcp.storage.GcsStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GcsStorageServiceTest {

    @Mock
    private Storage storage;

    @Mock
    private Blob blob;

    @InjectMocks
    private GcsStorageService gcsStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(gcsStorageService, "bucketName", "test-bucket");
    }

    @Test
    @DisplayName("download() returns bytes when object exists")
    void download_exists() {
        byte[] content = "file content".getBytes();
        when(storage.get(any(BlobId.class))).thenReturn(blob);
        when(blob.getContent()).thenReturn(content);

        byte[] result = gcsStorageService.download("path/to/file.txt");

        assertThat(result).isEqualTo(content);
    }

    @Test
    @DisplayName("download() returns empty array when object not found")
    void download_notFound() {
        when(storage.get(any(BlobId.class))).thenReturn(null);

        byte[] result = gcsStorageService.download("nonexistent.txt");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("delete() returns true when object deleted")
    void delete_success() {
        when(storage.delete(any(BlobId.class))).thenReturn(true);

        boolean deleted = gcsStorageService.delete("some/object.txt");

        assertThat(deleted).isTrue();
        verify(storage).delete(any(BlobId.class));
    }

    @Test
    @DisplayName("delete() returns false when object not found")
    void delete_notFound() {
        when(storage.delete(any(BlobId.class))).thenReturn(false);

        boolean deleted = gcsStorageService.delete("missing.txt");

        assertThat(deleted).isFalse();
    }
}
