package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.RegisterRequest;
import com.shrihari.evault.dto.TriggerTestAlarmRequest;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
import com.shrihari.evault.repository.SecurityAlarmRepository;
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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityAlarmControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SecurityAlarmRepository securityAlarmRepository;

    private String adminToken;
    private String officerToken;
    private String auditorToken;

    @BeforeEach
    void setUp() throws Exception {
        securityAlarmRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Register SYSTEM_ADMIN
        RegisterRequest adminReq = RegisterRequest.builder()
                .username("admin_security")
                .password("Admin@12345")
                .fullName("System Administrator")
                .email("admin@evault.gov.in")
                .role(RoleEnum.SYSTEM_ADMIN)
                .build();
        String adminJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        adminToken = objectMapper.readTree(adminJson).path("data").path("token").asText();

        // 2. Register AUDITOR
        RegisterRequest auditorReq = RegisterRequest.builder()
                .username("auditor_cag")
                .password("Auditor@12345")
                .fullName("CAG Judicial Auditor")
                .email("auditor@cag.gov.in")
                .role(RoleEnum.AUDITOR)
                .build();
        String auditorJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(auditorReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        auditorToken = objectMapper.readTree(auditorJson).path("data").path("token").asText();

        // 3. Register INVESTIGATING_OFFICER
        RegisterRequest officerReq = RegisterRequest.builder()
                .username("io_vikram")
                .password("Officer@12345")
                .fullName("Inspector Vikram")
                .email("vikram@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        String officerJson = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(officerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        officerToken = objectMapper.readTree(officerJson).path("data").path("token").asText();
    }

    @Test
    @DisplayName("Admin can trigger, list, retrieve, and acknowledge security alarms")
    void testSecurityAlarmLifecycleByAdmin() throws Exception {
        TriggerTestAlarmRequest triggerReq = TriggerTestAlarmRequest.builder()
                .alarmType(SecurityAlarmType.LEDGER_INTEGRITY_BREACH)
                .severity(SecurityAlarmLevel.CRITICAL)
                .resourceRef("LEDGER:seq#5")
                .message("Simulated audit ledger tamper detection drill")
                .details(Map.of("drillId", "DRILL-2026-09", "operator", "admin_security"))
                .build();

        // 1. Trigger test security alarm
        String triggerJson = mockMvc.perform(post("/api/v1/alarms/test-trigger")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.alarmType", is("LEDGER_INTEGRITY_BREACH")))
                .andExpect(jsonPath("$.data.severity", is("CRITICAL")))
                .andExpect(jsonPath("$.data.acknowledged", is(false)))
                .andReturn().getResponse().getContentAsString();

        UUID alarmId = UUID.fromString(objectMapper.readTree(triggerJson).path("data").path("id").asText());

        // 2. Query unacknowledged count
        mockMvc.perform(get("/api/v1/alarms/unacknowledged-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(1)));

        // 3. Auditor can list alarms
        mockMvc.perform(get("/api/v1/alarms")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.length()", greaterThanOrEqualTo(1)));

        // 4. Auditor can get alarm by ID
        mockMvc.perform(get("/api/v1/alarms/" + alarmId)
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id", is(alarmId.toString())))
                .andExpect(jsonPath("$.data.message", is("Simulated audit ledger tamper detection drill")));

        // 5. Auditor acknowledges the alarm
        mockMvc.perform(post("/api/v1/alarms/" + alarmId + "/acknowledge")
                        .header("Authorization", "Bearer " + auditorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.acknowledged", is(true)))
                .andExpect(jsonPath("$.data.acknowledgedAt").isNotEmpty());

        // 6. Unacknowledged count drops to 0
        mockMvc.perform(get("/api/v1/alarms/unacknowledged-count")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", is(0)));
    }

    @Test
    @DisplayName("Investigating officer is forbidden from triggering or accessing security alarms")
    void testRbacRestrictionsForInvestigatingOfficer() throws Exception {
        TriggerTestAlarmRequest triggerReq = TriggerTestAlarmRequest.builder()
                .alarmType(SecurityAlarmType.SYSTEM_ALERT)
                .severity(SecurityAlarmLevel.WARNING)
                .resourceRef("SYS:1")
                .message("Unauthorized trigger attempt")
                .build();

        // IO cannot trigger alarm -> 403 Forbidden
        mockMvc.perform(post("/api/v1/alarms/test-trigger")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(triggerReq)))
                .andExpect(status().isForbidden());

        // IO cannot query alarms -> 403 Forbidden
        mockMvc.perform(get("/api/v1/alarms")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());

        // IO cannot query unacknowledged count -> 403 Forbidden
        mockMvc.perform(get("/api/v1/alarms/unacknowledged-count")
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isForbidden());
    }
}
