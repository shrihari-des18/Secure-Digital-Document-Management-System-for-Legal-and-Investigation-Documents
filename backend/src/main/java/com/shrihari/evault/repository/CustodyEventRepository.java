package com.shrihari.evault.repository;

import com.shrihari.evault.model.CustodyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustodyEventRepository extends JpaRepository<CustodyEvent, UUID> {

    List<CustodyEvent> findByEvidenceIdOrderByOccurredAtAsc(UUID evidenceId);

    List<CustodyEvent> findByActorId(UUID actorId);
}
