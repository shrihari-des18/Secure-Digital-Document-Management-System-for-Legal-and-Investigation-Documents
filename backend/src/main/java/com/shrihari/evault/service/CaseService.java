package com.shrihari.evault.service;

import com.shrihari.evault.dto.CaseResponse;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.UpdateCaseRequest;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.InvalidOperationException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.model.CaseActivityType;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CaseService {

    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final AlarmDispatcherService alarmDispatcherService;

    @Transactional
    public CaseResponse createCase(CreateCaseRequest request) {
        log.info("Creating case with number: {}", request.getCaseNumber());

        if (caseRepository.existsByCaseNumber(request.getCaseNumber())) {
            throw new DuplicateResourceException("Case with number '" + request.getCaseNumber() + "' already exists");
        }

        // Validate lead officer
        if (!userRepository.existsById(request.getLeadOfficerId())) {
            throw new ResourceNotFoundException("Lead officer not found with ID: " + request.getLeadOfficerId());
        }

        Case newCase = Case.builder()
                .caseNumber(request.getCaseNumber().trim())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .jurisdiction(request.getJurisdiction().trim())
                .status(CaseStatus.OPEN)
                .leadOfficerId(request.getLeadOfficerId())
                .assignedOfficerIds(request.getAssignedOfficerIds() != null
                        ? new HashSet<>(request.getAssignedOfficerIds())
                        : new HashSet<>())
                .build();

        Case saved = caseRepository.save(newCase);
        log.info("Case created successfully with ID: {}", saved.getId());

        if (alarmDispatcherService != null) {
            try {
                ActorContext actor = ActorContextHolder.get();
                alarmDispatcherService.dispatchCaseActivity(
                        saved.getId(),
                        CaseActivityType.CASE_CREATED,
                        actor != null ? actor.getActorId() : null,
                        actor != null ? actor.getUsername() : null,
                        actor != null ? actor.getRole() : null,
                        "Case created: " + saved.getCaseNumber() + " - " + saved.getTitle(),
                        Map.of("caseNumber", saved.getCaseNumber(), "title", saved.getTitle(), "status", saved.getStatus().name())
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch case activity for case creation", e);
            }
        }

        return CaseResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCaseById(UUID id) {
        Case c = getEntityById(id);
        return CaseResponse.fromEntity(c);
    }

    @Transactional(readOnly = true)
    public CaseResponse getCaseByNumber(String caseNumber) {
        Case c = caseRepository.findByCaseNumber(caseNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with number: " + caseNumber));
        return CaseResponse.fromEntity(c);
    }

    @Transactional(readOnly = true)
    public List<CaseResponse> getAllCases(CaseStatus status) {
        List<Case> cases = (status != null)
                ? caseRepository.findByStatus(status)
                : caseRepository.findAll();

        return cases.stream()
                .map(CaseResponse::fromEntity)
                .toList();
    }

    @Transactional
    public CaseResponse updateCase(UUID id, UpdateCaseRequest request) {
        log.info("Updating case ID: {}", id);
        Case c = getEntityById(id);

        if (c.getStatus() == CaseStatus.ARCHIVED) {
            throw new InvalidOperationException("Cannot update an archived case");
        }

        c.setTitle(request.getTitle().trim());
        c.setDescription(request.getDescription());
        c.setJurisdiction(request.getJurisdiction().trim());

        if (request.getAssignedOfficerIds() != null) {
            c.setAssignedOfficerIds(new HashSet<>(request.getAssignedOfficerIds()));
        }

        Case updated = caseRepository.save(c);

        if (alarmDispatcherService != null) {
            try {
                ActorContext actor = ActorContextHolder.get();
                alarmDispatcherService.dispatchCaseActivity(
                        updated.getId(),
                        CaseActivityType.CASE_METADATA_UPDATED,
                        actor != null ? actor.getActorId() : null,
                        actor != null ? actor.getUsername() : null,
                        actor != null ? actor.getRole() : null,
                        "Case metadata updated for " + updated.getCaseNumber(),
                        Map.of("caseNumber", updated.getCaseNumber(), "title", updated.getTitle())
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch case activity for case update", e);
            }
        }

        return CaseResponse.fromEntity(updated);
    }

    @Transactional
    public CaseResponse updateStatus(UUID id, CaseStatus newStatus, String reason) {
        log.info("Transitioning case ID {} to status {} with reason: {}", id, newStatus, reason);
        Case c = getEntityById(id);

        if (c.getStatus() == CaseStatus.ARCHIVED && newStatus != CaseStatus.ARCHIVED) {
            throw new InvalidOperationException("Cannot reopen an archived case directly");
        }

        c.setStatus(newStatus);
        Case updated = caseRepository.save(c);

        if (alarmDispatcherService != null) {
            try {
                ActorContext actor = ActorContextHolder.get();
                alarmDispatcherService.dispatchCaseActivity(
                        updated.getId(),
                        CaseActivityType.CASE_STATUS_UPDATED,
                        actor != null ? actor.getActorId() : null,
                        actor != null ? actor.getUsername() : null,
                        actor != null ? actor.getRole() : null,
                        "Case status transitioned to " + newStatus + (reason != null ? " (Reason: " + reason + ")" : ""),
                        Map.of("caseNumber", updated.getCaseNumber(), "status", newStatus.name(), "reason", reason != null ? reason : "")
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch case activity for case status update", e);
            }
        }

        return CaseResponse.fromEntity(updated);
    }

    @Transactional(readOnly = true)
    public Case getEntityById(UUID id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + id));
    }
}
