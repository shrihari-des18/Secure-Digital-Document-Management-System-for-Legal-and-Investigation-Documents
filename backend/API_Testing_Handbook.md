# SIH26190 E-Vault API Testing Handbook & Step-by-Step Guide

This handbook provides an end-to-end testing workflow for the **Secure Digital Evidence & Legal Document Management System (E-Vault)** backend. It contains exact request bodies, Swagger UI instructions, and equivalent `curl` commands for every step.

---

## Swagger UI Quick Access
- **Swagger UI URL**: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
- **OpenAPI Specs**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## 1. Authentication & User Onboarding (`/api/v1/auth`)

### Step 1.1: Register an Investigating Officer
- **Endpoint**: `POST /api/v1/auth/register`
- **Description**: Registers a new officer account with BCrypt password hashing and returns an initial JWT bearer token.

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

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "officer_vikram",
    "password": "Password@123",
    "fullName": "Inspector Vikram Rathore",
    "email": "vikram@police.gov.in",
    "badgeNumber": "DL-4509",
    "department": "Cyber Crime Cell",
    "role": "INVESTIGATING_OFFICER"
  }'
```
> 📌 **Note down**: Save `data.user.id` (e.g. `d3b07384-...`) as `OFFICER_ID` and `data.token` as `JWT_TOKEN`.

---

### Step 1.2: Register a Forensic Analyst
- **Endpoint**: `POST /api/v1/auth/register`

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

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "analyst_meera",
    "password": "Password@123",
    "fullName": "Dr. Meera Nair",
    "email": "meera.nair@fsl.gov.in",
    "badgeNumber": "FSL-9021",
    "department": "Digital Forensics Division",
    "role": "FORENSIC_ANALYST"
  }'
```
> 📌 **Note down**: Save `data.user.id` as `ANALYST_ID`.

---

### Step 1.3: User Login
- **Endpoint**: `POST /api/v1/auth/login`

**JSON Request Body**:
```json
{
  "username": "officer_vikram",
  "password": "Password@123"
}
```

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "officer_vikram",
    "password": "Password@123"
  }'
```

---

### Step 1.4: Authorize Swagger UI
1. Click the green **`Authorize` 🔒** button at the top-right of Swagger UI.
2. In the box, paste your JWT token string. (Swagger will automatically add `Bearer `).
3. Click **Authorize** and then **Close**.

---

### Step 1.5: Verify Current Profile
- **Endpoint**: `GET /api/v1/auth/me`

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

## 2. Case Docket Management (`/api/v1/cases`)

### Step 2.1: Create a Case
- **Endpoint**: `POST /api/v1/cases`
- **Description**: Initializes a formal criminal investigation docket.

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

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/cases \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "caseNumber": "FIR-2026-DELHI-101",
    "title": "Operation Cyber Vault",
    "description": "Ransomware attack and data exfiltration targeting regional infrastructure",
    "jurisdiction": "Delhi NCT Cyber Crime Cell",
    "leadOfficerId": "<PASTE_OFFICER_ID>",
    "assignedOfficerIds": ["<PASTE_OFFICER_ID>", "<PASTE_ANALYST_ID>"]
  }'
```
> 📌 **Note down**: Save `data.id` as `CASE_ID`.

---

### Step 2.2: Get Case Details by ID
- **Endpoint**: `GET /api/v1/cases/{id}`

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/cases/<CASE_ID> \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

### Step 2.3: Update Case Status
- **Endpoint**: `PATCH /api/v1/cases/{id}/status`

**JSON Request Body**:
```json
{
  "status": "UNDER_INVESTIGATION",
  "reason": "Evidence seized from suspect gateway server and logged into vault"
}
```

**cURL Command**:
```bash
curl -X PATCH http://localhost:8080/api/v1/cases/<CASE_ID>/status \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "status": "UNDER_INVESTIGATION",
    "reason": "Evidence seized from suspect gateway server and logged into vault"
  }'
```

---

## 3. Evidence Intake & Cryptographic Storage (`/api/v1/evidence`)

### Step 3.1: Upload and Cryptographically Seal Evidence
- **Endpoint**: `POST /api/v1/evidence/upload`
- **Content-Type**: `multipart/form-data`

**Form Part 1 (`metadata` — application/json)**:
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

**Form Part 2 (`file`)**: Attach raw binary file (e.g. `access_logs.txt`).

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/evidence/upload \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -F 'metadata={
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
  };type=application/json' \
  -F 'file=@/path/to/your/sample_file.txt'
```
> 📌 **Note down**: Save `data.id` as `EVIDENCE_ID`.

---

### Step 3.2: Verify On-Disk Physical Integrity
- **Endpoint**: `POST /api/v1/evidence/{id}/verify-integrity`
- **Description**: Re-reads raw storage file, recomputes SHA-256 hash, and compares against ingestion hash snapshot.

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/evidence/<EVIDENCE_ID>/verify-integrity \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

## 4. Chain of Custody & Audit Ledger (`/api/v1/evidence`, `/api/v1/ledger`)

### Step 4.1: Transfer Evidence Custody
- **Endpoint**: `POST /api/v1/evidence/{evidenceId}/custody-transfer`

**JSON Request Body**:
```json
{
  "toPartyId": "<ANALYST_ID>",
  "reason": "Handover to digital forensics lab for log analysis and IP extraction"
}
```

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/evidence/<EVIDENCE_ID>/custody-transfer \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "toPartyId": "<ANALYST_ID>",
    "reason": "Handover to digital forensics lab for log analysis and IP extraction"
  }'
```

