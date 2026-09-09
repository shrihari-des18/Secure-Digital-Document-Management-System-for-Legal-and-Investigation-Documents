package com.shrihari.evault.repository;

import com.shrihari.evault.model.EvidenceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EvidenceFileRepository extends JpaRepository<EvidenceFile, UUID> {

    Optional<EvidenceFile> findByEvidenceId(UUID evidenceId);

    List<EvidenceFile> findBySha256(String sha256);

    boolean existsBySha256(String sha256);
}
