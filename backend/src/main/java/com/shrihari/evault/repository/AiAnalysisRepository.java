package com.shrihari.evault.repository;

import com.shrihari.evault.model.AiAnalysis;
import com.shrihari.evault.model.AnalysisStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    Optional<AiAnalysis> findByEvidenceId(UUID evidenceId);

    boolean existsByEvidenceId(UUID evidenceId);

    List<AiAnalysis> findByStatus(AnalysisStatus status);
}
