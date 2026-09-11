package com.shrihari.evault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalAiAnalysisResult {

    private boolean success;
    private String ocrText;
    private String detectedPii;
    private String classificationTags;
    private String perceptualHash;
    private Double confidenceScore;
    private String summary;
    private String errorMessage;
}
