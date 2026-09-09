package com.shrihari.evault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCaseRequest {

    @NotBlank(message = "Case number is required")
    @Size(max = 64, message = "Case number cannot exceed 64 characters")
    private String caseNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    @NotBlank(message = "Jurisdiction is required")
    @Size(max = 128, message = "Jurisdiction cannot exceed 128 characters")
    private String jurisdiction;

    @NotNull(message = "Lead officer ID is required")
    private UUID leadOfficerId;

    private Set<UUID> assignedOfficerIds;
}
