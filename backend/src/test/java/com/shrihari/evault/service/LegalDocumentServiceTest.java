package com.shrihari.evault.service;

import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.AddDocumentVersionRequest;
import com.shrihari.evault.dto.CreateLegalDocumentRequest;
import com.shrihari.evault.dto.DocumentVersionResponse;
import com.shrihari.evault.dto.LegalDocumentResponse;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.DocumentVersion;
import com.shrihari.evault.model.LegalDocument;
import com.shrihari.evault.model.LegalDocumentType;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.DocumentVersionRepository;
import com.shrihari.evault.repository.LegalDocumentRepository;
import com.shrihari.evault.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LegalDocumentServiceTest {

    @Mock
    private LegalDocumentRepository legalDocumentRepository;

    @Mock
    private DocumentVersionRepository documentVersionRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private AuditLedgerService auditLedgerService;

    @InjectMocks
    private LegalDocumentService legalDocumentService;

    private UUID caseId;
    private Case testCase;
    private CreateLegalDocumentRequest createRequest;
    private MockMultipartFile v1File;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID();
        testCase = Case.builder()
                .caseNumber("CASE-2026-001")
                .title("Investigation Docket")
                .status(CaseStatus.OPEN)
                .jurisdiction("Jurisdiction 1")
                .leadOfficerId(UUID.randomUUID())
                .build();
        testCase.setId(caseId);

        createRequest = CreateLegalDocumentRequest.builder()
                .caseId(caseId)
                .documentNumber("FIR-2026-001-DOC")
                .title("First Information Report")
                .documentType(LegalDocumentType.FIR)
                .changeSummary("Initial registered FIR")
                .build();

        v1File = new MockMultipartFile(
                "file",
                "fir_original.pdf",
                "application/pdf",
                "FIR-CONTENT-VERSION-1".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("Create legal document successfully with version 1 and ledger chaining")
    void shouldCreateLegalDocument() throws IOException {
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
        when(legalDocumentRepository.existsByDocumentNumber("FIR-2026-001-DOC")).thenReturn(false);
        when(storageService.store(eq(v1File), any(String.class))).thenReturn("documents/" + caseId + "/fir.pdf");

        when(legalDocumentRepository.save(any(LegalDocument.class))).thenAnswer(i -> {
            LegalDocument doc = i.getArgument(0);
            doc.setId(UUID.randomUUID());
            return doc;
        });

        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(i -> {
            DocumentVersion v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });

        LegalDocumentResponse response = legalDocumentService.createDocument(createRequest, v1File);

        assertThat(response).isNotNull();
        assertThat(response.getDocumentNumber()).isEqualTo("FIR-2026-001-DOC");
        assertThat(response.getCurrentVersion()).isEqualTo(1);
        assertThat(response.getLatestVersion()).isNotNull();
        assertThat(response.getLatestVersion().getVersionNumber()).isEqualTo(1);
        assertThat(response.getLatestVersion().getSha256())
                .isEqualTo(CryptoUtil.calculateSha256("FIR-CONTENT-VERSION-1".getBytes(StandardCharsets.UTF_8)));

        verify(auditLedgerService).appendEntry(any(), eq("DOCUMENT_CREATED"), any(), any());
    }

    @Test
    @DisplayName("Add document revision increments version number and updates ledger")
    void shouldAddDocumentRevision() throws IOException {
        UUID docId = UUID.randomUUID();
        LegalDocument existingDoc = LegalDocument.builder()
                .caseId(caseId)
                .documentNumber("FIR-2026-001-DOC")
                .title("First Information Report")
                .documentType(LegalDocumentType.FIR)
                .currentVersion(1)
                .createdBy(UUID.randomUUID())
                .build();
        existingDoc.setId(docId);

        when(legalDocumentRepository.findById(docId)).thenReturn(Optional.of(existingDoc));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));

        MockMultipartFile v2File = new MockMultipartFile(
                "file",
                "fir_amended.pdf",
                "application/pdf",
                "FIR-CONTENT-VERSION-2-AMENDED".getBytes(StandardCharsets.UTF_8)
        );

        when(storageService.store(eq(v2File), any(String.class))).thenReturn("documents/" + caseId + "/fir_v2.pdf");
        when(documentVersionRepository.save(any(DocumentVersion.class))).thenAnswer(i -> {
            DocumentVersion v = i.getArgument(0);
            v.setId(UUID.randomUUID());
            return v;
        });
        when(legalDocumentRepository.save(any(LegalDocument.class))).thenAnswer(i -> i.getArgument(0));

        AddDocumentVersionRequest addReq = AddDocumentVersionRequest.builder()
                .changeSummary("Added suspect name per witness statement")
                .build();

        DocumentVersionResponse response = legalDocumentService.addDocumentVersion(docId, addReq, v2File);

        assertThat(response).isNotNull();
        assertThat(response.getVersionNumber()).isEqualTo(2);
        assertThat(existingDoc.getCurrentVersion()).isEqualTo(2);
        assertThat(response.getChangeSummary()).isEqualTo("Added suspect name per witness statement");

        verify(auditLedgerService).appendEntry(any(), eq("DOCUMENT_REVISED"), any(), any());
    }

    @Test
    @DisplayName("Reject duplicate document number")
    void shouldRejectDuplicateDocNumber() {
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
        when(legalDocumentRepository.existsByDocumentNumber("FIR-2026-001-DOC")).thenReturn(true);

        assertThatThrownBy(() -> legalDocumentService.createDocument(createRequest, v1File))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }
}
