# SIH26190 E-Vault — Project Progress & Technical Remaining Work Roadmap

This document presents a **detailed technical and architectural status report** for the **Secure Digital Evidence and Legal Document Management System (E-Vault)**. It clearly differentiates completed capabilities from remaining tasks across the **Backend Subsystem (Java Spring Boot)**, the **AI/ML Microservice Subsystem (Python FastAPI)**, and **DevOps Infrastructure**, focusing on architectural design, operational mechanics, and functional requirements.

---

## 1. Executive Summary & System Overview

The E-Vault is designed for legal admissibility and chain-of-custody preservation under the **Bharatiya Sakshya Adhiniyam, 2023 (§63)** (replacing Evidence Act §65B). The system architecture comprises two decoupled server tiers:

1. **Backend Core Subsystem (Java 21 LTS / Spring Boot 4.1.1)**: Handles authentication, RBAC/ABAC access control, FIR case dockets, evidence intake, cryptographic hash sealing, S3 WORM storage, Vault KMS envelope encryption, search indexing, append-only audit ledgers, custody tracking, and statutory §63 PDF court certificates.
2. **AI/ML Microservice Subsystem (Python 3.12 / FastAPI / PaddleOCR)**: Handles document text extraction (OCR), PII entity recognition, bounding box detection, and forensic analysis.

- **Current Build Status**: **42 / 42 Automated Backend Tests Passing** (`mvn test` build success).

---

## 2. Completed Work Breakdown (Backend vs. AI/ML)

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                           COMPLETED SUBSYSTEM MATRIX                              │
├──────────────────────────┬────────────────────────────────────────────────────────┤
│ Subsystem                │ Implementation Details & Capabilities                  │
├──────────────────────────┼────────────────────────────────────────────────────────┤
│ **Backend (Spring Boot)**│ • Stateless JWT authentication (HMAC-SHA256).          │
│                          │ • 7 RBAC Roles: `INVESTIGATING_OFFICER`,               │
│                          │   `FORENSIC_ANALYST`, `PROSECUTOR`, `COURT`,           │
│                          │   `AUDITOR`, `RECORDS_CUSTODIAN`, `SYSTEM_ADMIN`.      │
│                          │ • FIR case docket state machine (`OPEN` → `CLOSED`).    │
│                          │ • Dual SHA-256 / SHA-512 streaming hash sealing.       │
│                          │ • Physical disk integrity check (`/verify-integrity`).  │
│                          │ • **S3 / MinIO WORM Object Storage (`S3WormStorage`)**. │
│                          │ • **Vault KMS Envelope Encryption (`VaultKmsClient`)**. │
│                          │ • **Hybrid Full-Text & Vector Search (`/api/v1/search`)**.│
│                          │ • Append-only, hash-chained ledger ($\text{entryHash}$).│
│                          │ • Whole-ledger audit verifier (`GET /ledger/verify`).  │
│                          │ • Chronological Chain of Custody tracking & transfers. │
│                          │ • OpenPDF BSA 2023 §63 court PDF generator.            │
│                          │ • Legal document versioning ($v1, v2, \dots$).         │
│                          │ • Resilient AI Gateway HTTP client with fallback.      │
├──────────────────────────┼────────────────────────────────────────────────────────┤
│ **AI/ML (FastAPI)**      │ • Asynchronous FastAPI Uvicorn engine.                 │
│                          │ • PaddleOCR v3.7+ text & bounding box extraction.      │
│                          │ • Regex + spaCy NER for Aadhaar, PAN, phone, email.    │
└──────────────────────────┴────────────────────────────────────────────────────────┘
```

### Detailed Breakdown of Completed Backend Components

#### 1. Authentication & Role-Based Access Control (`/api/v1/auth`)
- Stateless JWT authentication (`JwtTokenProvider`, `JwtAuthenticationFilter`).
- Password salting & BCrypt hashing (`BCryptPasswordEncoder`).
- 7 Granular roles enforced via `@PreAuthorize`: `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `PROSECUTOR`, `COURT`, `AUDITOR`, `RECORDS_CUSTODIAN`, `SYSTEM_ADMIN`.
- Thread-safe `ActorContextHolder` capturing actor identity per request.

