package com.shrihari.evault.service;

import com.shrihari.evault.common.ActorContext;
import com.shrihari.evault.common.ActorContextHolder;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.AddDocumentVersionRequest;
import com.shrihari.evault.dto.CreateLegalDocumentRequest;
import com.shrihari.evault.dto.DocumentVersionResponse;
import com.shrihari.evault.dto.LegalDocumentResponse;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.InvalidOperationException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.DocumentVersion;
import com.shrihari.evault.model.LegalDocument;
import com.shrihari.evault.model.CaseActivityType;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.DocumentVersionRepository;
import com.shrihari.evault.repository.LegalDocumentRepository;
import com.shrihari.evault.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LegalDocumentService {

    private final LegalDocumentRepository legalDocumentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final CaseRepository caseRepository;
    private final StorageService storageService;
    private final AuditLedgerService auditLedgerService;
    private final AlarmDispatcherService alarmDispatcherService;

    @Value("${app.crypto.system-secret:SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026}")
    private String systemSecret = "SIH26190_CHAIN_OF_CUSTODY_SECRET_KEY_2026";

    @Transactional
    public LegalDocumentResponse createDocument(CreateLegalDocumentRequest request, MultipartFile file) throws IOException {
        log.info("Creating legal document: {} for case: {}", request.getDocumentNumber(), request.getCaseId());

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Legal document payload must include a non-empty file");
        }

        Case parentCase = caseRepository.findById(request.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Case not found with ID: " + request.getCaseId()));

        if (parentCase.getStatus() == CaseStatus.ARCHIVED || parentCase.getStatus() == CaseStatus.CLOSED) {
            throw new InvalidOperationException("Cannot attach legal documents to a closed or archived case");
        }

        if (legalDocumentRepository.existsByDocumentNumber(request.getDocumentNumber())) {
            throw new DuplicateResourceException("Document number '" + request.getDocumentNumber() + "' already exists");
        }

        ActorContext actor = ActorContextHolder.get();
        UUID actorId = actor.getActorId();

        // 1. Calculate SHA-256 hash
        byte[] fileBytes = file.getBytes();
        String sha256 = CryptoUtil.calculateSha256(fileBytes);

        // 2. Digital Signature
        String signaturePayload = sha256 + "|" + request.getDocumentNumber() + "|v1|" + Instant.now();
        String digitalSignature = CryptoUtil.generateSignature(signaturePayload, systemSecret);

        // 3. Store file
        String subDirectory = "documents/" + request.getCaseId();
        String storagePath = storageService.store(file, subDirectory);

        // 4. Save LegalDocument
        LegalDocument doc = LegalDocument.builder()
                .caseId(request.getCaseId())
                .documentNumber(request.getDocumentNumber().trim())
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .documentType(request.getDocumentType())
                .currentVersion(1)
                .createdBy(actorId)
                .build();

        LegalDocument savedDoc = legalDocumentRepository.save(doc);

        // 5. Save DocumentVersion (Version 1)
        DocumentVersion version = DocumentVersion.builder()
                .documentId(savedDoc.getId())
                .versionNumber(1)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .storagePath(storagePath)
                .sha256(sha256)
                .changeSummary(request.getChangeSummary() != null ? request.getChangeSummary() : "Initial filing")
                .uploadedBy(actorId)
                .digitalSignature(digitalSignature)
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(version);

        // 6. Append to Audit Ledger
        String details = "{\"docNumber\":\"" + savedDoc.getDocumentNumber() + "\",\"type\":\"" + savedDoc.getDocumentType() + "\",\"v\":1,\"sha256\":\"" + sha256 + "\"}";
        auditLedgerService.appendEntry(actorId, "DOCUMENT_CREATED", "DOCUMENT:" + savedDoc.getDocumentNumber(), details);

        if (alarmDispatcherService != null) {
            try {
                alarmDispatcherService.dispatchCaseActivity(
                        savedDoc.getCaseId(),
                        CaseActivityType.DOCUMENT_VERSION_ADDED,
                        actorId,
                        actor != null ? actor.getUsername() : null,
                        actor != null ? actor.getRole() : null,
                        "Legal document filed: " + savedDoc.getDocumentNumber() + " (" + savedDoc.getTitle() + ") v1",
                        Map.of(
                                "documentId", savedDoc.getId().toString(),
                                "documentNumber", savedDoc.getDocumentNumber(),
                                "version", 1,
                                "sha256", sha256
                        )
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch case activity for legal document creation", e);
            }
        }

        return LegalDocumentResponse.fromEntity(savedDoc, DocumentVersionResponse.fromEntity(savedVersion));
    }

    @Transactional
    public DocumentVersionResponse addDocumentVersion(UUID documentId, AddDocumentVersionRequest request, MultipartFile file) throws IOException {
        log.info("Adding new version to document ID: {}", documentId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Revision file cannot be empty");
        }

        LegalDocument doc = getEntityById(documentId);

        Case parentCase = caseRepository.findById(doc.getCaseId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent case not found"));

        if (parentCase.getStatus() == CaseStatus.ARCHIVED || parentCase.getStatus() == CaseStatus.CLOSED) {
            throw new InvalidOperationException("Cannot revise documents on a closed or archived case");
        }

        ActorContext actor = ActorContextHolder.get();
        UUID actorId = actor.getActorId();

        int nextVersion = doc.getCurrentVersion() + 1;

        // 1. Calculate SHA-256 hash
        byte[] fileBytes = file.getBytes();
        String sha256 = CryptoUtil.calculateSha256(fileBytes);

        // 2. Digital Signature
        String signaturePayload = sha256 + "|" + doc.getDocumentNumber() + "|v" + nextVersion + "|" + Instant.now();
        String digitalSignature = CryptoUtil.generateSignature(signaturePayload, systemSecret);

        // 3. Store file
        String subDirectory = "documents/" + doc.getCaseId();
        String storagePath = storageService.store(file, subDirectory);

        // 4. Save Version
        DocumentVersion version = DocumentVersion.builder()
                .documentId(doc.getId())
                .versionNumber(nextVersion)
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .storagePath(storagePath)
                .sha256(sha256)
                .changeSummary(request.getChangeSummary())
                .uploadedBy(actorId)
                .digitalSignature(digitalSignature)
                .build();

        DocumentVersion savedVersion = documentVersionRepository.save(version);

        // 5. Update LegalDocument current version
        doc.setCurrentVersion(nextVersion);
        legalDocumentRepository.save(doc);

        // 6. Append to Audit Ledger
        String details = "{\"docNumber\":\"" + doc.getDocumentNumber() + "\",\"v\":" + nextVersion + ",\"sha256\":\"" + sha256 + "\",\"reason\":\"" + request.getChangeSummary() + "\"}";
        auditLedgerService.appendEntry(actorId, "DOCUMENT_REVISED", "DOCUMENT:" + doc.getDocumentNumber() + ":v" + nextVersion, details);

        if (alarmDispatcherService != null) {
            try {
                alarmDispatcherService.dispatchCaseActivity(
                        doc.getCaseId(),
                        CaseActivityType.DOCUMENT_VERSION_ADDED,
                        actorId,
                        actor != null ? actor.getUsername() : null,
                        actor != null ? actor.getRole() : null,
                        "Legal document revised: " + doc.getDocumentNumber() + " v" + nextVersion + " (" + request.getChangeSummary() + ")",
                        Map.of(
                                "documentId", doc.getId().toString(),
                                "documentNumber", doc.getDocumentNumber(),
                                "version", nextVersion,
                                "changeSummary", request.getChangeSummary() != null ? request.getChangeSummary() : "",
                                "sha256", sha256
                        )
                );
            } catch (Exception e) {
                log.warn("Failed to dispatch case activity for document revision", e);
            }
        }

        return DocumentVersionResponse.fromEntity(savedVersion);
    }

    @Transactional(readOnly = true)
    public LegalDocumentResponse getDocumentById(UUID documentId) {
        LegalDocument doc = getEntityById(documentId);
        DocumentVersion latest = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId).orElse(null);
        return LegalDocumentResponse.fromEntity(doc, DocumentVersionResponse.fromEntity(latest));
    }

    @Transactional(readOnly = true)
    public List<LegalDocumentResponse> getDocumentsByCaseId(UUID caseId) {
        List<LegalDocument> docs = legalDocumentRepository.findByCaseId(caseId);
        return docs.stream()
                .map(doc -> {
                    DocumentVersion latest = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(doc.getId()).orElse(null);
                    return LegalDocumentResponse.fromEntity(doc, DocumentVersionResponse.fromEntity(latest));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DocumentVersionResponse> getDocumentHistory(UUID documentId) {
        if (!legalDocumentRepository.existsById(documentId)) {
            throw new ResourceNotFoundException("Document not found with ID: " + documentId);
        }

        return documentVersionRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(DocumentVersionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public Resource loadDocumentFileResource(UUID documentId, Integer versionNumber) {
        DocumentVersion version;
        if (versionNumber != null) {
            version = documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Document version " + versionNumber + " not found for document: " + documentId));
        } else {
            version = documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("No versions found for document: " + documentId));
        }

        ActorContext actor = ActorContextHolder.get();
        auditLedgerService.appendEntry(actor.getActorId(), "DOCUMENT_DOWNLOAD", "DOCUMENT:" + documentId + ":v" + version.getVersionNumber(), "{}");

        return storageService.loadAsResource(version.getStoragePath());
    }

    @Transactional(readOnly = true)
    public DocumentVersion getVersionMetadata(UUID documentId, Integer versionNumber) {
        if (versionNumber != null) {
            return documentVersionRepository.findByDocumentIdAndVersionNumber(documentId, versionNumber)
                    .orElseThrow(() -> new ResourceNotFoundException("Document version " + versionNumber + " not found for document: " + documentId));
        } else {
            return documentVersionRepository.findTopByDocumentIdOrderByVersionNumberDesc(documentId)
                    .orElseThrow(() -> new ResourceNotFoundException("No versions found for document: " + documentId));
        }
    }

    @Transactional(readOnly = true)
    public LegalDocument getEntityById(UUID documentId) {
        return legalDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Legal document not found with ID: " + documentId));
    }
}
