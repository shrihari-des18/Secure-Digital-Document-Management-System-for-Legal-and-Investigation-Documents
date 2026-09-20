package com.shrihari.evault.controller;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.common.ApiResponse;
import com.shrihari.evault.dto.SecurityAlarmResponse;
import com.shrihari.evault.dto.TriggerTestAlarmRequest;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.service.AlarmDispatcherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/alarms")
@RequiredArgsConstructor
@Tag(name = "Security Alarms", description = "Endpoints for managing real-time security alerts and tamper alarms")
public class SecurityAlarmController {

    private final AlarmDispatcherService alarmDispatcherService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDITOR')")
    @Operation(summary = "List security alarms", description = "Retrieves security alarms filtered by status, severity, or case ID")
    public ResponseEntity<ApiResponse<List<SecurityAlarmResponse>>> getAlarms(
            @Parameter(description = "Filter only unacknowledged alarms")
            @RequestParam(required = false) Boolean unacknowledgedOnly,
            @Parameter(description = "Filter by severity level")
            @RequestParam(required = false) SecurityAlarmLevel severity,
            @Parameter(description = "Filter by case ID")
            @RequestParam(required = false) UUID caseId) {

        List<SecurityAlarmResponse> alarms = alarmDispatcherService.getAlarms(unacknowledgedOnly, severity, caseId);
        return ResponseEntity.ok(ApiResponse.success(alarms));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDITOR')")
    @Operation(summary = "Get security alarm by ID", description = "Retrieves full details of a specific security alarm")
    public ResponseEntity<ApiResponse<SecurityAlarmResponse>> getAlarmById(@PathVariable UUID id) {
        SecurityAlarmResponse alarm = alarmDispatcherService.getAlarmById(id);
        return ResponseEntity.ok(ApiResponse.success(alarm));
    }

    @PostMapping("/{id}/acknowledge")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDITOR')")
    @Operation(summary = "Acknowledge security alarm", description = "Marks a security alarm as acknowledged by the current auditor or admin")
    public ResponseEntity<ApiResponse<SecurityAlarmResponse>> acknowledgeAlarm(@PathVariable UUID id) {
        ActorContext actor = ActorContextHolder.get();
        UUID actorId = actor != null ? actor.getActorId() : null;
        SecurityAlarmResponse response = alarmDispatcherService.acknowledgeAlarm(id, actorId);
        return ResponseEntity.ok(ApiResponse.success("Alarm acknowledged successfully", response));
    }

    @PostMapping("/test-trigger")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    @Operation(summary = "Trigger test security alarm", description = "Simulates an operational drill or security alert broadcast")
    public ResponseEntity<ApiResponse<SecurityAlarmResponse>> triggerTestAlarm(
            @Valid @RequestBody TriggerTestAlarmRequest request) {

        SecurityAlarmResponse response = alarmDispatcherService.dispatchSecurityAlarm(
                request.getAlarmType(),
                request.getSeverity(),
                request.getResourceRef(),
                request.getMessage(),
                request.getDetails(),
                request.getCaseId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Test security alarm broadcast successfully", response));
    }

    @GetMapping("/unacknowledged-count")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'AUDITOR')")
    @Operation(summary = "Count unacknowledged alarms", description = "Returns total number of pending security alarms")
    public ResponseEntity<ApiResponse<Long>> countUnacknowledgedAlarms() {
        long count = alarmDispatcherService.countUnacknowledgedAlarms();
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
