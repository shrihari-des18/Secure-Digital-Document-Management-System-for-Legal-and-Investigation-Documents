package com.shrihari.evault.dto;

import com.shrihari.evault.model.CaseActivityType;
import com.shrihari.evault.model.RoleEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaseActivityEvent {

    @Builder.Default
    private UUID id = UUID.randomUUID();

    private UUID caseId;
    private CaseActivityType activityType;
    private String summary;
    private UUID actorId;
    private String actorUsername;
    private RoleEnum actorRole;
    private Map<String, Object> details;

    @Builder.Default
    private Instant timestamp = Instant.now();
}
