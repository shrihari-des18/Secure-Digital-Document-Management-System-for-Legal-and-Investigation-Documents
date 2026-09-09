package com.shrihari.evault.dto;

import com.shrihari.evault.model.IntegrityStatus;
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
public class IntegrityVerificationResponse {

    private UUID evidenceId;
    private String evidenceNumber;
    private String recordedSha256;
    private String calculatedSha256;
    private boolean match;
    private IntegrityStatus integrityStatus;
    private String digitalSignature;
    private String message;
    private Instant verifiedAt;
}
