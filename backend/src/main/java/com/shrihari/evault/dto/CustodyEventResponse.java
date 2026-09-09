package com.shrihari.evault.dto;

import com.shrihari.evault.model.CustodyEvent;
import com.shrihari.evault.model.CustodyEventType;
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
public class CustodyEventResponse {

    private UUID id;
    private UUID evidenceId;
    private CustodyEventType eventType;
    private UUID actorId;
    private UUID fromParty;
    private UUID toParty;
    private String reason;
    private String actorSignature;
    private Instant occurredAt;

    public static CustodyEventResponse fromEntity(CustodyEvent event) {
        return CustodyEventResponse.builder()
                .id(event.getId())
                .evidenceId(event.getEvidenceId())
                .eventType(event.getEventType())
                .actorId(event.getActorId())
                .fromParty(event.getFromParty())
                .toParty(event.getToParty())
                .reason(event.getReason())
                .actorSignature(event.getActorSignature())
                .occurredAt(event.getOccurredAt())
                .build();
    }
}
