package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.common.CryptoUtil;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.dto.CustodyTransferRequest;
import com.shrihari.evault.dto.UploadEvidenceRequest;
import com.shrihari.evault.model.AuditLedger;
import com.shrihari.evault.model.ClassificationLevel;
import com.shrihari.evault.model.RoleEnum;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LedgerAndCustodyIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private UUID officerId;
    private UUID analystId;
    private UUID caseId;

    @BeforeEach
    void setUp() throws Exception {
        custodyEventRepository.deleteAll();
        auditLedgerRepository.deleteAll();
        evidenceFileRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Officer
        CreateUserRequest userRequest1 = CreateUserRequest.builder()
                .username("io_karan")
                .fullName("Karan Singh")
                .email("karan@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        String u1Json = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest1)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        officerId = UUID.fromString(objectMapper.readTree(u1Json).path("data").path("id").asText());

        // 2. Create Forensic Analyst
        CreateUserRequest userRequest2 = CreateUserRequest.builder()
                .username("analyst_meera")
                .fullName("Meera Nair")
                .email("meera@fsl.gov.in")
                .role(RoleEnum.FORENSIC_ANALYST)
                .build();
        String u2Json = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userRequest2)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        analystId = UUID.fromString(objectMapper.readTree(u2Json).path("data").path("id").asText());

        // 3. Create Case
        CreateCaseRequest caseRequest = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-CHANDI-091")
                .title("Bank Security Breach")
                .jurisdiction("Chandigarh Central")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId, analystId))
                .build();
        String caseJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        caseId = UUID.fromString(objectMapper.readTree(caseJson).path("data").path("id").asText());
    }

    @Test
    @DisplayName("Complete Ledger & Custody Flow: Ingest, Transfer, Verify Ledger, and Detect Tampering")
    void testCustodyAndLedgerVerificationFlow() throws Exception {
        byte[] fileBytes = "SEIZED SERVER LOG ARCHIVE -- DATED 2026-09-08".getBytes(StandardCharsets.UTF_8);
        String expectedSha256 = CryptoUtil.calculateSha256(fileBytes);

        UploadEvidenceRequest uploadRequest = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-CHD-2026-001")
                .title("Access Logs from Gateway")
                .classification(ClassificationLevel.SECRET)
                .clientSha256(expectedSha256)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(uploadRequest)
        );

        MockMultipartFile filePart = new MockMultipartFile(
                "file",
                "auth_logs.tar.gz",
                "application/gzip",
                fileBytes
        );

        // 1. Upload Evidence (automatically writes INGEST custody event and audit ledger block #1)
        String uploadJson = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(metaPart)
                        .file(filePart)
                        .header("X-Actor-Id", officerId.toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID evidenceId = UUID.fromString(objectMapper.readTree(uploadJson).path("data").path("id").asText());

        // 2. Transfer Custody to Forensic Analyst
        CustodyTransferRequest transferRequest = CustodyTransferRequest.builder()
                .toPartyId(analystId)
                .reason("Handover to forensic analyst for packet analysis and log extraction")
                .build();

        mockMvc.perform(post("/api/v1/evidence/" + evidenceId + "/custody-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Actor-Id", officerId.toString())
                        .content(objectMapper.writeValueAsString(transferRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.eventType", is("TRANSFER")))
                .andExpect(jsonPath("$.data.toParty", is(analystId.toString())));

        // 3. Check Custody Timeline (should have INGEST and TRANSFER events)
        mockMvc.perform(get("/api/v1/evidence/" + evidenceId + "/custody"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].eventType", is("INGEST")))
                .andExpect(jsonPath("$.data[1].eventType", is("TRANSFER")));

        // 4. Verify Cryptographic Ledger Integrity (must be 100% valid)
        mockMvc.perform(get("/api/v1/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.valid", is(true)))
                .andExpect(jsonPath("$.data.totalEntries", greaterThanOrEqualTo(2)));

        // 5. Tamper Test: Modify details of sequence #1 directly in database using raw SQL
        List<AuditLedger> entries = auditLedgerRepository.findAllByOrderBySeqAsc();
        AuditLedger firstBlock = entries.get(0);
        jdbcTemplate.update("UPDATE audit_ledger SET details = ? WHERE seq = ?",
                "{\"TAMPERED\":\"Malicious actor altered ledger data directly in DB\"}",
                firstBlock.getSeq());

        // 6. Verify Cryptographic Ledger Integrity again: MUST detect tampering!
        mockMvc.perform(get("/api/v1/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(false)))
                .andExpect(jsonPath("$.data.brokenSeq", is(1)));
    }
}
