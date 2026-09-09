package com.shrihari.evault.dto;

import com.shrihari.evault.model.AuditLedger;
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
public class AuditLedgerResponse {

    private Long seq;
    private Instant occurredAt;
    private UUID actorId;
    private String action;
    private String resourceRef;
    private String details;
    private String prevHash;
    private String entryHash;

    public static AuditLedgerResponse fromEntity(AuditLedger ledger) {
        return AuditLedgerResponse.builder()
                .seq(ledger.getSeq())
                .occurredAt(ledger.getOccurredAt())
                .actorId(ledger.getActorId())
                .action(ledger.getAction())
                .resourceRef(ledger.getResourceRef())
                .details(ledger.getDetails())
                .prevHash(ledger.getPrevHash())
                .entryHash(ledger.getEntryHash())
                .build();
    }
}
