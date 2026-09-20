package com.shrihari.evault.dto;

import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.model.IntegrityStatus;
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
public class EvidenceFileResponse {

    private UUID fileId;
    private String originalFileName;
    private String contentType;
    private long sizeBytes;
    private String sha256;
    private String sha512;
    private String clientSha256;
    private IntegrityStatus integrityStatus;
    private String digitalSignature;
    private String signingKeyId;
    private String tsaToken;
    private Instant tsaTimestamp;
    private String tsaAuthority;
    private Instant createdAt;

    public static EvidenceFileResponse fromEntity(EvidenceFile file) {
        if (file == null) {
            return null;
        }
        return EvidenceFileResponse.builder()
                .fileId(file.getId())
                .originalFileName(file.getOriginalFileName())
                .contentType(file.getContentType())
                .sizeBytes(file.getSizeBytes())
                .sha256(file.getSha256())
                .sha512(file.getSha512())
                .clientSha256(file.getClientSha256())
                .integrityStatus(file.getIntegrityStatus())
                .digitalSignature(file.getDigitalSignature())
                .signingKeyId(file.getSigningKeyId())
                .tsaToken(file.getTsaToken())
                .tsaTimestamp(file.getTsaTimestamp())
                .tsaAuthority(file.getTsaAuthority())
                .createdAt(file.getCreatedAt())
                .build();
    }
}
