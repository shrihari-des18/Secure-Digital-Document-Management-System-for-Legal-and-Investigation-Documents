package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.dto.TriggerAnalysisRequest;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.AiAnalysisRepository;
import com.shrihari.evault.repository.AuditLedgerRepository;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.CustodyEventRepository;
import com.shrihari.evault.repository.EvidenceFileRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AiAnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private EvidenceItemRepository evidenceItemRepository;

    @Autowired
    private EvidenceFileRepository evidenceFileRepository;

    @Autowired
    private CustodyEventRepository custodyEventRepository;

    @Autowired
    private AuditLedgerRepository auditLedgerRepository;

    @Autowired
    private AiAnalysisRepository aiAnalysisRepository;

    private UUID evidenceId;
    private UUID officerId;

    @BeforeEach
    void setUp() throws Exception {
        aiAnalysisRepository.deleteAll();
        custodyEventRepository.deleteAll();
        auditLedgerRepository.deleteAll();
        evidenceFileRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Officer User
        CreateUserRequest userReq = CreateUserRequest.builder()
                .username("io_anita")
                .fullName("Inspector Anita Roy")
                .email("anita.roy@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        String uJson = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        officerId = UUID.fromString(objectMapper.readTree(uJson).path("data").path("id").asText());

        // 2. Create Case
        CreateCaseRequest caseReq = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-AI-001")
                .title("Financial Fraud Network")
                .jurisdiction("Cyber Cell")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId))
                .build();
        String cJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID caseId = UUID.fromString(objectMapper.readTree(cJson).path("data").path("id").asText());

        // 3. Upload Evidence
        byte[] fileBytes = "SEIZED FINANCIAL DUMP LOG WITH SUSPECT EMAIL suspect@darknet.org".getBytes(StandardCharsets.UTF_8);
        String expectedSha256 = CryptoUtil.calculateSha256(fileBytes);

        UploadEvidenceRequest uploadReq = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-AI-2026-001")
                .title("Financial Server Log Dump")
                .classification(ClassificationLevel.CONFIDENTIAL)
                .clientSha256(expectedSha256)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(uploadReq));
        MockMultipartFile filePart = new MockMultipartFile("file", "financial_log.txt", "text/plain", fileBytes);

        String uploadJson = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(metaPart)
                        .file(filePart)
                        .header("X-Actor-Id", officerId.toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        evidenceId = UUID.fromString(objectMapper.readTree(uploadJson).path("data").path("id").asText());
    }

    @Test
    @DisplayName("Complete AI/ML Analysis Flow: Trigger Analysis, Extract OCR & PII, and Retrieve Results")
    void testAiAnalysisFlow() throws Exception {
        TriggerAnalysisRequest request = TriggerAnalysisRequest.builder()
                .extractOcr(true)
                .detectPii(true)
                .generateTags(true)
                .build();

        // 1. Trigger AI Analysis
        String triggerJson = mockMvc.perform(post("/api/v1/evidence/" + evidenceId + "/analyze")
                        .header("X-Actor-Id", officerId.toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.ocrText", notNullValue()))
                .andExpect(jsonPath("$.data.detectedPii", notNullValue()))
                .andExpect(jsonPath("$.data.confidenceScore", is(0.985)))
                .andReturn().getResponse().getContentAsString();

        // 2. Get Analysis Results
        mockMvc.perform(get("/api/v1/evidence/" + evidenceId + "/analysis")
                        .header("X-Actor-Id", officerId.toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.evidenceId", is(evidenceId.toString())))
                .andExpect(jsonPath("$.data.status", is("COMPLETED")))
                .andExpect(jsonPath("$.data.perceptualHash", is("pHash-a8f9c2d7e1b4093f")));
    }
}
