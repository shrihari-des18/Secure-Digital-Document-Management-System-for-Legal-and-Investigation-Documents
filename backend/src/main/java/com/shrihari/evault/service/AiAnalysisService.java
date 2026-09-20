package com.shrihari.evault.service;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.dto.AiAnalysisResponse;
import com.shrihari.evault.dto.ExternalAiAnalysisResult;
import com.shrihari.evault.dto.TriggerAnalysisRequest;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.AiAnalysis;
import com.shrihari.evault.model.AnalysisStatus;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.repository.AiAnalysisRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.service.ai.AiGatewayClient;
import com.shrihari.evault.model.CaseActivityType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiAnalysisService {

    private final AiAnalysisRepository aiAnalysisRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final AiGatewayClient aiGatewayClient;
    private final CustodyService custodyService;
    private final AuditLedgerService auditLedgerService;
    private final AlarmDispatcherService alarmDispatcherService;

    @Transactional
    public AiAnalysisResponse triggerAnalysis(UUID evidenceId, TriggerAnalysisRequest request) {
        log.info("Triggering AI analysis for evidence ID: {}", evidenceId);

        EvidenceItem evidence = evidenceItemRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence item not found with ID: " + evidenceId));

        if (request == null) {
            request = new TriggerAnalysisRequest();
        }

        AiAnalysis analysis = aiAnalysisRepository.findByEvidenceId(evidenceId)
                .orElseGet(() -> AiAnalysis.builder()
                        .evidenceId(evidenceId)
                        .status(AnalysisStatus.PENDING)
                        .build());

        analysis.setStatus(AnalysisStatus.PROCESSING);
        aiAnalysisRepository.save(analysis);

        String nameToProcess = evidence.getTitle() != null ? evidence.getTitle() : evidence.getEvidenceNumber();
        ExternalAiAnalysisResult result = aiGatewayClient.processEvidence(evidenceId, nameToProcess, request);

        ActorContext actor = ActorContextHolder.get();
        UUID actorId = (actor != null && actor.getActorId() != null) ? actor.getActorId() : evidence.getCreatedBy();

        if (result.isSuccess()) {
            analysis.setStatus(AnalysisStatus.COMPLETED);
            analysis.setOcrText(result.getOcrText());
            analysis.setDetectedPii(result.getDetectedPii());
            analysis.setClassificationTags(result.getClassificationTags());
            analysis.setPerceptualHash(result.getPerceptualHash());
            analysis.setConfidenceScore(result.getConfidenceScore());
            analysis.setSummary(result.getSummary());
            analysis.setFailureReason(null);

            // Log analysis into chain of custody and audit ledger
            custodyService.recordEvent(
                    evidenceId,
                    CustodyEventType.ANALYZE,
                    actorId,
                    evidence.getCurrentCustodianId(),
                    evidence.getCurrentCustodianId(),
                    "AI/ML Analysis pipeline executed: OCR, PII detection, auto-tagging"
            );

            auditLedgerService.appendEntry(
                    actorId,
                    "EVIDENCE_ANALYZED",
                    "EVIDENCE:" + evidenceId,
                    "{\"evidenceId\":\"" + evidenceId + "\",\"status\":\"COMPLETED\",\"confidence\":" + result.getConfidenceScore() + "}"
            );
        } else {
            analysis.setStatus(AnalysisStatus.FAILED);
            analysis.setFailureReason(result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown AI gateway failure");
        }

        AiAnalysis saved = aiAnalysisRepository.save(analysis);
        log.info("AI analysis completed for evidence ID: {} with status: {}", evidenceId, saved.getStatus());

        if (alarmDispatcherService != null) {
            try {
                alarmDispatcherService.dispatchCaseActivity(
                        evidence.getCaseId(),
                        CaseActivityType.AI_ANALYSIS_COMPLETED,
                        actorId,
                        actor != null ? actor.getUsername() : null,
                        actor != null ? actor.getRole() : null,
                        "AI/ML analysis completed for evidence " + evidence.getEvidenceNumber() + " with status " + saved.getStatus(),
                        Map.of(
                                "evidenceId", evidenceId.toString(),
                                "evidenceNumber", evidence.getEvidenceNumber(),
                                "status", saved.getStatus().name(),
                                "confidenceScore", saved.getConfidenceScore() != null ? saved.getConfidenceScore() : 0.0
                        )
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch case activity for AI analysis", e);
            }
        }

        return AiAnalysisResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AiAnalysisResponse getAnalysisByEvidenceId(UUID evidenceId) {
        AiAnalysis analysis = aiAnalysisRepository.findByEvidenceId(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("AI analysis record not found for evidence ID: " + evidenceId));
        return AiAnalysisResponse.fromEntity(analysis);
    }
}
