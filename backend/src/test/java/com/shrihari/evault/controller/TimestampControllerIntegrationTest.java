package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.RegisterRequest;
import com.shrihari.evault.dto.StampHashRequest;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.dto.VerifyTimestampRequest;
import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.CertificateRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TimestampControllerIntegrationTest {

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
    private CertificateRepository certificateRepository;

    private String officerToken;
    private UUID officerId;

    @BeforeEach
    void setUp() throws Exception {
        certificateRepository.deleteAll();
        evidenceFileRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // Register officer
        RegisterRequest officerReq = RegisterRequest.builder()
                .username("officer_tsa")
                .password("Password@123")
                .email("officer_tsa@police.gov.in")
                .fullName("Inspector Vikram")
                .badgeNumber("TSA-999")
                .department("Cyber Forensics")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();

        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(officerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode regJson = objectMapper.readTree(regRes.getResponse().getContentAsString());
        officerToken = regJson.path("data").path("token").asText();
        officerId = UUID.fromString(regJson.path("data").path("user").path("id").asText());
    }

    @Test
    @DisplayName("POST /api/v1/timestamp/stamp-hash generates valid RFC 3161 token")
    void stampHash_endpoint_generatesValidToken() throws Exception {
        String testHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

        StampHashRequest req = StampHashRequest.builder()
                .sha256(testHash)
                .description("Test Evidence Payload")
                .build();

        mockMvc.perform(post("/api/v1/timestamp/stamp-hash")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.hashedMessage", is(testHash)))
                .andExpect(jsonPath("$.data.verified", is(true)))
                .andExpect(jsonPath("$.data.tokenBase64").isNotEmpty());
    }

    @Test
    @DisplayName("POST /api/v1/timestamp/verify verifies valid token and detects tampering")
    void verifyToken_endpoint_verifiesAndDetectsTampering() throws Exception {
        String testHash = "a591a6d40bf420404a011733cfb7b190d62c65bf0bcda32b57b277d9ad9f146e";

        StampHashRequest stampReq = StampHashRequest.builder()
                .sha256(testHash)
                .description("Tamper Test Payload")
                .build();

        MvcResult stampRes = mockMvc.perform(post("/api/v1/timestamp/stamp-hash")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stampReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode stampJson = objectMapper.readTree(stampRes.getResponse().getContentAsString());
        String tokenBase64 = stampJson.path("data").path("tokenBase64").asText();

        // 1. Verify with matching hash -> valid = true
        VerifyTimestampRequest validReq = VerifyTimestampRequest.builder()
                .tokenBase64(tokenBase64)
                .expectedSha256(testHash)
                .build();

        mockMvc.perform(post("/api/v1/timestamp/verify")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.hashMatches", is(true)))
                .andExpect(jsonPath("$.data.signatureVerified", is(true)));

        // 2. Verify with tampered hash -> valid = false
        VerifyTimestampRequest tamperedReq = VerifyTimestampRequest.builder()
                .tokenBase64(tokenBase64)
                .expectedSha256("0000000000000000000000000000000000000000000000000000000000000000")
                .build();

        mockMvc.perform(post("/api/v1/timestamp/verify")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(tamperedReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(false)))
                .andExpect(jsonPath("$.data.hashMatches", is(false)));
    }

    @Test
    @DisplayName("Evidence ingestion attaches RFC 3161 timestamp verifiable via GET /api/v1/timestamp/evidence/{id}")
    void evidenceIntake_attachesRfc3161Timestamp_verifiableViaEndpoint() throws Exception {
        // Create case
        CreateCaseRequest caseReq = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-TSA-001")
                .title("TSA Verification Case")
                .jurisdiction("Cyber Cell")
                .leadOfficerId(officerId)
                .build();

        MvcResult caseRes = mockMvc.perform(post("/api/v1/cases")
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID caseId = UUID.fromString(objectMapper.readTree(caseRes.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Upload evidence
        byte[] fileBytes = "Forensic Disk Image Binary Dump for Trial".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", "disk_dump.raw", "application/octet-stream", fileBytes);

        UploadEvidenceRequest uploadReq = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-TSA-001")
                .title("Forensic Disk Image")
                .classification(ClassificationLevel.CONFIDENTIAL)
                .build();
        MockMultipartFile metadataPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(uploadReq)
        );

        MvcResult uploadRes = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(file)
                        .file(metadataPart)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isCreated())
                .andReturn();

        UUID evidenceId = UUID.fromString(objectMapper.readTree(uploadRes.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Verify evidence has RFC 3161 timestamp
        mockMvc.perform(get("/api/v1/timestamp/evidence/" + evidenceId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.hashMatches", is(true)))
                .andExpect(jsonPath("$.data.signatureVerified", is(true)));

        // Issue BSA §63 certificate
        MvcResult certRes = mockMvc.perform(post("/api/v1/certificates/evidence/" + evidenceId)
                        .header("Authorization", "Bearer " + officerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        UUID certId = UUID.fromString(objectMapper.readTree(certRes.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Verify certificate has RFC 3161 timestamp
        mockMvc.perform(get("/api/v1/timestamp/certificate/" + certId)
                        .header("Authorization", "Bearer " + officerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.hashMatches", is(true)))
                .andExpect(jsonPath("$.data.signatureVerified", is(true)));
    }
}
