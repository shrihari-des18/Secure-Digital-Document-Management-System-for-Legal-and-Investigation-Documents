# SIH26190 E-Vault — Project Progress & In-Depth Technical Remaining Work

This document outlines the current completion status of the **Secure Digital Evidence and Legal Document Management System (E-Vault)**, clearly differentiating between the **Backend Subsystem (Java Spring Boot)** and the **AI/ML Microservice Subsystem (Python FastAPI)**. It provides an **in-depth technical specification of remaining tasks** required for production readiness (excluding frontend development).

---

## 1. Executive Summary & Build Status

The project architecture consists of two decoupled server subsystems:
1. **Backend Subsystem (Java 21 LTS / Spring Boot 4.1.1)**: Core business logic, authentication, access control, case management, cryptographic storage, audit ledger, and statutory BSA 2023 §63 certificate generation.
2. **AI/ML Microservice Subsystem (Python 3.12 / FastAPI / PaddleOCR)**: Document text extraction, PII entity recognition, bounding box detection, and analysis processing.

- **Automated Test Suite**: **38 / 38 Backend Tests Passing** (`mvn test` build success).
- **Statutory Compliance**: Full implementation of Bharatiya Sakshya Adhiniyam, 2023 (§63) court certificate generation and hash-chained audit ledgers.

---

## 2. Differentiated Summary of Completed Work

