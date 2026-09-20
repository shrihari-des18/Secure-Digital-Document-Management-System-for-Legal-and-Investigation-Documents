package com.shrihari.evault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.CaseActivityEvent;
import com.shrihari.evault.dto.SecurityAlarmResponse;
import com.shrihari.evault.model.CaseActivityType;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.SecurityAlarm;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
import com.shrihari.evault.repository.SecurityAlarmRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlarmDispatcherServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private SecurityAlarmRepository securityAlarmRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AlarmDispatcherService alarmDispatcherService;

    @Test
    @DisplayName("dispatchSecurityAlarm persists alarm and broadcasts to /topic/security/alarms")
    void testDispatchSecurityAlarm() {
        UUID alarmId = UUID.randomUUID();
        when(securityAlarmRepository.save(any(SecurityAlarm.class))).thenAnswer(invocation -> {
            SecurityAlarm a = invocation.getArgument(0);
            a.setId(alarmId);
            return a;
        });

        SecurityAlarmResponse response = alarmDispatcherService.dispatchSecurityAlarm(
                SecurityAlarmType.LEDGER_INTEGRITY_BREACH,
                SecurityAlarmLevel.CRITICAL,
                "LEDGER:42",
                "Cryptographic hash chain broken at sequence #42",
                Map.of("brokenSeq", 42, "reason", "Hash mismatch"),
                null
        );

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(alarmId);
        assertThat(response.getAlarmType()).isEqualTo(SecurityAlarmType.LEDGER_INTEGRITY_BREACH);
        assertThat(response.getSeverity()).isEqualTo(SecurityAlarmLevel.CRITICAL);
        assertThat(response.getResourceRef()).isEqualTo("LEDGER:42");
        assertThat(response.isAcknowledged()).isFalse();

        verify(messagingTemplate).convertAndSend(eq("/topic/security/alarms"), any(SecurityAlarmResponse.class));
    }

    @Test
    @DisplayName("dispatchSecurityAlarm with caseId broadcasts to both /topic/security/alarms and /topic/case/{caseId}")
    void testDispatchSecurityAlarmWithCaseId() {
        UUID alarmId = UUID.randomUUID();
        UUID caseId = UUID.randomUUID();

        when(securityAlarmRepository.save(any(SecurityAlarm.class))).thenAnswer(invocation -> {
            SecurityAlarm a = invocation.getArgument(0);
            a.setId(alarmId);
            return a;
        });

        SecurityAlarmResponse response = alarmDispatcherService.dispatchSecurityAlarm(
                SecurityAlarmType.FILE_BIT_ROT_DETECTED,
                SecurityAlarmLevel.CRITICAL,
                "EVIDENCE:EVID-001",
                "File byte hash mismatch detected during integrity verification",
                Map.of("evidenceId", UUID.randomUUID().toString()),
                caseId
        );

        assertThat(response).isNotNull();
        assertThat(response.getCaseId()).isEqualTo(caseId);

        verify(messagingTemplate).convertAndSend(eq("/topic/security/alarms"), any(SecurityAlarmResponse.class));
        verify(messagingTemplate).convertAndSend(eq("/topic/case/" + caseId), any(CaseActivityEvent.class));
    }

    @Test
    @DisplayName("dispatchCaseActivity broadcasts CaseActivityEvent to /topic/case/{caseId}")
    void testDispatchCaseActivity() {
        UUID caseId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        alarmDispatcherService.dispatchCaseActivity(
                caseId,
                CaseActivityType.CUSTODY_TRANSFERRED,
                actorId,
                "io_sharma",
                RoleEnum.INVESTIGATING_OFFICER,
                "Custody transferred to forensic analyst",
                Map.of("toParty", UUID.randomUUID().toString())
        );

        ArgumentCaptor<CaseActivityEvent> eventCaptor = ArgumentCaptor.forClass(CaseActivityEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/case/" + caseId), eventCaptor.capture());

        CaseActivityEvent captured = eventCaptor.getValue();
        assertThat(captured.getCaseId()).isEqualTo(caseId);
        assertThat(captured.getActivityType()).isEqualTo(CaseActivityType.CUSTODY_TRANSFERRED);
        assertThat(captured.getActorUsername()).isEqualTo("io_sharma");
        assertThat(captured.getSummary()).isEqualTo("Custody transferred to forensic analyst");
    }

    @Test
    @DisplayName("acknowledgeAlarm marks alarm as acknowledged and pushes update to STOMP")
    void testAcknowledgeAlarm() {
        UUID alarmId = UUID.randomUUID();
        UUID auditorId = UUID.randomUUID();

        SecurityAlarm alarm = SecurityAlarm.builder()
                .alarmType(SecurityAlarmType.LEDGER_INTEGRITY_BREACH)
                .severity(SecurityAlarmLevel.CRITICAL)
                .resourceRef("LEDGER:1")
                .message("Tampered sequence")
                .acknowledged(false)
                .triggeredAt(Instant.now())
                .build();
        alarm.setId(alarmId);

        when(securityAlarmRepository.findById(alarmId)).thenReturn(Optional.of(alarm));
        when(securityAlarmRepository.save(any(SecurityAlarm.class))).thenAnswer(i -> i.getArgument(0));

        SecurityAlarmResponse result = alarmDispatcherService.acknowledgeAlarm(alarmId, auditorId);

        assertThat(result.isAcknowledged()).isTrue();
        assertThat(result.getAcknowledgedBy()).isEqualTo(auditorId);
        assertThat(result.getAcknowledgedAt()).isNotNull();

        verify(messagingTemplate).convertAndSend(eq("/topic/security/alarms"), any(SecurityAlarmResponse.class));
    }

    @Test
    @DisplayName("Query methods and unacknowledged count operate as expected")
    void testQueryMethods() {
        SecurityAlarm alarm1 = SecurityAlarm.builder()
                .alarmType(SecurityAlarmType.SYSTEM_ALERT)
                .severity(SecurityAlarmLevel.HIGH)
                .resourceRef("SYS:1")
                .message("Test")
                .acknowledged(false)
                .triggeredAt(Instant.now())
                .build();

        when(securityAlarmRepository.findByAcknowledgedFalseOrderByTriggeredAtDesc()).thenReturn(List.of(alarm1));
        when(securityAlarmRepository.countByAcknowledgedFalse()).thenReturn(1L);

        List<SecurityAlarmResponse> unack = alarmDispatcherService.getAlarms(true, null, null);
        assertThat(unack).hasSize(1);

        long count = alarmDispatcherService.countUnacknowledgedAlarms();
        assertThat(count).isEqualTo(1L);
    }
}