---

### Step 4.2: Inspect Chain of Custody Timeline
- **Endpoint**: `GET /api/v1/evidence/{evidenceId}/custody`

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/evidence/<EVIDENCE_ID>/custody \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

### Step 4.3: Verify Whole-Ledger Cryptographic Chain Integrity
- **Endpoint**: `GET /api/v1/ledger/verify`
- **Description**: Traverses the append-only SHA-256 block chain from genesis to head, verifying row integrity and block linkages.

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/ledger/verify \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

## 5. Statutory BSA 2023 §63 Court Certification (`/api/v1/certificates`)

### Step 5.1: Issue Court Admissibility Certificate
- **Endpoint**: `POST /api/v1/certificates/evidence/{evidenceId}`

**JSON Request Body** *(Optional customization)*:
```json
{
  "mannerOfProduction": "Direct bit-stream image preservation from Dell PowerEdge server controller",
  "systemParticulars": "EnCase Forensic v23.4 running on Forensic Workstation FW-01",
  "certifyingOfficerRemarks": "Evidence seized in compliance with legal procedure and sealed immediately."
}
```

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/certificates/evidence/<EVIDENCE_ID> \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "mannerOfProduction": "Direct bit-stream image preservation from Dell PowerEdge server controller",
    "systemParticulars": "EnCase Forensic v23.4 running on Forensic Workstation FW-01",
    "certifyingOfficerRemarks": "Evidence seized in compliance with legal procedure and sealed immediately."
  }'
```
> 📌 **Note down**: Save `data.id` as `CERTIFICATE_ID`.

---

### Step 5.2: Download Court Certificate PDF
- **Endpoint**: `GET /api/v1/certificates/{id}/download`

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/certificates/<CERTIFICATE_ID>/download \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  --output BSA_Section63_Certificate.pdf
```

---

## 6. Legal Document Management & Versioning (`/api/v1/documents`)

### Step 6.1: File Initial Legal Document (v1)
- **Endpoint**: `POST /api/v1/documents`
- **Content-Type**: `multipart/form-data`

**Form Part 1 (`metadata` — application/json)**:
```json
{
  "caseId": "<CASE_ID>",
  "title": "First Information Report (FIR)",
  "documentType": "FIR",
  "changeSummary": "Initial FIR filed by Cyber Crime Cell"
}
```

**Form Part 2 (`file`)**: Attach `FIR_Document.pdf`.

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/documents \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -F 'metadata={
    "caseId": "<CASE_ID>",
    "title": "First Information Report (FIR)",
    "documentType": "FIR",
    "changeSummary": "Initial FIR filed by Cyber Crime Cell"
  };type=application/json' \
  -F 'file=@/path/to/fir_v1.pdf'
```
> 📌 **Note down**: Save `data.id` as `DOCUMENT_ID`.

---

### Step 6.2: Upload Amended Version (v2)
- **Endpoint**: `POST /api/v1/documents/{id}/versions`

**Form Part 1 (`metadata` — application/json)**:
```json
{
  "changeSummary": "Added supplementary suspect statements and forensic lab references"
}
```

**Form Part 2 (`file`)**: Attach `FIR_Document_v2.pdf`.

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/documents/<DOCUMENT_ID>/versions \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -F 'metadata={
    "changeSummary": "Added supplementary suspect statements and forensic lab references"
  };type=application/json' \
  -F 'file=@/path/to/fir_v2.pdf'
```

---

### Step 6.3: View Full Revision History
- **Endpoint**: `GET /api/v1/documents/{id}/versions`

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/documents/<DOCUMENT_ID>/versions \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

---

### Step 6.4: Download Versioned Document File
- **Endpoint**: `GET /api/v1/documents/{id}/download?version=1`

**cURL Command**:
```bash
curl -X GET "http://localhost:8080/api/v1/documents/<DOCUMENT_ID>/download?version=1" \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  --output FIR_v1_downloaded.pdf
```

---

## 7. AI/ML Evidence Analysis Subsystem (`/api/v1/evidence/{evidenceId}/*`)

### Step 7.1: Trigger AI Evidence Analysis
- **Endpoint**: `POST /api/v1/evidence/{evidenceId}/analyze`
- **Description**: Asynchronously dispatches the evidence item to the external Python FastAPI ML microservice for OCR extraction, PII scanning, auto-classification, and pHash calculation. If the ML service is unreachable, automatically uses built-in mock fallback.
- **Permitted Roles**: `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `SYSTEM_ADMIN`.

**JSON Request Body** *(Optional)*:
```json
{
  "forceReanalysis": true
}
```

**cURL Command**:
```bash
curl -X POST http://localhost:8080/api/v1/evidence/<EVIDENCE_ID>/analyze \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "forceReanalysis": true
  }'
```

---

### Step 7.2: Retrieve AI Analysis Results & Metadata
- **Endpoint**: `GET /api/v1/evidence/{evidenceId}/analysis`
- **Description**: Retrieves the completed AI analysis record including OCR text, detected PII (e.g., Aadhaar/PAN), auto-tags, pHash, and ML confidence score.
- **Permitted Roles**: `INVESTIGATING_OFFICER`, `FORENSIC_ANALYST`, `PROSECUTOR`, `COURT`, `SYSTEM_ADMIN`.

**cURL Command**:
```bash
curl -X GET http://localhost:8080/api/v1/evidence/<EVIDENCE_ID>/analysis \
  -H "Authorization: Bearer <YOUR_JWT_TOKEN>"
```

