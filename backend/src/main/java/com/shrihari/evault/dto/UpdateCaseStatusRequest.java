package com.shrihari.evault.dto;

import com.shrihari.evault.model.CaseStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCaseStatusRequest {

    @NotNull(message = "Status is required")
    private CaseStatus status;

    private String reason;
}
