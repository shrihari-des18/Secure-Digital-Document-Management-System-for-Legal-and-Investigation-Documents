package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.CertificateResponse;
import com.shrihari.evault.dto.IssueCertificateRequest;
import com.shrihari.evault.model.Certificate;
import com.shrihari.evault.service.CertificateService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/certificates")
@RequiredArgsConstructor
@Tag(name = "Evidence Certification (§63 BSA)", description = "Statutory electronic record certification under Bharatiya Sakshya Adhiniyam, 2023 §63 for courtroom admissibility")
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/evidence/{evidenceId}")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'PROSECUTOR', 'COURT', 'RECORDS_CUSTODIAN', 'SYSTEM_ADMIN')")
    @Operation(
            summary = "Issue BSA 2023 §63 court certificate",
            description = "Auto-generates a statutory court admissibility certificate as a digitally signed PDF with device particulars, SHA-256/512 hash snapshot, manner of production, and custody history."
    )
    public ResponseEntity<ApiResponse<CertificateResponse>> issueCertificate(
            @PathVariable UUID evidenceId,
            @Valid @RequestBody(required = false) IssueCertificateRequest request) throws IOException {

        CertificateResponse response = certificateService.issueCertificate(evidenceId, request != null ? request : new IssueCertificateRequest());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Statutory BSA §63 certificate issued and digitally signed", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get certificate metadata", description = "Retrieves certificate metadata, hash snapshots, and signing details")
    public ResponseEntity<ApiResponse<CertificateResponse>> getCertificateById(@PathVariable UUID id) {
        CertificateResponse response = certificateService.getCertificateById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/evidence/{evidenceId}")
    @Operation(summary = "List certificates for evidence", description = "Retrieves all statutory certificates issued for an evidence item")
    public ResponseEntity<ApiResponse<List<CertificateResponse>>> getCertificatesByEvidenceId(@PathVariable UUID evidenceId) {
        List<CertificateResponse> responses = certificateService.getCertificatesByEvidenceId(evidenceId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download court certificate PDF", description = "Streams the court-ready digitally signed BSA §63 certificate PDF")
    public ResponseEntity<Resource> downloadCertificatePdf(@PathVariable UUID id) {
        Resource resource = certificateService.loadCertificatePdfResource(id);
        Certificate cert = certificateService.getEntityById(id);

        String filename = cert.getCertificateNumber() + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
