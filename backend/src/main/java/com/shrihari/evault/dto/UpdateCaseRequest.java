package com.shrihari.evault.dto;

import jakarta.validation.constraints.NotBlank;
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
public class UpdateCaseRequest {

    @NotBlank(message = "Title cannot be blank")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    @NotBlank(message = "Jurisdiction cannot be blank")
    @Size(max = 128, message = "Jurisdiction cannot exceed 128 characters")
    private String jurisdiction;

    private Set<UUID> assignedOfficerIds;
}
