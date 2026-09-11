package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.AiAnalysisResponse;
import com.shrihari.evault.dto.TriggerAnalysisRequest;
import com.shrihari.evault.service.AiAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
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
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
@Tag(name = "AI/ML Evidence Enrichment", description = "Asynchronous AI analysis pipeline (OCR text extraction, PII entity detection, auto-tagging, perceptual hashing)")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/{evidenceId}/analyze")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'SYSTEM_ADMIN')")
    @Operation(
            summary = "Trigger AI analysis on digital evidence",
            description = "Dispatches evidence to the AI microservice gateway for OCR text extraction, PII entity detection, classification tagging, and perceptual hashing. Stores AI annotations in a separate table, maintaining original evidence immutability."
    )
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> triggerAnalysis(
            @PathVariable UUID evidenceId,
            @RequestBody(required = false) TriggerAnalysisRequest request) {

        AiAnalysisResponse response = aiAnalysisService.triggerAnalysis(evidenceId, request);
        return ResponseEntity.ok(ApiResponse.success("AI evidence analysis executed successfully", response));
    }

    @GetMapping("/{evidenceId}/analysis")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'FORENSIC_ANALYST', 'PROSECUTOR', 'COURT', 'RECORDS_CUSTODIAN', 'AUDITOR', 'SYSTEM_ADMIN')")
    @Operation(
            summary = "Get AI analysis results for evidence",
            description = "Retrieves extracted OCR text, detected PII entities, classification tags, perceptual hashes, and confidence scores for a specific evidence item."
    )
    public ResponseEntity<ApiResponse<AiAnalysisResponse>> getAnalysis(@PathVariable UUID evidenceId) {
        AiAnalysisResponse response = aiAnalysisService.getAnalysisByEvidenceId(evidenceId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
