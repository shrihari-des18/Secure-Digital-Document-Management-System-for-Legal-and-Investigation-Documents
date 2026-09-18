package com.shrihari.evault.service.crypto;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * Envelope Encryption Engine supporting AES-256-GCM payload encryption with
 * Vault KMS key wrapping. Guarantees raw evidence payloads are encrypted on disk.
 */
@Component
@Slf4j
public class EnvelopeEncryptionEngine {

    private static final int AES_KEY_SIZE_BITS = 256;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH_BYTES = 12;

    private final VaultKmsClientService kmsClientService;

    public EnvelopeEncryptionEngine(VaultKmsClientService kmsClientService) {
        this.kmsClientService = kmsClientService;
    }

    /**
     * Generates an ephemeral random 256-bit AES Data Encryption Key (DEK).
     */
    public byte[] generateDek() {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(AES_KEY_SIZE_BITS, new SecureRandom());
            return keyGen.generateKey().getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate DEK", e);
        }
    }

    /**
     * Encrypts plaintext bytes using AES-256-GCM with a random IV.
     * Returns combined IV + Ciphertext.
     */
    public byte[] encryptPayload(byte[] plaintext, byte[] rawDek) {
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            SecretKey secretKey = new SecretKeySpec(rawDek, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

            return combined;
        } catch (Exception e) {
            throw new IllegalStateException("Envelope encryption failed", e);
        }
    }

    /**
     * Decrypts ciphertext bytes using AES-256-GCM and raw DEK.
     */
    public byte[] decryptPayload(byte[] combinedIvAndCiphertext, byte[] rawDek) {
        try {
            if (combinedIvAndCiphertext.length <= IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Invalid encrypted payload length");
            }
            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] ciphertext = new byte[combinedIvAndCiphertext.length - IV_LENGTH_BYTES];
            System.arraycopy(combinedIvAndCiphertext, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combinedIvAndCiphertext, IV_LENGTH_BYTES, ciphertext, 0, ciphertext.length);

            SecretKey secretKey = new SecretKeySpec(rawDek, "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new IllegalStateException("Envelope decryption failed", e);
        }
    }

    public String wrapDek(byte[] rawDek) {
        return kmsClientService.wrapKey(rawDek);
    }

    public byte[] unwrapDek(String wrappedDekCiphertext) {
        return kmsClientService.unwrapKey(wrappedDekCiphertext);
    }

    public String getMasterKeyName() {
        return kmsClientService.getMasterKeyName();
    }
}
