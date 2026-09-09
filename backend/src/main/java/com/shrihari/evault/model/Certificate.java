package com.shrihari.evault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "certificates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Certificate extends BaseEntity {

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Column(name = "certificate_number", nullable = false, unique = true, length = 64)
    private String certificateNumber;

    @Column(name = "standard", nullable = false, length = 32)
    @Builder.Default
    private String standard = "BSA_2023_S63";

    @Column(name = "pdf_storage_path", nullable = false, length = 512)
    private String pdfStoragePath;

    @Column(name = "hash_snapshot_sha256", nullable = false, length = 64)
    private String hashSnapshotSha256;

    @Column(name = "hash_snapshot_sha512", length = 128)
    private String hashSnapshotSha512;

    @Column(name = "manner_of_production", columnDefinition = "TEXT")
    private String mannerOfProduction;

    @Column(name = "device_particulars", length = 255)
    private String deviceParticulars;

    @Column(name = "issued_by_officer_id", nullable = false)
    private UUID issuedByOfficerId;

    @Column(name = "issued_at", nullable = false)
    @Builder.Default
    private Instant issuedAt = Instant.now();

    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(name = "signing_key_id", length = 64)
    private String signingKeyId;
}
