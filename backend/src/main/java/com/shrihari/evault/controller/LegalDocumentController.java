package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.AddDocumentVersionRequest;
import com.shrihari.evault.dto.CreateLegalDocumentRequest;
import com.shrihari.evault.dto.DocumentVersionResponse;
import com.shrihari.evault.dto.LegalDocumentResponse;
import com.shrihari.evault.model.DocumentVersion;
import com.shrihari.evault.service.LegalDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
@Tag(name = "Legal Documents", description = "Versioned management of investigation legal documents (FIRs, charge sheets, search warrants, forensic reports)")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'PROSECUTOR', 'COURT', 'SYSTEM_ADMIN')")
    @Operation(summary = "Create a new legal document record", description = "Uploads initial version (v1) of a legal document (e.g. FIR, charge sheet) with cryptographic SHA-256 sealing")
    public ResponseEntity<ApiResponse<LegalDocumentResponse>> createDocument(
            @RequestPart("metadata") @Valid CreateLegalDocumentRequest request,
            @RequestPart("file") MultipartFile file) throws IOException {
        LegalDocumentResponse response = legalDocumentService.createDocument(request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Legal document filed and sealed as version 1", response));
    }

    @PostMapping(value = "/{id}/versions", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'PROSECUTOR', 'COURT', 'SYSTEM_ADMIN')")
    @Operation(summary = "Add a new document revision", description = "Uploads an amended version (e.g. Supplementary Charge Sheet), increments version counter, and cryptographically signs the new version")
    public ResponseEntity<ApiResponse<DocumentVersionResponse>> addVersion(
            @PathVariable UUID id,
            @RequestPart("metadata") @Valid AddDocumentVersionRequest request,
            @RequestPart("file") MultipartFile file) throws IOException {
        DocumentVersionResponse response = legalDocumentService.addDocumentVersion(id, request, file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document revised successfully to version " + response.getVersionNumber(), response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get legal document details", description = "Retrieves document metadata along with its latest active version")
    public ResponseEntity<ApiResponse<LegalDocumentResponse>> getDocumentById(@PathVariable UUID id) {
        LegalDocumentResponse response = legalDocumentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/case/{caseId}")
    @Operation(summary = "List legal documents for a case", description = "Retrieves all filed legal documents for a case")
    public ResponseEntity<ApiResponse<List<LegalDocumentResponse>>> getDocumentsByCaseId(@PathVariable UUID caseId) {
        List<LegalDocumentResponse> responses = legalDocumentService.getDocumentsByCaseId(caseId);
        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "Get document revision history", description = "Retrieves complete revision history showing all versions, SHA-256 hashes, and change summaries")
    public ResponseEntity<ApiResponse<List<DocumentVersionResponse>>> getDocumentHistory(@PathVariable UUID id) {
        List<DocumentVersionResponse> history = legalDocumentService.getDocumentHistory(id);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download legal document file", description = "Streams the requested version (or latest version if omitted)")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable UUID id,
            @Parameter(description = "Optional version number (defaults to latest)")
            @RequestParam(required = false) Integer version) {
        Resource resource = legalDocumentService.loadDocumentFileResource(id, version);
        DocumentVersion versionMeta = legalDocumentService.getVersionMetadata(id, version);

        String contentType = versionMeta.getContentType() != null ? versionMeta.getContentType() : "application/octet-stream";
        String contentDisposition = "attachment; filename=\"" + versionMeta.getOriginalFileName() + "\"";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                .body(resource);
    }
}
