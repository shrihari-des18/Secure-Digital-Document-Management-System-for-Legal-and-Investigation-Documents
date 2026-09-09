package com.shrihari.evault.repository;

import com.shrihari.evault.model.AuditLedger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AuditLedgerRepository extends JpaRepository<AuditLedger, Long> {

    Optional<AuditLedger> findTopByOrderBySeqDesc();

    List<AuditLedger> findAllByOrderBySeqAsc();
}
