package com.shrihari.evault.service;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.CustodyEventResponse;
import com.shrihari.evault.dto.CustodyTransferRequest;
import com.shrihari.evault.exception.InvalidOperationException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.CustodyEvent;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.repository.CustodyEventRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustodyService {

    private final CustodyEventRepository custodyEventRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final UserRepository userRepository;
    private final AuditLedgerService auditLedgerService;

    @Value("${app.crypto.system-secret:SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026}")
    private String systemSecret = "SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026";

    @Transactional
    public CustodyEventResponse recordEvent(UUID evidenceId, CustodyEventType eventType, UUID actorId,
                                            UUID fromParty, UUID toParty, String reason) {
        log.info("Recording custody event {} for evidence {}", eventType, evidenceId);

        Instant occurredAt = Instant.now();
        String signaturePayload = evidenceId + "|" + eventType + "|" + actorId + "|" + occurredAt.toEpochMilli();
        String signature = CryptoUtil.generateSignature(signaturePayload, systemSecret);

        CustodyEvent event = CustodyEvent.builder()
                .evidenceId(evidenceId)
                .eventType(eventType)
                .actorId(actorId)
                .fromParty(fromParty)
                .toParty(toParty)
                .reason(reason)
                .actorSignature(signature)
                .occurredAt(occurredAt)
                .build();

        CustodyEvent saved = custodyEventRepository.save(event);

        // Append to immutable hash chain
        String details = "{\"eventType\":\"" + eventType + "\",\"from\":\"" + fromParty + "\",\"to\":\"" + toParty + "\",\"reason\":\"" + (reason != null ? reason : "") + "\"}";
        auditLedgerService.appendEntry(actorId, "CUSTODY_" + eventType.name(), "EVIDENCE:" + evidenceId, details);

        return CustodyEventResponse.fromEntity(saved);
    }

    @Transactional
    public CustodyEventResponse transferCustody(UUID evidenceId, CustodyTransferRequest request) {
        EvidenceItem item = evidenceItemRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence item not found with ID: " + evidenceId));

        if (item.getCustodyState() == CustodyState.DISPOSED || item.getCustodyState() == CustodyState.ARCHIVED) {
            throw new InvalidOperationException("Cannot transfer evidence in " + item.getCustodyState() + " state");
        }

        // Validate recipient exists
        if (!userRepository.existsById(request.getToPartyId())) {
            throw new ResourceNotFoundException("Recipient custodian not found with ID: " + request.getToPartyId());
        }

        ActorContext actor = ActorContextHolder.get();
        UUID fromParty = item.getCurrentCustodianId();
        UUID toParty = request.getToPartyId();

        // Update current custodian and custody state
        item.setCurrentCustodianId(toParty);
        item.setCustodyState(CustodyState.TRANSFERRED);
        evidenceItemRepository.save(item);

        return recordEvent(
                evidenceId,
                CustodyEventType.TRANSFER,
                actor.getActorId(),
                fromParty,
                toParty,
                request.getReason()
        );
    }

    @Transactional(readOnly = true)
    public List<CustodyEventResponse> getCustodyTimeline(UUID evidenceId) {
        if (!evidenceItemRepository.existsById(evidenceId)) {
            throw new ResourceNotFoundException("Evidence item not found with ID: " + evidenceId);
        }

        return custodyEventRepository.findByEvidenceIdOrderByOccurredAtAsc(evidenceId).stream()
                .map(CustodyEventResponse::fromEntity)
                .toList();
    }
}
