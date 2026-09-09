package com.shrihari.evault.repository;

import com.shrihari.evault.model.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, UUID> {

    Optional<Certificate> findByCertificateNumber(String certificateNumber);

    boolean existsByCertificateNumber(String certificateNumber);

    List<Certificate> findByEvidenceId(UUID evidenceId);

    List<Certificate> findByIssuedByOfficerId(UUID officerId);
}
