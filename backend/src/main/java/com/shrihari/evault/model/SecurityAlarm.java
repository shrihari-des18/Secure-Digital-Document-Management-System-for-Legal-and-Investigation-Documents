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
@Table(name = "security_alarms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SecurityAlarm extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "alarm_type", nullable = false, length = 64)
    private SecurityAlarmType alarmType;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 32)
    private SecurityAlarmLevel severity;

    @Column(name = "resource_ref", nullable = false, length = 255)
    private String resourceRef;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "case_id")
    private UUID caseId;

    @Builder.Default
    @Column(name = "acknowledged", nullable = false)
    private boolean acknowledged = false;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Builder.Default
    @Column(name = "triggered_at", nullable = false)
    private Instant triggeredAt = Instant.now();
}
