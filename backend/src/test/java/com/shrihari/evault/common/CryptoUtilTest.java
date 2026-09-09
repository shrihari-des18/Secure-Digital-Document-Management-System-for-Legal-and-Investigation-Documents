package com.shrihari.evault.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CryptoUtilTest {

    @Test
    @DisplayName("Compute correct SHA-256 hash for known test vector")
    void testSha256() {
        // SHA-256 of empty string is e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
        String hashEmpty = CryptoUtil.calculateSha256(new byte[0]);
        assertThat(hashEmpty).isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");

        // SHA-256 of "hello world"
        String hashHelloWorld = CryptoUtil.calculateSha256("hello world".getBytes(StandardCharsets.UTF_8));
        assertThat(hashHelloWorld).isEqualTo("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9");
    }

    @Test
    @DisplayName("Compute correct SHA-512 hash length")
    void testSha512() {
        String hash = CryptoUtil.calculateSha512("evidence-data".getBytes(StandardCharsets.UTF_8));
        assertThat(hash).hasSize(128);
    }

    @Test
    @DisplayName("Generate deterministic digital signature with secret key")
    void testDigitalSignature() {
        String data = "data-to-sign";
        String secret = "test-secret-key";

        String sig1 = CryptoUtil.generateSignature(data, secret);
        String sig2 = CryptoUtil.generateSignature(data, secret);

        assertThat(sig1).isNotNull();
        assertThat(sig1).isEqualTo(sig2);

        String sigDiff = CryptoUtil.generateSignature("tampered-data", secret);
        assertThat(sig1).isNotEqualTo(sigDiff);
    }
}
