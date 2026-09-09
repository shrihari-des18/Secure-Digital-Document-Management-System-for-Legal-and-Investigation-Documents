package com.shrihari.evault.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_ledger")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLedger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seq", updatable = false, nullable = false)
    private Long seq;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();

    @Column(name = "actor_id", updatable = false)
    private UUID actorId;

    @Column(name = "action", nullable = false, updatable = false, length = 64)
    private String action;

    @Column(name = "resource_ref", nullable = false, updatable = false, length = 255)
    private String resourceRef;

    @Column(name = "details", columnDefinition = "TEXT", updatable = false)
    private String details;

    @Column(name = "prev_hash", nullable = false, updatable = false, length = 64)
    private String prevHash;

    @Column(name = "entry_hash", nullable = false, updatable = false, length = 64)
    private String entryHash;
}
