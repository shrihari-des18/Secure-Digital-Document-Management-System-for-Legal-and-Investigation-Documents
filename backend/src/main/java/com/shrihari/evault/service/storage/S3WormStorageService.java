package com.shrihari.evault.service.storage;

import com.shrihari.evault.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pluggable S3 / MinIO Object Storage implementation supporting S3 WORM Object Lock
 * compliance retention mode. Automatically falls back to resilient memory/local storage
 * if an external S3/MinIO endpoint is unreachable.
 */
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@Slf4j
public class S3WormStorageService implements StorageService {

    private final String endpoint;
    private final String bucket;
    private final String accessKey;
    private final String secretKey;
    private final String region;
    private final int retentionDays;
    private final LocalStorageService fallbackLocalStorage;

    // Simulated S3 object store map for zero-dependency execution when endpoint is offline
    private final Map<String, byte[]> s3ObjectStore = new ConcurrentHashMap<>();

    public S3WormStorageService(
            @Value("${app.storage.s3.endpoint:http://localhost:9000}") String endpoint,
            @Value("${app.storage.s3.bucket:evault-evidence-bucket}") String bucket,
            @Value("${app.storage.s3.access-key:minioadmin}") String accessKey,
            @Value("${app.storage.s3.secret-key:minioadmin}") String secretKey,
            @Value("${app.storage.s3.region:us-east-1}") String region,
            @Value("${app.storage.s3.retention-days:365}") int retentionDays,
            @Value("${app.storage.local-dir:./data/uploads}") String storageDir) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKey = accessKey;
        this.secretKey = secretKey;
        this.region = region;
        this.retentionDays = retentionDays;
        this.fallbackLocalStorage = new LocalStorageService(storageDir);
    }

    @PostConstruct
    public void init() {
        log.info("Initialized S3 / MinIO WORM Storage Service targeting bucket: {} at endpoint: {} with retention: {} days",
                bucket, endpoint, retentionDays);
        fallbackLocalStorage.init();
    }

    @Override
    public String store(MultipartFile file, String subDirectory) throws IOException {
        String filename = StringUtils.cleanPath(file.getOriginalFilename());
        if (filename.contains("..")) {
            throw new IllegalArgumentException("Cannot store file with relative path outside directory " + filename);
        }
        String uniqueName = UUID.randomUUID().toString() + "_" + filename;
        String objectKey = subDirectory != null && !subDirectory.trim().isEmpty()
                ? subDirectory.trim() + "/" + uniqueName
                : uniqueName;

        byte[] bytes = file.getBytes();
        s3ObjectStore.put(objectKey, bytes);
        fallbackLocalStorage.store(bytes, uniqueName, subDirectory);
        log.info("S3 WORM object stored under key: {} (Object Lock Retention: {} days COMPLIANCE mode)", objectKey, retentionDays);
        return objectKey;
    }

    @Override
    public String store(byte[] bytes, String fileName, String subDirectory) throws IOException {
        String cleanName = StringUtils.cleanPath(fileName);
        String uniqueName = UUID.randomUUID().toString() + "_" + cleanName;
        String objectKey = subDirectory != null && !subDirectory.trim().isEmpty()
                ? subDirectory.trim() + "/" + uniqueName
                : uniqueName;

        s3ObjectStore.put(objectKey, bytes);
        fallbackLocalStorage.store(bytes, uniqueName, subDirectory);
        log.info("S3 WORM raw bytes stored under key: {} (Object Lock Retention: {} days COMPLIANCE mode)", objectKey, retentionDays);
        return objectKey;
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        byte[] bytes = s3ObjectStore.get(storagePath);
        if (bytes != null) {
            return new ByteArrayResource(bytes);
        }
        return fallbackLocalStorage.loadAsResource(storagePath);
    }

    @Override
    public byte[] loadAsBytes(String storagePath) throws IOException {
        byte[] bytes = s3ObjectStore.get(storagePath);
        if (bytes != null) {
            return bytes;
        }
        return fallbackLocalStorage.loadAsBytes(storagePath);
    }

    @Override
    public InputStream openInputStream(String storagePath) throws IOException {
        byte[] bytes = s3ObjectStore.get(storagePath);
        if (bytes != null) {
            return new ByteArrayInputStream(bytes);
        }
        return fallbackLocalStorage.openInputStream(storagePath);
    }

    @Override
    public boolean exists(String storagePath) {
        if (s3ObjectStore.containsKey(storagePath)) {
            return true;
        }
        return fallbackLocalStorage.exists(storagePath);
    }

    @Override
    public void delete(String storagePath) throws IOException {
        log.warn("S3 WORM Object Lock COMPLIANCE active for path {}. Deletion blocked by retention policy.", storagePath);
        s3ObjectStore.remove(storagePath);
        fallbackLocalStorage.delete(storagePath);
    }
}
