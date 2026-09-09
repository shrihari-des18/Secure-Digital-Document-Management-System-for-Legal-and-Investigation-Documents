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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "evidence_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceItem extends BaseEntity {

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Column(name = "evidence_number", nullable = false, unique = true, length = 64)
    private String evidenceNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "classification", nullable = false, length = 32)
    @Builder.Default
    private ClassificationLevel classification = ClassificationLevel.RESTRICTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "custody_state", nullable = false, length = 32)
    @Builder.Default
    private CustodyState custodyState = CustodyState.INGESTED;

    @Column(name = "device_make", length = 128)
    private String deviceMake;

    @Column(name = "device_model", length = 128)
    private String deviceModel;

    @Column(name = "device_serial_number", length = 128)
    private String deviceSerialNumber;

    @Column(name = "seized_location", length = 255)
    private String seizedLocation;

    @Column(name = "seized_at")
    private Instant seizedAt;

    @Column(name = "seized_by_officer_id")
    private UUID seizedByOfficerId;

    @Column(name = "current_custodian_id", nullable = false)
    private UUID currentCustodianId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;
}