### 2.1 Backend Subsystem (Java Spring Boot) — COMPLETED
- **Auth & Access Control**: Stateless JWT authentication filter (HMAC-SHA256), 7 granular RBAC roles (`INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `PROSECUTOR`, `COURT`, `AUDITOR`, `RECORDS_CUSTODIAN`, `SYSTEM_ADMIN`), and ABAC case-ownership checks (`CaseSecurityService`).
- **Case Docket Management**: Full FIR case lifecycle state machine (`OPEN` → `UNDER_INVESTIGATION` → `CHARGESHEET_FILED` → `IN_COURT` → `CLOSED` → `ARCHIVED`).
- **Evidence Ingestion & Cryptography**: Streaming dual **SHA-256** and **SHA-512** hash computation at ingestion, in-transit client-hash validation, HMAC digital signing, and physical disk integrity re-verification (`POST /api/v1/evidence/{id}/verify-integrity`).
- **Append-Only Tamper-Evident Audit Ledger**: Hash-chained transaction logging ($\text{entryHash}_n = \text{SHA-256}(\text{seq}_n \parallel \dots \parallel \text{entryHash}_{n-1})$) and whole-ledger cryptographic audit verification (`GET /api/v1/ledger/verify`).
- **Chain of Custody**: Chronological timeline tracking for lifecycle events (`INGEST`, `TRANSFER`, `VIEW`, `ANALYZE`, `CERTIFY`, `DISPOSE`) with mandatory statutory transfer justifications.
- **Statutory BSA 2023 §63 PDF Generator**: OpenPDF auto-generation of official court-admissible certificates formatted per the statutory Schedule (Part A hardware particulars, Part B cryptographic hash snapshots, custody table, legal declaration).
- **Legal Document Management**: Versioning ($v1, v2, \dots$) with revision-level SHA-256 checksums for FIRs, Search Warrants, Charge Sheets, and Court Orders.
- **AI Gateway Integration**: Spring Boot `AiGatewayClient` with resilient fallback logic connecting to the Python AI microservice, storing AI outputs non-destructively as linked annotations.

### 2.2 AI/ML Microservice Subsystem (Python FastAPI) — COMPLETED
- **FastAPI Microservice Engine**: Asynchronous HTTP API running Uvicorn server (`AIML/main.py`).
- **PaddleOCR v3.7+ Integration**: Multi-stage OCR pipeline converting uploaded scanned document images/PDFs into bounding boxes and extracted text lines.
- **Regex & spaCy NER Engine**: Pattern matching and Named Entity Recognition (NER) for Indian regulatory PII:
  - **Aadhaar Numbers** (12-digit masked patterns)
  - **PAN Card Identifiers** (10-character alphanumeric strings)
  - **Phone Numbers & Email Addresses**
  - **Bounding Box Coordinate Mapping**: Associating extracted PII spans with spatial image coordinates.
- **Processing Endpoint**: `POST /api/v1/process-document` returning structured `DocumentProcessResponse` payloads.

---

## 3. In-Depth Technical Specification of Remaining Work (Differentiated)

```
                            REMAINING TASK ROADMAP
┌──────────────────────────────────────────┐  ┌──────────────────────────────────────────┐
│      BACKEND SUBSYSTEM (JAVA/SPRING)     │  │      AI/ML SUBSYSTEM (PYTHON/FASTAPI)   │
├──────────────────────────────────────────┤  ├──────────────────────────────────────────┤
│ 1. MinIO/S3 WORM Object Lock Storage     │  │ 1. Image Tamper & ELA Heuristics Engine  │
│ 2. Vault Transit KMS Envelope Encryption │  │ 2. Perceptual Hashing (pHash) & MinHash  │
│ 3. RFC 3161 Trusted Timestamping (TSA)   │  │ 3. Auto Multi-Class Legal Doc Classifier │
│ 4. Open Policy Agent (OPA) ABAC Engine   │  │ 4. Fine-Tuned LayoutLMv3 Indian Legal NER│
│ 5. OpenSearch Full-Text & Vector Search  │  └────────────────────┬─────────────────────┘
│ 6. WebSocket Real-time Tamper Alarms     │                       │
└────────────────────┬─────────────────────┘                       │
                     │                                             │
                     └──────────────────────┬──────────────────────┘
                                            │
┌───────────────────────────────────────────┴──────────────────────────────────────────┐
│                 CROSS-SERVICE DEVOPS, SECURITY & HARDENING                           │
├──────────────────────────────────────────────────────────────────────────────────────┤
│ • Mutual TLS (mTLS) between Spring Boot & Python FastAPI                             │
│ • Unified Multi-Container Docker Compose Stack (Postgres, MinIO, Vault, OpenSearch)  │
│ • Automated CI/CD GitHub Actions with SAST (CodeQL) & Trivy Container Scanning       │
└──────────────────────────────────────────────────────────────────────────────────────┘
```

---

### PART A: BACKEND SUBSYSTEM REMAINING WORK (Java Spring Boot)

#### A.1 Hardware WORM (Write-Once-Read-Many) Storage Integration
- **Objective**: Ensure evidence binaries cannot be modified or deleted on physical disk even by system administrators.
- **Implementation Plan**:
  - Replace `LocalStorageService` with an S3-compliant `MinioClient` / `S3Client`.
  - Configure MinIO / S3 **Object Lock** in `COMPLIANCE` mode with statutory retention periods (7–30 years).
  - Apply Legal Holds on active cases to block object deletion until court disposal.

#### A.2 Key Vault KMS Envelope Encryption
- **Objective**: Encrypt all evidence binaries at rest using envelope encryption.
- **Implementation Plan**:
  - Implement per-file **Data Encryption Key (DEK)** generation (`AES-256-GCM`).
  - Integrate **HashiCorp Vault Transit Engine** (or AWS KMS) to wrap DEKs with a **Key Encryption Key (KEK)**.
  - Store wrapped DEKs in `evidence_file` database records; keep plaintext keys strictly in ephemeral memory.

#### A.3 RFC 3161 Trusted Timestamping (TSA) Server Integration
- **Objective**: Provide independent 3rd-party cryptographic proof of time for ingestion hashes and §63 certificates.
- **Implementation Plan**:
  - Integrate BouncyCastle TSA client (`org.bouncycastle.tsp`).
  - Request signed Time-Stamp Tokens (TST) from an external/internal RFC 3161 TSA server during evidence sealing.

#### A.4 Open Policy Agent (OPA) Dynamic ABAC Engine
- **Objective**: Upgrade hardcoded security checks to a dynamic Policy Decision Point (PDP).
- **Implementation Plan**:
  - Integrate OPA via REST API or embedded Wasm evaluator.
  - Write fine-grained **Rego policies** evaluating User Role, Clearance Level, Case Assignment, IP Subnet, and Jurisdiction.

#### A.5 OpenSearch Full-Text & Hybrid Vector Search
- **Objective**: Index case metadata, document revisions, and AI OCR outputs for instant cross-case searching.
- **Implementation Plan**:
  - Deploy OpenSearch cluster and Spring Data OpenSearch repositories.
  - Build indexing listeners for case notes, legal documents, and OCR text.
  - Implement hybrid search combining **BM25 text queries** and **Dense Vector k-NN similarity search**.

#### A.6 WebSocket Real-Time Event Dispatcher & Tamper Alarms
- **Objective**: Push immediate alerts for chain-of-custody transfers and tamper detections.
- **Implementation Plan**:
  - Add `spring-boot-starter-websocket` with STOMP broker.
  - Expose `/topic/security/alarms` to broadcast immediate warnings if whole-ledger verification (`GET /ledger/verify`) flags a compromised hash sequence.

---

### PART B: AI/ML SUBSYSTEM REMAINING WORK (Python FastAPI)

#### B.1 Image Tampering & Manipulation Detection Engine (ELA & EXIF)
- **Objective**: Provide forensic analysts with automated indicators of image splicing or doctoring.
- **Implementation Plan**:
  - **Error Level Analysis (ELA)**: Implement in Python (`OpenCV`/`PIL`) to compute JPEG error scale maps. High-contrast variances highlight edited/spliced image regions.
  - **EXIF Metadata Anomaly Detector**: Parse EXIF headers to flag editing software signatures (Photoshop, GIMP) or creation timestamp anomalies.

#### B.2 Perceptual Hashing & Cross-Case Deduplication
- **Objective**: Identify identical or modified evidence media across different criminal cases.
- **Implementation Plan**:
  - Implement **`pHash` (perceptual hashing)** for images and video keyframes using Python `imagehash`.
  - Calculate Hamming distance between binary pHash values stored in database.
  - Implement **MinHash / SimHash** for document text deduplication.

#### B.3 Automatic Multi-Class Legal Document Classifier
- **Objective**: Automatically categorize uploaded document scans into standard legal types.
- **Implementation Plan**:
  - Train a lightweight classifier (Scikit-Learn TF-IDF or PyTorch) to auto-tag uploads: `FIR`, `SEIZURE_MEMO`, `BANK_STATEMENT`, `FORENSIC_REPORT`, `CHARGE_SHEET`.

#### B.4 Fine-Tuned Indian Legal NER Engine
- **Objective**: Extend NER beyond regex to recognize complex Indian legal entity structures.
- **Implementation Plan**:
  - Fine-tune spaCy / HuggingFace `LayoutLMv3` model on Indian court documents.
  - Extract entities for: **IPC/IT Act Sections**, **FIR Numbers**, **Vehicle Registration Numbers (e.g., DL-01-AB-1234)**, **Bank Account / IFSC Codes**, and **Court Names**.

---

### PART C: DEVOPS, SECURITY & CROSS-SERVICE INTEGRATION

#### C.1 Inter-Service Mutual TLS (mTLS) Security
- **Objective**: Secure communication between Spring Boot backend and Python AI microservice against eavesdropping or spoofing.
- **Implementation Plan**:
  - Issue X.509 client/server certificates via internal CA.
  - Configure mTLS on Spring Boot `WebClient` and FastAPI Uvicorn server.

#### C.2 Multi-Container Docker Compose Stack
- **Objective**: Provide single-command orchestration for the entire infrastructure.
- **Implementation Plan**:
  - Compose services: PostgreSQL 16, MinIO WORM, HashiCorp Vault, OpenSearch, Spring Boot Backend, Python AIML Microservice.

#### C.3 CI/CD & Automated Security Scanning
- **Objective**: Automated testing and vulnerability scanning on code commit.
- **Implementation Plan**:
  - GitHub Actions running JUnit/Pytest, CodeQL SAST scanning, OWASP Dependency Check, and Trivy container scanning.

---

## 4. Differentiated Implementation Task Matrix

| Task ID | Subsystem | Module / Feature Scope | Technology Stack | Priority |
|---|---|---|---|---|
| **BE-01** | **Backend** | MinIO / S3 WORM Object Lock Integration | MinIO SDK / Java AWS SDK v2 | **P0 (Critical)** |
| **BE-02** | **Backend** | Envelope Encryption via Vault Transit Engine | HashiCorp Vault / Spring Vault | **P0 (Critical)** |
| **BE-03** | **Backend** | Open Policy Agent (OPA) Dynamic ABAC Engine | OPA / Rego Policies | **P1 (High)** |
| **BE-04** | **Backend** | OpenSearch Hybrid Full-Text & Vector Search | OpenSearch / Java Client | **P2 (Medium)** |
| **BE-05** | **Backend** | Real-time WebSockets & Tamper Alarm Dispatcher | Spring WebSocket / STOMP | **P2 (Medium)** |
| **ML-01** | **AI/ML** | Image Tamper Heuristics (ELA & EXIF Anomalies) | OpenCV / PIL / Python | **P1 (High)** |
| **ML-02** | **AI/ML** | Perceptual Hashing (`pHash`) Media Deduplication | `imagehash` / Python | **P1 (High)** |
| **ML-03** | **AI/ML** | Multi-Class Legal Document Classifier | Scikit-Learn / PyTorch | **P2 (Medium)** |
| **ML-04** | **AI/ML** | Fine-Tuned Indian Legal NER Engine | LayoutLMv3 / spaCy | **P1 (High)** |
| **OPS-01**| **DevOps**  | Inter-Service mTLS & Docker Compose Stack | Docker Compose / TLS X.509 | **P1 (High)** |
