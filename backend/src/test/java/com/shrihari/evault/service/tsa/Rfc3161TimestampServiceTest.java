package com.shrihari.evault.service.tsa;

import com.shrihari.evault.dto.TimestampTokenDto;
import com.shrihari.evault.dto.TimestampVerificationResponse;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class Rfc3161TimestampServiceTest {

    private Rfc3161TimestampService timestampService;

    @BeforeEach
    void setUp() {
        timestampService = new Rfc3161TimestampService();
        ReflectionTestUtils.setField(timestampService, "tsaMode", "LOCAL");
        ReflectionTestUtils.setField(timestampService, "tsaUrl", "https://freetsa.org/tsr");
        ReflectionTestUtils.setField(timestampService, "timeoutMs", 3000);
        timestampService.init();
    }

    @Test
    @DisplayName("Successfully mints RFC 3161 TimeStampToken for SHA-256 hash")
    void timestampHash_success() throws Exception {
        String data = "Forensic digital evidence #42 - CCTV Frame";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
        String sha256Hex = Hex.toHexString(digest);

        TimestampTokenDto tokenDto = timestampService.timestampHash(sha256Hex, "Test CCTV Intake");

        assertThat(tokenDto).isNotNull();
        assertThat(tokenDto.getTokenBase64()).isNotBlank();
        assertThat(tokenDto.getHashedMessage()).isEqualTo(sha256Hex);
        assertThat(tokenDto.getHashAlgorithm()).isEqualTo("SHA-256");
        assertThat(tokenDto.isVerified()).isTrue();
        assertThat(tokenDto.getTsaAuthority()).contains("E-Vault Root Timestamp Authority");
        assertThat(tokenDto.getGenTime()).isBeforeOrEqualTo(Instant.now());
        assertThat(tokenDto.getGenTime()).isAfter(Instant.now().minus(1, ChronoUnit.MINUTES));
    }

    @Test
    @DisplayName("Successfully stamps raw byte payload")
    void timestampBytes_success() {
        byte[] payload = "Police Case Diary Exhibit A - FIR-2026-DEL-101".getBytes(StandardCharsets.UTF_8);

        TimestampTokenDto tokenDto = timestampService.timestampBytes(payload, "Case Diary");

        assertThat(tokenDto).isNotNull();
        assertThat(tokenDto.isVerified()).isTrue();
        assertThat(tokenDto.getTokenBase64()).isNotBlank();
    }

    @Test
    @DisplayName("Cryptographically verifies authentic RFC 3161 token")
    void verifyTimestampToken_authenticToken_returnsValid() throws Exception {
        String data = "Statutory BSA 2023 §63 Court Certificate Content";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
        String sha256Hex = Hex.toHexString(digest);

        TimestampTokenDto tokenDto = timestampService.timestampHash(sha256Hex, "BSA Certificate");

        TimestampVerificationResponse verification = timestampService.verifyTimestampToken(
                tokenDto.getTokenBase64(),
                sha256Hex
        );

        assertThat(verification.isValid()).isTrue();
        assertThat(verification.isHashMatches()).isTrue();
        assertThat(verification.isSignatureVerified()).isTrue();
        assertThat(verification.getSerialNumber()).isNotBlank();
        assertThat(verification.getMessage()).contains("cryptographically valid");
    }

    @Test
    @DisplayName("Fails verification when message imprint does not match altered hash")
    void verifyTimestampToken_tamperedHash_detectsMismatch() throws Exception {
        String data = "Original Untampered Data";
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(data.getBytes(StandardCharsets.UTF_8));
        String originalHex = Hex.toHexString(digest);

        TimestampTokenDto tokenDto = timestampService.timestampHash(originalHex, "Original Record");

        // Attempt verification with a tampered hash
        String tamperedHex = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

        TimestampVerificationResponse verification = timestampService.verifyTimestampToken(
                tokenDto.getTokenBase64(),
                tamperedHex
        );

        assertThat(verification.isValid()).isFalse();
        assertThat(verification.isHashMatches()).isFalse();
        assertThat(verification.getMessage()).contains("INTEGRITY MISMATCH");
    }

    @Test
    @DisplayName("Handles corrupted or invalid Base64 token gracefully")
    void verifyTimestampToken_corruptedToken_failsGracefully() {
        TimestampVerificationResponse response = timestampService.verifyTimestampToken(
                "NotAValidBase64TokenString==",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );

        assertThat(response.isValid()).isFalse();
        assertThat(response.isSignatureVerified()).isFalse();
        assertThat(response.getMessage()).isNotBlank();
    }

    @Test
    @DisplayName("Rejects blank or malformed hash inputs")
    void timestampHash_invalidInputs_throwsException() {
        assertThatThrownBy(() -> timestampService.timestampHash(null, "Test"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> timestampService.timestampHash("", "Test"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> timestampService.timestampHash("not-a-valid-sha256", "Test"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
