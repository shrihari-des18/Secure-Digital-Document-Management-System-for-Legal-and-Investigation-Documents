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
@Table(name = "legal_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalDocument extends BaseEntity {

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "document_number", nullable = false, unique = true, length = 64)
    private String documentNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 32)
    private LegalDocumentType documentType;

    @Column(name = "current_version", nullable = false)
    @Builder.Default
    private int currentVersion = 1;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
