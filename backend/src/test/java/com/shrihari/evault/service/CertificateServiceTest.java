package com.shrihari.evault.service;

import com.shrihari.evault.dto.CertificateResponse;
import com.shrihari.evault.dto.IssueCertificateRequest;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.Certificate;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.CertificateRepository;
import com.shrihari.evault.repository.EvidenceFileRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.repository.UserRepository;
import com.shrihari.evault.service.pdf.PdfCertificateGenerator;
import com.shrihari.evault.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private EvidenceFileRepository evidenceFileRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustodyService custodyService;

    @Mock
    private AuditLedgerService auditLedgerService;

    @Mock
    private StorageService storageService;

    @Mock
    private PdfCertificateGenerator pdfCertificateGenerator;

    @Mock
    private com.shrihari.evault.service.tsa.Rfc3161TimestampService rfc3161TimestampService;

    @InjectMocks
    private CertificateService certificateService;

    private UUID evidenceId;
    private UUID caseId;
    private UUID officerId;
    private EvidenceItem item;
    private EvidenceFile file;
    private Case testCase;
    private User officer;

    @BeforeEach
    void setUp() {
        evidenceId = UUID.randomUUID();
        caseId = UUID.randomUUID();
        officerId = UUID.randomUUID();

        item = EvidenceItem.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-TEST-001")
                .title("Mobile Dump")
                .currentCustodianId(officerId)
                .custodyState(CustodyState.INGESTED)
                .build();
        item.setId(evidenceId);

        file = EvidenceFile.builder()
                .evidenceId(evidenceId)
                .originalFileName("dump.tar")
                .sha256("b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9")
                .sha512("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e")
                .build();

        testCase = Case.builder()
                .caseNumber("CASE-2026-001")
                .jurisdiction("Jurisdiction Test")
                .status(CaseStatus.UNDER_INVESTIGATION)
                .leadOfficerId(officerId)
                .build();
        testCase.setId(caseId);

        officer = User.builder()
                .fullName("Inspector Vikram")
                .badgeNumber("VK-100")
                .department("Investigation")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        officer.setId(officerId);
    }

    @Test
    @DisplayName("Issue BSA §63 certificate, render PDF, update custody, and log to audit ledger")
    void shouldIssueCertificateSuccessfully() throws IOException {
        when(evidenceItemRepository.findById(evidenceId)).thenReturn(Optional.of(item));
        when(evidenceFileRepository.findByEvidenceId(evidenceId)).thenReturn(Optional.of(file));
        when(caseRepository.findById(caseId)).thenReturn(Optional.of(testCase));
        when(userRepository.findById(any())).thenReturn(Optional.of(officer));
        when(custodyService.getCustodyTimeline(evidenceId)).thenReturn(Collections.emptyList());

        byte[] dummyPdf = "%PDF-1.4 DUMMY CERT".getBytes();
        when(pdfCertificateGenerator.generateBsa63Certificate(any(), any(), any(), any(), any(), any()))
                .thenReturn(dummyPdf);

        when(storageService.store(eq(dummyPdf), any(String.class), any(String.class)))
                .thenReturn("certificates/" + caseId + "/cert.pdf");

        when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> {
            Certificate c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        IssueCertificateRequest req = IssueCertificateRequest.builder()
                .mannerOfProduction("Bitstream copy produced under ISO/IEC 27037 protocol")
                .build();

        CertificateResponse response = certificateService.issueCertificate(evidenceId, req);

        assertThat(response).isNotNull();
        assertThat(response.getStandard()).isEqualTo("BSA_2023_S63");
        assertThat(response.getHashSnapshotSha256()).isEqualTo(file.getSha256());
        assertThat(item.getCustodyState()).isEqualTo(CustodyState.CERTIFIED);

        verify(custodyService).recordEvent(eq(evidenceId), eq(CustodyEventType.CERTIFY), any(), any(), any(), any());
        verify(auditLedgerService).appendEntry(any(), eq("CERTIFICATE_ISSUED"), any(), any());
    }
}
