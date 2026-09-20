# Secure Digital Evidence & Legal Document Management System (E-Vault)
### Smart India Hackathon (SIH26190) — Backend Subsystem

[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-orange.svg?style=flat-square&logo=openjdk)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT%20Stateless-red.svg?style=flat-square&logo=springsecurity)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%2B-blue.svg?style=flat-square&logo=postgresql)](https://www.postgresql.org/)
[![OpenPDF](https://img.shields.io/badge/OpenPDF-2.0.3-yellow.svg?style=flat-square)](https://github.com/LibrePDF/OpenPDF)
[![OpenAPI 3.0](https://img.shields.io/badge/OpenAPI-3.0%20Swagger-green.svg?style=flat-square&logo=swagger)](http://localhost:8080/swagger-ui/index.html)
[![Build Status](https://img.shields.io/badge/Build-Passing%20(67%2F67%20Tests)-success.svg?style=flat-square)]()
[![Compliance](https://img.shields.io/badge/Compliance-BSA%202023%20§63-informational.svg?style=flat-square)]()

---

## 1. Executive Summary & Legal Statutory Context

The **Secure Digital Evidence and Legal Document Management System (E-Vault)** is a mission-critical backend designed to secure the integrity, custody, and lifecycle of digital evidence and case files for police departments, forensic laboratories, prosecutors, and judicial courts across India.

### The Problem
In criminal investigations, digital evidence (CCTV footage, hard drive images, seized smartphones, call detail records) is vulnerable to:
1. **Chain-of-Custody Ambiguity**: Lack of an indisputable record of who possessed, viewed, transferred, or analyzed evidence.
2. **Silent File Tampering / Bit Rot**: Unnoticed manipulation or accidental alteration of evidence files on physical disk.
3. **Statutory Admissibility Rejection**: Electronic records being thrown out of court due to non-compliance with statutory certificate requirements.

### Statutory Admissibility under Bharatiya Sakshya Adhiniyam, 2023 (§63)
Effective July 1, 2024, the Indian Parliament enacted the **Bharatiya Sakshya Adhiniyam, 2023 (BSA)**, replacing the Indian Evidence Act, 1872. Section 63 governs the **Admissibility of Electronic Records** (superseding the former Section 65B):
- **§63(2)** establishes the conditions under which computer output is deemed an admissible document.
- **§63(4)** mandates that a statutory certificate be produced containing:
  - Identification of the electronic record and device particulars (make, model, serial number).
  - Manner of production and custody details by the officer in charge.
  - Cryptographic hash snapshots ensuring the record has not been altered or tampered with.
  - Digital or physical signatures of the certifying officer.

This backend provides a **cryptographically verifiable, automated implementation of Section 63 BSA**, generating Schedule-compliant court certificates as digitally signed PDFs while maintaining an immutable hash-chained audit ledger.

---

## 2. System Architecture

The backend follows an enterprise layered architecture strictly adhering to the Single Responsibility Principle:

```mermaid
flowchart TD
  subgraph ClientLayer["Client & Investigation Portal"]
    WebClient["Web / Mobile Portal (React / Flutter)"]
    CourtClient["Court Docket System (e-Courts API)"]
  end

  subgraph GatewayLayer["Gateway & Security Perimeter"]
    SecFilter["Spring Security Filter Chain\n(CORS, CSRF Disabled, Stateless)"]
    JwtAuth["JwtAuthenticationFilter\n(HMAC-SHA256 Token Validation)"]
    ActorCtx["ActorContextHolder (ThreadLocal)"]
  end

  subgraph ControllerLayer["REST Controllers (/api/v1) & STOMP"]
    AuthCtrl["AuthController\n(/api/v1/auth)"]
    CaseCtrl["CaseController\n(/api/v1/cases)"]
    EvidCtrl["EvidenceController\n(/api/v1/evidence)"]
    CustCtrl["CustodyController\n(/api/v1/evidence/{id}/custody)"]
    LedgCtrl["LedgerController\n(/api/v1/ledger)"]
    DocCtrl["LegalDocumentController\n(/api/v1/documents)"]
    CertCtrl["CertificateController\n(/api/v1/certificates)"]
    SearchCtrl["SearchController\n(/api/v1/search)"]
    AlarmCtrl["SecurityAlarmController\n(/api/v1/alarms & STOMP /ws-evault)"]
    TsCtrl["TimestampController\n(/api/v1/timestamp)"]
    UserCtrl["UserController\n(/api/v1/users)"]
  end

  subgraph ServiceLayer["Business Logic & Cryptographic Engine"]
    AuthSvc["AuthService & CustomUserDetailsService"]
    CaseSvc["CaseService & CaseSecurityService (ABAC)"]
    EvidSvc["EvidenceService & CryptoUtil (SHA-256/512)"]
    CustSvc["CustodyService"]
    LedgSvc["AuditLedgerService (Hash Chain)"]
    DocSvc["LegalDocumentService (Versioning)"]
    CertSvc["CertificateService & PdfCertificateGenerator"]
    WormSvc["S3WormStorageService (Object Lock COMPLIANCE)"]
    KmsSvc["EnvelopeEncryptionEngine & VaultKmsClientService"]
    AlarmSvc["AlarmDispatcherService (Real-Time STOMP Broker)"]
    TsSvc["Rfc3161TimestampService (BouncyCastle TSP)"]
  end

  subgraph PersistenceLayer["Data & File Storage"]
    JPA["Spring Data JPA Repositories"]
    DB[("PostgreSQL / H2 In-Memory DB")]
    Storage["LocalStorageService & S3 WORM\n(Partitioned File Blobs ./data/uploads)"]
  end

  ClientLayer --> GatewayLayer
  GatewayLayer --> ControllerLayer
  ControllerLayer --> ServiceLayer
  ServiceLayer --> PersistenceLayer
```

---

## 3. Technology Stack

| Layer | Technology | Purpose |
|---|---|---|
| **Runtime & Language** | Java 21 LTS (`Temurin-21`) | Modern Java runtime, Records, Pattern Matching, Sealed Classes |
| **Framework** | Spring Boot 4.1.1 | Core application framework and autoconfiguration |
| **Web MVC & WebSockets** | Spring Web MVC + Spring WebSocket | RESTful API controllers, multipart streaming, STOMP real-time messaging |
| **Security** | Spring Security 6/7 + JJWT 0.12.6 | Stateless JWT authentication, RBAC, Case-level ABAC |
| **Cryptographic Timestamping** | BouncyCastle 1.78.1 (`bcpkix` / `bcprov`) | RFC 3161 Time-Stamp Protocol (TSP) for non-repudiation time tokens |
| **Envelope Encryption** | HashiCorp Vault Transit KMS | AES-256-GCM DEK generation, master KEK wrapping & unwrapping |
| **Object Storage** | MinIO / AWS S3 WORM | Object Lock COMPLIANCE mode hardware-enforced immutability |
| **Persistence** | Spring Data JPA + Hibernate ORM 7 | Object-relational mapping, custom repositories, optimistic locking |
| **Primary Database** | PostgreSQL 16+ | Production relational database (Neon Cloud / Local) |
| **Test Database** | H2 In-Memory (`MODE=PostgreSQL`) | Zero-dependency, ultra-fast test database |
| **PDF Generation** | OpenPDF 2.0.3 | Statutory BSA 2023 §63 court certificate PDF generation |
| **API Documentation** | SpringDoc OpenAPI 3.0 / Swagger UI | Interactive OpenAPI explorer and REST client UI |
| **Boilerplate Reduction** | Project Lombok | Auto-generation of getters, setters, builders, and loggers |
| **Build Tool** | Apache Maven Wrapper (`./mvnw`) | Dependency management, compilation, and automated test execution |

---

## 4. Subsystems Built & Verified (Chunks 1–7)

### Chunk 1: Foundation, Database & Case Management
- **Base Infrastructure**: Standardized RFC 7807 problem details in `GlobalExceptionHandler`, unified envelope `ApiResponse<T>`, and `ActorContextHolder` capturing thread-safe actor identity.
- **Role Hierarchy**: `RoleEnum` with granular roles:
  - `INVESTIGATING_OFFICER` — Case registration, evidence intake, handover.
  - `FORENSIC_ANALYST` — Extraction, packet analysis, hash verification.
  - `PROSECUTOR` — Document review, trial bundle assembly, certificate filing.
  - `COURT` — Judicial review, digital docket inspection, certificate download.
  - `AUDITOR` — Whole-ledger cryptographic integrity auditing.
  - `RECORDS_CUSTODIAN` — Physical and digital vault inventory management.
  - `SYSTEM_ADMIN` — System administration and tenant user lifecycle.
- **Case Docket Lifecycle**: `Case` entity tracking FIR numbers, jurisdiction, lead investigating officer, assigned officers, and status state machine:
  $$\text{OPEN} \longrightarrow \text{UNDER\_INVESTIGATION} \longrightarrow \text{CHARGESHEET\_FILED} \longrightarrow \text{IN\_COURT} \longrightarrow \text{CLOSED} \longrightarrow \text{ARCHIVED}$$

### Chunk 2: Evidence Intake & Cryptographic Storage Subsystem
- **Dual Cryptographic Hashing**:
  - Computes streaming **SHA-256** (256 bits, 64 hex characters) and **SHA-512** (512 bits, 128 hex characters) at the exact moment of ingestion.
  - Validates against `clientSha256` (if provided by field acquisition software) to detect in-transit transmission corruption or tampering.
- **Digital Signatures**: Computes HMAC-SHA256 server signatures over raw binaries.
- **Storage Partitioning**: `LocalStorageService` partitions evidence blobs under `./data/uploads/cases/{caseId}/evidence/{evidenceId}/` with directory-traversal prevention.
- **Physical Integrity Verification**:
  - `POST /api/v1/evidence/{id}/verify-integrity`: Re-reads the file from disk, recalculates SHA-256 on the fly, compares against the ingestion hash snapshot, and flags any disk alteration or bit rot.

### Chunk 3: Tamper-Evident Append-Only Audit Ledger & Chain of Custody
- **Cryptographic Hash Chain Formula**:
  Every database modification (upload, transfer, view, certificate issuance) appends an immutable block into `audit_ledger`:
  $$\text{entryHash}_n = \text{SHA-256}(\text{seq}_n \parallel \text{occurredAt}_n \parallel \text{actorId}_n \parallel \text{action}_n \parallel \text{resourceRef}_n \parallel \text{details}_n \parallel \text{entryHash}_{n-1})$$
  - Genesis block ($n=1$) links to `0000000000000000000000000000000000000000000000000000000000000000`.
  - Database columns are annotated with `@Column(updatable = false)` to prevent JPA updates.
- **Whole-Ledger Cryptographic Audit (`GET /api/v1/ledger/verify`)**:
  - Recursively recomputes all SHA-256 hashes from genesis block to head.
  - If a malicious database administrator modifies even one byte of historical data directly in SQL, the verification algorithm instantly detects the violation and pinpoints the exact sequence number (`brokenSeq`).
- **Chain of Custody**:
  - Tracks all lifecycle events: `INGEST`, `TRANSFER`, `VIEW`, `DOWNLOAD`, `ANALYZE`, `CERTIFY`, `DISPOSE`.
  - `POST /api/v1/evidence/{id}/custody-transfer`: Records officer handoffs with mandatory statutory justifications.
  - `GET /api/v1/evidence/{id}/custody`: Returns a court-ready chronological custody timeline.

### Chunk 4: Legal Document Management & Versioning
- **Legal Document Types**: `FIR`, `CASE_DIARY`, `SEARCH_WARRANT`, `CHARGE_SHEET`, `SEIZURE_MEMO`, `FORENSIC_REPORT`, `BAIL_APPLICATION`, `COURT_ORDER`.
- **Immutable Version History**:
  - Each document tracks an incrementing version number ($v1, v2, \dots, vN$).
  - Every amended version records its own unique SHA-256 hash, upload timestamp, uploading officer, and change summary.
  - Enables downloading specific revisions (e.g., initial charge sheet vs. supplementary charge sheet).

### Chunk 5: Statutory Evidence Certification (BSA 2023 §63)
- **Official Schedule Format**:
  - Auto-generates official PDF certificates formatted strictly according to the statutory Schedule under Section 63 of the Bharatiya Sakshya Adhiniyam, 2023:
    - **Official Header**: Ministry of Home Affairs / Police Department insignia and formal certificate identifier (`CERT-BSA63-...`).
    - **Metadata Block**: Case/FIR number, court jurisdiction, date, and certifying officer badge number.
    - **Part A (Particulars of Device & Extraction)**: Hardware device make, model, serial number, seizure timestamp, extraction software, and manner of production.
    - **Part B (Cryptographic Hash Snapshot)**: Prominent monospace display of SHA-256 and SHA-512 hashes, plus HMAC digital signature ID.
    - **Chain of Custody Table**: Complete chronological event history including seizure, ingest, handovers, and forensic access.
    - **Statutory Declaration under §63(4)**: Mandatory legal declaration asserting system integrity, lack of tampering, and lawful custody during extraction.
    - **Signature Blocks**: Official certifying officer signature line and automated digital verification seal.
- **Lifecycle Automation**:
  - Issuing a certificate automatically updates evidence `custodyState` to `CERTIFIED`.
  - Records a `CERTIFY` event into the chain of custody.
  - Appends an audit record (`CERTIFICATE_ISSUED`) into the immutable hash ledger.

### Chunk 6: Authentication & Role-Based Access Control (Security)
- **Stateless JWT Engine**:
  - Uses `io.jsonwebtoken` (JJWT 0.12.6) with HMAC-SHA256 signature verification.
  - JWT tokens encode `sub` (username), `userId` (UUID), `role` (`RoleEnum`), `email`, and configurable expiration.
- **Password Security**:
  - Salted and hashed passwords stored using `BCryptPasswordEncoder`.
- **Authentication Endpoints**:
  - `POST /api/v1/auth/register`: Onboards users with role assignment and returns initial Bearer token.
  - `POST /api/v1/auth/login`: Validates credentials with BCrypt verification and issues Bearer token.
  - `GET /api/v1/auth/me`: Retrieves current authenticated actor profile.
- **Role-Based Access Control (RBAC)**:
  - Secured via Spring Security `@PreAuthorize` method security.
  - Example: `POST /api/v1/cases` is restricted to `INVESTIGATING_OFFICER` and `SYSTEM_ADMIN`; a `FORENSIC_ANALYST` receives `403 Forbidden`.
  - Example: `/api/v1/ledger/entries` is restricted to `AUDITOR` and `SYSTEM_ADMIN`.
- **Attribute-Based Access Control (ABAC)**:
  - `CaseSecurityService.java`: Verifies whether an investigating officer is the lead officer or an assigned team member for a specific case ID before granting mutation rights.
  - Unassigned officers attempting to alter case status receive `403 Forbidden`.

### Chunk 7: AI/ML Gateway Integration Subsystem
- **AI Evidence Analysis**:
  - `AiAnalysis` entity linked to `EvidenceItem` with status tracking (`PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`).
  - Extracts OCR text, detects PII entities (Aadhaar numbers, PAN cards, phone numbers), applies auto-classification tags, computes perceptual hash (pHash), and calculates ML confidence scores.
- **Microservice Gateway Client**:
  - `AiGatewayClient.java`: Communicates with the external Python FastAPI ML microservice at `http://localhost:8000`. Features resilient mock fallback logic for zero-dependency execution if the Python service is offline.
- **Custody & Ledger Audit Integration**:
  - Triggering analysis (`POST /api/v1/evidence/{evidenceId}/analyze`) automatically logs an `ANALYZE` event in `CustodyService` and appends an `EVIDENCE_ANALYZED` transaction block into the immutable `AuditLedger`.
- **Immutability Guarantee**:
  - Raw evidence files and primary SHA-256/SHA-512 hashes remain untouched; AI annotations reside strictly in the dedicated `ai_analyses` entity.

### Tasks 1, 2 & 4: Storage WORM, KMS Envelope Encryption & Hybrid Search Subsystems
- **Task 1: BE-01 (MinIO / S3 WORM Object Storage Integration)**:
  - `S3WormStorageService.java`: Implements S3 / MinIO Object Lock `COMPLIANCE` retention mode for hardware-enforced immutability. Automatically defaults to `LocalStorageService` (`app.storage.type=local`) with zero-dependency execution.
- **Task 2: BE-02 (Key Vault KMS Envelope Encryption Subsystem)**:
  - `EnvelopeEncryptionEngine.java` & `VaultKmsClientService.java`: Performs AES-256-GCM envelope encryption over raw evidence binary payloads. Wraps/unwraps ephemeral 256-bit DEKs using HashiCorp Vault Transit Engine with resilient local Master KEK fallback for dev/test environments.
- **Task 4: BE-04 (OpenSearch Hybrid Full-Text & Vector Search Integration)**:
  - `OpenSearchClientService.java` & `SearchController.java`: Exposes `GET /api/v1/search` for full-text and semantic vector search across cases, evidence OCR text, detected PII, and versioned legal documents with automated JPA database search fallback.

---

## 5. API Reference Guide

Base URL: `http://localhost:8080/api/v1`

### Authentication (`/auth`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/auth/register` | Register new user account | Public | `RegisterRequest` |
| `POST` | `/api/v1/auth/login` | Authenticate and obtain JWT token | Public | `LoginRequest` |
| `GET` | `/api/v1/auth/me` | Get current actor profile | Authenticated | None |

### Cases (`/cases`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/cases` | Create new criminal/legal investigation case | `INVESTIGATING_OFFICER`, `SYSTEM_ADMIN` | `CreateCaseRequest` |
| `GET` | `/api/v1/cases/{id}` | Get case metadata by ID | Assigned Officers, Court, Admin, Auditor | None |
| `GET` | `/api/v1/cases/number/{caseNumber}` | Get case by FIR/Case number | Authenticated | None |
| `GET` | `/api/v1/cases` | List cases (optional `status` filter) | Authenticated | None |
| `PUT` | `/api/v1/cases/{id}` | Update case metadata | Assigned Lead/Team Officer, Admin | `UpdateCaseRequest` |
| `PATCH` | `/api/v1/cases/{id}/status` | Transition case lifecycle status | Assigned Lead/Team Officer, Admin | `UpdateCaseStatusRequest` |

### Evidence Intake & Management (`/evidence`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/evidence/upload` | Ingest and seal evidence with SHA-256/512 | `INVESTIGATING_OFFICER`, `SYSTEM_ADMIN` | `multipart/form-data`: `metadata` (JSON) + `file` (Binary) |
| `GET` | `/api/v1/evidence/{id}` | Get evidence details & hash snapshot | IO, Analyst, Prosecutor, Court, Custodian, Admin | None |
| `GET` | `/api/v1/evidence/case/{caseId}` | List all evidence items in a case | IO, Analyst, Prosecutor, Court, Custodian, Admin | None |
| `GET` | `/api/v1/evidence/{id}/download` | Stream raw sealed evidence file | IO, Analyst, Prosecutor, Court, Custodian, Admin | None |
| `POST` | `/api/v1/evidence/{id}/verify-integrity` | On-disk physical integrity re-verification | IO, Analyst, Prosecutor, Court, Custodian, Auditor, Admin | None |

### AI Evidence Analysis (`/evidence/{evidenceId}/*`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/evidence/{evidenceId}/analyze` | Dispatch evidence to AI/ML microservice | `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `SYSTEM_ADMIN` | `TriggerAnalysisRequest` (Optional) |
| `GET` | `/api/v1/evidence/{evidenceId}/analysis` | Retrieve AI OCR, PII, tags, & pHash result | `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `PROSECUTOR`, `COURT`, `SYSTEM_ADMIN` | None |

### Chain of Custody (`/evidence/{id}/custody*`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/evidence/{id}/custody-transfer` | Transfer custody to another officer/court | `INVESTIGATING_OFFICER`, `RECORDS_CUSTODIAN`, `FORENSIC_ANALYST`, `SYSTEM_ADMIN` | `CustodyTransferRequest` |
| `GET` | `/api/v1/evidence/{id}/custody` | Get complete chronological custody timeline | IO, Custodian, Analyst, Prosecutor, Court, Auditor, Admin | None |

### Tamper-Evident Audit Ledger (`/ledger`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `GET` | `/api/v1/ledger/verify` | Verify whole-ledger SHA-256 cryptographic chain | `AUDITOR`, `SYSTEM_ADMIN`, `INVESTIGATING_OFFICER`, `COURT`, `PROSECUTOR`, `RECORDS_CUSTODIAN` | None |
| `GET` | `/api/v1/ledger/entries` | List all hash-chained ledger transaction blocks | `AUDITOR`, `SYSTEM_ADMIN` | None |

### Statutory BSA §63 Certification (`/certificates`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/certificates/evidence/{evidenceId}` | Issue statutory BSA 2023 §63 certificate | `INVESTIGATING_OFFICER`, `PROSECUTOR`, `COURT`, `RECORDS_CUSTODIAN`, `SYSTEM_ADMIN` | `IssueCertificateRequest` (Optional) |
| `GET` | `/api/v1/certificates/{id}` | Get certificate metadata and hash snapshot | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |
| `GET` | `/api/v1/certificates/evidence/{evidenceId}` | List certificates for an evidence item | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |
| `GET` | `/api/v1/certificates/{id}/download` | Download court-admissible certificate PDF | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |

### Legal Document Management (`/documents`)
| Method | Endpoint | Description | Permitted Roles | Request Body |
|---|---|---|---|---|
| `POST` | `/api/v1/documents` | Upload initial version (v1) of legal doc | `INVESTIGATING_OFFICER`, `PROSECUTOR`, `COURT`, `SYSTEM_ADMIN` | `multipart/form-data`: `metadata` + `file` |
| `POST` | `/api/v1/documents/{id}/versions` | Upload revision ($vN$) with change summary | `INVESTIGATING_OFFICER`, `PROSECUTOR`, `COURT`, `SYSTEM_ADMIN` | `multipart/form-data`: `metadata` + `file` |
| `GET` | `/api/v1/documents/{id}` | Get document metadata and latest active version | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |
| `GET` | `/api/v1/documents/case/{caseId}` | List legal documents for a case | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |
| `GET` | `/api/v1/documents/{id}/versions` | Get full revision history and hashes | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |
| `GET` | `/api/v1/documents/{id}/download` | Stream document file (`?version=N` optional) | IO, Prosecutor, Court, Custodian, Auditor, Admin | None |

### Search Engine Subsystem (`/search`)
| Method | Endpoint | Description | Permitted Roles | Request Body / Query Params |
|---|---|---|---|---|
| `GET` | `/api/v1/search` | Hybrid full-text & semantic vector search across cases, evidence OCR, PII, & legal docs | Authenticated Users | Query Params: `query`, `type` (`ALL`/`CASE`/`EVIDENCE`/`DOCUMENT`), `page`, `size` |

---

## 6. Directory Structure

```text
backend/
├── pom.xml                                   # Maven POM with Spring Boot 4.1.1, Security, OpenPDF, JJWT
├── src/
│   ├── main/
│   │   ├── java/com/shrihari/evault/
│   │   │   ├── EvaultApplication.java         # Spring Boot entry point
│   │   │   ├── common/                        # Shared utilities and interceptors
│   │   │   │   ├── ActorContext.java          # Thread-safe actor identity model
│   │   │   │   ├── ActorContextHolder.java    # ThreadLocal holder for actor
│   │   │   │   ├── ActorContextInterceptor.java # Backward-compatible header bridge
│   │   │   │   ├── ApiResponse.java           # Standardized JSON response envelope
│   │   │   │   └── CryptoUtil.java            # SHA-256, SHA-512, and HMAC helper
│   │   │   ├── config/                        # Configuration beans
│   │   │   │   ├── OpenApiConfig.java         # OpenAPI 3.0 info, servers, and security scheme
│   │   │   │   └── WebConfig.java             # CORS and Jackson date/time mappings
│   │   │   ├── controller/                    # REST API Controllers
│   │   │   │   ├── AiAnalysisController.java  # AI/ML evidence analysis controller
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── CaseController.java
│   │   │   │   ├── CertificateController.java
│   │   │   │   ├── CustodyController.java
│   │   │   │   ├── EvidenceController.java
│   │   │   │   ├── LedgerController.java
│   │   │   │   ├── LegalDocumentController.java
│   │   │   │   └── UserController.java
│   │   │   ├── dto/                           # Data Transfer Objects & Validation
│   │   │   │   ├── AddDocumentVersionRequest.java
│   │   │   │   ├── AiAnalysisResponse.java
│   │   │   │   ├── AuditLedgerResponse.java
│   │   │   │   ├── AuthResponse.java
│   │   │   │   ├── CaseResponse.java
│   │   │   │   ├── CertificateResponse.java
│   │   │   │   ├── CreateCaseRequest.java
│   │   │   │   ├── CreateLegalDocumentRequest.java
│   │   │   │   ├── CreateUserRequest.java
│   │   │   │   ├── CustodyEventResponse.java
│   │   │   │   ├── CustodyTransferRequest.java
│   │   │   │   ├── DocumentVersionResponse.java
│   │   │   │   ├── EvidenceDetailResponse.java
│   │   │   │   ├── ExternalAiAnalysisResult.java
│   │   │   │   ├── IntegrityVerificationResponse.java
│   │   │   │   ├── IssueCertificateRequest.java
│   │   │   │   ├── LedgerVerificationResult.java
│   │   │   │   ├── LegalDocumentResponse.java
│   │   │   │   ├── LoginRequest.java
│   │   │   │   ├── RegisterRequest.java
│   │   │   │   ├── TriggerAnalysisRequest.java
│   │   │   │   ├── UpdateCaseRequest.java
│   │   │   │   ├── UpdateCaseStatusRequest.java
│   │   │   │   ├── UploadEvidenceRequest.java
│   │   │   │   └── UserResponse.java
│   │   │   ├── exception/                     # Exception Handling & RFC 7807 Problems
│   │   │   │   ├── DuplicateResourceException.java
│   │   │   │   ├── GlobalExceptionHandler.java
│   │   │   │   ├── InvalidOperationException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   ├── model/                         # JPA Entities & Enumerations
│   │   │   │   ├── AiAnalysis.java            # AI OCR, PII, and pHash analysis model
│   │   │   │   ├── AnalysisStatus.java        # PENDING, PROCESSING, COMPLETED, FAILED
│   │   │   │   ├── AuditLedger.java           # Cryptographic hash chain table
│   │   │   │   ├── BaseEntity.java            # UUID id, createdAt, updatedAt
│   │   │   │   ├── Case.java
│   │   │   │   ├── CaseStatus.java
│   │   │   │   ├── Certificate.java           # BSA 2023 §63 certificate metadata
│   │   │   │   ├── ClassificationLevel.java   # UNCLASSIFIED to TOP_SECRET
│   │   │   │   ├── CustodyEvent.java          # Chain of custody timeline log
│   │   │   │   ├── CustodyEventType.java
│   │   │   │   ├── CustodyState.java
│   │   │   │   ├── DocumentVersion.java       # Revision history table
│   │   │   │   ├── EvidenceFile.java          # Physical storage & hash table
│   │   │   │   ├── EvidenceItem.java
│   │   │   │   ├── IntegrityStatus.java
│   │   │   │   ├── LegalDocument.java
│   │   │   │   ├── LegalDocumentType.java
│   │   │   │   ├── RoleEnum.java
│   │   │   │   └── User.java
│   │   │   ├── repository/                    # Spring Data JPA Repositories
│   │   │   │   ├── AiAnalysisRepository.java
│   │   │   │   ├── AuditLedgerRepository.java
│   │   │   │   ├── CaseRepository.java
│   │   │   │   ├── CertificateRepository.java
│   │   │   │   ├── CustodyEventRepository.java
│   │   │   │   ├── DocumentVersionRepository.java
│   │   │   │   ├── EvidenceFileRepository.java
│   │   │   │   ├── EvidenceItemRepository.java
│   │   │   │   ├── LegalDocumentRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── security/                      # Spring Security Subsystem
│   │   │   │   ├── CaseSecurityService.java   # ABAC case-level access checks
│   │   │   │   ├── CustomUserDetailsService.java
│   │   │   │   ├── JwtAccessDeniedHandler.java # 403 RFC 7807 handler
│   │   │   │   ├── JwtAuthenticationEntryPoint.java # 401 RFC 7807 handler
│   │   │   │   ├── JwtAuthenticationFilter.java # Stateless Bearer filter
│   │   │   │   ├── JwtTokenProvider.java      # HMAC-SHA256 token manager
│   │   │   │   └── SecurityConfig.java        # Security filter chain config
│   │   │   └── service/                       # Business Logic Layer
│   │   │       ├── AiAnalysisService.java     # AI trigger and result management
│   │   │       ├── AiGatewayClient.java       # REST client & mock fallback for ML gateway
│   │   │       ├── AuditLedgerService.java
│   │   │       ├── AuthService.java
│   │   │       ├── CaseService.java
│   │   │       ├── CertificateService.java
│   │   │       ├── CustodyService.java
│   │   │       ├── EvidenceService.java
│   │   │       ├── LegalDocumentService.java
│   │   │       ├── crypto/
│   │   │       │   ├── EnvelopeEncryptionEngine.java # AES-256-GCM payload encryption
│   │   │       │   └── VaultKmsClientService.java    # Vault Transit DEK wrapping & KEK fallback
│   │   │       ├── pdf/
│   │   │       │   └── PdfCertificateGenerator.java # Statutory PDF engine
│   │   │       ├── search/
│   │   │       │   └── OpenSearchClientService.java  # OpenSearch hybrid & JPA fallback search
│   │   │       └── storage/
│   │   │           ├── LocalStorageService.java     # Local filesystem driver
│   │   │           ├── S3WormStorageService.java    # MinIO / S3 WORM Object Lock storage
│   │   │           └── StorageService.java          # Pluggable storage SPI
│   │   └── resources/
│   │       └── application.properties         # Main config (PostgreSQL, Storage, KMS, OpenSearch, JWT)
│   └── test/
│       ├── java/com/shrihari/evault/
│       │   ├── EvaultApplicationTests.java
│       │   ├── common/
│       │   │   └── CryptoUtilTest.java
│       │   ├── controller/
│       │   │   ├── AiAnalysisControllerIntegrationTest.java
│       │   │   ├── AuthControllerIntegrationTest.java
│       │   │   ├── CaseControllerIntegrationTest.java
│       │   │   ├── CertificateControllerIntegrationTest.java
│       │   │   ├── EvidenceControllerIntegrationTest.java
│       │   │   ├── LedgerAndCustodyIntegrationTest.java
│       │   │   ├── LegalDocumentControllerIntegrationTest.java
│       │   │   ├── SearchControllerIntegrationTest.java
│       │   │   └── SecurityAccessControlIntegrationTest.java
│       │   └── service/
│       │       ├── AiAnalysisServiceTest.java
│       │       ├── AuditLedgerServiceTest.java
│       │       ├── CaseServiceTest.java
│       │       ├── CertificateServiceTest.java
│       │       ├── CustodyServiceTest.java
│       │       ├── EnvelopeEncryptionEngineTest.java
│       │       ├── EvidenceServiceTest.java
│       │       ├── LegalDocumentServiceTest.java
│       │       ├── PdfCertificateGeneratorTest.java
│       │       ├── S3WormStorageServiceTest.java
│       │       └── UserServiceTest.java
│       └── resources/
│           └── application-test.properties    # H2 In-Memory DB test configuration
```

---

## 7. Automated Test Suite

The system includes a 100% automated test suite with **67 passing unit and integration tests**:

```text
[INFO] Results:
[INFO] Tests run: 67, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

### Test Breakdown

| Category | Test Class | Methods Tested | Focus |
|---|---|:---:|---|
| **RFC 3161 TSA** | `Rfc3161TimestampServiceTest` | 6 | Standard TSP token minting, local/remote generation, tamper detection on hash change, corrupted token handling |
| **RFC 3161 TSA** | `TimestampControllerIntegrationTest` | 3 | REST token generation, validation endpoint, end-to-end evidence intake & §63 certificate timestamp verification |
| **Real-Time WebSockets** | `WebSocketSecurityTest` | 7 | STOMP CONNECT authentication, RBAC on `/topic/security/alarms`, case-level subscription authorization |
| **Real-Time WebSockets** | `WebSocketAndTamperAlarmIntegrationTest` | 2 | Real bit-rot detection on physical disk, SQL database ledger row tampering detection triggering alarms |
| **Real-Time WebSockets** | `SecurityAlarmControllerIntegrationTest` | 2 | REST alarms listing, badge count aggregation, auditor acknowledgment, test drill trigger |
| **Real-Time WebSockets** | `AlarmDispatcherServiceTest` | 5 | Multi-topic dispatching, `Propagation.REQUIRES_NEW` transaction isolation, alarm acknowledgment |
| **Storage & KMS** | `S3WormStorageServiceTest` | 1 | S3/MinIO Object Lock WORM retention rules, byte retrieval, and local storage fallback |
| **Storage & KMS** | `EnvelopeEncryptionEngineTest` | 2 | AES-256-GCM payload encryption/decryption roundtrip, Vault DEK wrapping & local master KEK fallback |
| **Search Subsystem** | `SearchControllerIntegrationTest` | 1 | `GET /api/v1/search` REST integration with query parameters and authentication |
| **AI/ML Gateway** | `AiAnalysisServiceTest` | 1 | AI trigger execution, mock client fallback, status state transition to `COMPLETED` |
| **AI/ML Gateway** | `AiAnalysisControllerIntegrationTest` | 1 | `POST /analyze` dispatch and `GET /analysis` retrieval MockMvc integration |
| **Security & Auth** | `AuthControllerIntegrationTest` | 2 | Registration, Login, Token validation, Bad credentials (401), Tampered JWT rejection |
| **Security & Auth** | `SecurityAccessControlIntegrationTest` | 2 | RBAC enforcement (Analyst prevented from creating cases), ABAC enforcement (Unassigned officer rejected vs. Lead officer accepted) |
| **Statutory Cert** | `PdfCertificateGeneratorTest` | 1 | §63 BSA PDF generation, Schedule table layout, `%PDF-` header byte validation |
| **Statutory Cert** | `CertificateServiceTest` | 1 | Lifecycle issuance, state transition to `CERTIFIED`, and audit chaining |
| **Statutory Cert** | `CertificateControllerIntegrationTest` | 1 | Full REST issuance, metadata retrieval, and PDF download stream |
| **Legal Docs** | `LegalDocumentServiceTest` | 3 | Creation of v1, version incrementing to v2, duplicate handling |
| **Legal Docs** | `LegalDocumentControllerIntegrationTest` | 1 | End-to-end filing v1, version increment v2, history, and versioned file download |
| **Audit Ledger** | `AuditLedgerServiceTest` | 4 | SHA-256 hash chaining, clean traversal verification, row tamper detection, broken link detection |
| **Audit Ledger** | `CustodyServiceTest` | 2 | Event logging, HMAC signature generation, custody transfer tracking |
| **Audit Ledger** | `LedgerAndCustodyIntegrationTest` | 1 | Ingest -> Transfer -> Timeline -> Verify intact -> DB tamper injection -> Verify tamper detection |
| **Crypto & Storage**| `CryptoUtilTest` | 3 | SHA-256 test vectors, SHA-512 128-char length, HMAC determinism |
| **Crypto & Storage**| `EvidenceServiceTest` | 4 | Upload, server hashing, client hash mismatch detection, on-disk physical verification |
| **Crypto & Storage**| `EvidenceControllerIntegrationTest` | 1 | Multipart upload, retrieval, binary download, and physical integrity check |
| **Case Subsystem** | `CaseServiceTest` | 4 | Case creation, duplicate check, status state transitions |
| **Case Subsystem** | `CaseControllerIntegrationTest` | 2 | Complete case REST lifecycle (Create, Get, List, Patch Status) |
| **User Subsystem** | `UserServiceTest` | 3 | User registration with BCrypt hashing, duplicate handling, lookup |
| **Application** | `EvaultApplicationTests` | 1 | Spring context bootstrapper |

---

## 8. Developer Quickstart Guide

### Prerequisites
- **Java 21 LTS** installed (`java -version`).
- **Maven 3.9+** (or use the included `./mvnw` wrapper).

### 1. Build and Run All Automated Tests
Run the entire test suite against the in-memory H2 database (zero external setup needed):
```bash
./mvnw test
```
*(All 67 tests will run and pass).*

### 2. Run the Application Locally
To boot up the application:
```bash
./mvnw spring-boot:run
```
The server will start on port `8080`.

### 3. Access Swagger UI & OpenAPI Explorer
Once the backend is running, explore and test the interactive API docs:
- **Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI JSON Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

### 4. Configuration Properties Reference

| Property | Default Value | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port for REST API |
| `spring.datasource.url` | Cloud PostgreSQL URL | JDBC connection URL for primary database |
| `app.storage.local-dir` | `./data/uploads` | Root directory for raw encrypted evidence and legal document blobs |
| `app.storage.s3.endpoint` | `http://localhost:9000` | S3 / MinIO WORM Object Storage endpoint |
| `app.kms.vault.url` | `http://localhost:8200` | HashiCorp Vault Transit KMS server URL |
| `app.opensearch.url` | `http://localhost:9200` | OpenSearch hybrid search cluster endpoint |
| `evault.tsa.url` | `https://freetsa.org/tsr` | External RFC 3161 Timestamp Authority endpoint |
| `evault.tsa.mode` | `AUTO` | `REMOTE`, `LOCAL`, or `AUTO` (remote with graceful local fallback) |
| `evault.tsa.timeout-ms` | `5000` | RFC 3161 HTTP connection & read timeout in milliseconds |
| `app.ai.fastapi.url` | `http://localhost:8000` | External FastAPI AI/ML microservice endpoint |
| `evault.jwt.secret` | Configurable 256-bit secret | HMAC-SHA256 secret key for signing JWT tokens |
| `evault.jwt.expiration-ms` | `86400000` (24 Hours) | JWT token lifespan in milliseconds |
| `evault.security.allow-anonymous-fallback` | `false` | When false, strictly enforces JWT Bearer tokens |

---

## 9. Completion Summary & Project Status

All **11 architectural chunks** for the SIH26190 E-Vault backend are 100% completed, fully integrated, and verified with **67 passing unit and integration tests**:

1. ✅ **Chunk 1: Base Infrastructure & Case Subsystem**
2. ✅ **Chunk 2: Evidence Intake & Cryptographic Storage Subsystem**
3. ✅ **Chunk 3: Tamper-Evident Append-Only Audit Ledger & Custody**
4. ✅ **Chunk 4: Legal Document Subsystem & Versioning**
5. ✅ **Chunk 5: Statutory Evidence Certification (BSA 2023 §63 PDF Engine)**
6. ✅ **Chunk 6: Security, Auth & RBAC Subsystem**
7. ✅ **Chunk 7: AI/ML Gateway Integration Subsystem**
8. ✅ **Chunk 8: MinIO / S3 WORM Object Storage (COMPLIANCE Object Lock)**
9. ✅ **Chunk 9: HashiCorp Vault KMS Envelope Encryption (AES-256-GCM DEK)**
10. ✅ **Chunk 10: Real-Time WebSockets & Security Alarm Dispatcher (STOMP)**
11. ✅ **Chunk 11: RFC 3161 Trusted Timestamp Authority (TSA / BouncyCastle)**


