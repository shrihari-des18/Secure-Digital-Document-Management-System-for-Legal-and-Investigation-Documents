package com.shrihari.evault.dto;

import com.shrihari.evault.model.LegalDocumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLegalDocumentRequest {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotBlank(message = "Document number is required")
    @Size(max = 64, message = "Document number cannot exceed 64 characters")
    private String documentNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Document type is required")
    private LegalDocumentType documentType;

    @Builder.Default
    private String changeSummary = "Initial document filing";
}
