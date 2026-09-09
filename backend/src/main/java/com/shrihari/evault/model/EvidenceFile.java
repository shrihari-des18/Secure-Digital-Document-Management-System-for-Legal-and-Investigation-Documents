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
@Table(name = "evidence_files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceFile extends BaseEntity {

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "content_type", length = 128)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "storage_path", nullable = false, length = 512)
    private String storagePath;

    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;

    @Column(name = "sha512", length = 128)
    private String sha512;

    @Column(name = "client_sha256", length = 64)
    private String clientSha256;

    @Enumerated(EnumType.STRING)
    @Column(name = "integrity_status", nullable = false, length = 32)
    @Builder.Default
    private IntegrityStatus integrityStatus = IntegrityStatus.VERIFIED;

    @Column(name = "digital_signature", columnDefinition = "TEXT")
    private String digitalSignature;

    @Column(name = "signing_key_id", length = 64)
    private String signingKeyId;
}
