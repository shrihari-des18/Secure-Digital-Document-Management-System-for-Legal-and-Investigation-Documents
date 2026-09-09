package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.CaseRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EvidenceControllerIntegrationTest {

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

    private UUID caseId;

    @BeforeEach
    void setUp() throws Exception {
        evidenceFileRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Officer
        CreateUserRequest userRequest = CreateUserRequest.builder()
                .username("io_varun")
                .fullName("Varun Kapoor")
                .email("varun@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();

        String userJson = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID officerId = UUID.fromString(objectMapper.readTree(userJson).path("data").path("id").asText());

        // 2. Create Case
        CreateCaseRequest caseRequest = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-MUM-4411")
                .title("Financial Fraud Network")
                .jurisdiction("Mumbai Cyber Cell")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId))
                .build();

        String caseJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        caseId = UUID.fromString(objectMapper.readTree(caseJson).path("data").path("id").asText());
    }

    @Test
    @DisplayName("Complete Evidence Flow: Upload, Retrieve, Download, and Verify Physical Integrity")
    void testEvidenceUploadAndIntegrityVerification() throws Exception {
        byte[] fileBytes = "CONFIDENTIAL FORENSIC DUMP CONTENT -- SEIZED ITEM".getBytes(StandardCharsets.UTF_8);
        String expectedSha256 = CryptoUtil.calculateSha256(fileBytes);

        UploadEvidenceRequest request = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-MUM-2026-001")
                .title("Pen Drive Seized from Accused")
                .description("SanDisk Cruzer 64GB USB drive")
                .classification(ClassificationLevel.CONFIDENTIAL)
                .deviceMake("SanDisk")
                .deviceModel("Cruzer Blade 64GB")
                .clientSha256(expectedSha256)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "sandisk_dump.bin",
                "application/octet-stream",
                fileBytes
        );

        // 1. Upload Evidence
        String uploadResponseJson = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(metaPart)
                        .file(filePart)
                        .header("X-Actor-Id", UUID.randomUUID().toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.evidenceNumber", is("EVID-MUM-2026-001")))
                .andExpect(jsonPath("$.data.file.sha256", is(expectedSha256)))
                .andExpect(jsonPath("$.data.file.integrityStatus", is("VERIFIED")))
                .andReturn().getResponse().getContentAsString();

        String evidenceIdStr = objectMapper.readTree(uploadResponseJson).path("data").path("id").asText();
        UUID evidenceId = UUID.fromString(evidenceIdStr);

        // 2. Get Evidence Details
        mockMvc.perform(get("/api/v1/evidence/" + evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(evidenceIdStr)))
                .andExpect(jsonPath("$.data.title", is("Pen Drive Seized from Accused")));

        // 3. Download File and verify bytes
        mockMvc.perform(get("/api/v1/evidence/" + evidenceId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"sandisk_dump.bin\""))
                .andExpect(content().bytes(fileBytes));

        // 4. Verify Physical Integrity on Disk
        mockMvc.perform(post("/api/v1/evidence/" + evidenceId + "/verify-integrity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.match", is(true)))
                .andExpect(jsonPath("$.data.recordedSha256", is(expectedSha256)))
                .andExpect(jsonPath("$.data.calculatedSha256", is(expectedSha256)))
                .andExpect(jsonPath("$.data.integrityStatus", is("VERIFIED")));
    }
}
