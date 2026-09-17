# SIH26190 E-Vault — Project Progress & In-Depth Technical Remaining Work

This document outlines the current completion status of the **Secure Digital Evidence and Legal Document Management System (E-Vault)** backend and AI/ML microservices, followed by an **in-depth technical specification of remaining tasks** required for production deployment (excluding frontend development).

---

## 1. Executive Summary & Current Build Status

The backend core and AI/ML microservices are functional, robust, and verified by automated unit and integration tests.

- **Automated Test Suite**: **38 / 38 Tests Passing** (`mvn test` build success).
- **Core Architecture**: Java 21 LTS, Spring Boot 4.1.1, Spring Security (JWT), PostgreSQL/H2, OpenPDF 2.0.3, Python 3.12, FastAPI, PaddleOCR v3.7+.
- **Statutory Compliance**: Full implementation of Bharatiya Sakshya Adhiniyam, 2023 (§63) statutory certificate generation and tamper-evident append-only audit ledgers.

---

## 2. Comprehensive Summary of Completed Subsystems

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                           COMPLETED SUBSYSTEM MATRIX                            │
├──────────────────────────┬──────────────────────────────────────────────────────┤
│ Subsystem                │ Implementation Details & Capabilities                │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **Auth & RBAC/ABAC**     │ • Stateless JWT authentication (HMAC-SHA256).        │
│                          │ • 7 Granular roles: `INVESTIGATING_OFFICER`,         │
│                          │   `FORENSIC_ANALYST`, `PROSECUTOR`, `COURT`,         │
│                          │   `AUDITOR`, `RECORDS_CUSTODIAN`, `SYSTEM_ADMIN`.    │
│                          │ • Attribute-based checks (`CaseSecurityService`).    │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **Case Management**      │ • Full FIR docket lifecycle state machine:           │
│                          │   `OPEN` → `UNDER_INVESTIGATION` →                   │
│                          │   `CHARGESHEET_FILED` → `IN_COURT` → `CLOSED`.        │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **Evidence Ingestion**   │ • Streaming dual **SHA-256** and **SHA-512** hashes.  │
│                          │ • In-transit client hash validation.                 │
│                          │ • Physical disk integrity re-verification            │
│                          │   (`POST /api/v1/evidence/{id}/verify-integrity`).   │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **Audit Ledger**         │ • Append-only, hash-chained ledger:                  │
│                          │   $\text{entryHash}_n = \text{SHA-256}(\text{seq}_n  │
│                          │   \parallel \dots \parallel \text{entryHash}_{n-1})$ │
│                          │ • Whole-ledger verification algorithm                │
│                          │   (`GET /api/v1/ledger/verify`).                      │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **Chain of Custody**     │ • Chronological timeline tracking lifecycle events: │
│                          │   `INGEST`, `TRANSFER`, `VIEW`, `ANALYZE`, etc.      │
│                          │ • Mandatory statutory transfer justifications.       │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **BSA 2023 §63 Certs**   │ • OpenPDF auto-generation of official court PDFs.    │
│                          │ • Formatted strictly per BSA 2023 §63 schedule       │
│                          │   (Part A hardware, Part B hash snapshots, declaration).│
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **Legal Documents**      │ • Immutable versioning ($v1, v2, \dots$) for FIRs,   │
│                          │   Seizure Memos, Charge Sheets, and Court Orders.    │
├──────────────────────────┼──────────────────────────────────────────────────────┤
│ **AI/ML Microservice**   │ • Python FastAPI service with **PaddleOCR v3.7+**.   │
│                          │ • Regex/Spacy NER for Aadhaar, PAN, phone, email.    │
│                          │ • Non-destructive annotation linking in Spring Boot. │
└──────────────────────────┴──────────────────────────────────────────────────────┘
```

---

## 3. In-Depth Technical Specification of Remaining Work

To evolve the current prototype into a production-ready enterprise solution, the following non-frontend architectural modules must be designed, implemented, and hardened.

```
                           REMAINING TECHNICAL ROADMAP
┌─────────────────────────┐  ┌─────────────────────────┐  ┌─────────────────────────┐
│ 3.1 Storage & KMS       │  │ 3.2 Advanced ABAC       │  │ 3.3 Advanced AI/ML      │
│     Subsystem           │  │     Policy Engine       │  │     Capabilities        │
│ • MinIO/S3 WORM Lock    │  │ • OPA Integration       │  │ • ELA Image Tamper      │
│ • Vault DEK/KEK KMS     │  │ • Rego Rules Engine     │  │ • pHash Deduplication   │
│ • RFC 3161 Timestamping │  │ • Context PDP Evaluation│  │ • Auto Doc Classifier   │
└────────────┬────────────┘  └────────────┬────────────┘  └────────────┬────────────┘
             │                            │                            │
             └────────────────────────────┼────────────────────────────┘
                                          │
