package com.shrihari.evault.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimestampVerificationResponse {
    private boolean valid;
    private boolean hashMatches;
    private boolean signatureVerified;
    private Instant genTime;
    private String tsaAuthority;
    private String serialNumber;
    private String message;
}
