# Secure Digital Evidence & Legal Document Management System (E-Vault)
### Smart India Hackathon (SIH26190) — Nodal Ministry: Ministry of Home Affairs (MHA)

[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-14-black.svg?style=flat-square&logo=next.js)](https://nextjs.org/)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-Stateless%20JWT-red.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![Python 3.12](https://img.shields.io/badge/Python-3.12-blue.svg?style=flat-square&logo=python)](https://www.python.org/)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.110%2B-009688.svg?style=flat-square&logo=fastapi)](https://fastapi.tiangolo.com/)
[![PaddleOCR](https://img.shields.io/badge/PaddleOCR-v3.7%2B-008080.svg?style=flat-square)](https://github.com/PaddlePaddle/PaddleOCR)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![Vault KMS](https://img.shields.io/badge/HashiCorp%20Vault-Transit%20Engine-black.svg?style=flat-square&logo=vault)](https://www.vaultproject.io/)
[![MinIO S3 WORM](https://img.shields.io/badge/MinIO%20S3-COMPLIANCE%20WORM-red.svg?style=flat-square&logo=minio)](https://min.io/)
[![OpenSearch](https://img.shields.io/badge/OpenSearch-Hybrid%20Search-blue.svg?style=flat-square&logo=opensearch)](https://opensearch.org/)
[![OpenPDF](https://img.shields.io/badge/OpenPDF-2.0.3-yellow.svg?style=flat-square)](https://github.com/LibrePDF/OpenPDF)
[![Build Status](https://img.shields.io/badge/Build-Passing%20(67%2F67%20Tests)-success.svg?style=flat-square)]()
[![Compliance](https://img.shields.io/badge/Compliance-BSA%202023%20§63-informational.svg?style=flat-square)]()

---

## 1. Executive Summary & System Vision

The **Secure Digital Evidence and Legal Document Management System (E-Vault)** is an enterprise-grade, zero-trust platform engineered to secure the acquisition, storage, analysis, chain of custody, and legal certification of digital evidence and case files for Indian law enforcement agencies, forensic laboratories, prosecutors, and judicial courts.

> [!IMPORTANT]
> Developed for **Problem Statement ID SIH26190** under the guidance of the **Ministry of Home Affairs (MHA)**, the E-Vault bridges the gap between field investigation digital capture and court admissibility under the **Bharatiya Sakshya Adhiniyam, 2023 (BSA)**.

### 1.1 The Systemic Problem (Problem Statement ID: SIH26190)
The Indian criminal justice lifecycle relies heavily on sensitive documentation—including First Information Reports (FIRs), Case Diaries, Seizure Memos, Charge Sheets, CCTV footage, phone extractions, and Forensic Laboratory (FSL) Reports. Currently, digitized legal records face three systemic vulnerabilities:

1. **Evidentiary Compromise**: Electronic evidence submitted in court is frequently rejected under **Section 63 of the Bharatiya Sakshya Adhiniyam, 2023 (BSA)** if the prosecution cannot prove an unbroken, tamper-evident chain of custody from the exact second of seizure.
2. **Insider Threat & Retroactive Tampering**: Database administrators or malicious insiders can silently alter, swap, or backdate evidence files in standard SQL databases without leaving an unalterable audit trail.
3. **Information Leaks & Data Exfiltration**: Uncontrolled file downloads, lack of view-only sandboxing, and missing attribute-based access controls lead to leaks of sensitive witness statements, victim identities, and active investigation strategies.

---

## 2. Statutory Legal Framework — BSA 2023 (§63)

Effective **July 1, 2024**, the Indian Parliament enacted the **Bharatiya Sakshya Adhiniyam, 2023 (BSA)**, replacing the Indian Evidence Act, 1872. Section 63 governs the **Admissibility of Electronic Records** (superseding the former Section 65B):

- **§63(2)** defines statutory conditions under which computer outputs and digital prints are legally deemed admissible documents.
- **§63(4)** mandates that an official statutory certificate accompany every electronic record presented in court, declaring:
  1. Complete identification of the electronic record and production particulars.
  2. Device parameters (hardware make, model, serial number, operating system).
  3. Continuous lawful custody and proper device operation during extraction.
  4. Cryptographic hash snapshots establishing that the record has not been altered or tampered with.
  5. Official digital or physical signature of the certifying officer.

The E-Vault provides an **automated, cryptographically signed implementation of Section 63 BSA**, auto-generating Schedule-compliant court certificates while maintaining an append-only hash-chained audit ledger.

---

## 3. High-Level Enterprise System Architecture

The E-Vault operates across a **5-Tier Zero-Trust Architecture**, decoupling client interaction, security perimeter, core application logic, microservice integrations, and immutable persistence.

```mermaid
flowchart TD
    subgraph T1 ["🌐 Tier 1: Client & Sandboxed UI Layer (Next.js 14 / React 18)"]
        UI["React 18 Web Dashboard"]
        WebCrypto["WebCrypto API (Client SHA-256 Pre-Hashing)"]
        CanvasViewer["Sandboxed HTML5 Canvas Viewer (PDF.js)"]
        Watermark["Dynamic Watermark Overlay (Badge, IP, Timestamp)"]
    end

    subgraph T2 ["🛡️ Tier 2: Gateway & Security Perimeter (Spring Boot 4.1.1)"]
        JWTFilter["JwtAuthenticationFilter (Stateless Bearer Tokens)"]
        RBACGate["RBAC Evaluator (7 Security Roles)"]
        ABACService["CaseSecurityService (Lead Officer Scoping)"]
        OPAEngine["OPA Policy Engine (Rego ABAC Engine)"]
    end

    subgraph T3 ["⚙️ Tier 3: Core Application Services"]
        EvidenceService["EvidenceIngestionService (Dual Hash Streaming)"]
        CustodyService["ChainOfCustodyService (Handoff Management)"]
        CertService["BsaCertificateGenerator (OpenPDF BSA §63 Engine)"]
        LedgerService["HashChainedAuditLedger (Immutable Block Chain)"]
        AlarmService["SecurityAlarmService (WebSocket Alarm Engine)"]
    end

    subgraph T4 ["🧩 Tier 4: Microservices & Cloud Integrations"]
        AIService["Python FastAPI AI Service (PaddleOCR + spaCy NER)"]
        VaultKMS["HashiCorp Vault KMS (Transit Engine DEK Wrapping)"]
        MinIOStorage["MinIO / S3 Storage (COMPLIANCE Object Lock WORM)"]
        OpenSearch["Hybrid OpenSearch Engine (BM25 + k-NN Search)"]
        TSAService["RFC 3161 Timestamping (BouncyCastle TSP)"]
    end

    subgraph T5 ["💾 Tier 5: Immutability & Persistence Layer"]
        PostgresDB[("PostgreSQL 16 DB (Relational Data)")]
        AuditChain[("Append-Only Hash Ledger (Genesis-to-Head Chain)")]
    end

    UI --> WebCrypto
    UI --> CanvasViewer
    CanvasViewer --> Watermark

    UI -->|"HTTPS REST (Bearer JWT)"| JWTFilter
    JWTFilter --> RBACGate
    RBACGate --> ABACService
    ABACService --> OPAEngine

    OPAEngine --> EvidenceService
    OPAEngine --> CustodyService
    OPAEngine --> CertService

    EvidenceService --> VaultKMS
    EvidenceService --> MinIOStorage
    EvidenceService --> AIService
    EvidenceService --> TSAService
    EvidenceService --> OpenSearch

    CustodyService --> LedgerService
    LedgerService --> AuditChain
    CertService --> TSAService
    EvidenceService --> PostgresDB
    AlarmService -->|"STOMP WebSockets"| UI
```

---

## 4. End-to-End Operational Workflows & Flowcharts

### 4.1 End-to-End System Sequence (5-Tier Lifecycle)

```mermaid
sequenceDiagram
    autonumber
    actor IO as 👮 Investigating Officer
    actor FA as 🔬 Forensic Analyst
    actor PR as ⚖️ Public Prosecutor
    actor CT as 🏛️ Judge / Court
    actor AD as 🕵️ Independent Auditor

    participant Web as 🌐 Next.js Frontend
    participant API as 🛡️ Backend Gateway
    participant KMS as 🔑 Vault KMS
    participant S3 as 📦 MinIO S3 WORM
    participant DB as 💾 PostgreSQL DB
    participant AI as 🤖 Python AI Service

    Note over IO, Web: Tier 1: Pre-Acquisition & Client Pre-Sealing
    IO->>Web: Select evidence file (e.g., CCTV_dump.mp4)
    Web->>Web: WebCrypto calculates client SHA-256 checksum
    Web->>API: POST /api/v1/evidence/upload (Binary + X-Client-SHA256 + JWT)

    Note over API: Tier 2: Ingress Security & Stream Verification
    API->>API: JwtAuthenticationFilter validates Bearer token
    API->>API: CaseSecurityService evaluates ABAC lead officer check
    API->>API: DigestInputStream calculates server SHA-256 & SHA-512
    API->>API: Validate clientSha256 == serverSha256

    Note over API, DB: Tier 3: Cryptographic Envelope Encryption & WORM Storage
    API->>API: Generate random single-use AES-256 DEK & 12-byte IV
    API->>KMS: Wrap DEK using Master KEK (evault-master-kek)
    KMS-->>API: Return Base64 wrappedDek ciphertext
    API->>S3: Store encrypted payload (COMPLIANCE Object Lock)
    API->>DB: ATOMIC COMMIT: EvidenceItem + CustodyEvent + AuditLedger block
    API-->>Web: 201 Created (Evidence ID + HMAC Seal)

    Note over FA, AI: Tier 4: AI/ML Analysis & Non-Destructive Annotation
    IO->>API: Transfer custody to Analyst + statutory reason
    FA->>API: POST /api/v1/evidence/id/analyze
    API->>AI: POST /api/v1/process-document (Image/PDF payload)
    AI->>AI: PaddleOCR extracts text + spaCy NER flags PII (Aadhaar/PAN)
    AI-->>API: Return JSON Extracted Text + Bounding Box Coordinates
    API->>DB: Save AiAnalysis annotation (Original binary UNTOUCHED)

    Note over PR, AD: Tier 5: Statutory BSA §63 Certification & Independent Audit
    PR->>API: POST /api/v1/certificates/evidence/id
    API->>API: OpenPDF compiles §63 PDF (Part A hardware, Part B hashes, QR Code)
    API->>DB: Update custodyState -> CERTIFIED + append Ledger block
    CT->>Web: Inspect docket in Sandboxed Canvas Viewer (Dynamic Watermark)
    AD->>API: GET /api/v1/ledger/verify
    API-->>AD: Recalculate Hash Chain -> "LEDGER_INTEGRITY_VERIFIED"
```

---

### 4.2 Evidence Ingestion, Streaming Dual-Digest & Envelope Encryption Workflow

```mermaid
flowchart TD
    Start([" Upload Digital Evidence File"]) --> PreHash["Client WebCrypto calculates SHA-256 checksum"]
    PreHash --> Request["HTTP Multipart POST /api/v1/evidence/upload<br>Header: X-Client-SHA256"]
    Request --> AuthGate{"JWT Valid & Lead Officer ABAC Passed?"}
    AuthGate -- "No" --> Reject403["403 Forbidden: Unauthorized Access"]
    AuthGate -- "Yes" --> Stream["Pass stream to DigestInputStream"]

    Stream --> HashCalc["Compute Server SHA-256 & SHA-512 Digests in Streaming Mode"]
    HashCalc --> MatchCheck{"Server SHA-256 == Client SHA-256?"}
    MatchCheck -- "Mismatch" --> Abort400["400 Bad Request: In-Flight Hash Tamper Abort"]
    MatchCheck -- "Match" --> KeyGen["Generate single-use AES-256 DEK & random 12-byte IV"]

    KeyGen --> VaultWrap["Call HashiCorp Vault Transit Engine to wrap DEK under Master KEK"]
    VaultWrap --> PayloadEncrypt["Encrypt raw payload with AES-256-GCM DEK"]
    PayloadEncrypt --> MinIOWrite["Store encrypted binary in MinIO S3 WORM COMPLIANCE Lock mode"]

    MinIOWrite --> TimestampTSA["Fetch RFC 3161 Time-Stamp Token (TST) from TSA"]
    TimestampTSA --> DBCommit["Atomic PostgreSQL Transaction:<br>EvidenceItem + CustodyEvent + AuditLedger block"]
    DBCommit --> Success(["Return 201 Created with Digital HMAC Seal & Evidence ID"])
```

---

### 4.3 Append-Only Hash-Chained Audit Ledger & Tamper Verification Workflow

```mermaid
flowchart TD
    subgraph LedgerStructure ["Genesis-to-Head Cryptographic Hash Chain"]
        Genesis["Block 1 (Genesis)<br>seq=1 | prevHash=000...000<br>entryHash=H(seq1 + data1 + prevHash)"]
        Block2["Block 2 (Ingest)<br>seq=2 | prevHash=entryHash1<br>entryHash=H(seq2 + data2 + entryHash1)"]
        Block3["Block 3 (Custody Transfer)<br>seq=3 | prevHash=entryHash2<br>entryHash=H(seq3 + data3 + entryHash2)"]
        BlockN["Block N (BSA §63 Cert)<br>seq=N | prevHash=entryHashN-1<br>entryHash=H(seqN + dataN + entryHashN-1)"]
    end

    Genesis -->|"SHA-256 Link"| Block2
    Block2 -->|"SHA-256 Link"| Block3
    Block3 -->|"... Hash Chain"| BlockN

    subgraph VerificationProcess ["1-Click Auditor Ledger Verification (GET /api/v1/ledger/verify)"]
        FetchBlocks["Fetch all ledger entries ordered by sequence BIGSERIAL"] --> LoopInit["Set computedPreviousHash = 000...000"]
        LoopInit --> LoopStart["For each Block N in ledger"]
        LoopStart --> CheckPrev{"block.previousHash == computedPreviousHash?"}
        CheckPrev -- "Mismatch" --> PinpointBreak["Pinpoint exact tampered block: brokenSeq = N"]
        CheckPrev -- "Match" --> RecalcHash["Recalculate SHA-256(seq + timestamp + actor + action + ref + details + prevHash)"]
        RecalcHash --> CheckHash{"recalculatedHash == block.entryHash?"}
        CheckHash -- "Mismatch" --> PinpointBreak
        CheckHash -- "Match" --> NextBlock["Set computedPreviousHash = block.entryHash"]
        NextBlock --> LoopNext{"More blocks?"}
        LoopNext -- "Yes" --> LoopStart
        LoopNext -- "No" --> LedgerVerified(["LEDGER_INTEGRITY_VERIFIED Seal Returned"])
    end

    PinpointBreak --> AlertTrigger["Dispatch WebSockets Security Alarm to /topic/security/alarms"]
```

---

### 4.4 Statutory BSA 2023 §63 PDF Certificate Generation Workflow

```mermaid
flowchart TD
    ReqCert["User or Prosecutor requests Certificate for Evidence ID"] --> FetchData["Fetch Evidence, Case, Officers & Custody Timeline"]
    FetchData --> HardwareMeta["Extract seizure device make, model, serial number & OS"]
    HardwareMeta --> HashSnapshots["Extract SHA-256 & SHA-512 intake cryptographic hashes"]
    HashSnapshots --> CustodyTable["Format complete officer custody handoff timeline table"]

    CustodyTable --> OpenPDF["OpenPDF 2.0.3 Engine compiles Schedule PDF template"]
    OpenPDF --> RenderPartA["Render Part A: Case Details, Seizure Location & Hardware Parameters"]
    RenderPartA --> RenderPartB["Render Part B: Monospace Hash Snapshots & Custody Log"]
    RenderPartB --> StatutoryDecl["Embed Statutory BSA §63 Officer Legal Declaration"]
    StatutoryDecl --> QRGen["Generate dynamic QR Code linking to verification URL"]

    QRGen --> TSASign["Fetch RFC 3161 Cryptographic Timestamp & Embed Digital Seal"]
    TSASign --> PDFOutput(["Output Statutory BSA §63 PDF Court Certificate"])
```

---

### 4.5 AI/ML Computer Vision & Legal NER Processing Pipeline

```mermaid
flowchart TD
    TriggerAI["POST /api/v1/evidence/id/analyze"] --> CheckType{"File Type Image or PDF?"}
    CheckType -- "Image / PDF" --> PyGateway["HTTP POST to Python FastAPI Microservice :8000"]
    PyGateway --> PaddleOCRProc["PaddleOCR structural layout analysis & text extraction"]

    PaddleOCRProc --> TextExtracted["Raw text lines + pixel spatial bounding box coordinates"]
    TextExtracted --> SpacyNER["spaCy legal NER engine + Regex scanners"]

    SpacyNER --> FlagAadhaar["Detect 12-Digit Aadhaar numbers"]
    SpacyNER --> FlagPAN["Detect 10-Char PAN identifiers"]
    SpacyNER --> FlagPhone["Detect phone numbers & email addresses"]
    SpacyNER --> FlagIPC["Detect IPC / BNS statutory sections"]

    FlagAadhaar & FlagPAN & FlagPhone & FlagIPC --> CoordinatesMap["Map PII tokens to spatial bounding boxes: min_x, min_y, max_x, max_y"]
    CoordinatesMap --> DBStore["Store annotations in PostgreSQL AiAnalysis entity"]
    DBStore --> FinishAI(["Return JSON OCR & Bounding Box payload to Frontend Canvas"])
```

---

### 4.6 Security Perimeter: 7-Role RBAC & ABAC Scoping Workflow

```mermaid
flowchart TD
    ReqIn["HTTP Request Received"] --> ExtractJWT["Extract Bearer JWT token"]
    ExtractJWT --> VerifySig{"JWT Signature Valid & Not Expired?"}
    VerifySig -- "No" --> Err401["401 Unauthorized: Invalid Token"]
    VerifySig -- "Yes" --> ExtractRole["Extract user role from claims"]

    ExtractRole --> CheckRBAC{"Role Authorized by @PreAuthorize Gate?"}
    CheckRBAC -- "No" --> Err403["403 Access Denied: Role Not Allowed"]
    CheckRBAC -- "Yes" --> EvaluateABAC{"Evaluate ABAC CaseSecurityService"}

    EvaluateABAC --> IsAdminOrAuditor{"Role is SYSTEM_ADMIN or AUDITOR?"}
    IsAdminOrAuditor -- "Yes" --> GrantAccess["Access Granted: Full System Scope"]
    IsAdminOrAuditor -- "No" --> IsAssigned{"User is Lead Officer or Active Custodian?"}
    IsAssigned -- "Yes" --> GrantAccess
    IsAssigned -- "No" --> Err403ABAC["403 Access Denied: Outside Case Scoping Bounds"]

    GrantAccess --> DispatchHandler["Dispatch request to Controller Handler"]
```

---

## 5. Comprehensive Technology Stack & Services Used

| Subsystem / Layer | Technology | Version | Operational Purpose |
|---|---|---|---|
| **Runtime & Language** | Java LTS | 21 (`Temurin-21`) | Enterprise backend runtime, virtual threads, records, pattern matching |
| **Backend Framework** | Spring Boot | 4.1.1 | REST Web Services, dependency injection, autoconfiguration |
| **Frontend Framework** | Next.js / React | 14 / 18 | SSR web application, sandboxed document viewing, dynamic watermarking |
| **Security Perimeter** | Spring Security | Stateless JWT | Bearer token authentication, RBAC method security, ABAC case scoping |
| **Policy Engine** | Open Policy Agent (OPA) | Rego Engine | Fine-grained Attribute-Based Access Control (ABAC) evaluation |
| **Persistence Layer** | Spring Data JPA / Hibernate | ORM 7 | PostgreSQL data mapping, optimistic locking, audit entity persistence |
| **Primary Database** | PostgreSQL | 16+ | Production relational store & append-only hash-chained audit ledger |
| **Test Database** | H2 Database | In-Memory | Zero-dependency, ultra-fast test database (`MODE=PostgreSQL`) |
| **PDF Certificate Generator** | OpenPDF | 2.0.3 | Automated BSA 2023 §63 statutory PDF certificate compilation |
| **API Documentation** | SpringDoc OpenAPI | 3.0 (Swagger UI) | Interactive REST API documentation and client sandbox |
| **AI/ML Runtime** | Python | 3.12 | Microservice runtime for heavy computer vision tasks |
| **AI Framework** | FastAPI + Uvicorn | 0.110+ | Asynchronous REST microservice for OCR and NER processing |
| **OCR Computer Vision** | PaddleOCR | v3.7+ | Structural layout analysis & text extraction from scanned documents |
| **Legal NER Engine** | spaCy + Regex | Custom | Detection of Indian PII (Aadhaar, PAN, Phone) & statutory IPC sections |
| **WORM Object Storage** | MinIO / AWS S3 | S3 API | Object Lock COMPLIANCE retention mode (Write-Once-Read-Many) |
| **Key Management (KMS)** | HashiCorp Vault | Transit Engine | Envelope encryption, master KEK wrapping & unwrapping of per-file DEKs |
| **Search Engine** | Hybrid OpenSearch | REST Client | Lexical BM25 & dense vector k-NN similarity search over OCR text |
| **RFC 3161 Timestamping (TSA)** | BouncyCastle (`bcprov` / `bcpkix`) | 1.78.1 | Cryptographic 3rd-party non-repudiation timestamps on evidence & certificates |
| **Real-Time WebSockets** | Spring WebSocket + STOMP | 4.1.1 | Instant security alarm broadcasting and case activity streams |

---

## 6. Role-Based & Attribute-Based Access Control (RBAC / ABAC) Matrix

The E-Vault defines **7 granular security roles** operating under strict RBAC and lead-officer ABAC scoping:

| Role Name | Target User Persona | Primary System Capabilities |
|---|---|---|
| `INVESTIGATING_OFFICER` | Police IOs, Station House Officers (SHOs) | Case docket creation, evidence upload & pre-hashing, custody handoffs |
| `FORENSIC_ANALYST` | FSL Specialists, Cyber Crime Analysts | Evidence ingestion review, AI OCR dispatch, non-destructive annotation |
| `PUBLIC_PROSECUTOR` | State Prosecutors, Legal Counsel | Docket inspection, trial bundle assembly, statutory BSA §63 cert issuance |
| `JUDGE_COURT` | Judicial Magistrates, High Court Judges | View-only sandboxed canvas inspection, BSA §63 certificate verification |
| `SYSTEM_ADMIN` | Police IT Admins, Infrastructure Engineers | User onboarding, system configuration, alarm monitoring & acknowledgment |
| `AUDITOR` | Independent Cyber Auditors, Oversight Panels | Whole-ledger verification (`GET /ledger/verify`), security alarm monitoring |
| `CITIZEN` | General Public, Complainants | Limited FIR status lookup via public access portal |

---

## 7. Feasibility, Viability & Engineering Mitigation Matrix

### 7.1 System Feasibility & Standards Compliance
- **BSA 2023 §63**: Statutory certificate automation for court admissibility.
- **ISO/IEC 27037**: Digital evidence collection, acquisition, and preservation standards.
- **NIST SP 800-38D**: AES-GCM specification for authenticated envelope encryption.
- **RFC 6962 / RFC 3161**: Hash-chained audit structures and trusted timestamping.

### 7.2 Technical Challenges Faced & Engineering Solutions

| Challenge Experienced | Technical Root Cause | Engineering Solution Implemented |
|---|---|---|
| **PaddleOCR Win32 CPU Thread Crash** | PaddlePaddle 3.3+ PIR executor hits oneDNN/MKLDNN attribute conversion error on Windows CPU. | Implemented runtime environment overrides (`PADDLE_PDX_ENABLE_MKLDNN_BYDEFAULT=0`) and explicit `enable_mkldnn=False` initialization parameters in `ocr_service.py`. |
| **Audit Ledger Race Conditions** | Concurrent uploads could attempt to link to the same previous block hash. | Enforced database sequence locking (`BIGSERIAL`) and atomic single-transaction commit gates where evidence write, custody event, and ledger append execute in one database transaction. |
| **Large Evidence Ingest Memory Spikes** | Loading multi-gigabyte binaries into RAM causes OutOfMemory (OOM) errors. | Replaced in-memory byte arrays with Spring `DigestInputStream` streaming channels, computing SHA-256/512 checksums as bytes pass directly to storage. |
| **Statutory PDF Layout Rigidity** | Traditional PDF libraries alter layout when inserting dynamic hash values or tables. | Implemented OpenPDF with fixed table cell heights, monospace font formatting for cryptographic hashes, and automated page numbering. |

---

## 8. Impact & Stakeholder Benefits

| Stakeholder | Direct Operational Impact & Benefit |
|---|---|
| **Judicial Courts** | • Zero evidence rejections under BSA 2023 §63.<br>• 1-Click statutory certificate verification with QR seal. |
| **Investigating Officers (IOs)** | • Automated dual-hash sealing at instant of seizure.<br>• Seamless custody handoffs with statutory logging. |
| **Forensic Labs (FSL)** | • Fast automated OCR text extraction & PII entity detection.<br>• Physical disk integrity re-verification (`/verify-integrity`). |
| **Public Prosecutors** | • Streamlined trial bundle assembly with version history.<br>• Instant cross-case search over legal documents and OCR text. |
| **Independent Auditors** | • Whole-ledger cryptographic verification catching insider database modification (`GET /api/v1/ledger/verify`). |

---

## 9. Comprehensive API Reference Summary

Base REST URL: `http://localhost:8080/api/v1`

### 9.1 Authentication & Profile (`/auth`)
- `POST /api/v1/auth/register` — Onboard user account with assigned role (`RegisterRequest`).
- `POST /api/v1/auth/login` — Authenticate credentials, returning JWT token (`LoginRequest`).
- `GET /api/v1/auth/me` — Retrieve current authenticated actor profile.

### 9.2 Case Docket Management (`/cases`)
- `POST /api/v1/cases` — Initialize criminal case docket (`CreateCaseRequest`).
- `GET /api/v1/cases/{id}` — Get case docket details by ID.
- `GET /api/v1/cases/number/{caseNumber}` — Get case by FIR/case number.
- `GET /api/v1/cases` — List all cases (optional status filter).
- `PATCH /api/v1/cases/{id}/status` — Transition case status state machine.

### 9.3 Evidence Ingestion & Cryptography (`/evidence`)
- `POST /api/v1/evidence/upload` — Ingest evidence with streaming SHA-256/512 sealing (`multipart/form-data`).
- `GET /api/v1/evidence/{id}` — Retrieve evidence metadata & hash snapshots.
- `GET /api/v1/evidence/case/{caseId}` — List all evidence items in a case docket.
- `GET /api/v1/evidence/{id}/download` — Stream raw evidence binary file.
- `POST /api/v1/evidence/{id}/verify-integrity` — Re-read disk file & verify SHA-256 hash match.

### 9.4 AI Analysis & Processing (`/evidence/{evidenceId}/*`)
- `POST /api/v1/evidence/{evidenceId}/analyze` — Dispatch evidence to Python AI microservice.
- `GET /api/v1/evidence/{evidenceId}/analysis` — Retrieve AI OCR text, PII, and bounding box coordinates.

### 9.5 Chain of Custody (`/evidence/{id}/custody*`)
- `POST /api/v1/evidence/{id}/custody-transfer` — Record signed officer custody handoff (`CustodyTransferRequest`).
- `GET /api/v1/evidence/{id}/custody` — Retrieve complete chronological custody timeline.

### 9.6 Audit Ledger (`/ledger`)
- `GET /api/v1/ledger/verify` — Execute whole-ledger cryptographic hash chain verification.
- `GET /api/v1/ledger/entries` — List all immutable ledger transaction blocks.

### 9.7 Statutory BSA §63 Certification (`/certificates`)
- `POST /api/v1/certificates/evidence/{evidenceId}` — Issue statutory BSA 2023 §63 PDF certificate (`IssueCertificateRequest`).

### 9.8 Search Engine (`/search`)
- `GET /api/v1/search?query={term}&type={ALL|CASE|EVIDENCE|DOCUMENT}` — Execute hybrid search.

### 9.9 Real-Time WebSockets & Security Alarms (`/alarms` & `/ws-evault`)
- `WS /ws-evault` / `/ws` — STOMP WebSocket connection endpoint (SockJS fallback + JWT authentication).
- `SUB /topic/security/alarms` — Real-time security alarms feed (Restricted to `SYSTEM_ADMIN` & `AUDITOR`).
- `SUB /topic/case/{id}` — Real-time docket activity events for assigned case.
- `GET /api/v1/alarms` — List security alarms (filterable by acknowledgment status).
- `GET /api/v1/alarms/count-unacknowledged` — Unacknowledged alarm badge count.
- `POST /api/v1/alarms/{id}/acknowledge` — Acknowledge an active security alarm (`AUDITOR` / `SYSTEM_ADMIN`).
- `POST /api/v1/alarms/trigger-test` — Trigger security drill alarm for auditor testing.

### 9.10 RFC 3161 Trusted Timestamp Authority (`/timestamp`)
- `POST /api/v1/timestamp/stamp-hash` — Generate certified RFC 3161 Time-Stamp Token (TST) for SHA-256 hash.
- `POST /api/v1/timestamp/verify` — Cryptographically verify Base64 RFC 3161 token against expected hash.
- `GET /api/v1/timestamp/evidence/{id}` — Retrieve and verify RFC 3161 token recorded during evidence intake.
- `GET /api/v1/timestamp/certificate/{id}` — Retrieve and verify RFC 3161 token recorded on BSA §63 certificate.

---

## 10. End-to-End Setup, Execution & Testing Guide

### Prerequisites
- **Java 21 LTS** (`openjdk-21`)
- **Node.js 18+** & **npm**
- **Python 3.12+** & `pip`
- **Maven 3.9+** (or use `./mvnw`)

### Step 1: Run Backend Unit & Integration Tests
```bash
cd backend
./mvnw clean test
```
*(All 67 automated tests run and pass out-of-the-box).*

### Step 2: Launch Backend Application
```bash
cd backend
./mvnw spring-boot:run
```
- REST Service Base URL: `http://localhost:8080/api/v1`
- Swagger UI Interactive Docs: `http://localhost:8080/swagger-ui/index.html`

### Step 3: Launch AI/ML Microservice
```bash
cd AIML
python3 -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```
- AI OpenAPI Docs: `http://localhost:8000/docs`

### Step 4: Launch Next.js Frontend Dashboard
```bash
cd frontend
npm install
npm run dev
```
- Web App UI: `http://localhost:3000`

---

*SIH26190 E-Vault — Built for Smart India Hackathon. Nodal Ministry: Ministry of Home Affairs (MHA).*
