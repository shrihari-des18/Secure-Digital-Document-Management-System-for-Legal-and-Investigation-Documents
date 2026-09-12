# SIH26190 E-Vault API Testing Handbook & Step-by-Step Guide

This handbook provides an end-to-end testing workflow for the **Secure Digital Evidence & Legal Document Management System (E-Vault)** backend. It contains exact request bodies, parameter specifications, header requirements, and step-by-step Swagger UI testing instructions for every endpoint.

---

## Swagger UI Quick Access
- **Swagger UI URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Specs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 1. Authentication & User Onboarding (`/api/v1/auth`)

### Step 1.1: Register an Investigating Officer
- **Endpoint**: `POST /api/v1/auth/register`
- **Description**: Registers a new officer account with BCrypt password hashing and returns an initial JWT bearer token.
- **Headers**: `Content-Type: application/json`

**JSON Request Body**:
```json
{
  "username": "officer_vikram",
  "password": "Password@123",
  "fullName": "Inspector Vikram Rathore",
  "email": "vikram@police.gov.in",
  "badgeNumber": "DL-4509",
  "department": "Cyber Crime Cell",
  "role": "INVESTIGATING_OFFICER"
}
```
> 📌 **Note down**: Save `data.user.id` (e.g. `d3b07384-...`) as `OFFICER_ID` and `data.token` as `JWT_TOKEN`.

---

### Step 1.2: Register a Forensic Analyst
- **Endpoint**: `POST /api/v1/auth/register`
- **Description**: Registers a forensic analyst account.
- **Headers**: `Content-Type: application/json`

**JSON Request Body**:
```json
{
  "username": "analyst_meera",
  "password": "Password@123",
  "fullName": "Dr. Meera Nair",
  "email": "meera.nair@fsl.gov.in",
  "badgeNumber": "FSL-9021",
  "department": "Digital Forensics Division",
  "role": "FORENSIC_ANALYST"
}
```
> 📌 **Note down**: Save `data.user.id` as `ANALYST_ID`.

---

### Step 1.3: User Login
- **Endpoint**: `POST /api/v1/auth/login`
- **Description**: Authenticates user credentials and returns a signed HMAC-SHA256 JWT Bearer token.
- **Headers**: `Content-Type: application/json`

**JSON Request Body**:
```json
{
  "username": "officer_vikram",
  "password": "Password@123"
}
```

---

### Step 1.4: Authorize Swagger UI
1. Click the green **`Authorize` 🔒** button at the top-right of Swagger UI.
2. In the box, paste your JWT token string. (Swagger UI will automatically prepend `Bearer `).
3. Click **Authorize** and then **Close**.

---

### Step 1.5: Verify Current Profile
- **Endpoint**: `GET /api/v1/auth/me`
- **Description**: Retrieves current authenticated actor profile details.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

## 2. Case Docket Management (`/api/v1/cases`)

### Step 2.1: Create a Case
- **Endpoint**: `POST /api/v1/cases`
- **Description**: Initializes a formal criminal investigation docket.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: application/json`
- **Permitted Roles**: `INVESTIGATING_OFFICER`, `SYSTEM_ADMIN`

**JSON Request Body**:
```json
{
  "caseNumber": "FIR-2026-DELHI-101",
  "title": "Operation Cyber Vault",
  "description": "Ransomware attack and data exfiltration targeting regional infrastructure",
  "jurisdiction": "Delhi NCT Cyber Crime Cell",
  "leadOfficerId": "<PASTE_OFFICER_ID>",
  "assignedOfficerIds": [
    "<PASTE_OFFICER_ID>",
    "<PASTE_ANALYST_ID>"
  ]
}
```
> 📌 **Note down**: Save `data.id` as `CASE_ID`.

---

### Step 2.2: Get Case Details by ID
- **Endpoint**: `GET /api/v1/cases/{id}`
- **Description**: Retrieves full metadata for a case including assigned officers and status.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

### Step 2.3: Update Case Status
- **Endpoint**: `PATCH /api/v1/cases/{id}/status`
- **Description**: Transitions case lifecycle status (`OPEN` → `UNDER_INVESTIGATION` → `CHARGESHEET_FILED` → `IN_COURT` → `CLOSED` → `ARCHIVED`).
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: application/json`

**JSON Request Body**:
```json
{
  "status": "UNDER_INVESTIGATION",
  "reason": "Evidence seized from suspect gateway server and logged into vault"
}
```

---

## 3. Evidence Intake & Cryptographic Storage (`/api/v1/evidence`)

### Step 3.1: Upload and Cryptographically Seal Evidence
- **Endpoint**: `POST /api/v1/evidence/upload`
- **Description**: Ingests evidence, computes streaming SHA-256 and SHA-512 hashes, generates HMAC server signature, stores blob in partitioned local storage, and logs audit events.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: multipart/form-data`

**Multipart Form Data Parts**:
- **`metadata`** (`application/json`):
```json
{
  "caseId": "<CASE_ID>",
  "evidenceNumber": "EVID-DEL-2026-001",
  "title": "Gateway Access Logs Archive",
  "description": "Seized server syslog tarball containing access traces",
  "classification": "CONFIDENTIAL",
  "deviceMake": "Dell",
  "deviceModel": "PowerEdge R740 Server",
  "serialNumber": "SN-7798-DEL-2026",
  "seizureLocation": "Data Center Rack 4, Okhla Phase III, Delhi",
  "seizedAt": "2026-09-10T10:30:00Z"
}
```
- **`file`** (`binary`): Select and upload sample evidence file (e.g. `access_logs.txt`).

> 📌 **Note down**: Save `data.id` as `EVIDENCE_ID`.

---

### Step 3.2: Verify On-Disk Physical Integrity
- **Endpoint**: `POST /api/v1/evidence/{id}/verify-integrity`
- **Description**: Re-reads stored binary file from disk, recalculates SHA-256 on the fly, compares against ingestion hash snapshot, and flags any bit rot or disk alteration.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

### Step 3.3: Stream Evidence File Download
- **Endpoint**: `GET /api/v1/evidence/{id}/download`
- **Description**: Streams the raw sealed evidence file for inspection and logs a `DOWNLOAD` event into chain of custody and audit ledger.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

## 4. Chain of Custody & Audit Ledger (`/api/v1/evidence`, `/api/v1/ledger`)

### Step 4.1: Transfer Evidence Custody
- **Endpoint**: `POST /api/v1/evidence/{evidenceId}/custody-transfer`
- **Description**: Transfers physical/digital evidence custody to another officer or court official with mandatory statutory justification.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: application/json`

