package com.shrihari.evault.service.storage;

import com.shrihari.evault.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.local-dir:./data/uploads}") String storageDir) {
        this.rootLocation = Paths.get(storageDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootLocation);
            log.info("Local storage directory initialized at: {}", rootLocation);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize storage directory", e);
        }
    }

    @Override
    public String store(MultipartFile file, String subDirectory) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Failed to store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed_file");
        if (originalFilename.contains("..")) {
            throw new IllegalArgumentException("Filename contains invalid relative path sequence: " + originalFilename);
        }

        String storedFileName = UUID.randomUUID() + "_" + originalFilename;
        Path targetDir = resolveDirectory(subDirectory);
        Files.createDirectories(targetDir);

        Path targetLocation = targetDir.resolve(storedFileName).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        String relativePath = rootLocation.relativize(targetLocation).toString();
        log.info("File stored successfully at: {}", relativePath);
        return relativePath;
    }

    @Override
    public String store(byte[] bytes, String fileName, String subDirectory) throws IOException {
        String cleanName = StringUtils.cleanPath(fileName);
        String storedFileName = UUID.randomUUID() + "_" + cleanName;
        Path targetDir = resolveDirectory(subDirectory);
        Files.createDirectories(targetDir);

        Path targetLocation = targetDir.resolve(storedFileName).normalize();
        try (InputStream inputStream = new ByteArrayInputStream(bytes)) {
            Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
        }

        return rootLocation.relativize(targetLocation).toString();
    }

    @Override
    public Resource loadAsResource(String storagePath) {
        try {
            Path file = rootLocation.resolve(storagePath).normalize();
            if (!file.startsWith(rootLocation)) {
                throw new SecurityException("Cannot access file outside current storage directory");
            }
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or unreadable: " + storagePath);
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path is invalid: " + storagePath);
        }
    }

    @Override
    public byte[] loadAsBytes(String storagePath) throws IOException {
        Path file = rootLocation.resolve(storagePath).normalize();
        if (!file.startsWith(rootLocation) || !Files.exists(file)) {
            throw new ResourceNotFoundException("File not found: " + storagePath);
        }
        return Files.readAllBytes(file);
    }

    @Override
    public InputStream openInputStream(String storagePath) throws IOException {
        Path file = rootLocation.resolve(storagePath).normalize();
        if (!file.startsWith(rootLocation) || !Files.exists(file)) {
            throw new ResourceNotFoundException("File not found: " + storagePath);
        }
        return Files.newInputStream(file);
    }

    @Override
    public boolean exists(String storagePath) {
        Path file = rootLocation.resolve(storagePath).normalize();
        return file.startsWith(rootLocation) && Files.exists(file);
    }

    @Override
    public void delete(String storagePath) throws IOException {
        Path file = rootLocation.resolve(storagePath).normalize();
        if (file.startsWith(rootLocation)) {
            Files.deleteIfExists(file);
        }
    }

    private Path resolveDirectory(String subDirectory) {
        if (subDirectory == null || subDirectory.isBlank()) {
            return rootLocation;
        }
        return rootLocation.resolve(subDirectory).normalize();
    }
}
