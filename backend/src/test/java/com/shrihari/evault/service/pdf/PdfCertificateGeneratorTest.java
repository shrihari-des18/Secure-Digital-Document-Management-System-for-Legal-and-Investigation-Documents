package com.shrihari.evault.service.pdf;

import com.shrihari.evault.dto.CustodyEventResponse;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.Certificate;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfCertificateGeneratorTest {

    private final PdfCertificateGenerator generator = new PdfCertificateGenerator();

    @Test
    @DisplayName("Generate valid court-ready BSA §63 certificate PDF")
    void testGenerateCertificatePdf() {
        UUID caseId = UUID.randomUUID();
        UUID officerId = UUID.randomUUID();
        UUID evidenceId = UUID.randomUUID();

        Case testCase = Case.builder()
                .caseNumber("FIR-2026-DEL-042")
                .title("Financial Fraud Investigation")
                .jurisdiction("New Delhi Central")
                .status(CaseStatus.IN_COURT)
                .leadOfficerId(officerId)
                .build();
        testCase.setId(caseId);

        EvidenceItem item = EvidenceItem.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-DEL-042")
                .title("Seized Encrypted USB Drive")
                .deviceMake("Kingston")
                .deviceModel("DataTraveler 32GB")
                .deviceSerialNumber("SN-892182")
                .seizedLocation("Room 102, Connaught Place, New Delhi")
                .seizedAt(Instant.now().minusSeconds(86400))
                .currentCustodianId(officerId)
                .build();
        item.setId(evidenceId);

        EvidenceFile file = EvidenceFile.builder()
                .evidenceId(evidenceId)
                .originalFileName("usb_image.dd")
                .sizeBytes(32000000000L)
                .sha256("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855")
                .sha512("cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e")
                .build();

        User officer = User.builder()
                .fullName("Rajesh Kumar")
                .badgeNumber("DL-POL-4411")
                .department("Cyber Crime Unit")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        officer.setId(officerId);

        Certificate certificate = Certificate.builder()
                .certificateNumber("CERT-BSA63-2026-0001")
                .standard("BSA_2023_S63")
                .hashSnapshotSha256(file.getSha256())
                .hashSnapshotSha512(file.getSha512())
                .mannerOfProduction("Forensic disk clone acquired using Tableau write-blocker.")
                .issuedByOfficerId(officerId)
                .issuedAt(Instant.now())
                .digitalSignature("3f9a81b2c3d4e5f6...")
                .signingKeyId("VAULT_KMS_ED25519_2026")
                .build();

        CustodyEventResponse custodyEvent = CustodyEventResponse.builder()
                .eventType(CustodyEventType.INGEST)
                .actorId(officerId)
                .reason("Evidence sealed upon seizure")
                .actorSignature("sig-ingest-01")
                .occurredAt(Instant.now().minusSeconds(80000))
                .build();

        byte[] pdfBytes = generator.generateBsa63Certificate(
                certificate,
                item,
                file,
                testCase,
                officer,
                List.of(custodyEvent)
        );

        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(500);

        // Verify PDF Header Magic Bytes "%PDF-"
        String header = new String(pdfBytes, 0, 5, StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }
}
