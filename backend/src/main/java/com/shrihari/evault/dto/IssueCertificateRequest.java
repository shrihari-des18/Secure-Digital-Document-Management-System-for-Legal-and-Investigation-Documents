package com.shrihari.evault.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCertificateRequest {

    @Size(max = 1024, message = "Manner of production cannot exceed 1024 characters")
    private String mannerOfProduction;

    @Size(max = 1024, message = "Certifying officer notes cannot exceed 1024 characters")
    private String certifyingOfficerNotes;
}
