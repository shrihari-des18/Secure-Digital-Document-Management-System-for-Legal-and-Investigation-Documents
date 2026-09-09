package com.shrihari.evault.service;

import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.EvidenceDetailResponse;
import com.shrihari.evault.dto.IntegrityVerificationResponse;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.model.IntegrityStatus;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.EvidenceFileRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
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
class EvidenceServiceTest {

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private EvidenceFileRepository evidenceFileRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private StorageService storageService;

    @Mock
    private CustodyService custodyService;

    @InjectMocks
    private EvidenceService evidenceService;

    private UUID caseId;
    private Case testCase;
    private UploadEvidenceRequest uploadRequest;
    private MockMultipartFile mockFile;

    @BeforeEach
    void setUp() {
        caseId = UUID.randomUUID();
        testCase = Case.builder()
                .caseNumber("CASE-2026-001")
                .title("Test Investigation")
                .status(CaseStatus.OPEN)
                .jurisdiction("Jurisdiction A")
                .leadOfficerId(UUID.randomUUID())
                .build();
        testCase.setId(caseId);

        uploadRequest = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-2026-001")
                .title("Suspect Hard Drive Image")
                .classification(ClassificationLevel.SECRET)
                .deviceMake("Western Digital")
                .deviceModel("WD10EZEX")
                .build();

        mockFile = new MockMultipartFile(
                "file",
                "disk_dump.dd",
                "application/octet-stream",
                "forensic-raw-disk-content".getBytes(StandardCharsets.UTF_8)
        );
    }

    @Test
    @DisplayName("Upload evidence successfully and compute SHA-256 integrity")
    void shouldUploadAndIngestEvidence() throws IOException {
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
        when(evidenceItemRepository.existsByEvidenceNumber("EVID-2026-001")).thenReturn(false);
        when(storageService.store(eq(mockFile), any(String.class))).thenReturn("cases/" + caseId + "/disk_dump.dd");

        when(evidenceItemRepository.save(any(EvidenceItem.class))).thenAnswer(invocation -> {
            EvidenceItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        when(evidenceFileRepository.save(any(EvidenceFile.class))).thenAnswer(invocation -> {
            EvidenceFile file = invocation.getArgument(0);
            file.setId(UUID.randomUUID());
            return file;
        });

        EvidenceDetailResponse response = evidenceService.uploadAndIngestEvidence(uploadRequest, mockFile);

        assertThat(response).isNotNull();
        assertThat(response.getEvidenceNumber()).isEqualTo("EVID-2026-001");
        assertThat(response.getCustodyState()).isEqualTo(CustodyState.INGESTED);
        assertThat(response.getFile()).isNotNull();

        String expectedSha256 = CryptoUtil.calculateSha256("forensic-raw-disk-content".getBytes(StandardCharsets.UTF_8));
        assertThat(response.getFile().getSha256()).isEqualTo(expectedSha256);
        assertThat(response.getFile().getIntegrityStatus()).isEqualTo(IntegrityStatus.VERIFIED);
        assertThat(response.getFile().getDigitalSignature()).isNotNull();

        verify(storageService).store(eq(mockFile), any(String.class));
    }

    @Test
    @DisplayName("Flag IntegrityStatus.MISMATCH when client SHA-256 differs from computed")
    void shouldFlagMismatchWhenClientHashDiffers() throws IOException {
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
        when(evidenceItemRepository.existsByEvidenceNumber("EVID-2026-001")).thenReturn(false);
        when(storageService.store(eq(mockFile), any(String.class))).thenReturn("cases/" + caseId + "/disk_dump.dd");

        when(evidenceItemRepository.save(any(EvidenceItem.class))).thenAnswer(invocation -> {
            EvidenceItem item = invocation.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        when(evidenceFileRepository.save(any(EvidenceFile.class))).thenAnswer(invocation -> {
            EvidenceFile file = invocation.getArgument(0);
            file.setId(UUID.randomUUID());
            return file;
        });

        // Set a wrong client hash
        uploadRequest.setClientSha256("wrong_hash_00000000000000000000000000000000000000000000000000000000");

        EvidenceDetailResponse response = evidenceService.uploadAndIngestEvidence(uploadRequest, mockFile);

        assertThat(response.getFile().getIntegrityStatus()).isEqualTo(IntegrityStatus.MISMATCH);
    }

    @Test
    @DisplayName("Verify physical file integrity compares accurately with stored hash")
    void shouldVerifyPhysicalIntegrity() throws IOException {
        UUID evidenceId = UUID.randomUUID();
        EvidenceItem item = EvidenceItem.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-2026-001")
                .build();
        item.setId(evidenceId);

        byte[] content = "immutable-evidence-content".getBytes(StandardCharsets.UTF_8);
        String sha256 = CryptoUtil.calculateSha256(content);

        EvidenceFile file = EvidenceFile.builder()
                .evidenceId(evidenceId)
                .storagePath("path/to/file")
                .sha256(sha256)
                .build();

        when(evidenceItemRepository.findById(evidenceId)).thenReturn(Optional.of(item));
        when(evidenceFileRepository.findByEvidenceId(evidenceId)).thenReturn(Optional.of(file));
        when(storageService.openInputStream("path/to/file")).thenReturn(new ByteArrayInputStream(content));

        IntegrityVerificationResponse response = evidenceService.verifyEvidenceIntegrity(evidenceId);

        assertThat(response.isMatch()).isTrue();
        assertThat(response.getIntegrityStatus()).isEqualTo(IntegrityStatus.VERIFIED);
        assertThat(response.getRecordedSha256()).isEqualTo(sha256);
        assertThat(response.getCalculatedSha256()).isEqualTo(sha256);
    }

    @Test
    @DisplayName("Throw exception when empty file is uploaded")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        assertThatThrownBy(() -> evidenceService.uploadAndIngestEvidence(uploadRequest, emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid non-empty file");
    }
}
