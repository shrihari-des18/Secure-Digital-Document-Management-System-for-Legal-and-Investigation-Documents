package com.shrihari.evault.dto;

import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceItem;
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
public class EvidenceDetailResponse {

    private UUID id;
    private UUID caseId;
    private String evidenceNumber;
    private String title;
    private String description;
    private ClassificationLevel classification;
    private CustodyState custodyState;
    private String deviceMake;
    private String deviceModel;
    private String deviceSerialNumber;
    private String seizedLocation;
    private Instant seizedAt;
    private UUID seizedByOfficerId;
    private UUID currentCustodianId;
    private UUID createdBy;
    private EvidenceFileResponse file;
    private Instant createdAt;
    private Instant updatedAt;

    public static EvidenceDetailResponse fromEntity(EvidenceItem item, EvidenceFileResponse fileResponse) {
        return EvidenceDetailResponse.builder()
                .id(item.getId())
                .caseId(item.getCaseId())
                .evidenceNumber(item.getEvidenceNumber())
                .title(item.getTitle())
                .description(item.getDescription())
                .classification(item.getClassification())
                .custodyState(item.getCustodyState())
                .deviceMake(item.getDeviceMake())
                .deviceModel(item.getDeviceModel())
                .deviceSerialNumber(item.getDeviceSerialNumber())
                .seizedLocation(item.getSeizedLocation())
                .seizedAt(item.getSeizedAt())
                .seizedByOfficerId(item.getSeizedByOfficerId())
                .currentCustodianId(item.getCurrentCustodianId())
                .createdBy(item.getCreatedBy())
                .file(fileResponse)
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }
}