#### 2. Case Docket Lifecycle Management (`/api/v1/cases`)
- Case creation, lookup by FIR/case number, assigned officer management, and status updates.
- Lifecycle state machine: `OPEN` $\rightarrow$ `UNDER_INVESTIGATION` $\rightarrow$ `CHARGESHEET_FILED` $\rightarrow$ `IN_COURT` $\rightarrow$ `CLOSED` $\rightarrow$ `ARCHIVED`.
- ABAC case-ownership checks (`CaseSecurityService`).

#### 3. Evidence Ingestion & Dual Cryptographic Sealing (`/api/v1/evidence`)
- Streaming dual **SHA-256** and **SHA-512** hash computation on raw incoming binaries.
- In-transit client-hash validation (`clientSha256`).
- Physical disk integrity re-verification (`POST /api/v1/evidence/{id}/verify-integrity`).

#### 4. S3 / MinIO WORM Object Storage (`S3WormStorageService`) — *COMPLETED*
- Pluggable storage architecture supporting S3 WORM Object Lock in `COMPLIANCE` retention mode (e.g. 365 days retention).
- Hardware-enforced object immutability preventing deletion/tampering during retention window.
- Local disk storage fallback for zero-dependency execution.

#### 5. Vault KMS & Envelope Encryption (`VaultKmsClientService` & `EnvelopeEncryptionEngine`) — *COMPLETED*
- Ephemeral 256-bit AES Data Encryption Keys (DEKs) generated per evidence payload.
- AES-256-GCM payload encryption with random IVs.
- Vault Transit Engine integration wrapping DEKs with Master KEK (`evault-master-kek`) plus deterministic local KEK fallback.

#### 6. Append-Only Tamper-Evident Audit Ledger (`/api/v1/ledger`)
- Hash-chained transaction logging:
  $$\text{entryHash}_n = \text{SHA-256}(\text{seq}_n \parallel \text{occurredAt}_n \parallel \text{actorId}_n \parallel \text{action}_n \parallel \text{resourceRef}_n \parallel \text{details}_n \parallel \text{entryHash}_{n-1})$$
- Whole-ledger cryptographic audit verification (`GET /api/v1/ledger/verify`).
- Database immutability enforced via `@Column(updatable = false)`.

#### 7. Chain of Custody System (`/api/v1/evidence/{id}/custody`)
- Lifecycle custody tracking for `INGEST`, `TRANSFER`, `VIEW`, `DOWNLOAD`, `ANALYZE`, `CERTIFY`, `DISPOSE`.
- Statutory transfer endpoint requiring target custodian ID and mandatory legal justification.

#### 8. Statutory BSA 2023 §63 Court Certificate Generator (`/api/v1/certificates`)
- OpenPDF auto-generation of official court-admissible PDF certificates formatted per BSA 2023 §63 schedule (Part A hardware particulars, Part B cryptographic hash snapshots, custody table, legal declaration).

#### 9. Legal Document Management & Versioning (`/api/v1/documents`)
- Multi-version tracking ($v1, v2, \dots$) with revision-level SHA-256 checksums for FIRs, Search Warrants, Charge Sheets, Court Orders.

#### 10. Hybrid Full-Text & Vector Search Subsystem (`/api/v1/search`) — *COMPLETED*
- `SearchController` and `OpenSearchClientService`.
- Multi-field search across cases, sealed evidence metadata, OCR text annotations, and legal documents.

#### 11. AI Microservice Gateway (`/api/v1/evidence/{id}/analyze`)
- Spring Boot `AiGatewayClient` & `AiAnalysisService` connecting backend to Python FastAPI service.
- Python FastAPI microservice (`AIML/`) running PaddleOCR v3.7+ and PII NER engine.

---

## 3. In-Depth Remaining Work Roadmap (Conceptual & Architectural)

---

