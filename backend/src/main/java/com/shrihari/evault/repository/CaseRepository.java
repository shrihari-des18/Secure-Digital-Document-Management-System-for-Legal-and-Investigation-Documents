package com.shrihari.evault.repository;

import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CaseRepository extends JpaRepository<Case, UUID> {

    Optional<Case> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);

    List<Case> findByLeadOfficerId(UUID leadOfficerId);

    List<Case> findByStatus(CaseStatus status);
}
