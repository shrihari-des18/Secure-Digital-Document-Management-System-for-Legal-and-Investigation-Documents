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
public class LedgerVerificationResult {

    private boolean valid;
    private long totalEntries;
    private Long lastVerifiedSeq;
    private String headHash;
    private Long brokenSeq;
    private String failureReason;
    private String message;
    private Instant verifiedAt;
}
