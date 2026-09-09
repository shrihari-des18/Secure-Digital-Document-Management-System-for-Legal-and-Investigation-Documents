package com.shrihari.evault.controller;

import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.CustodyEventResponse;
import com.shrihari.evault.dto.CustodyTransferRequest;
import com.shrihari.evault.service.CustodyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/evidence")
@RequiredArgsConstructor
@Tag(name = "Chain of Custody", description = "Chain-of-custody tracking, officer handoffs, and timeline history")
public class CustodyController {

    private final CustodyService custodyService;

    @PostMapping("/{evidenceId}/custody-transfer")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'RECORDS_CUSTODIAN', 'FORENSIC_ANALYST', 'SYSTEM_ADMIN')")
    @Operation(
            summary = "Transfer evidence custody",
            description = "Transfers physical/digital custody of evidence to another officer, forensic analyst, or courtroom. Mandates legal reason and digitally signs the transfer event into the audit ledger."
    )
    public ResponseEntity<ApiResponse<CustodyEventResponse>> transferCustody(
            @PathVariable UUID evidenceId,
            @Valid @RequestBody CustodyTransferRequest request) {
        CustodyEventResponse response = custodyService.transferCustody(evidenceId, request);
        return ResponseEntity.ok(ApiResponse.success("Custody transferred successfully and recorded in audit ledger", response));
    }

    @GetMapping("/{evidenceId}/custody")
    @PreAuthorize("hasAnyRole('INVESTIGATING_OFFICER', 'RECORDS_CUSTODIAN', 'FORENSIC_ANALYST', 'PROSECUTOR', 'COURT', 'AUDITOR', 'SYSTEM_ADMIN')")
    @Operation(
            summary = "Get evidence chain-of-custody timeline",
            description = "Retrieves the complete, signed chronological custody timeline (seizure, ingest, transfers, analyses, and downloads) for court admissibility under BSA §63."
    )
    public ResponseEntity<ApiResponse<List<CustodyEventResponse>>> getCustodyTimeline(@PathVariable UUID evidenceId) {
        List<CustodyEventResponse> timeline = custodyService.getCustodyTimeline(evidenceId);
        return ResponseEntity.ok(ApiResponse.success(timeline));
    }
}
