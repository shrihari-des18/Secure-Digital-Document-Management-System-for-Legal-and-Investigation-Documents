package com.shrihari.evault.dto;

import com.shrihari.evault.model.SecurityAlarm;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
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
public class SecurityAlarmResponse {

    private UUID id;
    private SecurityAlarmType alarmType;
    private SecurityAlarmLevel severity;
    private String resourceRef;
    private String message;
    private String details;
    private UUID caseId;
    private boolean acknowledged;
    private UUID acknowledgedBy;
    private Instant acknowledgedAt;
    private Instant triggeredAt;

    public static SecurityAlarmResponse fromEntity(SecurityAlarm entity) {
        if (entity == null) {
            return null;
        }
        return SecurityAlarmResponse.builder()
                .id(entity.getId())
                .alarmType(entity.getAlarmType())
                .severity(entity.getSeverity())
                .resourceRef(entity.getResourceRef())
                .message(entity.getMessage())
                .details(entity.getDetails())
                .caseId(entity.getCaseId())
                .acknowledged(entity.isAcknowledged())
                .acknowledgedBy(entity.getAcknowledgedBy())
                .acknowledgedAt(entity.getAcknowledgedAt())
                .triggeredAt(entity.getTriggeredAt())
                .build();
    }
}
