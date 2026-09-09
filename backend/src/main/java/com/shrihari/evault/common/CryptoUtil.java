package com.shrihari.evault.common;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
public final class CryptoUtil {

    private static final String SHA_256 = "SHA-256";
    private static final String SHA_512 = "SHA-512";

    private CryptoUtil() {
    }

    public static String calculateSha256(byte[] data) {
        return hash(data, SHA_256);
    }

    public static String calculateSha256(InputStream inputStream) throws IOException {
        return hashStream(inputStream, SHA_256);
    }

    public static String calculateSha512(byte[] data) {
        return hash(data, SHA_512);
    }

    public static String calculateSha512(InputStream inputStream) throws IOException {
        return hashStream(inputStream, SHA_512);
    }

    public static String calculateSha256(String text) {
        return calculateSha256(text.getBytes(StandardCharsets.UTF_8));
    }

    private static String hash(byte[] data, String algorithm) {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cryptographic algorithm not available: " + algorithm, e);
        }
    }

    private static String hashStream(InputStream inputStream, String algorithm) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance(algorithm);
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            byte[] hashBytes = digest.digest();
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cryptographic algorithm not available: " + algorithm, e);
        }
    }

    /**
     * Signs data using HMAC-SHA256 with a system signing key.
     * In production this can be delegated to Vault KMS / HSM.
     */
    public static String generateSignature(String dataToSign, String signingSecret) {
        try {
            String secret = (signingSecret != null && !signingSecret.isBlank())
                    ? signingSecret
                    : "DEFAULT_FALLBACK_SIGNING_KEY_SIH26190";
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] signedBytes = mac.doFinal(dataToSign.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(signedBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Error generating digital signature", e);
        }
    }
}
