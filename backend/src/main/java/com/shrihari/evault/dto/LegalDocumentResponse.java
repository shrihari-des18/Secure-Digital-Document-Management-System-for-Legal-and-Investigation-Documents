package com.shrihari.evault.dto;

import com.shrihari.evault.model.LegalDocument;
import com.shrihari.evault.model.LegalDocumentType;
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
public class LegalDocumentResponse {

    private UUID id;
    private UUID caseId;
    private String documentNumber;
    private String title;
    private String description;
    private LegalDocumentType documentType;
    private int currentVersion;
    private UUID createdBy;
    private DocumentVersionResponse latestVersion;
    private Instant createdAt;
    private Instant updatedAt;

    public static LegalDocumentResponse fromEntity(LegalDocument doc, DocumentVersionResponse latestVersion) {
        return LegalDocumentResponse.builder()
                .id(doc.getId())
                .caseId(doc.getCaseId())
                .documentNumber(doc.getDocumentNumber())
                .title(doc.getTitle())
                .description(doc.getDescription())
                .documentType(doc.getDocumentType())
                .currentVersion(doc.getCurrentVersion())
                .createdBy(doc.getCreatedBy())
                .latestVersion(latestVersion)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .build();
    }
}
