package com.shrihari.evault.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.CaseActivityEvent;
import com.shrihari.evault.dto.SecurityAlarmResponse;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.CaseActivityType;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.SecurityAlarm;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
import com.shrihari.evault.repository.SecurityAlarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmDispatcherService {

    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityAlarmRepository securityAlarmRepository;
    private final ObjectMapper objectMapper;

    /**
     * Persists and broadcasts a security alarm event over STOMP to /topic/security/alarms
     * and optionally to the relevant case channel /topic/case/{caseId}.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SecurityAlarmResponse dispatchSecurityAlarm(
            SecurityAlarmType type,
            SecurityAlarmLevel severity,
            String resourceRef,
            String message,
            Map<String, Object> details,
            UUID caseId) {

        String detailsJson = null;
        if (details != null && !details.isEmpty()) {
            try {
                detailsJson = objectMapper.writeValueAsString(details);
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize alarm details to JSON", e);
                detailsJson = details.toString();
            }
        }

        SecurityAlarm alarm = SecurityAlarm.builder()
                .alarmType(type)
                .severity(severity)
                .resourceRef(resourceRef)
                .message(message)
                .details(detailsJson)
                .caseId(caseId)
                .acknowledged(false)
                .triggeredAt(Instant.now())
                .build();

        SecurityAlarm saved = securityAlarmRepository.save(alarm);
        SecurityAlarmResponse response = SecurityAlarmResponse.fromEntity(saved);

        // Broadcast to security alarms topic
        messagingTemplate.convertAndSend("/topic/security/alarms", response);

        // If associated with a case, broadcast to case channel as well
        if (caseId != null) {
            CaseActivityEvent caseAlarmEvent = CaseActivityEvent.builder()
                    .id(UUID.randomUUID())
                    .caseId(caseId)
                    .activityType(CaseActivityType.SECURITY_ALARM)
                    .summary("SECURITY ALARM: " + message)
                    .details(details)
                    .timestamp(saved.getTriggeredAt())
                    .build();
            messagingTemplate.convertAndSend("/topic/case/" + caseId, caseAlarmEvent);
        }

        if (severity == SecurityAlarmLevel.CRITICAL) {
            log.error("CRITICAL SECURITY ALARM [type={}, ref={}, case={}]: {}", type, resourceRef, caseId, message);
        } else if (severity == SecurityAlarmLevel.HIGH) {
            log.warn("HIGH SECURITY ALARM [type={}, ref={}, case={}]: {}", type, resourceRef, caseId, message);
        } else {
            log.info("SECURITY ALARM [type={}, ref={}, case={}]: {}", type, resourceRef, caseId, message);
        }

        return response;
    }

    /**
     * Broadcasts real-time operational case activity to /topic/case/{caseId}.
     */
    public void dispatchCaseActivity(
            UUID caseId,
            CaseActivityType activityType,
            UUID actorId,
            String actorUsername,
            RoleEnum actorRole,
            String summary,
            Map<String, Object> details) {

        CaseActivityEvent event = CaseActivityEvent.builder()
                .id(UUID.randomUUID())
                .caseId(caseId)
                .activityType(activityType)
                .actorId(actorId)
                .actorUsername(actorUsername)
                .actorRole(actorRole)
                .summary(summary)
                .details(details)
                .timestamp(Instant.now())
                .build();

        messagingTemplate.convertAndSend("/topic/case/" + caseId, event);
        log.info("Dispatched case activity [{}] for case {}: {}", activityType, caseId, summary);
    }

    /**
     * Acknowledges an existing alarm and pushes the update to /topic/security/alarms.
     */
    @Transactional
    public SecurityAlarmResponse acknowledgeAlarm(UUID alarmId, UUID acknowledgedBy) {
        SecurityAlarm alarm = securityAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new ResourceNotFoundException("Security alarm not found with ID: " + alarmId));

        alarm.setAcknowledged(true);
        alarm.setAcknowledgedBy(acknowledgedBy);
        alarm.setAcknowledgedAt(Instant.now());

        SecurityAlarm updated = securityAlarmRepository.save(alarm);
        SecurityAlarmResponse response = SecurityAlarmResponse.fromEntity(updated);

        // Broadcast updated state to subscribers
        messagingTemplate.convertAndSend("/topic/security/alarms", response);
        log.info("Security alarm {} acknowledged by user {}", alarmId, acknowledgedBy);

        return response;
    }

    @Transactional(readOnly = true)
    public List<SecurityAlarmResponse> getAlarms(Boolean unacknowledgedOnly, SecurityAlarmLevel severity, UUID caseId) {
        List<SecurityAlarm> alarms;

        if (caseId != null) {
            alarms = securityAlarmRepository.findByCaseIdOrderByTriggeredAtDesc(caseId);
        } else if (Boolean.TRUE.equals(unacknowledgedOnly)) {
            alarms = securityAlarmRepository.findByAcknowledgedFalseOrderByTriggeredAtDesc();
        } else if (severity != null) {
            alarms = securityAlarmRepository.findBySeverityOrderByTriggeredAtDesc(severity);
        } else {
            alarms = securityAlarmRepository.findAllByOrderByTriggeredAtDesc();
        }

        return alarms.stream()
                .map(SecurityAlarmResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SecurityAlarmResponse getAlarmById(UUID alarmId) {
        SecurityAlarm alarm = securityAlarmRepository.findById(alarmId)
                .orElseThrow(() -> new ResourceNotFoundException("Security alarm not found with ID: " + alarmId));
        return SecurityAlarmResponse.fromEntity(alarm);
    }

    @Transactional(readOnly = true)
    public long countUnacknowledgedAlarms() {
        return securityAlarmRepository.countByAcknowledgedFalse();
    }
}
