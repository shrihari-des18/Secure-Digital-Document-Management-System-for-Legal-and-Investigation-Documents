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
public class VerifyTimestampRequest {

    @NotBlank(message = "Token Base64 must not be blank")
    private String tokenBase64;

    @NotBlank(message = "Expected SHA-256 must not be blank")
    private String expectedSha256;
}
