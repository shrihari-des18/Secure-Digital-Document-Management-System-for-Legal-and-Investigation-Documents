package com.shrihari.evault.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StampHashRequest {

    @NotBlank(message = "SHA-256 hash must not be blank")
    private String sha256;

    private String description;
}
