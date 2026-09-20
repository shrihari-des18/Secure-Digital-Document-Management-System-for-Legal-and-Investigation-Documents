package com.shrihari.evault.service;

import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.AuditLedgerResponse;
import com.shrihari.evault.dto.LedgerVerificationResult;
import com.shrihari.evault.model.AuditLedger;
import com.shrihari.evault.repository.AuditLedgerRepository;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLedgerService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLedgerRepository auditLedgerRepository;
    private final AlarmDispatcherService alarmDispatcherService;

    /**
     * Atomically appends a new block to the immutable hash-chained audit ledger.
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public synchronized AuditLedger appendEntry(UUID actorId, String action, String resourceRef, String details) {
        Optional<AuditLedger> latestEntryOpt = auditLedgerRepository.findTopByOrderBySeqDesc();

        long nextSeq = latestEntryOpt.map(ledger -> ledger.getSeq() + 1).orElse(1L);
        String prevHash = latestEntryOpt.map(AuditLedger::getEntryHash).orElse(GENESIS_HASH);
        Instant occurredAt = Instant.now();

        String entryHash = calculateHash(nextSeq, occurredAt, actorId, action, resourceRef, details, prevHash);

        AuditLedger ledger = AuditLedger.builder()
                .occurredAt(occurredAt)
                .actorId(actorId)
                .action(action)
                .resourceRef(resourceRef)
                .details(details)
                .prevHash(prevHash)
                .entryHash(entryHash)
                .build();

        AuditLedger saved = auditLedgerRepository.save(ledger);
        log.info("Appended audit ledger seq #{} [action={}, resource={}, hash={}]",
                saved.getSeq(), action, resourceRef, entryHash.substring(0, 12));
        return saved;
    }

    /**
     * Recomputes the entire cryptographic ledger chain from sequence 1 to current head.
     * Flags any broken linkage, missing block, or tampered payload.
     */
    @Transactional(readOnly = true)
    public LedgerVerificationResult verifyLedgerIntegrity() {
        log.info("Starting complete cryptographic audit ledger integrity verification...");
        List<AuditLedger> entries = auditLedgerRepository.findAllByOrderBySeqAsc();

        if (entries.isEmpty()) {
            return LedgerVerificationResult.builder()
                    .valid(true)
                    .totalEntries(0)
                    .lastVerifiedSeq(0L)
                    .headHash(GENESIS_HASH)
                    .message("Ledger is empty (Genesis block ready)")
                    .verifiedAt(Instant.now())
                    .build();
        }

        String expectedPrevHash = GENESIS_HASH;
        long expectedSeq = 1L;

        for (AuditLedger entry : entries) {
            // 1. Verify sequence continuity
            if (entry.getSeq() != expectedSeq) {
                log.error("Ledger sequence gap or tampering detected! Expected seq {}, found {}", expectedSeq, entry.getSeq());
                LedgerVerificationResult failure = LedgerVerificationResult.builder()
                        .valid(false)
                        .totalEntries(entries.size())
                        .brokenSeq(entry.getSeq())
                        .failureReason("Sequence discontinuity: Expected #" + expectedSeq + " but found #" + entry.getSeq())
                        .verifiedAt(Instant.now())
                        .build();
                dispatchLedgerAlarm(failure);
                return failure;
            }

            // 2. Verify previous hash linkage
            if (!entry.getPrevHash().equalsIgnoreCase(expectedPrevHash)) {
                log.error("Broken chain linkage at seq #{}: expected prevHash {}, found {}",
                        entry.getSeq(), expectedPrevHash, entry.getPrevHash());
                LedgerVerificationResult failure = LedgerVerificationResult.builder()
                        .valid(false)
                        .totalEntries(entries.size())
                        .brokenSeq(entry.getSeq())
                        .failureReason("Broken block linkage at sequence #" + entry.getSeq() +
                                ": prevHash does not match hash of sequence #" + (expectedSeq - 1))
                        .verifiedAt(Instant.now())
                        .build();
                dispatchLedgerAlarm(failure);
                return failure;
            }

            // 3. Recompute row hash over payload
            String calculatedHash = calculateHash(
                    entry.getSeq(),
                    entry.getOccurredAt(),
                    entry.getActorId(),
                    entry.getAction(),
                    entry.getResourceRef(),
                    entry.getDetails(),
                    entry.getPrevHash()
            );

            if (!entry.getEntryHash().equalsIgnoreCase(calculatedHash)) {
                log.error("Row data tampering detected at seq #{}: recorded {}, recalculated {}",
                        entry.getSeq(), entry.getEntryHash(), calculatedHash);
                LedgerVerificationResult failure = LedgerVerificationResult.builder()
                        .valid(false)
                        .totalEntries(entries.size())
                        .brokenSeq(entry.getSeq())
                        .failureReason("Tampered row content at sequence #" + entry.getSeq() +
                                ": stored entryHash does not match hash recalculated over row contents")
                        .verifiedAt(Instant.now())
                        .build();
                dispatchLedgerAlarm(failure);
                return failure;
            }

            expectedPrevHash = entry.getEntryHash();
            expectedSeq++;
        }

        AuditLedger lastEntry = entries.get(entries.size() - 1);
        log.info("Ledger integrity verified successfully. Total entries: {}, Head hash: {}",
                entries.size(), lastEntry.getEntryHash());

        return LedgerVerificationResult.builder()
                .valid(true)
                .totalEntries(entries.size())
                .lastVerifiedSeq(lastEntry.getSeq())
                .headHash(lastEntry.getEntryHash())
                .message("Ledger integrity 100% verified. All " + entries.size() + " blocks cryptographically valid.")
                .verifiedAt(Instant.now())
                .build();
    }

    @Transactional(readOnly = true)
    public List<AuditLedgerResponse> getAllEntries() {
        return auditLedgerRepository.findAllByOrderBySeqAsc().stream()
                .map(AuditLedgerResponse::fromEntity)
                .toList();
    }

    public static String calculateHash(long seq, Instant occurredAt, UUID actorId, String action,
                                       String resourceRef, String details, String prevHash) {
        String payload = seq + "|"
                + occurredAt.toEpochMilli() + "|"
                + (actorId != null ? actorId.toString() : "SYSTEM") + "|"
                + action + "|"
                + resourceRef + "|"
                + (details != null ? details : "") + "|"
                + prevHash;

        return CryptoUtil.calculateSha256(payload);
    }

    private void dispatchLedgerAlarm(LedgerVerificationResult failure) {
        if (alarmDispatcherService != null) {
            try {
                alarmDispatcherService.dispatchSecurityAlarm(
                        SecurityAlarmType.LEDGER_INTEGRITY_BREACH,
                        SecurityAlarmLevel.CRITICAL,
                        "LEDGER:" + failure.getBrokenSeq(),
                        "CRITICAL AUDIT LEDGER COMPROMISE: " + failure.getFailureReason(),
                        Map.of(
                                "brokenSeq", failure.getBrokenSeq(),
                                "failureReason", failure.getFailureReason(),
                                "totalEntries", failure.getTotalEntries()
                        ),
                        null
                );
            } catch (Exception e) {
                log.error("Failed to dispatch security alarm for ledger breach", e);
            }
        }
    }
}
