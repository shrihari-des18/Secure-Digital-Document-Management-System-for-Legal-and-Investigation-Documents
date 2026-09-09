package com.shrihari.evault.dto;

import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseResponse {

    private UUID id;
    private String caseNumber;
    private String title;
    private String description;
    private String jurisdiction;
    private CaseStatus status;
    private UUID leadOfficerId;
    private Set<UUID> assignedOfficerIds;
    private Instant createdAt;
    private Instant updatedAt;

    public static CaseResponse fromEntity(Case c) {
        return CaseResponse.builder()
                .id(c.getId())
                .caseNumber(c.getCaseNumber())
                .title(c.getTitle())
                .description(c.getDescription())
                .jurisdiction(c.getJurisdiction())
                .status(c.getStatus())
                .leadOfficerId(c.getLeadOfficerId())
                .assignedOfficerIds(c.getAssignedOfficerIds())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }
}
