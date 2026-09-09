package com.shrihari.evault.dto;

import com.shrihari.evault.model.DocumentVersion;
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
public class DocumentVersionResponse {

    private UUID id;
    private UUID documentId;
    private int versionNumber;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;
    private String sha256;
    private String changeSummary;
    private UUID uploadedBy;
    private String digitalSignature;
    private Instant createdAt;

    public static DocumentVersionResponse fromEntity(DocumentVersion v) {
        if (v == null) {
            return null;
        }
        return DocumentVersionResponse.builder()
                .id(v.getId())
                .documentId(v.getDocumentId())
                .versionNumber(v.getVersionNumber())
                .originalFileName(v.getOriginalFileName())
                .contentType(v.getContentType())
                .sizeBytes(v.getSizeBytes())
                .sha256(v.getSha256())
                .changeSummary(v.getChangeSummary())
                .uploadedBy(v.getUploadedBy())
                .digitalSignature(v.getDigitalSignature())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
