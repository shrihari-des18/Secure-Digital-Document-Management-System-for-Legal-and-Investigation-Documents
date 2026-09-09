package com.shrihari.evault.dto;

import com.shrihari.evault.model.Certificate;
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
public class CertificateResponse {

    private UUID id;
    private UUID evidenceId;
    private String certificateNumber;
    private String standard;
    private String hashSnapshotSha256;
    private String hashSnapshotSha512;
    private String mannerOfProduction;
    private String deviceParticulars;
    private UUID issuedByOfficerId;
    private Instant issuedAt;
    private String digitalSignature;
    private String signingKeyId;

    public static CertificateResponse fromEntity(Certificate cert) {
        return CertificateResponse.builder()
                .id(cert.getId())
                .evidenceId(cert.getEvidenceId())
                .certificateNumber(cert.getCertificateNumber())
                .standard(cert.getStandard())
                .hashSnapshotSha256(cert.getHashSnapshotSha256())
                .hashSnapshotSha512(cert.getHashSnapshotSha512())
                .mannerOfProduction(cert.getMannerOfProduction())
                .deviceParticulars(cert.getDeviceParticulars())
                .issuedByOfficerId(cert.getIssuedByOfficerId())
                .issuedAt(cert.getIssuedAt())
                .digitalSignature(cert.getDigitalSignature())
                .signingKeyId(cert.getSigningKeyId())
                .build();
    }
}
