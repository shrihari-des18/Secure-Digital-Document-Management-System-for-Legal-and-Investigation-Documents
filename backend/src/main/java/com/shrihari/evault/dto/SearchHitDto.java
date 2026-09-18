package com.shrihari.evault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchHitDto {
    private String id;
    private String title;
    private String type; // CASE, EVIDENCE, DOCUMENT
    private String snippet;
    private double score;
    private Map<String, Object> metadata;
}
