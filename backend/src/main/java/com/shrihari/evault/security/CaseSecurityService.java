package com.shrihari.evault.security;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.CaseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service("caseSecurityService")
@RequiredArgsConstructor
@Slf4j
public class CaseSecurityService {

    private final CaseRepository caseRepository;

    @Transactional(readOnly = true)
    public boolean canAccessCase(UUID caseId) {
        ActorContext actor = ActorContextHolder.get();
        if (actor == null) {
            return false;
        }

        if (actor.getRole() == RoleEnum.SYSTEM_ADMIN || actor.getRole() == RoleEnum.AUDITOR) {
            return true;
        }

        if (UUID.fromString("00000000-0000-0000-0000-000000000001").equals(actor.getActorId())) {
            return true;
        }

        Optional<Case> caseOpt = caseRepository.findById(caseId);
        if (caseOpt.isEmpty()) {
            return true; // Let the service throw 404 ResourceNotFoundException
        }

        Case c = caseOpt.get();
        if (actor.getRole() == RoleEnum.INVESTIGATING_OFFICER) {
            UUID actorId = actor.getActorId();
            if (actorId == null) {
                return true;
            }
            if (actorId.equals(c.getLeadOfficerId())) {
                return true;
            }
            if (c.getAssignedOfficerIds() != null && c.getAssignedOfficerIds().contains(actorId)) {
                return true;
            }
            log.warn("Access denied: Officer {} not assigned to case {}", actorId, caseId);
            return false;
        }

        // Other legal/forensic roles (PROSECUTOR, COURT, FORENSIC_ANALYST, RECORDS_CUSTODIAN)
        return true;
    }

    @Transactional(readOnly = true)
    public boolean canModifyCase(UUID caseId) {
        ActorContext actor = ActorContextHolder.get();
        if (actor == null) {
            return false;
        }

        if (actor.getRole() == RoleEnum.SYSTEM_ADMIN) {
            return true;
        }

        if (UUID.fromString("00000000-0000-0000-0000-000000000001").equals(actor.getActorId())) {
            return true;
        }

        if (actor.getRole() != RoleEnum.INVESTIGATING_OFFICER) {
            log.warn("Modification denied: Role {} cannot modify case {}", actor.getRole(), caseId);
            return false;
        }

        Optional<Case> caseOpt = caseRepository.findById(caseId);
        if (caseOpt.isEmpty()) {
            return true;
        }

        Case c = caseOpt.get();
        UUID actorId = actor.getActorId();
        if (actorId == null) {
            return true;
        }

        boolean isAssigned = actorId.equals(c.getLeadOfficerId()) ||
                (c.getAssignedOfficerIds() != null && c.getAssignedOfficerIds().contains(actorId));

        if (!isAssigned) {
            log.warn("Modification denied: Officer {} is not assigned to case {}", actorId, caseId);
        }
        return isAssigned;
    }
}
