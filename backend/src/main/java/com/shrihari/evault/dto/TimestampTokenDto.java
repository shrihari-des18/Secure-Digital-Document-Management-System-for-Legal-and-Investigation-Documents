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
public class TimestampTokenDto {
    private String tsaAuthority;
    private Instant genTime;
    private String serialNumber;
    private String hashAlgorithm;
    private String hashedMessage;
    private String tokenBase64;
    private String policyOid;
    private boolean verified;
}
