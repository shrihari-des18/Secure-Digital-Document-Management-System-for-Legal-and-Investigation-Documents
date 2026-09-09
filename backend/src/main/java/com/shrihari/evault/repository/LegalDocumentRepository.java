package com.shrihari.evault.repository;

import com.shrihari.evault.model.LegalDocument;
import com.shrihari.evault.model.LegalDocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, UUID> {

    Optional<LegalDocument> findByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumber(String documentNumber);

    List<LegalDocument> findByCaseId(UUID caseId);

    List<LegalDocument> findByCaseIdAndDocumentType(UUID caseId, LegalDocumentType documentType);
}
