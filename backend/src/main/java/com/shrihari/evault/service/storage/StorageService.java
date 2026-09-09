package com.shrihari.evault.service.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

public interface StorageService {

    /**
     * Stores a multipart file under a relative subdirectory.
     * @return The stored relative file path or object key.
     */
    String store(MultipartFile file, String subDirectory) throws IOException;

    /**
     * Stores raw bytes with a given filename under a relative subdirectory.
     * @return The stored relative file path or object key.
     */
    String store(byte[] bytes, String fileName, String subDirectory) throws IOException;

    /**
     * Loads file as a Spring Resource for download streaming.
     */
    Resource loadAsResource(String storagePath);

    /**
     * Loads file content as byte array.
     */
    byte[] loadAsBytes(String storagePath) throws IOException;

    /**
     * Opens an InputStream to the stored file.
     */
    InputStream openInputStream(String storagePath) throws IOException;

    /**
     * Checks whether the file exists in storage.
     */
    boolean exists(String storagePath);

    /**
     * Deletes the file if permitted by retention policy.
     */
    void delete(String storagePath) throws IOException;
}
