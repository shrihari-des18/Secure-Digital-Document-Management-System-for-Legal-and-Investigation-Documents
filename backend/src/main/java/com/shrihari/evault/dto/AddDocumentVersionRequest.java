package com.shrihari.evault.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDocumentVersionRequest {

    @NotBlank(message = "Change summary is required to explain this revision")
    @Size(max = 512, message = "Change summary cannot exceed 512 characters")
    private String changeSummary;
}
