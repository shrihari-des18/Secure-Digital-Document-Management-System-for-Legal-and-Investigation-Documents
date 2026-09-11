package com.shrihari.evault.dto;

import com.shrihari.evault.model.AiAnalysis;
import com.shrihari.evault.model.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAnalysisResponse {

    private UUID id;
    private UUID evidenceId;
    private AnalysisStatus status;
    private String ocrText;
    private String detectedPii;
    private String classificationTags;
    private String perceptualHash;
    private Double confidenceScore;
    private String summary;
    private String failureReason;
    private Instant createdAt;
    private Instant updatedAt;

    public static AiAnalysisResponse fromEntity(AiAnalysis entity) {
        if (entity == null) return null;
        return AiAnalysisResponse.builder()
                .id(entity.getId())
                .evidenceId(entity.getEvidenceId())
                .status(entity.getStatus())
                .ocrText(entity.getOcrText())
                .detectedPii(entity.getDetectedPii())
                .classificationTags(entity.getClassificationTags())
                .perceptualHash(entity.getPerceptualHash())
                .confidenceScore(entity.getConfidenceScore())
                .summary(entity.getSummary())
                .failureReason(entity.getFailureReason())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
