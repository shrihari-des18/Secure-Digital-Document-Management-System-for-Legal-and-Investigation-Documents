# SIH26190 E-Vault — Project Progress & Technical Remaining Work Roadmap

This document presents a **detailed technical and architectural roadmap** for the **Secure Digital Evidence and Legal Document Management System (E-Vault)**. It clearly differentiates completed capabilities from remaining tasks across the **Backend Subsystem (Java Spring Boot)**, the **AI/ML Microservice Subsystem (Python FastAPI)**, and **DevOps Infrastructure**, focusing on architectural design, operational mechanics, and functional requirements.

---

## 1. Executive Summary & System Overview

The E-Vault is designed for legal admissibility and chain-of-custody preservation under the **Bharatiya Sakshya Adhiniyam, 2023 (§63)** (replacing Evidence Act §65B). The system architecture comprises two decoupled server tiers:

1. **Backend Core Subsystem (Java 21 LTS / Spring Boot 4.1.1)**: Handles authentication, RBAC/ABAC access control, FIR case dockets, evidence intake, cryptographic hash sealing, append-only audit ledgers, custody tracking, and statutory §63 PDF court certificates.
2. **AI/ML Microservice Subsystem (Python 3.12 / FastAPI / PaddleOCR)**: Handles document text extraction (OCR), PII entity recognition, bounding box detection, and forensic analysis.

- **Current Build Status**: **54 / 54 Automated Backend Tests Passing** (`mvn test` build success).

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
│                          │ • Append-only, hash-chained ledger ($\text{entryHash}$).│
│                          │ • Whole-ledger audit verifier (`GET /ledger/verify`).  │
│                          │ • Chronological Chain of Custody tracking & transfers. │
│                          │ • OpenPDF BSA 2023 §63 court PDF generator.            │
│                          │ • Legal document versioning ($v1, v2, \dots$).         │
│                          │ • Resilient AI Gateway HTTP client with fallback.      │
│                          │ • STOMP WebSocket broker with JWT auth (`/ws-evault`). │
│                          │ • Real-time Security Alarms (`/topic/security/alarms`).│
│                          │ • Real-time Case Activity feeds (`/topic/case/{id}`).  │
│                          │ • Auto-triggered alerts on ledger tamper & bit-rot.    │
│                          │ • Security Alarms REST API with auditor acknowledgment.│
├──────────────────────────┼────────────────────────────────────────────────────────┤
│ **AI/ML (FastAPI)**      │ • Asynchronous FastAPI Uvicorn engine.                 │
│                          │ • PaddleOCR v3.7+ text & bounding box extraction.      │
│                          │ • Regex + spaCy NER for Aadhaar, PAN, phone, email.    │
└──────────────────────────┴────────────────────────────────────────────────────────┘
```

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
│ **BE-01**     │ MinIO / S3 WORM Object Storage   │ Hardware-enforced immutability │
│ **BE-02**     │ Vault KMS Envelope Encryption    │ At-rest data key protection    │
│ **BE-03**     │ Open Policy Agent (OPA) ABAC PDP │ Dynamic policy authorization   │
│ **BE-04**     │ OpenSearch Hybrid Search Engine  │ Lexical & vector search        │
│ **BE-05**     │ WebSocket Real-Time Tamper Alarms│ Instant security broadcasting  │
└───────────────┴──────────────────────────────────┴────────────────────────────────┘
```

#### Task BE-01: Hardware WORM (Write-Once-Read-Many) Storage Integration
- **Operational Need**: Prevents malicious actors or rogue database administrators from modifying or deleting stored evidence files, satisfying strict chain-of-custody immutability rules.
- **Architectural Design**:
  - Replace local file storage with an S3-compliant object store (`MinioClient` / `S3Client`).
  - Configure Object Lock in **COMPLIANCE mode** with a statutory retention period (7–30 years based on crime severity).
  - Implement Legal Hold capabilities on active case dockets to block file deletion until judicial disposal.
- **How It Works**: Once an officer ingests evidence, the storage engine enforces hardware-level write-once-read-many locks. Any `PUT`, `POST`, or `DELETE` attempt on the binary object is rejected at the storage API layer.

