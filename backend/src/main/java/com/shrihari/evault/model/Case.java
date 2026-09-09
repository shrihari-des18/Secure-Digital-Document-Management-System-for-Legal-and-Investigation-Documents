package com.shrihari.evault.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Case extends BaseEntity {

    @Column(name = "case_number", nullable = false, unique = true, length = 64)
    private String caseNumber;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "jurisdiction", nullable = false, length = 128)
    private String jurisdiction;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private CaseStatus status = CaseStatus.OPEN;

    @Column(name = "lead_officer_id", nullable = false)
    private UUID leadOfficerId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "case_assigned_officers", joinColumns = @JoinColumn(name = "case_id"))
    @Column(name = "officer_id")
    @Builder.Default
    private Set<UUID> assignedOfficerIds = new HashSet<>();
}
