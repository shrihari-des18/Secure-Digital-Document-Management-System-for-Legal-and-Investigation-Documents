package com.shrihari.evault.service;

import com.shrihari.evault.dto.CaseResponse;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.exception.DuplicateResourceException;
import com.shrihari.evault.exception.ResourceNotFoundException;
import com.shrihari.evault.model.Case;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CaseService caseService;

    private UUID leadOfficerId;
    private CreateCaseRequest createRequest;

    @BeforeEach
    void setUp() {
        leadOfficerId = UUID.randomUUID();
        createRequest = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-DEL-001")
                .title("Cyber Intrusion Case")
                .description("Investigation into unauthorized server access")
                .jurisdiction("New Delhi Central")
                .leadOfficerId(leadOfficerId)
                .assignedOfficerIds(Set.of(leadOfficerId))
                .build();
    }

    @Test
    @DisplayName("Successfully create a case when valid inputs are provided")
    void shouldCreateCaseSuccessfully() {
        when(caseRepository.existsByCaseNumber("FIR-2026-DEL-001")).thenReturn(false);
        when(userRepository.existsById(leadOfficerId)).thenReturn(true);

        Case savedCase = Case.builder()
                .caseNumber(createRequest.getCaseNumber())
                .title(createRequest.getTitle())
                .description(createRequest.getDescription())
                .jurisdiction(createRequest.getJurisdiction())
                .status(CaseStatus.OPEN)
                .leadOfficerId(leadOfficerId)
                .assignedOfficerIds(Set.of(leadOfficerId))
                .build();
        savedCase.setId(UUID.randomUUID());

        when(caseRepository.save(any(Case.class))).thenReturn(savedCase);

        CaseResponse response = caseService.createCase(createRequest);

        assertThat(response).isNotNull();
        assertThat(response.getCaseNumber()).isEqualTo("FIR-2026-DEL-001");
        assertThat(response.getStatus()).isEqualTo(CaseStatus.OPEN);
        verify(caseRepository).save(any(Case.class));
    }

    @Test
    @DisplayName("Throw DuplicateResourceException when case number already exists")
    void shouldThrowWhenCaseNumberExists() {
        when(caseRepository.existsByCaseNumber("FIR-2026-DEL-001")).thenReturn(true);

        assertThatThrownBy(() -> caseService.createCase(createRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Throw ResourceNotFoundException when lead officer does not exist")
    void shouldThrowWhenLeadOfficerNotFound() {
        when(caseRepository.existsByCaseNumber("FIR-2026-DEL-001")).thenReturn(false);
        when(userRepository.existsById(leadOfficerId)).thenReturn(false);

        assertThatThrownBy(() -> caseService.createCase(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Lead officer not found");
    }

    @Test
    @DisplayName("Successfully transition case status")
    void shouldTransitionStatus() {
        UUID caseId = UUID.randomUUID();
        Case existingCase = Case.builder()
                .caseNumber("FIR-2026-DEL-001")
                .title("Cyber Intrusion Case")
                .status(CaseStatus.OPEN)
                .jurisdiction("New Delhi Central")
                .leadOfficerId(leadOfficerId)
                .build();
        existingCase.setId(caseId);

        when(caseRepository.findById(caseId)).thenReturn(Optional.of(existingCase));
        when(caseRepository.save(any(Case.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseResponse response = caseService.updateStatus(caseId, CaseStatus.UNDER_INVESTIGATION, "Investigation started");

        assertThat(response.getStatus()).isEqualTo(CaseStatus.UNDER_INVESTIGATION);
    }
}
