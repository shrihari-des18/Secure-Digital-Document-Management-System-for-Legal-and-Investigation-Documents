package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.EvidenceDetailResponse;
import com.shrihari.evault.dto.IntegrityVerificationResponse;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.service.EvidenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
@Tag(name = "Evidence", description = "Digital evidence intake, cryptographic sealing, hashing, and retrieval")
public class EvidenceController {

    private final EvidenceService evidenceService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'SYSTEM_ADMIN')")
    @Operation(
            summary = "Upload and cryptographically seal digital evidence",
            description = "Receives evidence file, computes server-side SHA-256 and SHA-512 hashes, validates against client hash (if provided), digitally signs payload, and seals the record."
    )
    public ResponseEntity<ApiResponse<EvidenceDetailResponse>> uploadEvidence(
            @Parameter(description = "Evidence item metadata (JSON payload)", schema = @Schema(type = "string", format = "binary"))
            @RequestPart("metadata") @Valid UploadEvidenceRequest request,
            @Parameter(description = "Raw digital evidence file binary")
            @RequestPart("file") MultipartFile file) throws IOException {

        EvidenceDetailResponse response = evidenceService.uploadAndIngestEvidence(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Evidence ingested and sealed successfully", response));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'PROSECUTOR', 'COURT', 'RECORDS_CUSTODIAN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Get evidence details by ID", description = "Retrieves evidence item details, device metadata, and cryptographic hash summary")
    public ResponseEntity<ApiResponse<EvidenceDetailResponse>> getEvidenceById(@PathVariable UUID id) {
        EvidenceDetailResponse response = evidenceService.getEvidenceById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/case/{caseId}")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'PROSECUTOR', 'COURT', 'RECORDS_CUSTODIAN', 'SYSTEM_ADMIN')")
    @Operation(summary = "List all evidence items in a case", description = "Retrieves all evidence items associated with a specific investigation case")
    public ResponseEntity<ApiResponse<List<EvidenceDetailResponse>>> getEvidenceByCaseId(@PathVariable UUID caseId) {
        List<EvidenceDetailResponse> responses = evidenceService.getEvidenceByCaseId(caseId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'PROSECUTOR', 'COURT', 'RECORDS_CUSTODIAN', 'SYSTEM_ADMIN')")
    @Operation(summary = "Download evidence file", description = "Streams the sealed evidence file binary with proper MIME type and filename headers")
    public ResponseEntity<Resource> downloadEvidenceFile(@PathVariable UUID id) {
        Resource resource = evidenceService.loadEvidenceFileResource(id);
        EvidenceFile fileMeta = evidenceService.getEvidenceFileMetadata(id);

        String contentType = fileMeta.getContentType() != null ? fileMeta.getContentType() : "application/octet-stream";
        String contentDisposition = "attachment; filename=\"" + fileMeta.getOriginalFileName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }

    @PostMapping("/{id}/verify-integrity")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'PROSECUTOR', 'COURT', 'RECORDS_CUSTODIAN', 'SYSTEM_ADMIN', 'AUDITOR')")
    @Operation(
            summary = "Verify physical evidence integrity",
            description = "Re-reads the raw file from storage, recomputes the SHA-256 hash, and compares it against the ingestion hash snapshot"
    )
    public ResponseEntity<ApiResponse<IntegrityVerificationResponse>> verifyIntegrity(@PathVariable UUID id) {
        IntegrityVerificationResponse response = evidenceService.verifyEvidenceIntegrity(id);
        return ResponseEntity.ok(ApiResponse.success("Integrity verification completed", response));
    }
}
