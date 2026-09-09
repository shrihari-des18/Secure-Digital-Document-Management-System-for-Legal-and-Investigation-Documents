package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.CreateUserRequest;
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

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CaseControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    private UUID officerId;

    @BeforeEach
    void setUp() throws Exception {
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // Create an officer user first
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username("officer_sharma")
                .fullName("Inspector Rajesh Sharma")
                .email("sharma@police.gov.in")
                .badgeNumber("DL-9821")
                .department("Cyber Crime Cell")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();

        String userJson = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String idStr = objectMapper.readTree(userJson).path("data").path("id").asText();
        officerId = UUID.fromString(idStr);
    }

    @Test
    @DisplayName("Complete Case lifecycle: Create, Get, List, and Status Update")
    void testCaseLifecycle() throws Exception {
        // 1. Create Case
        CreateCaseRequest createRequest = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-CYBER-001")
                .title("Financial Phishing Ring")
                .description("Phishing attack targeting senior citizens")
                .jurisdiction("Delhi NCT")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId))
                .build();

        String createResponseJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.caseNumber", is("FIR-2026-CYBER-001")))
                .andExpect(jsonPath("$.data.status", is("OPEN")))
                .andReturn().getResponse().getContentAsString();

        String caseIdStr = objectMapper.readTree(createResponseJson).path("data").path("id").asText();
        UUID caseId = UUID.fromString(caseIdStr);

        // 2. Get Case by ID
        mockMvc.perform(get("/api/v1/cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.title", is("Financial Phishing Ring")));

        // 3. Get Case by Number
        mockMvc.perform(get("/api/v1/cases/number/FIR-2026-CYBER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(caseId.toString())));

        // 4. List all cases
        mockMvc.perform(get("/api/v1/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        // 5. Update Case Status
        UpdateCaseStatusRequest statusRequest = UpdateCaseStatusRequest.builder()
                .status(CaseStatus.UNDER_INVESTIGATION)
                .reason("Evidence gathered from bank")
                .build();

        mockMvc.perform(patch("/api/v1/cases/" + caseId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status", is("UNDER_INVESTIGATION")));
    }

    @Test
    @DisplayName("Validation failure when creating case with missing fields")
    void testValidationFailure() throws Exception {
        CreateCaseRequest invalidRequest = CreateCaseRequest.builder().build();

        mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.message", is("Input validation failed")));
    }
}
