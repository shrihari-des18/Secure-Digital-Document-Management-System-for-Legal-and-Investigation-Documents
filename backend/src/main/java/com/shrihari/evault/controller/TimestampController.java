package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.StampHashRequest;
import com.shrihari.evault.dto.TimestampTokenDto;
import com.shrihari.evault.dto.TimestampVerificationResponse;
import com.shrihari.evault.dto.VerifyTimestampRequest;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.Certificate;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.repository.CertificateRepository;
import com.shrihari.evault.repository.EvidenceFileRepository;
import com.shrihari.evault.service.tsa.Rfc3161TimestampService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/timestamp")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "RFC 3161 Timestamp Authority (TSA)", description = "Cryptographic 3rd-party RFC 3161 proof-of-existence and non-repudiation timestamping for evidence hashes and BSA §63 certificates")
public class TimestampController {

    private final Rfc3161TimestampService timestampService;
    private final EvidenceFileRepository evidenceFileRepository;
    private final CertificateRepository certificateRepository;

    @PostMapping("/stamp-hash")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'SYSTEM_ADMIN', 'AUDITOR', 'COURT', 'PROSECUTOR', 'RECORDS_CUSTODIAN')")
    @Operation(summary = "Generate RFC 3161 timestamp token for SHA-256 hash", description = "Generates a certified RFC 3161 Time-Stamp Token (TST) for an arbitrary SHA-256 message digest")
    public ResponseEntity<ApiResponse<TimestampTokenDto>> stampHash(@Valid @RequestBody StampHashRequest request) {
        log.info("REST request to stamp SHA-256 hash: {}", request.getSha256());
        TimestampTokenDto result = timestampService.timestampHash(request.getSha256(), request.getDescription());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("RFC 3161 Timestamp Token generated successfully", result));
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify RFC 3161 timestamp token", description = "Verifies the cryptographic signature, message imprint, and TSA certificate for a Base64-encoded RFC 3161 token")
    public ResponseEntity<ApiResponse<TimestampVerificationResponse>> verifyToken(@Valid @RequestBody VerifyTimestampRequest request) {
        TimestampVerificationResponse result = timestampService.verifyTimestampToken(request.getTokenBase64(), request.getExpectedSha256());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/evidence/{evidenceId}")
    @Operation(summary = "Get and verify RFC 3161 timestamp for evidence item", description = "Retrieves the RFC 3161 token recorded during evidence intake and verifies its cryptographic seal against the current evidence hash")
    public ResponseEntity<ApiResponse<TimestampVerificationResponse>> getEvidenceTimestamp(@PathVariable UUID evidenceId) {
        EvidenceFile file = evidenceFileRepository.findByEvidenceId(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file record not found for ID: " + evidenceId));

        if (file.getTsaToken() == null || file.getTsaToken().isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(TimestampVerificationResponse.builder()
                    .valid(false)
                    .hashMatches(false)
                    .signatureVerified(false)
                    .message("No RFC 3161 timestamp token recorded for this evidence file")
                    .build()));
        }

        TimestampVerificationResponse verification = timestampService.verifyTimestampToken(file.getTsaToken(), file.getSha256());
        return ResponseEntity.ok(ApiResponse.success(verification));
    }

    @GetMapping("/certificate/{certificateId}")
    @Operation(summary = "Get and verify RFC 3161 timestamp for BSA §63 certificate", description = "Retrieves the RFC 3161 token recorded during certificate issuance and validates its seal against the certificate hash snapshot")
    public ResponseEntity<ApiResponse<TimestampVerificationResponse>> getCertificateTimestamp(@PathVariable UUID certificateId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + certificateId));

        if (certificate.getTsaToken() == null || certificate.getTsaToken().isBlank()) {
            return ResponseEntity.ok(ApiResponse.success(TimestampVerificationResponse.builder()
                    .valid(false)
                    .hashMatches(false)
                    .signatureVerified(false)
                    .message("No RFC 3161 timestamp token recorded for this certificate")
                    .build()));
        }

        TimestampVerificationResponse verification = timestampService.verifyTimestampToken(certificate.getTsaToken(), certificate.getHashSnapshotSha256());
        return ResponseEntity.ok(ApiResponse.success(verification));
    }
}
