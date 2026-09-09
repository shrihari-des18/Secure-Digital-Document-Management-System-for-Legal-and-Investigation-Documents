package com.shrihari.evault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "custody_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustodyEvent extends BaseEntity {

    @Column(name = "evidence_id", nullable = false)
    private UUID evidenceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private CustodyEventType eventType;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "from_party")
    private UUID fromParty;

    @Column(name = "to_party")
    private UUID toParty;

    @Column(name = "reason", length = 512)
    private String reason;

    @Column(name = "actor_signature", columnDefinition = "TEXT")
    private String actorSignature;

    @Column(name = "occurred_at", nullable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
