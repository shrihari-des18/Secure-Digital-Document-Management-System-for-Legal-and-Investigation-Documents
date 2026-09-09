package com.shrihari.evault.repository;

import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceItemRepository extends JpaRepository<EvidenceItem, UUID> {

    Optional<EvidenceItem> findByEvidenceNumber(String evidenceNumber);

    boolean existsByEvidenceNumber(String evidenceNumber);

    List<EvidenceItem> findByCaseId(UUID caseId);

    List<EvidenceItem> findByCaseIdAndCustodyState(UUID caseId, CustodyState custodyState);

    List<EvidenceItem> findByCurrentCustodianId(UUID currentCustodianId);
}
