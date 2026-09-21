# Comprehensive Swagger UI End-to-End Testing Guide
## Secure Digital Document & Evidence Management System (E-Vault)

This handbook provides an **exhaustive, 100% verified step-by-step testing guide** tailored specifically for testing all system capabilities via **Swagger UI** (`http://localhost:8080/swagger-ui/index.html`).

Every request body schema, field name, parameter type, and endpoint path in this guide is verified against the backend DTO classes and `API_Testing_Handbook.md`.

---

## 📍 System Access & Base URLs

- **Backend Swagger UI**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **Backend OpenAPI JSON**: `http://localhost:8080/v3/api-docs`
- **Python AI/ML Microservice Docs**: [http://localhost:8000/docs](http://localhost:8000/docs)

---

## 🔑 How to Manage Authorization Tokens in Swagger UI

1. At the top-right corner of Swagger UI, click the green **`Authorize` 🔒** button.
2. In the `Value:` input field, paste the raw **JWT Token string** returned by registration/login.
   > ⚠️ **IMPORTANT**: Do **NOT** type `Bearer `; Swagger UI automatically prepends `Bearer ` to your token string.
3. Click **Authorize** $\rightarrow$ Click **Close**.
4. When switching users/roles for restricted tasks, click **`Authorize` 🔒** $\rightarrow$ Click **Logout** $\rightarrow$ Paste the new role's JWT token $\rightarrow$ Click **Authorize** $\rightarrow$ Click **Close**.

---

## 👥 Master User & Role Accounts Matrix

To test all Role-Based Access Control (RBAC) rules, register these 4 distinct user accounts in Phase 1:

| User Designation | Username | Role Name | Required Credentials & ID Tracking | Endpoints / Tasks Authorized |
| :--- | :--- | :--- | :--- | :--- |
| **User A** | `officer_sharma` | `INVESTIGATING_OFFICER` | Copy `IO_TOKEN` & `IO_USER_ID` | Cases, Evidence Upload, AI Trigger, Transfers, Court §63 Certificates, Legal Docs |
| **User B** | `analyst_verma` | `FORENSIC_ANALYST` | Copy `ANALYST_TOKEN` & `ANALYST_USER_ID` | Evidence Inspection, AI Trigger, Custody Receival |
| **User C** | `custodian_singh`| `RECORDS_CUSTODIAN` | Copy `CUSTODIAN_TOKEN` & `CUSTODIAN_USER_ID` | Evidence Viewing, Custody Transfers, Certificate Verification |
| **User D** | `auditor_gupta` | `AUDITOR` | Copy `AUDITOR_TOKEN` & `AUDITOR_USER_ID` | **Whole Ledger Cryptographic Verification (`/api/v1/ledger/verify`)**, Audit Logs |

---

## 🗺️ Complete End-to-End Execution Flow

---

### Phase 1: User Onboarding & RBAC Registration (`auth-controller`)

- 🔒 **Authorization Needed**: **None (Public Endpoints)**

#### Task 1.1: Register User A (Investigating Officer)
1. In Swagger UI, expand **`auth-controller`** $\rightarrow$ Click **`POST /api/v1/auth/register`** $\rightarrow$ Click **Try it out**.
2. Paste into Request Body:
```json
{
  "username": "officer_sharma",
  "password": "Password123!",
  "fullName": "Inspector R. Sharma",
  "email": "sharma@police.gov.in",
  "badgeNumber": "DL-9081",
  "department": "Cyber Crime Cell",
  "role": "INVESTIGATING_OFFICER"
}
```
3. Click **Execute**.
4. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.token` $\rightarrow$ Save as **`IO_TOKEN`**.
   - Copy `data.user.id` $\rightarrow$ Save as **`IO_USER_ID`**.

---

#### Task 1.2: Register User B (Forensic Analyst)
1. In `POST /api/v1/auth/register`, paste into Request Body:
```json
{
  "username": "analyst_verma",
  "password": "Password123!",
  "fullName": "Dr. A. Verma",
  "email": "verma@fsl.gov.in",
  "badgeNumber": "FSL-4412",
  "department": "Digital Forensics Division",
  "role": "FORENSIC_ANALYST"
}
```
2. Click **Execute**.
3. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.token` $\rightarrow$ Save as **`ANALYST_TOKEN`**.
   - Copy `data.user.id` $\rightarrow$ Save as **`ANALYST_USER_ID`**.

---

#### Task 1.3: Register User C (Records Custodian)
1. In `POST /api/v1/auth/register`, paste into Request Body:
```json
{
  "username": "custodian_singh",
  "password": "Password123!",
  "fullName": "Sgt. K. Singh",
  "email": "singh@evault.gov.in",
  "badgeNumber": "CUST-3310",
  "department": "Evidence Repository Unit",
  "role": "RECORDS_CUSTODIAN"
}
```
2. Click **Execute**.
3. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.token` $\rightarrow$ Save as **`CUSTODIAN_TOKEN`**.
   - Copy `data.user.id` $\rightarrow$ Save as **`CUSTODIAN_USER_ID`**.

---

#### Task 1.4: Register User D (Auditor)
1. In `POST /api/v1/auth/register`, paste into Request Body:
```json
{
  "username": "auditor_gupta",
  "password": "Password123!",
  "fullName": "S. K. Gupta",
  "email": "gupta.audit@gov.in",
  "badgeNumber": "AUD-1102",
  "department": "State Forensic Audit Bureau",
  "role": "AUDITOR"
}
```
2. Click **Execute**.
3. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.token` $\rightarrow$ Save as **`AUDITOR_TOKEN`**.
   - Copy `data.user.id` $\rightarrow$ Save as **`AUDITOR_USER_ID`**.

---

#### Task 1.5: Authorize Swagger UI as Investigating Officer
1. Click green **`Authorize` 🔒** button at top-right of Swagger UI.
2. Paste **`IO_TOKEN`**.
3. Click **Authorize** $\rightarrow$ Click **Close**.

---

#### Task 1.6: Verify Profile Details
- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`)
1. Expand **`user-controller`** $\rightarrow$ Click **`GET /api/v1/users/me`** $\rightarrow$ Click **Try it out**.
2. Click **Execute**. Verify response returns `username: "officer_sharma"` and `role: "INVESTIGATING_OFFICER"`.

---

### Phase 2: Case Docket Lifecycle Management (`case-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`)

#### Task 2.1: Initialize Criminal Case Docket
1. Expand **`case-controller`** $\rightarrow$ Click **`POST /api/v1/cases`** $\rightarrow$ Click **Try it out**.
2. Paste into Request Body (`CreateCaseRequest`):
```json
{
  "caseNumber": "FIR-2026-DELHI-101",
  "title": "Banking Infrastructure Ransomware & Data Exfiltration",
  "description": "Unauthorized root intrusion, transaction database manipulation, and extortion",
  "jurisdiction": "Delhi NCT Cyber Crime Cell",
  "leadOfficerId": "<PASTE_IO_USER_ID>",
  "assignedOfficerIds": [
    "<PASTE_IO_USER_ID>",
    "<PASTE_ANALYST_USER_ID>"
  ]
}
```
*(Replace `<PASTE_IO_USER_ID>` and `<PASTE_ANALYST_USER_ID>` with saved UUIDs).*
3. Click **Execute**.
4. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.id` from response $\rightarrow$ Save as **`CASE_ID`**.
   - Observe initial status is `OPEN`.

---

#### Task 2.2: Retrieve Case Details by ID
1. Click **`GET /api/v1/cases/{id}`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `id`: `<CASE_ID>`. Click **Execute**.

---

#### Task 2.3: Transition Case Lifecycle Status
1. Click **`PATCH /api/v1/cases/{id}/status`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `id`: `<CASE_ID>`.
3. Paste into Request Body:
```json
{
  "status": "UNDER_INVESTIGATION",
  "reason": "Seized server gateway logs and media drives from bank site"
}
```
4. Click **Execute**. Verify status updated to `UNDER_INVESTIGATION`.

---

### Phase 3: Digital Evidence Ingestion & Cryptographic Sealing (`evidence-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`)

#### Task 3.1: Upload & Dual Cryptographic Seal Evidence File
1. Expand **`evidence-controller`** $\rightarrow$ Click **`POST /api/v1/evidence/upload`** $\rightarrow$ Click **Try it out**.
2. Fill multipart form parts:
   - **`metadata`** (`application/json`):
```json
{
  "caseId": "<PASTE_CASE_ID>",
  "evidenceNumber": "EVID-DEL-2026-001",
  "title": "Gateway Access Logs Archive",
  "description": "Seized server syslog tarball containing access traces",
  "classification": "CONFIDENTIAL",
  "deviceMake": "Dell",
  "deviceModel": "PowerEdge R740 Server",
  "deviceSerialNumber": "SN-7798-DEL-2026",
  "seizedLocation": "Data Center Rack 4, Okhla Phase III, Delhi",
  "seizedAt": "2026-09-10T10:30:00Z"
}
```
   - **`file`** (`binary`): Click **Choose File** / **Browse** and select any sample evidence file from your system (e.g. `.txt`, `.log`, `.jpg`, or `.pdf`).
3. Click **Execute**.
4. 📝 **SAVE DATA & VERIFY CRYPTOGRAPHY**:
   - Copy `data.id` from response $\rightarrow$ Save as **`EVIDENCE_ID`**.
   - Observe computed `sha256Hash` (64 hex characters).
   - Observe computed `sha512Hash` (128 hex characters).
   - Observe `encryptedS3Path` (S3 WORM object path).

---

#### Task 3.2: Stream Decrypted Evidence File Download
1. Click **`GET /api/v1/evidence/{id}/download`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `id`: `<EVIDENCE_ID>`. Click **Execute**.
3. Click **Download file** to verify decrypted binary streaming download.

---

### Phase 4: Physical Disk Integrity Re-Verification (`evidence-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, or `AUDITOR`

#### Task 4.1: Execute On-Disk Integrity Checksum Re-verification
1. In `evidence-controller`, click **`POST /api/v1/evidence/{id}/verify-integrity`**.
2. Click **Try it out**.
3. Fill parameter `id`: `<EVIDENCE_ID>`. Click **Execute**.
4. Verify response fields:
   - `"integrityStatus": "INTACT"`
   - `"expectedSha256"` matches `"recomputedSha256"`.

---

### Phase 5: AI/ML Document Forensics & Gateway Integration (`ai-analysis-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`) or `FORENSIC_ANALYST` (`ANALYST_TOKEN`)

#### Task 5.1: Trigger AI Microservice Pipeline (PaddleOCR & PII NER)
1. Expand **`ai-analysis-controller`** $\rightarrow$ Click **`POST /api/v1/evidence/{evidenceId}/analyze`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `evidenceId`: `<EVIDENCE_ID>`.
3. Paste into Request Body (`TriggerAnalysisRequest`):
```json
{
  "extractOcr": true,
  "detectPii": true,
  "generateTags": true
}
```
4. Click **Execute**.
5. **VERIFY LIVE AI INTEGRATION OUTPUT**:
   - Backend dispatches REST HTTP request to Python FastAPI on `http://localhost:8000/api/v1/analyze`.
   - Verify response contains:
     - `status: "COMPLETED"`
     - `ocrText`: Extracted OCR document text
     - `detectedPii`: Detected Aadhaar, Email, and IP address entities
     - `classificationTags`: `CYBER_CRIME, LOG_ANALYSIS, SEIZED_MEDIA, HIGH_PRIORITY`
     - `perceptualHash`: `pHash-a8f9c2d7e1b4093f`

---

#### Task 5.2: Retrieve Stored AI Analysis Record
1. Click **`GET /api/v1/evidence/{evidenceId}/analysis`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `evidenceId`: `<EVIDENCE_ID>`. Click **Execute**.

---

### Phase 6: Chain of Custody & Legal Custodian Transfers (`evidence-controller` & `custody-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`)

#### Task 6.1: Retrieve Immutable Custody Audit Trail Timeline
1. Expand **`evidence-controller`** $\rightarrow$ Click **`GET /api/v1/evidence/{evidenceId}/custody`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `evidenceId`: `<EVIDENCE_ID>`. Click **Execute**.
3. Verify event log timeline contains chronological events: `INGEST` $\rightarrow$ `ANALYZE`.

---

#### Task 6.2: Transfer Evidence Custody to Forensic Analyst
1. Click **`POST /api/v1/evidence/{evidenceId}/custody-transfer`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `evidenceId`: `<EVIDENCE_ID>`.
3. Paste into Request Body (`CustodyTransferRequest`):
```json
{
  "toPartyId": "<PASTE_ANALYST_USER_ID>",
  "reason": "Handover to digital forensics lab for log analysis and IP extraction"
}
```
*(Replace `<PASTE_ANALYST_USER_ID>` with saved Analyst UUID).*
4. Click **Execute**. Verify transfer event recorded and custodian updated.

---

### Phase 7: BSA 2023 §63 Statutory Court PDF Certificate Generator (`certificate-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`), `PROSECUTOR`, `COURT`, or `RECORDS_CUSTODIAN`

#### Task 7.1: Issue Statutory §63 Certificate Record
1. Expand **`certificate-controller`** $\rightarrow$ Click **`POST /api/v1/certificates/evidence/{evidenceId}`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `evidenceId`: `<EVIDENCE_ID>`.
3. Paste into Request Body (`IssueCertificateRequest`):
```json
{
  "mannerOfProduction": "Direct bit-stream image preservation from Dell PowerEdge server controller",
  "certifyingOfficerNotes": "Evidence seized in compliance with legal procedure and sealed immediately."
}
```
4. Click **Execute**.
5. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.id` from response $\rightarrow$ Save as **`CERTIFICATE_ID`**.

---

#### Task 7.2: Download Court-Admissible PDF Certificate
1. Click **`GET /api/v1/certificates/{id}/download`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `id`: `<CERTIFICATE_ID>`. Click **Execute**.
3. Click **Download file**. Open the downloaded PDF file to view:
   - Part A: Device Particulars & Custody Period
   - Part B: SHA-256 & SHA-512 Cryptographic Hashes
   - Complete Custody Transfer Table
   - BSA 2023 §63 Statutory Legal Affirmation

---

### Phase 8: Legal Document Management & Versioning (`legal-document-controller`)

- 🔒 **Authorization Needed**: `INVESTIGATING_OFFICER` (`IO_TOKEN`), `PROSECUTOR`, or `COURT`

#### Task 8.1: File Initial Legal Document (Version 1)
1. Expand **`legal-document-controller`** $\rightarrow$ Click **`POST /api/v1/documents`** $\rightarrow$ Click **Try it out**.
2. Fill multipart form parts:
   - **`metadata`** (`application/json`):
```json
{
  "caseId": "<PASTE_CASE_ID>",
  "documentNumber": "FIR-DOC-2026-001",
  "title": "First Information Report (FIR)",
  "documentType": "FIR",
  "changeSummary": "Initial FIR filed by Cyber Crime Cell"
}
```
   - **`file`** (`binary`): Click **Choose File** and upload initial PDF/Text document (`FIR_v1.pdf`).
3. Click **Execute**.
4. 📝 **SAVE DATA FROM RESPONSE**:
   - Copy `data.id` from response $\rightarrow$ Save as **`DOCUMENT_ID`**.
   - Verify `currentVersion: 1`.

---

#### Task 8.2: Append Amended Legal Document Version (Version 2)
1. Click **`POST /api/v1/documents/{id}/versions`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `id`: `<DOCUMENT_ID>`.
3. Fill multipart form parts:
   - **`metadata`** (`application/json`):
```json
{
  "changeSummary": "Added supplementary suspect statements and forensic lab references"
}
```
   - **`file`** (`binary`): Click **Choose File** and upload amended PDF/Text document (`FIR_v2.pdf`).
4. Click **Execute**. Verify version updated to `2` and revision SHA-256 hash computed.

---

#### Task 8.3: View Full Version History
1. Click **`GET /api/v1/documents/{id}/versions`** $\rightarrow$ Click **Try it out**.
2. Fill parameter `id`: `<DOCUMENT_ID>`. Click **Execute**. Verify version history array ($v1 \to v2$).

---

### ⚠️ Phase 9: Append-Only Tamper-Evident Audit Ledger Verification (`ledger-controller`)

> 🔐 **RBAC TOKEN SWITCH REQUIRED**:
> The endpoint `GET /api/v1/ledger/verify` enforces strict RBAC security: `@PreAuthorize("hasAnyRole('AUDITOR', 'SYSTEM_ADMIN')")`.
> If executed under `officer_sharma`'s token, Swagger UI returns **`403 Forbidden`**.

#### 🔄 Instructions to Switch Token in Swagger UI:
1. Scroll to top-right of Swagger UI and click green **`Authorize` 🔒**.
2. Click **Logout**.
3. Paste **`AUDITOR_TOKEN`** (the token saved from Task 1.4 for `auditor_gupta`).
4. Click **Authorize** $\rightarrow$ Click **Close**.

---

#### Task 9.1: Retrieve Complete Audit Ledger Logs
- 🔒 **Authorization Needed**: `AUDITOR` (`AUDITOR_TOKEN`)
1. Expand **`ledger-controller`** $\rightarrow$ Click **`GET /api/v1/ledger/entries`** $\rightarrow$ Click **Try it out**.
2. Click **Execute**. Inspect transaction log entries recorded across Phases 1–8.

---

#### Task 9.2: Verify Cryptographic Hash Chain of Entire Ledger
- 🔒 **Authorization Needed**: `AUDITOR` (`AUDITOR_TOKEN`)
1. Click **`GET /api/v1/ledger/verify`** $\rightarrow$ Click **Try it out**.
2. Click **Execute**.
3. **VERIFY RESPONSE OUTPUT**:
```json
{
  "success": true,
  "data": {
    "ledgerIntact": true,
    "totalEntriesScanned": 9,
    "verificationTimestamp": "2026-09-20T00:00:00Z",
    "genesisHash": "0000000000000000000000000000000000000000000000000000000000000000",
    "latestEntryHash": "a8f9c2d7e1b4093f66a87b120938..."
  }
}
```
*Confirms every ledger transaction is sequentially linked via cryptographic SHA-256 hash chaining ($\text{entryHash}_n = \text{SHA-256}(\dots \parallel \text{entryHash}_{n-1})$).*

---

### Phase 10: Hybrid System Search Subsystem (`search-controller`)

- 🔒 **Authorization Needed**: Any Authenticated User (`AUDITOR_TOKEN` or `IO_TOKEN`)

#### Task 10.1: Perform Hybrid Full-Text & Vector Search
1. Expand **`search-controller`** $\rightarrow$ Click **`GET /api/v1/search`** $\rightarrow$ Click **Try it out**.
2. Fill query parameters:
   - `query`: `cyber` (or `dell` or `syslog`)
   - `type`: `ALL`
   - `page`: `0`
   - `size`: `10`
3. Click **Execute**.
4. Verify returned search hits across Case dockets, Evidence metadata, OCR text, and Legal documents.

---

### Phase 11: Direct Python AI/ML Microservice Testing (`http://localhost:8000/docs`)

In addition to backend integration, you can test the Python AI/ML microservice directly via FastAPI Swagger UI.

1. Open **[http://localhost:8000/docs](http://localhost:8000/docs)** in a new browser tab.
2. Expand **`POST /api/v1/process-document`** $\rightarrow$ Click **Try it out**.
3. Click **Choose File** and upload an image or PDF file.
4. Click **Execute**.
5. **VERIFY DIRECT FASTAPI RESPONSE**:
   - `extracted_text`: Extracted PaddleOCR text lines
   - `entities`: Extracted PII entities with pixel bounding boxes `[min_x, min_y, max_x, max_y]`
   - `document_type`: Classified document type (`FIR`, `CHARGE_SHEET`, or `SEIZURE_MEMO`)
   - `is_tampered` & `tamper_score`: Error Level Analysis (ELA) tamper score
   - `phash` & `text_hash`: Perceptual visual hash and structural text hash