#### Task BE-02: Key Vault KMS Envelope Encryption (HashiCorp Vault / Cloud KMS)
- **Operational Need**: Prevents raw evidence binaries from residing in plaintext on disk storage, securing data even if physical disks are stolen or directly accessed.
- **Architectural Design**:
  - Implement an **Envelope Encryption** architecture.
  - Ephemeral Data Encryption Keys (DEKs) generated per file (`AES-256-GCM`).
  - Key Encryption Key (KEK) managed in HashiCorp Vault's Transit Engine to wrap and unwrap DEKs.
- **How It Works**: During upload, a random DEK encrypts the binary stream. The DEK is sent to Vault, wrapped by the master KEK, and stored as ciphertext in PostgreSQL. Decryption requires authenticated Vault authorization, ensuring plaintext keys never persist on disk.

#### Task BE-03: Open Policy Agent (OPA) Dynamic ABAC Policy Engine Integration
- **Operational Need**: Hardcoded security checks cannot accommodate dynamic organizational rules, clearance levels, shift hours, or location constraints across different police stations and courts.
- **Architectural Design**:
  - Integrate Open Policy Agent (OPA) as a centralized Policy Decision Point (PDP).
  - Define declarative **Rego policies** evaluating fine-grained context attributes.
- **How It Works**: Before executing any request, Spring Security passes context attributes (`User Role`, `Clearance`, `Case Assignment`, `Client IP Subnet`, `Jurisdiction`) to OPA, which evaluates rules and returns a binary `allow / deny` decision.

#### Task BE-04: OpenSearch Hybrid Full-Text & Vector Search Integration
- **Operational Need**: Investigating officers and prosecutors require instant search across millions of OCR-extracted document pages, case diaries, and FIRs.
- **Architectural Design**:
  - Deploy an OpenSearch cluster and establish automated data sync listeners from PostgreSQL.
  - Implement hybrid search combining **BM25 lexical matching** and **Dense Vector k-NN similarity search**.
- **How It Works**: Text content from FIRs, legal document versions, and AI OCR outputs is indexed into OpenSearch alongside vector embeddings, enabling both keyword matching and semantic context search.

#### Task BE-05: Real-Time WebSockets & Security Alarm Dispatcher
- **Operational Need**: Immediate operational alerts are required when custody handoffs occur or when the audit ledger detects a security compromise.
- **Architectural Design**:
  - Configure STOMP WebSockets broker in Spring Boot.
  - Expose real-time topic channels (`/topic/case/{id}` and `/topic/security/alarms`).
- **How It Works**: If a whole-ledger verification scan detects a broken sequence hash or file bit-rot, an automated alert payload is instantly pushed to active auditor and administrator dashboards.

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

| Module ID | Subsystem | Module Scope | Key Operational Benefit | Priority |
|---|---|---|---|---|
| **BE-01** | **Backend** | MinIO / S3 WORM Storage Integration | Hardware-enforced file immutability | **P0 (Critical)** |
| **BE-02** | **Backend** | Vault Transit KMS Envelope Encryption | Protection of encrypted data keys at rest | **P0 (Critical)** |
| **BE-03** | **Backend** | Open Policy Agent (OPA) Dynamic ABAC PDP | Dynamic clearance & context policy enforcement | **P1 (High)** |
| **BE-04** | **Backend** | OpenSearch Hybrid Lexical & Vector Search | High-speed semantic search across evidence text | **P2 (Medium)** |
| **BE-05** | **Backend** | Real-Time WebSockets & Security Alarms | Live tamper alerts broadcast to dashboards | **COMPLETED (54/54 Tests Passing)** |
| **ML-01** | **AI/ML** | Image Tampering Detection (ELA & EXIF) | Automated visual evidence manipulation check | **P1 (High)** |
| **ML-02** | **AI/ML** | Perceptual Hashing (`pHash`) Deduplication | Visual similarity search across cases | **P1 (High)** |
| **ML-03** | **AI/ML** | Multi-Class Legal Document Classifier | Automated document intake tagging | **P2 (Medium)** |
| **ML-04** | **AI/ML** | Fine-Tuned Indian Legal NER Engine | Precision Indian legal & PII extraction | **P1 (High)** |
| **OPS-01**| **DevOps**  | Inter-Service mTLS & Docker Compose Stack | Secure encrypted inter-service channel | **P1 (High)** |