**JSON Request Body**:
```json
{
  "toPartyId": "<ANALYST_ID>",
  "reason": "Handover to digital forensics lab for log analysis and IP extraction"
}
```

---

### Step 4.2: Inspect Chain of Custody Timeline
- **Endpoint**: `GET /api/v1/evidence/{evidenceId}/custody`
- **Description**: Returns chronological custody timeline logs including seizure, ingestion, transfers, views, downloads, and certification events.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

### Step 4.3: Verify Whole-Ledger Cryptographic Chain Integrity
- **Endpoint**: `GET /api/v1/ledger/verify`
- **Description**: Traverses the entire append-only SHA-256 block chain from genesis to head, verifying row integrity and block linkages. Detects SQL-level direct DB tampering.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

## 5. Statutory BSA 2023 §63 Court Certification (`/api/v1/certificates`)

### Step 5.1: Issue Court Admissibility Certificate
- **Endpoint**: `POST /api/v1/certificates/evidence/{evidenceId}`
- **Description**: Generates an official statutory certificate PDF under Section 63 of Bharatiya Sakshya Adhiniyam, 2023, binding evidence hashes, device particulars, custody summary, and certifying officer digital seal.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: application/json`

**JSON Request Body** *(Optional)*:
```json
{
  "mannerOfProduction": "Direct bit-stream image preservation from Dell PowerEdge server controller",
  "systemParticulars": "EnCase Forensic v23.4 running on Forensic Workstation FW-01",
  "certifyingOfficerRemarks": "Evidence seized in compliance with legal procedure and sealed immediately."
}
```
> 📌 **Note down**: Save `data.id` as `CERTIFICATE_ID`.

---

### Step 5.2: Download Court Certificate PDF
- **Endpoint**: `GET /api/v1/certificates/{id}/download`
- **Description**: Streams the generated Section 63 statutory court certificate PDF file.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

## 6. Legal Document Management & Versioning (`/api/v1/documents`)

### Step 6.1: File Initial Legal Document (v1)
- **Endpoint**: `POST /api/v1/documents`
- **Description**: Creates initial version ($v1$) of legal document (e.g. FIR, Search Warrant, Seizure Memo, Forensic Report).
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: multipart/form-data`

**Multipart Form Data Parts**:
- **`metadata`** (`application/json`):
```json
{
  "caseId": "<CASE_ID>",
  "title": "First Information Report (FIR)",
  "documentType": "FIR",
  "changeSummary": "Initial FIR filed by Cyber Crime Cell"
}
```
- **`file`** (`binary`): Select and upload PDF document file (e.g. `FIR_v1.pdf`).

> 📌 **Note down**: Save `data.id` as `DOCUMENT_ID`.

---

### Step 6.2: Upload Amended Version (v2)
- **Endpoint**: `POST /api/v1/documents/{id}/versions`
- **Description**: Appends a new revision ($v2 \dots vN$) to an existing legal document, maintaining an immutable version history and calculating per-version SHA-256 hashes.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: multipart/form-data`

**Multipart Form Data Parts**:
- **`metadata`** (`application/json`):
```json
{
  "changeSummary": "Added supplementary suspect statements and forensic lab references"
}
```
- **`file`** (`binary`): Select and upload amended PDF document file (e.g. `FIR_v2.pdf`).

---

### Step 6.3: View Full Revision History
- **Endpoint**: `GET /api/v1/documents/{id}/versions`
- **Description**: Lists all historical document versions with timestamps, change summaries, uploaders, and SHA-256 hashes.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

### Step 6.4: Download Versioned Document File
- **Endpoint**: `GET /api/v1/documents/{id}/download?version=1`
- **Description**: Streams specific document version file. If `version` query parameter is omitted, downloads latest active version.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None

---

## 7. AI/ML Evidence Analysis Subsystem (`/api/v1/evidence/{evidenceId}/*`)

### Step 7.1: Trigger AI Evidence Analysis
- **Endpoint**: `POST /api/v1/evidence/{evidenceId}/analyze`
- **Description**: Asynchronously dispatches evidence to external Python FastAPI ML microservice for OCR text extraction, PII scanning (Aadhaar/PAN/phone), auto-classification tags, and pHash calculation. Features automatic mock fallback if microservice is offline.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`, `Content-Type: application/json`
- **Permitted Roles**: `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `SYSTEM_ADMIN`

**JSON Request Body** *(Optional)*:
```json
{
  "forceReanalysis": true
}
```

---

### Step 7.2: Retrieve AI Analysis Results & Metadata
- **Endpoint**: `GET /api/v1/evidence/{evidenceId}/analysis`
- **Description**: Retrieves completed AI analysis record including OCR text, detected PII, auto-tags, perceptual hash (pHash), ML confidence score, and summary.
- **Headers**: `Authorization: Bearer <YOUR_JWT_TOKEN>`
- **Request Body**: None
