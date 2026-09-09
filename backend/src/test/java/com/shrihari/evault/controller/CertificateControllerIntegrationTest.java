package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.dto.IssueCertificateRequest;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.AuditLedgerRepository;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.CertificateRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
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
class CertificateControllerIntegrationTest {

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

    @Autowired
    private CustodyEventRepository custodyEventRepository;

    @Autowired
    private AuditLedgerRepository auditLedgerRepository;

    private UUID officerId;
    private UUID caseId;
    private UUID evidenceId;
    private String evidenceSha256;

    @BeforeEach
    void setUp() throws Exception {
        certificateRepository.deleteAll();
        custodyEventRepository.deleteAll();
        auditLedgerRepository.deleteAll();
        evidenceFileRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Officer
        CreateUserRequest userReq = CreateUserRequest.builder()
                .username("dsp_anil")
                .fullName("DSP Anil Deshmukh")
                .email("anil.deshmukh@police.gov.in")
                .badgeNumber("MH-DSP-882")
                .department("CID Cyber Forensics")
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
                .caseNumber("FIR-2026-PUNE-771")
                .title("Unauthorized Wiretap Investigation")
                .jurisdiction("Pune Special Crime Branch")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId))
                .build();
        String cJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        caseId = UUID.fromString(objectMapper.readTree(cJson).path("data").path("id").asText());

        // 3. Upload Evidence
        byte[] fileBytes = "WIRETAP AUDIO RECORDING - CONVERSATION INTERCEPT 2026".getBytes(StandardCharsets.UTF_8);
        evidenceSha256 = CryptoUtil.calculateSha256(fileBytes);

        UploadEvidenceRequest uploadReq = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-PUNE-2026-AUDIO-01")
                .title("Intercepted Telecom Recording")
                .classification(ClassificationLevel.SECRET)
                .deviceMake("Voice Logger Server")
                .deviceModel("VLS-4000")
                .deviceSerialNumber("SN-VLS-8812")
                .seizedLocation("Telecom Server Room, Pune")
                .clientSha256(evidenceSha256)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(uploadReq));
        MockMultipartFile filePart = new MockMultipartFile("file", "intercept_call.wav", "audio/wav", fileBytes);

        String evidJson = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(metaPart)
                        .file(filePart)
                        .header("X-Actor-Id", officerId.toString()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        evidenceId = UUID.fromString(objectMapper.readTree(evidJson).path("data").path("id").asText());
    }

    @Test
    @DisplayName("Statutory Certificate Flow: Issue BSA §63 certificate, verify metadata, download valid PDF, and verify custody event")
    void testIssueAndDownloadBsa63Certificate() throws Exception {
        IssueCertificateRequest issueRequest = IssueCertificateRequest.builder()
                .mannerOfProduction("Forensic bitstream audio stream captured directly from lawful telecom intercept buffer")
                .certifyingOfficerNotes("Certified for admission in Sessions Court trial")
                .build();

        // 1. Issue Certificate
        String certJson = mockMvc.perform(post("/api/v1/certificates/evidence/" + evidenceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Actor-Id", officerId.toString())
                        .content(objectMapper.writeValueAsString(issueRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.standard", is("BSA_2023_S63")))
                .andExpect(jsonPath("$.data.certificateNumber", startsWith("CERT-BSA63-")))
                .andExpect(jsonPath("$.data.hashSnapshotSha256", is(evidenceSha256)))
                .andReturn().getResponse().getContentAsString();

        UUID certId = UUID.fromString(objectMapper.readTree(certJson).path("data").path("id").asText());

        // 2. Download Certificate PDF
        byte[] pdfResponseBytes = mockMvc.perform(get("/api/v1/certificates/" + certId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andExpect(header().string("Content-Disposition", containsString(".pdf")))
                .andReturn().getResponse().getContentAsByteArray();

        // Verify PDF Header Magic Bytes
        assertThat(new String(pdfResponseBytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
        assertThat(pdfResponseBytes.length).isGreaterThan(1000);

        // 3. Verify that Evidence item custody state is now CERTIFIED
        mockMvc.perform(get("/api/v1/evidence/" + evidenceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.custodyState", is("CERTIFIED")));

        // 4. Verify that Custody Timeline has CERTIFY event
        mockMvc.perform(get("/api/v1/evidence/" + evidenceId + "/custody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].eventType", hasItem("CERTIFY")));
    }
}
