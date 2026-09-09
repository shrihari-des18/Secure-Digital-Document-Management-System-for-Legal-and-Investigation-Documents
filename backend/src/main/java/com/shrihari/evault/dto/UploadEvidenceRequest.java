package com.shrihari.evault.dto;

import com.shrihari.evault.model.ClassificationLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UploadEvidenceRequest {

    @NotNull(message = "Case ID is required")
    private UUID caseId;

    @NotBlank(message = "Evidence number is required")
    @Size(max = 64, message = "Evidence number cannot exceed 64 characters")
    private String evidenceNumber;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title cannot exceed 255 characters")
    private String title;

    private String description;

    @NotNull(message = "Classification level is required")
    private ClassificationLevel classification;

    private String deviceMake;
    private String deviceModel;
    private String deviceSerialNumber;
    private String seizedLocation;
    private Instant seizedAt;
    private UUID seizedByOfficerId;

    /**
     * Optional client-side SHA-256 hash computed via WebCrypto API before upload.
     */
    private String clientSha256;
}
