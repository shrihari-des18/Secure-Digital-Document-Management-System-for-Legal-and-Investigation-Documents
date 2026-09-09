package com.shrihari.evault.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shrihari.evault.dto.AddDocumentVersionRequest;
import com.shrihari.evault.dto.CreateCaseRequest;
import com.shrihari.evault.dto.CreateLegalDocumentRequest;
import com.shrihari.evault.dto.CreateUserRequest;
import com.shrihari.evault.model.LegalDocumentType;
import com.shrihari.evault.model.RoleEnum;
import com.shrihari.evault.repository.AuditLedgerRepository;
import com.shrihari.evault.repository.CaseRepository;
import com.shrihari.evault.repository.DocumentVersionRepository;
import com.shrihari.evault.repository.LegalDocumentRepository;
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

import static org.hamcrest.Matchers.hasSize;
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
class LegalDocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CaseRepository caseRepository;

    @Autowired
    private LegalDocumentRepository legalDocumentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private AuditLedgerRepository auditLedgerRepository;

    private UUID caseId;
    private UUID officerId;

    @BeforeEach
    void setUp() throws Exception {
        documentVersionRepository.deleteAll();
        legalDocumentRepository.deleteAll();
        auditLedgerRepository.deleteAll();
        caseRepository.deleteAll();
        userRepository.deleteAll();

        // 1. Create User
        CreateUserRequest userReq = CreateUserRequest.builder()
                .username("io_ajay")
                .fullName("Inspector Ajay Verma")
                .email("ajay@police.gov.in")
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
                .caseNumber("FIR-2026-DEL-102")
                .title("Identity Theft Syndicate")
                .jurisdiction("New Delhi Special Cell")
                .leadOfficerId(officerId)
                .assignedOfficerIds(Set.of(officerId))
                .build();
        String cJson = mockMvc.perform(post("/api/v1/cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        caseId = UUID.fromString(objectMapper.readTree(cJson).path("data").path("id").asText());
    }

    @Test
    @DisplayName("Legal Document Lifecycle: Create v1, Add Revision v2, Get History, and Download specific versions")
    void testLegalDocumentLifecycle() throws Exception {
        byte[] v1Bytes = "FIRST INFORMATION REPORT DATED 2026-09-08 -- ORIGINAL".getBytes(StandardCharsets.UTF_8);
        byte[] v2Bytes = "FIRST INFORMATION REPORT DATED 2026-09-09 -- REVISED WITH WITNESS STATEMENT".getBytes(StandardCharsets.UTF_8);

        // 1. Create Document (v1)
        CreateLegalDocumentRequest createRequest = CreateLegalDocumentRequest.builder()
                .caseId(caseId)
                .documentNumber("FIR-DOC-DEL-2026-001")
                .title("FIR Document Copy")
                .description("Scanned attested FIR registered at police station")
                .documentType(LegalDocumentType.FIR)
                .changeSummary("Initial lodging of FIR")
                .build();

        MockMultipartFile metaPart1 = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(createRequest)
        );

        MockMultipartFile filePart1 = new MockMultipartFile(
                "file",
                "fir_v1.pdf",
                "application/pdf",
                v1Bytes
        );

        String docCreateJson = mockMvc.perform(multipart("/api/v1/documents")
                        .file(metaPart1)
                        .file(filePart1)
                        .header("X-Actor-Id", officerId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.documentNumber", is("FIR-DOC-DEL-2026-001")))
                .andExpect(jsonPath("$.data.currentVersion", is(1)))
                .andReturn().getResponse().getContentAsString();

        UUID docId = UUID.fromString(objectMapper.readTree(docCreateJson).path("data").path("id").asText());

        // 2. Add Revision (v2)
        AddDocumentVersionRequest versionRequest = AddDocumentVersionRequest.builder()
                .changeSummary("Attached supplementary statement from witness A")
                .build();

        MockMultipartFile metaPart2 = new MockMultipartFile(
                "metadata",
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(versionRequest)
        );

        MockMultipartFile filePart2 = new MockMultipartFile(
                "file",
                "fir_v2_amended.pdf",
                "application/pdf",
                v2Bytes
        );

        mockMvc.perform(multipart("/api/v1/documents/" + docId + "/versions")
                        .file(metaPart2)
                        .file(filePart2)
                        .header("X-Actor-Id", officerId.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.versionNumber", is(2)))
                .andExpect(jsonPath("$.data.changeSummary", is("Attached supplementary statement from witness A")));

        // 3. Get Document Revision History (should have 2 versions)
        mockMvc.perform(get("/api/v1/documents/" + docId + "/versions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].versionNumber", is(2)))
                .andExpect(jsonPath("$.data[1].versionNumber", is(1)));

        // 4. Download Version 1
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download?version=1"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fir_v1.pdf\""))
                .andExpect(content().bytes(v1Bytes));

        // 5. Download Version 2 (or default latest)
        mockMvc.perform(get("/api/v1/documents/" + docId + "/download"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"fir_v2_amended.pdf\""))
                .andExpect(content().bytes(v2Bytes));
    }
}