### PART A: BACKEND SUBSYSTEM REMAINING WORK (Java Spring Boot)

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                        BACKEND SUBSYSTEM MODULE ROADMAP                           │
├───────────────┬──────────────────────────────────┬────────────────────────────────┤
│ Module ID     │ Feature Description              │ Primary Architectural Purpose  │
├───────────────┼──────────────────────────────────┼────────────────────────────────┤
│ **BE-01**     │ Open Policy Agent (OPA) ABAC PDP │ Dynamic policy authorization   │
│ **BE-02**     │ WebSocket Real-Time Tamper Alarms│ Instant security broadcasting  │
│ **BE-03**     │ RFC 3161 Timestamp Authority     │ 3rd-party cryptographic time   │
└───────────────┴──────────────────────────────────┴────────────────────────────────┘
```

#### Task BE-01: Open Policy Agent (OPA) Dynamic ABAC Policy Engine Integration
- **Operational Need**: Hardcoded security checks cannot accommodate dynamic organizational rules, clearance levels, shift hours, or location constraints across different police stations and courts.
- **Architectural Design**:
  - Integrate Open Policy Agent (OPA) as a centralized Policy Decision Point (PDP).
  - Define declarative **Rego policies** evaluating fine-grained context attributes (`User Role`, `Clearance`, `Case Assignment`, `Client IP Subnet`, `Jurisdiction`).
- **How It Works**: Before executing any request, Spring Security passes context attributes to OPA, which evaluates rules and returns a binary `allow / deny` decision.

#### Task BE-02: Real-Time WebSockets & Security Alarm Dispatcher
- **Operational Need**: Immediate operational alerts are required when custody handoffs occur or when the audit ledger detects a security compromise.
- **Architectural Design**:
  - Configure STOMP WebSockets broker in Spring Boot (`WebSocketConfig.java`).
  - Expose real-time topic channels (`/topic/case/{id}` and `/topic/security/alarms`).
- **How It Works**: If a whole-ledger verification scan detects a broken sequence hash or file bit-rot, an automated alert payload is instantly pushed to active auditor and administrator dashboards.

#### Task BE-03: RFC 3161 Trusted Timestamping Server (TSA) Integration
- **Operational Need**: Provide independent 3rd-party proof of existence and timestamp for ingestion hashes and BSA §63 certificates.
- **Architectural Design**:
  - Integrate an RFC 3161 compliant Timestamp Authority (TSA) client using BouncyCastle (`org.bouncycastle.tsp`).
  - Request signed Time-Stamp Tokens (TST) for SHA-256 hashes during ingestion and certificate issuance.

---

### PART B: AI/ML SUBSYSTEM REMAINING WORK (Python FastAPI)

```
┌───────────────────────────────────────────────────────────────────────────────────┐
│                        AI/ML SUBSYSTEM MODULE ROADMAP                             │
├───────────────┬──────────────────────────────────┬────────────────────────────────┤
│ Module ID     │ Feature Description              │ Primary Architectural Purpose  │
├───────────────┼──────────────────────────────────┼────────────────────────────────┤
│ **ML-01**     │ Image Tamper (ELA & EXIF Anomaly)│ Digital image manipulation check│
│ **ML-02**     │ Perceptual Hashing (pHash) Media │ Visual media deduplication     │
│ **ML-03**     │ Multi-Class Legal Doc Classifier │ Auto document category tagging │
│ **ML-04**     │ Indian Legal Entity NER Pipeline │ Legal PII & section extraction │
└───────────────┴──────────────────────────────────┴────────────────────────────────┘
```

#### Task ML-01: Image Tampering & Manipulation Detection Engine (ELA & EXIF)
- **Operational Need**: Digital images (CCTV snapshots, crime scene photos, scanned documents) submitted as evidence may be doctored or spliced using photo editing software.
- **Architectural Design**:
  - Implement **Error Level Analysis (ELA)** to measure compression variances across image regions.
  - Implement **EXIF Metadata Anomaly Detection** to identify editing software signatures (Photoshop, GIMP) and creation date mismatches.
- **How It Works**: ELA resaves the image at a known compression quality level and analyzes the difference matrix. Spliced or edited regions show significantly higher error levels, providing forensic analysts with visual heatmaps and quantitative tamper probability scores.

#### Task ML-02: Perceptual Hashing (`pHash`) & Cross-Case Media Deduplication
- **Operational Need**: Suspects often distribute identical or slightly altered image/video files across multiple crimes. Cryptographic SHA-256 changes completely if a single pixel changes, whereas perceptual hashing identifies visual similarity.
- **Architectural Design**:
  - Implement Discrete Cosine Transform (DCT) **`pHash`** for visual media.
  - Implement **MinHash / SimHash** for document text similarity.
- **How It Works**: Calculates a 64-bit visual fingerprint for media files. Performs bitwise Hamming distance calculations to instantly highlight duplicate or visually modified evidence across historical cases.

#### Task ML-03: Multi-Class Legal Document Auto-Classifier
- **Operational Need**: Manual document classification during evidence intake is slow and prone to human tagging errors.
- **Architectural Design**:
  - Train a multi-class document classification model (TF-IDF + Linear Classifier / PyTorch).
  - Classify uploads into standard categories: `FIR`, `SEIZURE_MEMO`, `BANK_STATEMENT`, `CHARGE_SHEET`, `FORENSIC_REPORT`.
- **How It Works**: Text extracted via OCR is analyzed by the classifier, returning predicted document category tags with statistical confidence scores.

#### Task ML-04: Fine-Tuned Indian Legal NER Engine
- **Operational Need**: Standard NER engines fail on Indian legal terminology, FIR naming conventions, IPC sections, and vehicle registration numbers.
- **Architectural Design**:
  - Fine-tune spaCy / HuggingFace LayoutLM model on Indian legal text corpora.
  - Extract entities for **IPC/IT Act Sections**, **FIR Numbers**, **Vehicle Registration Numbers**, **Bank IFSC Codes**, and **Court Names**.
- **How It Works**: Specialized pattern matchers and contextual NLP models identify legal entities and map them to spatial image bounding boxes for auto-redaction suggestions.

---

### PART C: DEVOPS, SECURITY & CROSS-SERVICE INTEGRATION

#### Task OPS-01: Inter-Service Mutual TLS (mTLS) Security
- **Operational Need**: Prevents eavesdropping, tampering, or unauthorized microservice calls between the Spring Boot backend and Python FastAPI.
- **Architectural Design**: Generate private X.509 CA certificates and configure bidirectional client/server TLS verification for all REST requests between services.

#### Task OPS-02: Unified Multi-Container Production Stack (`docker-compose.yml`)
- **Operational Need**: Simplifies single-command deployment across PostgreSQL, MinIO, Vault, OpenSearch, Spring Boot Backend, and Python AIML microservice.
- **Architectural Design**: Define service dependencies, environment variables, healthchecks, and persistent volumes in a production-ready Compose specification.

#### Task OPS-03: CI/CD & Automated Security Scanning
- **Operational Need**: Guarantees code security, unit test coverage, and dependency compliance on every code commit.
- **Architectural Design**: Set up GitHub Actions workflow executing JUnit/Pytest automated tests, CodeQL SAST static analysis, OWASP Dependency Check, and Trivy container vulnerability scanning.

---

## 4. Architectural Summary & Task Matrix

| Module ID | Subsystem | Module Scope | Status | Priority |
|---|---|---|---|---|
| **BE-COMP-01** | **Backend** | S3 / MinIO WORM Storage Service | **COMPLETED** | **P0** |
| **BE-COMP-02** | **Backend** | Vault KMS Envelope Encryption Engine | **COMPLETED** | **P0** |
| **BE-COMP-03** | **Backend** | Hybrid Search Engine Subsystem (`/search`) | **COMPLETED** | **P1** |
| **BE-01** | **Backend** | Open Policy Agent (OPA) Dynamic ABAC PDP | Pending | **P1 (High)** |
| **BE-02** | **Backend** | Real-Time WebSockets & Security Alarms | Pending | **P2 (Medium)** |
| **BE-03** | **Backend** | RFC 3161 Trusted Timestamp Authority (TSA) | Pending | **P2 (Medium)** |
| **ML-01** | **AI/ML** | Image Tampering Detection (ELA & EXIF) | Pending | **P1 (High)** |
| **ML-02** | **AI/ML** | Perceptual Hashing (`pHash`) Deduplication | Pending | **P1 (High)** |
| **ML-03** | **AI/ML** | Multi-Class Legal Document Classifier | Pending | **P2 (Medium)** |
| **ML-04** | **AI/ML** | Fine-Tuned Indian Legal NER Engine | Pending | **P1 (High)** |
| **OPS-01**| **DevOps**  | Inter-Service mTLS & Docker Compose Stack | Pending | **P1 (High)** |
