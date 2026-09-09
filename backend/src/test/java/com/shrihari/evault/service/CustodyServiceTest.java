package com.shrihari.evault.service;

import com.shrihari.evault.dto.CustodyEventResponse;
import com.shrihari.evault.dto.CustodyTransferRequest;
import com.shrihari.evault.model.CustodyEvent;
import com.shrihari.evault.model.CustodyEventType;
import com.shrihari.evault.model.CustodyState;
import com.shrihari.evault.model.EvidenceItem;
import com.shrihari.evault.repository.CustodyEventRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustodyServiceTest {

    @Mock
    private CustodyEventRepository custodyEventRepository;

    @Mock
    private EvidenceItemRepository evidenceItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLedgerService auditLedgerService;

    @InjectMocks
    private CustodyService custodyService;

    private UUID evidenceId;
    private UUID officerAId;
    private UUID analystBId;

    @BeforeEach
    void setUp() {
        evidenceId = UUID.randomUUID();
        officerAId = UUID.randomUUID();
        analystBId = UUID.randomUUID();
    }

    @Test
    @DisplayName("Record custody event generates signature and writes to audit ledger")
    void shouldRecordCustodyEvent() {
        when(custodyEventRepository.save(any(CustodyEvent.class))).thenAnswer(i -> {
            CustodyEvent e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        CustodyEventResponse response = custodyService.recordEvent(
                evidenceId,
                CustodyEventType.INGEST,
                officerAId,
                null,
                officerAId,
                "Initial Seizure"
        );

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(CustodyEventType.INGEST);
        assertThat(response.getActorSignature()).isNotNull();

        verify(auditLedgerService).appendEntry(
                eq(officerAId),
                eq("CUSTODY_INGEST"),
                eq("EVIDENCE:" + evidenceId),
                any(String.class)
        );
    }

    @Test
    @DisplayName("Transfer custody updates current custodian and saves transfer event")
    void shouldTransferCustody() {
        EvidenceItem item = EvidenceItem.builder()
                .evidenceNumber("EVID-001")
                .title("Mobile Phone")
                .custodyState(CustodyState.INGESTED)
                .currentCustodianId(officerAId)
                .build();
        item.setId(evidenceId);

        when(evidenceItemRepository.findById(evidenceId)).thenReturn(Optional.of(item));
        when(userRepository.existsById(analystBId)).thenReturn(true);
        when(custodyEventRepository.save(any(CustodyEvent.class))).thenAnswer(i -> {
            CustodyEvent e = i.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });

        CustodyTransferRequest request = CustodyTransferRequest.builder()
                .toPartyId(analystBId)
                .reason("Handover to forensic lab for memory extraction")
                .build();

        CustodyEventResponse response = custodyService.transferCustody(evidenceId, request);

        assertThat(response).isNotNull();
        assertThat(response.getEventType()).isEqualTo(CustodyEventType.TRANSFER);
        assertThat(item.getCurrentCustodianId()).isEqualTo(analystBId);
        assertThat(item.getCustodyState()).isEqualTo(CustodyState.TRANSFERRED);

        verify(evidenceItemRepository).save(item);
        verify(auditLedgerService).appendEntry(
                any(),
                eq("CUSTODY_TRANSFER"),
                eq("EVIDENCE:" + evidenceId),
                any(String.class)
        );
    }
}
