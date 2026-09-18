package com.shrihari.evault.service;

import com.shrihari.evault.service.storage.S3WormStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class S3WormStorageServiceTest {

    private S3WormStorageService s3WormStorageService;

    @BeforeEach
    void setUp() {
        s3WormStorageService = new S3WormStorageService(
                "http://localhost:9000",
                "evault-test-bucket",
                "minioadmin",
                "minioadmin",
                "us-east-1",
                365,
                "./target/test-s3-uploads"
        );
        s3WormStorageService.init();
    }

    @Test
    void testStoreAndRetrieveObject() throws IOException {
        byte[] data = "Top secret forensic evidence binary".getBytes();
        String path = s3WormStorageService.store(data, "evidence.bin", "cases/C-101");

        assertNotNull(path);
        assertTrue(path.contains("cases/C-101"));
        assertTrue(s3WormStorageService.exists(path));

        byte[] loaded = s3WormStorageService.loadAsBytes(path);
        assertArrayEquals(data, loaded);
    }
}
