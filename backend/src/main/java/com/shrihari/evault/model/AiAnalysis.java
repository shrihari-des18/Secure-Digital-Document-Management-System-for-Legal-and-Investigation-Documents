package com.shrihari.evault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "ai_analyses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysis extends BaseEntity {

    @Column(name = "evidence_id", nullable = false, unique = true)
    private UUID evidenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private AnalysisStatus status;

    @Column(name = "ocr_text", columnDefinition = "TEXT")
    private String ocrText;

    @Column(name = "detected_pii", columnDefinition = "TEXT")
    private String detectedPii;

    @Column(name = "classification_tags", columnDefinition = "TEXT")
    private String classificationTags;

    @Column(name = "perceptual_hash", length = 128)
    private String perceptualHash;

    @Column(name = "confidence_score")
    private Double confidenceScore;

    @Column(name = "summary", length = 1024)
    private String summary;

    @Column(name = "failure_reason", length = 512)
    private String failureReason;
}
