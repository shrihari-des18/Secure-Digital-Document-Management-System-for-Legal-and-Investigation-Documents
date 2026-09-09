package com.shrihari.evault.service;

import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.LedgerVerificationResult;
import com.shrihari.evault.model.AuditLedger;
import com.shrihari.evault.repository.AuditLedgerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLedgerServiceTest {

    @Mock
    private AuditLedgerRepository auditLedgerRepository;

    @InjectMocks
    private AuditLedgerService auditLedgerService;

    @Test
    @DisplayName("Append entry correctly links to genesis block for sequence 1")
    void shouldAppendGenesisEntry() {
        when(auditLedgerRepository.findTopByOrderBySeqDesc()).thenReturn(Optional.empty());
        when(auditLedgerRepository.save(any(AuditLedger.class))).thenAnswer(i -> {
            AuditLedger l = i.getArgument(0);
            l.setSeq(1L);
            return l;
        });

        UUID actorId = UUID.randomUUID();
        AuditLedger ledger = auditLedgerService.appendEntry(actorId, "EVIDENCE_INGEST", "EVIDENCE:EVID-001", "{}");

        assertThat(ledger).isNotNull();
        assertThat(ledger.getSeq()).isEqualTo(1L);
        assertThat(ledger.getPrevHash()).isEqualTo(AuditLedgerService.GENESIS_HASH);
        assertThat(ledger.getEntryHash()).hasSize(64);
        verify(auditLedgerRepository).save(any(AuditLedger.class));
    }

    @Test
    @DisplayName("Verify clean ledger returns 100% valid")
    void shouldVerifyCleanLedger() {
        Instant t1 = Instant.now();
        Instant t2 = t1.plusSeconds(5);
        UUID actor = UUID.randomUUID();

        // Block 1
        String hash1 = AuditLedgerService.calculateHash(1L, t1, actor, "INGEST", "EVID-001", "details1", AuditLedgerService.GENESIS_HASH);
        AuditLedger block1 = AuditLedger.builder()
                .seq(1L)
                .occurredAt(t1)
                .actorId(actor)
                .action("INGEST")
                .resourceRef("EVID-001")
                .details("details1")
                .prevHash(AuditLedgerService.GENESIS_HASH)
                .entryHash(hash1)
                .build();

        // Block 2
        String hash2 = AuditLedgerService.calculateHash(2L, t2, actor, "TRANSFER", "EVID-001", "details2", hash1);
        AuditLedger block2 = AuditLedger.builder()
                .seq(2L)
                .occurredAt(t2)
                .actorId(actor)
                .action("TRANSFER")
                .resourceRef("EVID-001")
                .details("details2")
                .prevHash(hash1)
                .entryHash(hash2)
                .build();

        when(auditLedgerRepository.findAllByOrderBySeqAsc()).thenReturn(List.of(block1, block2));

        LedgerVerificationResult result = auditLedgerService.verifyLedgerIntegrity();

        assertThat(result.isValid()).isTrue();
        assertThat(result.getTotalEntries()).isEqualTo(2);
        assertThat(result.getLastVerifiedSeq()).isEqualTo(2L);
        assertThat(result.getHeadHash()).isEqualTo(hash2);
    }

    @Test
    @DisplayName("Detect tampering when block content has been modified")
    void shouldDetectTamperedRowData() {
        Instant t1 = Instant.now();
        UUID actor = UUID.randomUUID();

        String hash1 = AuditLedgerService.calculateHash(1L, t1, actor, "INGEST", "EVID-001", "original", AuditLedgerService.GENESIS_HASH);

        // Tamper with details without updating entryHash
        AuditLedger tamperedBlock = AuditLedger.builder()
                .seq(1L)
                .occurredAt(t1)
                .actorId(actor)
                .action("INGEST")
                .resourceRef("EVID-001")
                .details("TAMPERED_DETAILS") // altered!
                .prevHash(AuditLedgerService.GENESIS_HASH)
                .entryHash(hash1)
                .build();

        when(auditLedgerRepository.findAllByOrderBySeqAsc()).thenReturn(List.of(tamperedBlock));

        LedgerVerificationResult result = auditLedgerService.verifyLedgerIntegrity();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getBrokenSeq()).isEqualTo(1L);
        assertThat(result.getFailureReason()).contains("Tampered row content");
    }

    @Test
    @DisplayName("Detect tampering when prevHash linkage is broken")
    void shouldDetectBrokenLinkage() {
        Instant t1 = Instant.now();
        Instant t2 = t1.plusSeconds(5);
        UUID actor = UUID.randomUUID();

        String hash1 = AuditLedgerService.calculateHash(1L, t1, actor, "INGEST", "EVID-001", "d1", AuditLedgerService.GENESIS_HASH);
        AuditLedger block1 = AuditLedger.builder()
                .seq(1L).occurredAt(t1).actorId(actor).action("INGEST").resourceRef("EVID-001").details("d1")
                .prevHash(AuditLedgerService.GENESIS_HASH).entryHash(hash1).build();

        // Block 2 with bogus prevHash
        String fakePrevHash = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";
        String hash2 = AuditLedgerService.calculateHash(2L, t2, actor, "TRANSFER", "EVID-001", "d2", fakePrevHash);
        AuditLedger block2 = AuditLedger.builder()
                .seq(2L).occurredAt(t2).actorId(actor).action("TRANSFER").resourceRef("EVID-001").details("d2")
                .prevHash(fakePrevHash).entryHash(hash2).build();

        when(auditLedgerRepository.findAllByOrderBySeqAsc()).thenReturn(List.of(block1, block2));

        LedgerVerificationResult result = auditLedgerService.verifyLedgerIntegrity();

        assertThat(result.isValid()).isFalse();
        assertThat(result.getBrokenSeq()).isEqualTo(2L);
        assertThat(result.getFailureReason()).contains("Broken block linkage");
    }
}
