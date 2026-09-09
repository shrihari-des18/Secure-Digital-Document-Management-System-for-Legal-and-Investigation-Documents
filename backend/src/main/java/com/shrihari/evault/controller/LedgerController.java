package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.AuditLedgerResponse;
import com.shrihari.evault.dto.LedgerVerificationResult;
import com.shrihari.evault.service.AuditLedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
@RequiredArgsConstructor
@Tag(name = "Audit Ledger", description = "Append-only cryptographic hash chain verification and audit logs")
public class LedgerController {

    private final AuditLedgerService auditLedgerService;

    @GetMapping("/verify")
    @PreAuthorize("hasAnyRole('AUDITOR', 'SYSTEM_ADMIN', 'INVESTIGATING_OFFICER', 'COURT', 'PROSECUTOR', 'RECORDS_CUSTODIAN')")
    @Operation(
            summary = "Verify complete audit ledger cryptographic integrity",
            description = "Traverses the ledger from genesis block to current head, verifying SHA-256 block linkages and re-computing hashes over every recorded transaction. Flags any tampered or missing sequence."
    )
    public ResponseEntity<ApiResponse<LedgerVerificationResult>> verifyLedgerIntegrity() {
        LedgerVerificationResult result = auditLedgerService.verifyLedgerIntegrity();
        return ResponseEntity.ok(ApiResponse.success(
                result.isValid() ? "Audit ledger is 100% cryptographically intact" : "Tampering detected in audit ledger!",
                result
        ));
    }

    @GetMapping("/entries")
    @PreAuthorize("hasAnyRole('AUDITOR', 'SYSTEM_ADMIN')")
    @Operation(summary = "List all audit ledger entries", description = "Retrieves all hash-chained transaction records")
    public ResponseEntity<ApiResponse<List<AuditLedgerResponse>>> getAllEntries() {
        List<AuditLedgerResponse> entries = auditLedgerService.getAllEntries();
        return ResponseEntity.ok(ApiResponse.success(entries));
    }
}
