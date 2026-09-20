package com.shrihari.evault.dto;

import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TriggerTestAlarmRequest {

    @NotNull(message = "Alarm type is required")
    private SecurityAlarmType alarmType;

    @NotNull(message = "Severity level is required")
    private SecurityAlarmLevel severity;

    @NotBlank(message = "Resource reference is required")
    private String resourceRef;

    @NotBlank(message = "Alarm message is required")
    private String message;

    private Map<String, Object> details;

    private UUID caseId;
}
