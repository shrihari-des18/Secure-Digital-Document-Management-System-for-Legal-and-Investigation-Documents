package com.shrihari.evault.service;

import com.shrihari.evault.service.crypto.EnvelopeEncryptionEngine;
import com.shrihari.evault.service.crypto.VaultKmsClientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EnvelopeEncryptionEngineTest {

    private EnvelopeEncryptionEngine encryptionEngine;

    @BeforeEach
    void setUp() {
        VaultKmsClientService kmsClientService = new VaultKmsClientService(
                "http://localhost:8200",
                "root",
                "evault-master-kek"
        );
        encryptionEngine = new EnvelopeEncryptionEngine(kmsClientService);
    }

    @Test
    void testEnvelopeEncryptionAndDecryptionRoundTrip() {
        byte[] plaintext = "CONFIDENTIAL_FINANCIAL_LOGS_2026".getBytes();
        byte[] dek = encryptionEngine.generateDek();

        assertNotNull(dek);
        assertEquals(32, dek.length); // 256 bits

        byte[] encryptedPayload = encryptionEngine.encryptPayload(plaintext, dek);
        assertNotNull(encryptedPayload);
        assertNotEquals(new String(plaintext), new String(encryptedPayload));

        byte[] decryptedPayload = encryptionEngine.decryptPayload(encryptedPayload, dek);
        assertArrayEquals(plaintext, decryptedPayload);
    }

    @Test
    void testVaultKmsDekWrappingAndUnwrapping() {
        byte[] rawDek = encryptionEngine.generateDek();
        String wrappedDek = encryptionEngine.wrapDek(rawDek);

        assertNotNull(wrappedDek);
        assertTrue(wrappedDek.startsWith("vault:v1:"));

        byte[] unwrappedDek = encryptionEngine.unwrapDek(wrappedDek);
        assertArrayEquals(rawDek, unwrappedDek);
    }
}
