package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.RegisterRequest;
import com.shrihari.evault.dto.UpdateCaseStatusRequest;
import com.shrihari.evault.model.CaseStatus;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAccessControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    private String officerToken;
    private UUID officerId;

    private String analystToken;
    private UUID analystId;

    private String secondOfficerToken;
    private UUID secondOfficerId;

    @BeforeEach
    void setUp() throws Exception {
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register Officer 1 (Assigned Lead Officer)
        RegisterRequest officerReq = RegisterRequest.builder()
                .username("io_lead")
                .password("Password@123")
                .fullName("Inspector Lead")
                .email("lead@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        String offJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(officerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        officerToken = objectMapper.readTree(offJson).path("data").path("token").asText();
        officerId = UUID.fromString(objectMapper.readTree(offJson).path("data").path("user").path("id").asText());

        // 2. Register Forensic Analyst
        RegisterRequest analystReq = RegisterRequest.builder()
                .username("analyst_user")
                .password("Password@123")
                .fullName("Dr. Forensic")
                .email("forensic@fsl.gov.in")
                .role(RoleEnum.FORENSIC_ANALYST)
                .build();
        String anJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analystReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        analystToken = objectMapper.readTree(anJson).path("data").path("token").asText();
        analystId = UUID.fromString(objectMapper.readTree(anJson).path("data").path("user").path("id").asText());

        // 3. Register Officer 2 (Unassigned Officer)
        RegisterRequest unassignedReq = RegisterRequest.builder()
                .username("io_unassigned")
                .password("Password@123")
                .fullName("Inspector Unassigned")
                .email("unassigned@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        String unJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unassignedReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        secondOfficerToken = objectMapper.readTree(unJson).path("data").path("token").asText();
        secondOfficerId = UUID.fromString(objectMapper.readTree(unJson).path("data").path("user").path("id").asText());
    }

    @Test
    @DisplayName("RBAC: Forensic Analyst cannot create a case (403 Forbidden)")
    void testForensicAnalystCannotCreateCase() throws Exception {
        CreateCaseRequest caseReq = CreateCaseRequest.builder()
                .caseNumber("FIR-ILLEGAL-01")
                .title("Unauthorized Case Creation")
                .jurisdiction("Central")
                .leadOfficerId(analystId)
                .assignedOfficerIds(Set.of(analystId))
                .build();

        mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + analystToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ABAC: Unassigned officer cannot modify case status (403 Forbidden), but Lead Officer can (200 OK)")
    void testCaseAbacSecurity() throws Exception {
        // 1. Lead Officer creates case
        CreateCaseRequest caseReq = CreateCaseRequest.builder()
                .caseNumber("FIR-SEC-2026-009")
                .title("State Level Cyber Heist")
                .jurisdiction("Special Cyber Crime Unit")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId))
                .build();

        String createJson = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID caseId = UUID.fromString(objectMapper.readTree(createJson).path("data").path("id").asText());

        UpdateCaseStatusRequest updateRequest = UpdateCaseStatusRequest.builder()
                .status(CaseStatus.UNDER_INVESTIGATION)
                .reason("Evidence gathered from suspect devices")
                .build();

        // 2. Unassigned officer tries to update case status -> 403 Forbidden!
        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/status")
                        .header("Authorization", "Bearer " + secondOfficerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        // 3. Lead officer updates case status -> 200 OK!
        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/status")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("UNDER_INVESTIGATION")));
    }
}
