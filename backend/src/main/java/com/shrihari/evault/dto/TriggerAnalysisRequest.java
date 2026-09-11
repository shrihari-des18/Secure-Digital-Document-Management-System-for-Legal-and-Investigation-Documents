package com.shrihari.evault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerAnalysisRequest {

    @Builder.Default
    private boolean extractOcr = true;

    @Builder.Default
    private boolean detectPii = true;

    @Builder.Default
    private boolean generateTags = true;
}