┌─────────────────────────┐  ┌────────────┴────────────┐  ┌─────────────────────────┐
│ 3.4 Search Subsystem    │  │ 3.5 Webhooks & Alarms   │  │ 3.6 Production DevOps   │
│ • OpenSearch BM25       │  │ • Real-time WebSockets  │  │ • mTLS Security         │
│ • Vector k-NN Search    │  │ • Tamper Alarm Dispatch │  │ • Multi-Container Stack │
└─────────────────────────┘  └─────────────────────────┘  └─────────────────────────┘
```

---

### Section 3.1: Cryptographic Storage & KMS Subsystem

#### 1. Hardware WORM (Write-Once-Read-Many) Storage Integration
- **Goal**: Ensure that once evidence is uploaded, storage hardware physically rejects modifications or deletions even if root/admin privileges are compromised.
- **Implementation Plan**:
  - Replace `LocalStorageService` with an S3-compliant object store client (`MinioClient` / `S3Client`).
  - Configure MinIO / S3 **Object Lock** in `COMPLIANCE` retention mode with retention windows governed by Indian legal statutes (e.g., 7–30 years).
  - Enforce Legal Holds on active dockets so objects cannot be purged prior to court verdict.

#### 2. Envelope Encryption with Key Management Service (Vault / KMS)
- **Goal**: Prevent raw evidence binaries from residing in plaintext on disk storage.
- **Implementation Plan**:
  - Implement **Envelope Encryption**:
    1. Generate a unique, symmetric **Data Encryption Key (DEK)** (`AES-256-GCM`) per evidence blob.
    2. Encrypt the file stream with DEK on the fly.
    3. Send DEK to **HashiCorp Vault Transit Engine** (or AWS KMS) to wrap it with a **Key Encryption Key (KEK)**.
    4. Store wrapped DEK alongside `EvidenceFile` metadata.
  - Plaintext keys must exist strictly in ephemeral JVM memory during processing and be purged immediately.

#### 3. RFC 3161 Trusted Timestamping Server (TSA) Integration
- **Goal**: Provide independent 3rd-party proof of existence and time for ingestion hashes and BSA §63 certificates.
- **Implementation Plan**:
  - Integrate an RFC 3161 compliant Timestamp Authority (TSA) client using BouncyCastle (`org.bouncycastle.tsp`).
  - Request signed Time-Stamp Tokens (TST) for SHA-256 hashes during ingestion and certificate issuance.

---

### Section 3.2: Advanced Access Control & Policy Engine (OPA / ABAC)

#### 1. Migration to Open Policy Agent (OPA)
- **Goal**: Upgrade hardcoded `CaseSecurityService` logic to a centralized, hot-swappable Policy Decision Point (PDP).
- **Implementation Plan**:
  - Embed or query **Open Policy Agent (OPA)** via REST / WebAssembly.
  - Define policy logic in **Rego**:
    ```rego
    package evault.authz

    default allow = false

    # Rule: Investigating Officer can access case evidence if assigned and within active status
    allow {
        input.subject.role == "INVESTIGATING_OFFICER"
        input.subject.id == input.resource.caseLeadId
        input.action in ["EVIDENCE_VIEW", "EVIDENCE_UPLOAD"]
        input.resource.caseStatus in ["OPEN", "UNDER_INVESTIGATION"]
        input.environment.subnet == "POLICE_INTRANET"
    }

    # Rule: Auditor can view ledger and verify chain from any authorized network
    allow {
        input.subject.role == "AUDITOR"
        input.action in ["LEDGER_VIEW", "LEDGER_VERIFY"]
    }
    ```
  - Create `OpaPolicyEnforcerInterceptor` to evaluate requests dynamically against user attributes, resource classification, action, and network environment context.

---

### Section 3.3: Advanced AI/ML Pipeline Capabilities

#### 1. Image Tamper & Manipulation Detection Heuristics (ELA & EXIF)
- **Goal**: Provide forensic analysts with automated indicators of digital image tampering or splicing.
- **Implementation Plan**:
  - **Error Level Analysis (ELA)**: Implement in Python (`OpenCV`/`PIL`) to resave images at a known compression level (e.g., 95%) and compute error scale maps. High contrast differences highlight spliced or edited regions.
  - **EXIF Metadata Anomaly Detector**: Extract EXIF headers to detect editing software signatures (e.g., Photoshop, GIMP) or inconsistencies between creation timestamps and file modification dates.

#### 2. Perceptual Hashing & Cross-Case Deduplication
- **Goal**: Detect identical or visually modified media across multiple criminal dockets.
- **Implementation Plan**:
  - Compute **perceptual hash (`pHash`)** for images and video keyframes using `imagehash` in Python.
  - Store 64-bit binary pHash in PostgreSQL using `bit(64)` type.
  - Calculate Hamming distance queries to surface duplicate media files across historical cases.
  - Implement **MinHash / SimHash** for text document deduplication.

#### 3. Automatic Legal Document Classifier
- **Goal**: Automatically categorize uploaded document scans into standard legal types.
- **Implementation Plan**:
  - Train a multi-class text/layout classifier (using Scikit-Learn TF-IDF or lightweight Transformer) to predict document categories: `FIR`, `SEIZURE_MEMO`, `BANK_STATEMENT`, `FORENSIC_REPORT`, `CHARGE_SHEET`.

#### 4. Fine-Tuned Indian Legal NER Engine
- **Goal**: Expand entity extraction beyond regex to cover complex Indian legal phrasing.
- **Implementation Plan**:
  - Fine-tune a spaCy / HuggingFace `LayoutLMv3` model on Indian legal text corpora.
  - Add entity recognition for: **IPC/IT Act Sections**, **FIR Numbers**, **Vehicle Registration Numbers (e.g., DL-01-AB-1234)**, **Bank Account Numbers / IFSC Codes**, and **Court Names**.

---

### Section 3.4: Data Search & Vector Indexing Subsystem

#### 1. OpenSearch High-Speed Indexing Engine
- **Goal**: Enable instant full-text search across millions of evidence metadata records, case diaries, and OCR extracted texts.
- **Implementation Plan**:
  - Deploy an **OpenSearch** cluster.
  - Implement Spring Data OpenSearch repositories or custom REST client sync.
  - Build ingestion listeners to automatically index:
    - Case metadata & FIR text.
    - Legal document version content.
    - AI OCR extracted text blocks & NER entities.
  - Implement hybrid search combining **BM25 lexical matching** and **Dense Vector k-NN similarity search** (using `sentence-transformers` embeddings).

---

### Section 3.5: Event Streaming, Notifications & Real-Time Alarms

#### 1. Spring Boot WebSocket & Webhook Dispatcher
- **Goal**: Alert officers, analysts, and auditors immediately when critical events occur.
- **Implementation Plan**:
  - Add `spring-boot-starter-websocket` / STOMP broker.
  - Create real-time notification topics:
    - `/topic/case/{caseId}` — Chain of custody transfers, status changes.
    - `/topic/security/alarms` — **Critical Security Alarms** (triggered if whole-ledger verification detects broken hashes or disk bit-rot).
  - Implement outward Webhook dispatching to external e-Courts and Police Headquarter APIs.

---

### Section 3.6: Production DevOps, mTLS & Security Hardening

#### 1. Mutual TLS (mTLS) Inter-Service Security
- **Goal**: Ensure that communications between the Spring Boot backend core and the Python AI microservice cannot be intercepted or spoofed.
- **Implementation Plan**:
  - Generate internal Certificate Authority (CA) and X.509 client/server certificates.
  - Configure Spring Boot `RestTemplate` / `WebClient` with SSL context requiring client certificate verification.
  - Configure FastAPI (Uvicorn) with `--ssl-keyfile`, `--ssl-certfile`, and `--ssl-ca-certs`.

#### 2. Multi-Container Orchestration (`docker-compose.yml`)
- **Goal**: Enable a single-command local and production deployment stack.
- **Implementation Plan**:
  - Orchestrate all components in a unified Compose file:
    1. `postgres:16-alpine` (Primary Relational Store)
    2. `minio/minio` (WORM Object Store)
    3. `hashicorp/vault` (KMS Key Management)
    4. `opensearchproject/opensearch` (Search Engine)
    5. `evault-backend` (Spring Boot Java 21)
    6. `evault-aiml` (Python 3.12 FastAPI)
  - Configure health check dependencies (`depends_on` with `service_healthy`).

#### 3. Automated CI/CD & Security Audit Pipeline
- **Goal**: Enforce code security and dependency hygiene on every git push.
- **Implementation Plan**:
  - Set up GitHub Actions workflow:
    - **Build & Test**: Run `./mvnw test` and Python `pytest`.
    - **SAST Scan**: CodeQL analysis for Java and Python security vulnerabilities.
    - **Dependency Audit**: OWASP Dependency-Check plugin to flag CVEs in Maven/Pip packages.
    - **Container Scanning**: Trivy container vulnerability scanner.

---

## 4. Priority Implementation Checklist

| Task # | Module | Scope / Description | Target Tech Stack | Priority |
|---|---|---|---|---|
| **T-01** | **Storage** | MinIO S3 WORM Object Lock Integration | MinIO SDK / Java AWS SDK v2 | **P0 (Critical)** |
| **T-02** | **Security** | Envelope Encryption with Vault Transit Engine | HashiCorp Vault / Spring Vault | **P0 (Critical)** |
| **T-03** | **Access Control**| Open Policy Agent (OPA) ABAC Engine Integration | OPA / Rego Policies | **P1 (High)** |
| **T-04** | **AI/ML** | Image Tamper Detection (ELA & EXIF Anomalies) | OpenCV / PIL / Python | **P1 (High)** |
| **T-05** | **AI/ML** | Media `pHash` Deduplication & Hamming Search | imagehash / PostgreSQL bit(64) | **P1 (High)** |
| **T-06** | **Search** | OpenSearch Full-Text & Vector Search Pipeline | OpenSearch / Java Client | **P2 (Medium)** |
| **T-07** | **Events** | WebSockets & Emergency Tamper Alarm Dispatcher | Spring WebSocket / STOMP | **P2 (Medium)** |
| **T-08** | **DevOps** | Inter-Service mTLS & Complete Docker Compose Stack | Docker Compose / TLS X.509 | **P1 (High)** |
