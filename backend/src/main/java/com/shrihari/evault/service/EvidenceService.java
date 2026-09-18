package com.shrihari.evault.service;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.EvidenceDetailResponse;
import com.shrihari.evault.dto.EvidenceFileResponse;
import com.shrihari.evault.dto.IntegrityVerificationResponse;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.InvalidOperationException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceFile;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.model.IntegrityStatus;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.EvidenceFileRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.service.crypto.EnvelopeEncryptionEngine;
import com.shrihari.evault.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EvidenceService {

    private final EvidenceItemRepository evidenceItemRepository;
    private final EvidenceFileRepository evidenceFileRepository;
    private final CaseRepository caseRepository;
    private final StorageService storageService;
    private final CustodyService custodyService;
    private final EnvelopeEncryptionEngine envelopeEncryptionEngine;

    @Value("${app.crypto.system-secret:SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026}")
    private String systemSecret = "SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026";

    private static final String SIGNING_KEY_ID = "VAULT_KMS_HMAC256_2026";

    @Transactional
    public EvidenceDetailResponse uploadAndIngestEvidence(UploadEvidenceRequest request, MultipartFile file) throws IOException {
        log.info("Ingesting evidence: {} for case: {}", request.getEvidenceNumber(), request.getCaseId());

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Evidence payload must contain a valid non-empty file");
        }

        // Verify case exists and allows new evidence
        Case parentCase = caseRepository.findById(request.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + request.getCaseId()));

        if (parentCase.getStatus() == CaseStatus.ARCHIVED || parentCase.getStatus() == CaseStatus.CLOSED) {
            throw new InvalidOperationException("Cannot add evidence to a closed or archived case");
        }

        // Check duplicate evidence number
        if (evidenceItemRepository.existsByEvidenceNumber(request.getEvidenceNumber())) {
            throw new DuplicateResourceException("Evidence with number '" + request.getEvidenceNumber() + "' already exists");
        }

        ActorContext actor = ActorContextHolder.get();
        UUID actorId = actor.getActorId();

        // 1. Calculate server-side hashes
        byte[] fileBytes = file.getBytes();
        String serverSha256 = CryptoUtil.calculateSha256(fileBytes);
        String serverSha512 = CryptoUtil.calculateSha512(fileBytes);

        // 2. Validate client-side hash if supplied
        IntegrityStatus integrityStatus = IntegrityStatus.VERIFIED;
        if (request.getClientSha256() != null && !request.getClientSha256().isBlank()) {
            String clientHash = request.getClientSha256().trim().toLowerCase();
            if (!clientHash.equalsIgnoreCase(serverSha256)) {
                log.warn("Integrity hash mismatch! Client: {}, Server: {}", clientHash, serverSha256);
                integrityStatus = IntegrityStatus.MISMATCH;
            }
        }

        // 3. Digital signature over hash + metadata
        String signaturePayload = serverSha256 + "|" + request.getEvidenceNumber() + "|" + Instant.now();
        String digitalSignature = CryptoUtil.generateSignature(signaturePayload, systemSecret);

        // 4. Store file to storage subsystem
        String subDirectory = "cases/" + request.getCaseId();
        String storagePath = storageService.store(file, subDirectory);

        // 5. Persist EvidenceItem
        EvidenceItem item = EvidenceItem.builder()
                .caseId(request.getCaseId())
                .evidenceNumber(request.getEvidenceNumber().trim())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .classification(request.getClassification())
                .custodyState(CustodyState.INGESTED)
                .deviceMake(request.getDeviceMake())
                .deviceModel(request.getDeviceModel())
                .deviceSerialNumber(request.getDeviceSerialNumber())
                .seizedLocation(request.getSeizedLocation())
                .seizedAt(request.getSeizedAt() != null ? request.getSeizedAt() : Instant.now())
                .seizedByOfficerId(request.getSeizedByOfficerId() != null ? request.getSeizedByOfficerId() : actorId)
                .currentCustodianId(actorId)
                .createdBy(actorId)
                .build();

        EvidenceItem savedItem = evidenceItemRepository.save(item);

        // 6. Generate DEK and wrap via Vault KMS
        byte[] rawDek = envelopeEncryptionEngine.generateDek();
        String wrappedDek = envelopeEncryptionEngine.wrapDek(rawDek);

        // 6. Persist EvidenceFile
        EvidenceFile evidenceFile = EvidenceFile.builder()
                .evidenceId(savedItem.getId())
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .storagePath(storagePath)
                .sha256(serverSha256)
                .sha512(serverSha512)
                .clientSha256(request.getClientSha256())
                .integrityStatus(integrityStatus)
                .digitalSignature(digitalSignature)
                .signingKeyId(envelopeEncryptionEngine.getMasterKeyName())
                .dekWrapped(wrappedDek)
                .build();

        EvidenceFile savedFile = evidenceFileRepository.save(evidenceFile);

        // 7. Record Custody Ingestion Event and Chain into Audit Ledger
        custodyService.recordEvent(
                savedItem.getId(),
                CustodyEventType.INGEST,
                actorId,
                null,
                actorId,
                "Initial intake and cryptographic sealing"
        );

        log.info("Evidence item {} sealed with SHA-256: {}", savedItem.getId(), serverSha256);
        return EvidenceDetailResponse.fromEntity(savedItem, EvidenceFileResponse.fromEntity(savedFile));
    }

    @Transactional
    public EvidenceDetailResponse getEvidenceById(UUID evidenceId) {
        EvidenceItem item = getEntityById(evidenceId);
        EvidenceFile file = evidenceFileRepository.findByEvidenceId(evidenceId).orElse(null);

        // Record Custody VIEW event
        ActorContext actor = ActorContextHolder.get();
        custodyService.recordEvent(
                evidenceId,
                CustodyEventType.VIEW,
                actor.getActorId(),
                null,
                null,
                "Viewed evidence item details"
        );

        return EvidenceDetailResponse.fromEntity(item, EvidenceFileResponse.fromEntity(file));
    }

    @Transactional(readOnly = true)
    public List<EvidenceDetailResponse> getEvidenceByCaseId(UUID caseId) {
        List<EvidenceItem> items = evidenceItemRepository.findByCaseId(caseId);
        return items.stream()
                .map(item -> {
                    EvidenceFile file = evidenceFileRepository.findByEvidenceId(item.getId()).orElse(null);
                    return EvidenceDetailResponse.fromEntity(item, EvidenceFileResponse.fromEntity(file));
                })
                .toList();
    }

    @Transactional
    public Resource loadEvidenceFileResource(UUID evidenceId) {
        EvidenceFile file = evidenceFileRepository.findByEvidenceId(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file record not found for evidence ID: " + evidenceId));

        // Record Custody DOWNLOAD event
        ActorContext actor = ActorContextHolder.get();
        custodyService.recordEvent(
                evidenceId,
                CustodyEventType.DOWNLOAD,
                actor.getActorId(),
                null,
                null,
                "Downloaded raw evidence binary file"
        );

        return storageService.loadAsResource(file.getStoragePath());
    }

    @Transactional(readOnly = true)
    public EvidenceFile getEvidenceFileMetadata(UUID evidenceId) {
        return evidenceFileRepository.findByEvidenceId(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file not found for evidence ID: " + evidenceId));
    }

    @Transactional(readOnly = true)
    public IntegrityVerificationResponse verifyEvidenceIntegrity(UUID evidenceId) {
        log.info("Verifying integrity for evidence ID: {}", evidenceId);
        EvidenceItem item = getEntityById(evidenceId);
        EvidenceFile file = evidenceFileRepository.findByEvidenceId(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence file not found for evidence ID: " + evidenceId));

        try (InputStream is = storageService.openInputStream(file.getStoragePath())) {
            String calculatedSha256 = CryptoUtil.calculateSha256(is);
            boolean matches = calculatedSha256.equalsIgnoreCase(file.getSha256());

            return IntegrityVerificationResponse.builder()
                    .evidenceId(evidenceId)
                    .evidenceNumber(item.getEvidenceNumber())
                    .recordedSha256(file.getSha256())
                    .calculatedSha256(calculatedSha256)
                    .match(matches)
                    .integrityStatus(matches ? IntegrityStatus.VERIFIED : IntegrityStatus.MISMATCH)
                    .digitalSignature(file.getDigitalSignature())
                    .message(matches ? "Evidence integrity VERIFIED — byte sequence unchanged from intake"
                            : "CRITICAL ALERT: Evidence integrity compromised! Computed hash does not match intake hash.")
                    .verifiedAt(Instant.now())
                    .build();
        } catch (IOException e) {
            log.error("Failed to read evidence file from storage for verification", e);
            throw new IllegalStateException("Failed to verify evidence file integrity", e);
        }
    }

    @Transactional(readOnly = true)
    public EvidenceItem getEntityById(UUID evidenceId) {
        return evidenceItemRepository.findById(evidenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Evidence item not found with ID: " + evidenceId));
    }
}
