package com.shrihari.evault.service;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.CertificateResponse;
import com.shrihari.evault.dto.CustodyEventResponse;
import com.shrihari.evault.dto.IssueCertificateRequest;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.Case;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final EvidenceItemRepository evidenceItemRepository;
    private final EvidenceFileRepository evidenceFileRepository;
    private final CaseRepository caseRepository;
    private final UserRepository userRepository;
    private final CustodyService custodyService;
    private final AuditLedgerService auditLedgerService;
    private final StorageService storageService;
    private final PdfCertificateGenerator pdfCertificateGenerator;

    @Value("${app.crypto.system-secret:SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026}")
    private String systemSecret = "SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026";

    private static final String SIGNING_KEY_ID = "VAULT_KMS_BSA63_CERT_2026";

    @Transactional
    public CertificateResponse issueCertificate(UUID evidenceId, IssueCertificateRequest request) throws IOException {
        log.info("Issuing BSA 2023 §63 Certificate for evidence ID: {}", evidenceId);

        EvidenceItem evidence = evidenceItemRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence item not found with ID: " + evidenceId));

        EvidenceFile file = evidenceFileRepository.findByEvidenceId(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file record not found for ID: " + evidenceId));

        Case parentCase = caseRepository.findById(evidence.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + evidence.getCaseId()));

        ActorContext actor = ActorContextHolder.get();
        User officer = userRepository.findById(actor.getActorId())
                .orElseGet(() -> userRepository.findById(evidence.getCurrentCustodianId())
                        .orElse(User.builder()
                                .fullName("Investigating Officer")
                                .badgeNumber("IND-POLICE")
                                .department("Law Enforcement Division")
                                .role(RoleEnum.INVESTIGATING_OFFICER)
                                .build()));

        List<CustodyEventResponse> custodyEvents = custodyService.getCustodyTimeline(evidenceId);

        String certNumber = "CERT-BSA63-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Instant issuedAt = Instant.now();

        String signaturePayload = evidence.getId() + "|" + certNumber + "|" + file.getSha256() + "|" + issuedAt.toEpochMilli();
        String digitalSignature = CryptoUtil.generateSignature(signaturePayload, systemSecret);

        String devParticulars = (evidence.getDeviceMake() != null ? evidence.getDeviceMake() : "Storage Media") + " " +
                (evidence.getDeviceModel() != null ? evidence.getDeviceModel() : "") +
                (evidence.getDeviceSerialNumber() != null ? " (S/N: " + evidence.getDeviceSerialNumber() + ")" : "");

        String mannerOfProduction = (request != null && request.getMannerOfProduction() != null && !request.getMannerOfProduction().isBlank())
                ? request.getMannerOfProduction()
                : "Cryptographically acquired via forensic intake protocol in official custody under BSA §63.";

        Certificate certificate = Certificate.builder()
                .evidenceId(evidenceId)
                .certificateNumber(certNumber)
                .standard("BSA_2023_S63")
                .pdfStoragePath("") // set after storage
                .hashSnapshotSha256(file.getSha256())
                .hashSnapshotSha512(file.getSha512())
                .mannerOfProduction(mannerOfProduction)
                .deviceParticulars(devParticulars)
                .issuedByOfficerId(officer.getId() != null ? officer.getId() : actor.getActorId())
                .issuedAt(issuedAt)
                .digitalSignature(digitalSignature)
                .signingKeyId(SIGNING_KEY_ID)
                .build();

        // Generate PDF
        byte[] pdfBytes = pdfCertificateGenerator.generateBsa63Certificate(
                certificate,
                evidence,
                file,
                parentCase,
                officer,
                custodyEvents
        );

        // Store PDF file
        String pdfStoragePath = storageService.store(pdfBytes, certNumber + ".pdf", "certificates/" + evidence.getCaseId());
        certificate.setPdfStoragePath(pdfStoragePath);

        Certificate savedCert = certificateRepository.save(certificate);

        // Update evidence custody state
        evidence.setCustodyState(CustodyState.CERTIFIED);
        evidenceItemRepository.save(evidence);

        // Record Custody CERTIFY Event
        custodyService.recordEvent(
                evidenceId,
                CustodyEventType.CERTIFY,
                officer.getId() != null ? officer.getId() : actor.getActorId(),
                null,
                null,
                "Issued statutory court admissibility certificate " + certNumber + " under BSA §63"
        );

        // Append to Audit Ledger
        String details = "{\"certNumber\":\"" + certNumber + "\",\"standard\":\"BSA_2023_S63\",\"evidenceSha256\":\"" + file.getSha256() + "\"}";
        auditLedgerService.appendEntry(actor.getActorId(), "CERTIFICATE_ISSUED", "CERTIFICATE:" + certNumber, details);

        log.info("BSA §63 Certificate {} successfully issued and sealed", certNumber);
        return CertificateResponse.fromEntity(savedCert);
    }

    @Transactional(readOnly = true)
    public CertificateResponse getCertificateById(UUID id) {
        Certificate cert = getEntityById(id);
        return CertificateResponse.fromEntity(cert);
    }

    @Transactional(readOnly = true)
    public List<CertificateResponse> getCertificatesByEvidenceId(UUID evidenceId) {
        return certificateRepository.findByEvidenceId(evidenceId).stream()
                .map(CertificateResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Resource loadCertificatePdfResource(UUID id) {
        Certificate cert = getEntityById(id);
        return storageService.loadAsResource(cert.getPdfStoragePath());
    }

    @Transactional(readOnly = true)
    public Certificate getEntityById(UUID id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Certificate not found with ID: " + id));
    }
}
