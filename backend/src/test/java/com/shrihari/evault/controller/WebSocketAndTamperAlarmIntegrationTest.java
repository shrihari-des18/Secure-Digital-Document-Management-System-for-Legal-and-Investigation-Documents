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
import com.shrihari.evault.model.SecurityAlarm;
import com.shrihari.evault.model.SecurityAlarmLevel;
import com.shrihari.evault.model.SecurityAlarmType;
import com.shrihari.evault.repository.AuditLedgerRepository;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.CustodyEventRepository;
import com.shrihari.evault.repository.EvidenceFileRepository;
import com.shrihari.evault.repository.EvidenceItemRepository;
import com.shrihari.evault.repository.SecurityAlarmRepository;
import com.shrihari.evault.repository.UserRepository;
import com.shrihari.evault.service.storage.StorageService;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
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
class WebSocketAndTamperAlarmIntegrationTest {

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

    @Autowired
    private SecurityAlarmRepository securityAlarmRepository;

    @Autowired
    private StorageService storageService;

    private UUID officerId;
    private UUID analystId;
    private UUID caseId;

    @BeforeEach
    void setUp() throws Exception {
        securityAlarmRepository.deleteAll();
        custodyEventRepository.deleteAll();
        auditLedgerRepository.deleteAll();
        jdbcTemplate.execute("ALTER TABLE audit_ledger ALTER COLUMN seq RESTART WITH 1");
        evidenceFileRepository.deleteAll();
        evidenceItemRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create Officer
        CreateUserRequest officerReq = CreateUserRequest.builder()
                .username("io_tamper_tester")
                .fullName("Inspector Tamper Tester")
                .email("tamper@police.gov.in")
                .role(RoleEnum.INVESTIGATING_OFFICER)
                .build();
        String u1Json = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(officerReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        officerId = UUID.fromString(objectMapper.readTree(u1Json).path("data").path("id").asText());

        // 2. Create Analyst
        CreateUserRequest analystReq = CreateUserRequest.builder()
                .username("analyst_tamper_tester")
                .fullName("Dr. Forensic Tester")
                .email("analyst_tamper@fsl.gov.in")
                .role(RoleEnum.FORENSIC_ANALYST)
                .build();
        String u2Json = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(analystReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        analystId = UUID.fromString(objectMapper.readTree(u2Json).path("data").path("id").asText());

        // 3. Create Case
        CreateCaseRequest caseReq = CreateCaseRequest.builder()
                .caseNumber("FIR-2026-TAMPER-001")
                .title("Cyber Security Tamper Drill Case")
                .jurisdiction("Cyber Crime HQ")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId, analystId))
                .build();
        String caseJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        caseId = UUID.fromString(objectMapper.readTree(caseJson).path("data").path("id").asText());
    }

    @Test
    @DisplayName("Audit Ledger Tamper Trigger: Modifying row content in DB causes verify to trigger CRITICAL LEDGER_INTEGRITY_BREACH alarm")
    void testAuditLedgerTamperGeneratesSecurityAlarm() throws Exception {
        byte[] fileBytes = "IMMUTABLE AUDIT LOG SAMPLE".getBytes(StandardCharsets.UTF_8);
        String sha256 = CryptoUtil.calculateSha256(fileBytes);

        UploadEvidenceRequest uploadReq = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-TAMPER-001")
                .title("Audit Proof Log")
                .classification(ClassificationLevel.SECRET)
                .clientSha256(sha256)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(uploadReq));
        MockMultipartFile filePart = new MockMultipartFile("file", "audit_proof.log", "text/plain", fileBytes);

        mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(metaPart)
                        .file(filePart)
                        .header("X-Actor-Id", officerId.toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER"))
                .andExpect(status().isCreated());

        // Ensure initially valid
        mockMvc.perform(get("/api/v1/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(true)));

        assertThat(securityAlarmRepository.findAll()).isEmpty();

        // Tamper directly in database
        List<AuditLedger> entries = auditLedgerRepository.findAllByOrderBySeqAsc();
        assertThat(entries).isNotEmpty();
        AuditLedger firstBlock = entries.get(0);
        jdbcTemplate.update("UPDATE audit_ledger SET details = ? WHERE seq = ?",
                "{\"TAMPERED\":\"Malicious actor modified row directly in database\"}",
                firstBlock.getSeq());

        // Run ledger verify -> Must fail and trigger CRITICAL alarm!
        mockMvc.perform(get("/api/v1/ledger/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valid", is(false)));

        // Verify that security alarm was automatically created
        List<SecurityAlarm> alarms = securityAlarmRepository.findAll();
        assertThat(alarms).isNotEmpty();

        SecurityAlarm ledgerAlarm = alarms.stream()
                .filter(a -> a.getAlarmType() == SecurityAlarmType.LEDGER_INTEGRITY_BREACH)
                .findFirst()
                .orElse(null);

        assertThat(ledgerAlarm).isNotNull();
        assertThat(ledgerAlarm.getSeverity()).isEqualTo(SecurityAlarmLevel.CRITICAL);
        assertThat(ledgerAlarm.getResourceRef()).contains("LEDGER:");
        assertThat(ledgerAlarm.isAcknowledged()).isFalse();
    }

    @Test
    @DisplayName("File Bit-Rot Tamper Trigger: Mutating file on disk causes verifyEvidenceIntegrity to trigger FILE_BIT_ROT_DETECTED alarm")
    void testFileBitRotGeneratesSecurityAlarm() throws Exception {
        byte[] originalBytes = "ORIGINAL CRIME SCENE EVIDENCE DATA 2026".getBytes(StandardCharsets.UTF_8);
        String sha256 = CryptoUtil.calculateSha256(originalBytes);

        UploadEvidenceRequest uploadReq = UploadEvidenceRequest.builder()
                .caseId(caseId)
                .evidenceNumber("EVID-BITROT-001")
                .title("Physical Evidence Snapshot")
                .classification(ClassificationLevel.CONFIDENTIAL)
                .clientSha256(sha256)
                .build();

        MockMultipartFile metaPart = new MockMultipartFile("metadata", "", MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(uploadReq));
        MockMultipartFile filePart = new MockMultipartFile("file", "crime_scene.raw", "application/octet-stream", originalBytes);

        String uploadJson = mockMvc.perform(multipart("/api/v1/evidence/upload")
                        .file(metaPart)
                        .file(filePart)
                        .header("X-Actor-Id", officerId.toString())
                        .header("X-Actor-Role", "INVESTIGATING_OFFICER"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        UUID evidenceId = UUID.fromString(objectMapper.readTree(uploadJson).path("data").path("id").asText());
        String storagePath = evidenceFileRepository.findByEvidenceId(evidenceId).orElseThrow().getStoragePath();

        // 1. Initial integrity check succeeds
        mockMvc.perform(post("/api/v1/evidence/" + evidenceId + "/verify-integrity")
                        .header("X-Actor-Id", officerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.match", is(true)));

        assertThat(securityAlarmRepository.findAll()).isEmpty();

        // 2. Tamper with the raw file bytes on disk directly (simulating bit-rot or offline file swap)
        Path physicalPath = storageService.loadAsResource(storagePath).getFile().toPath();
        Files.write(physicalPath, "TAMPERED BYTES OVERWRITTEN BY MALICIOUS PROCESS".getBytes(StandardCharsets.UTF_8));

        // 3. Verify integrity -> Mismatch detected and CRITICAL FILE_BIT_ROT_DETECTED alarm triggered!
        mockMvc.perform(post("/api/v1/evidence/" + evidenceId + "/verify-integrity")
                        .header("X-Actor-Id", officerId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.match", is(false)))
                .andExpect(jsonPath("$.data.integrityStatus", is("MISMATCH")));

        List<SecurityAlarm> alarms = securityAlarmRepository.findAll();
        assertThat(alarms).isNotEmpty();

        SecurityAlarm bitRotAlarm = alarms.stream()
                .filter(a -> a.getAlarmType() == SecurityAlarmType.FILE_BIT_ROT_DETECTED)
                .findFirst()
                .orElse(null);

        assertThat(bitRotAlarm).isNotNull();
        assertThat(bitRotAlarm.getSeverity()).isEqualTo(SecurityAlarmLevel.CRITICAL);
        assertThat(bitRotAlarm.getResourceRef()).isEqualTo("EVIDENCE:" + evidenceId);
        assertThat(bitRotAlarm.getCaseId()).isEqualTo(caseId);
    }
}
