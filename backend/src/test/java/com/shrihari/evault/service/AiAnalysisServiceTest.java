package com.shrihari.evault.service;

import com.shrihari.evault.dto.AiAnalysisResponse;
import com.shrihari.evault.dto.ExternalAiAnalysisResult;
import com.shrihari.evault.dto.TriggerAnalysisRequest;
import com.shrihari.evault.model.AiAnalysis;
import com.shrihari.evault.model.AnalysisStatus;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.repository.AiAnalysisRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.service.ai.AiGatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisServiceTest {

    @Mock
    private AiAnalysisRepository aiAnalysisRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private AiGatewayClient aiGatewayClient;

    @Mock
    private CustodyService custodyService;

    @Mock
    private AuditLedgerService auditLedgerService;

    @InjectMocks
    private AiAnalysisService aiAnalysisService;

    private UUID evidenceId;
    private EvidenceItem evidenceItem;

    @BeforeEach
    void setUp() {
        evidenceId = UUID.randomUUID();
        evidenceItem = EvidenceItem.builder()
                .evidenceNumber("EVID-2026-001")
                .title("Server Access Log Dump")
                .currentCustodianId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        evidenceItem.setId(evidenceId);
    }

    @Test
    @DisplayName("Trigger AI Analysis successfully: updates status, logs custody event, and appends to audit ledger")
    void shouldTriggerAnalysisSuccessfully() {
        when(evidenceItemRepository.findById(evidenceId)).thenReturn(Optional.of(evidenceItem));
        when(aiAnalysisRepository.findByEvidenceId(evidenceId)).thenReturn(Optional.empty());

        ExternalAiAnalysisResult externalResult = ExternalAiAnalysisResult.builder()
                .success(true)
                .ocrText("[OCR] Suspect logs")
                .detectedPii("{\"emails\":[\"test@suspect.org\"]}")
                .classificationTags("CYBER_CRIME")
                .perceptualHash("pHash-12345")
                .confidenceScore(0.99)
                .summary("Analysis completed")
                .build();

        when(aiGatewayClient.processEvidence(eq(evidenceId), eq("Server Access Log Dump"), any(TriggerAnalysisRequest.class)))
                .thenReturn(externalResult);

        when(aiAnalysisRepository.save(any(AiAnalysis.class))).thenAnswer(invocation -> {
            AiAnalysis saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(UUID.randomUUID());
            }
            return saved;
        });

        TriggerAnalysisRequest request = TriggerAnalysisRequest.builder()
                .extractOcr(true)
                .detectPii(true)
                .generateTags(true)
                .build();

        AiAnalysisResponse response = aiAnalysisService.triggerAnalysis(evidenceId, request);

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(response.getOcrText()).contains("[OCR] Suspect logs");
        assertThat(response.getDetectedPii()).contains("test@suspect.org");
        assertThat(response.getClassificationTags()).isEqualTo("CYBER_CRIME");

        verify(custodyService).recordEvent(eq(evidenceId), eq(CustodyEventType.ANALYZE), any(), any(), any(), any());
        verify(auditLedgerService).appendEntry(any(), eq("EVIDENCE_ANALYZED"), eq("EVIDENCE:" + evidenceId), any());
    }
}
