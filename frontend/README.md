# E-Vault Frontend Setup & Architecture

## Overview
The E-Vault frontend is the secure, client-side interface for digital evidence management. It is designed to allow law enforcement officers, investigators, and court personnel to manage cases, verify cryptographically sealed evidence, trace chains of custody, and issue statutory certificates. Rather than using mocked data, the frontend is directly wired to the real E-Vault Spring Boot backend API, ensuring all interactionsâ€”from appending to the ledger to uploading evidenceâ€”are persisted securely in the database.

## Tech Stack Summary
- **Framework**: Next.js 16.3.5 (App Router)
- **Styling**: Tailwind CSS v4
- **State Management (Client)**: Zustand (used primarily for authentication state and RBAC)
- **Data Fetching (Server State)**: TanStack Query (React Query) for caching and asynchronous requests to the Spring Boot API
- **Icons**: Lucide React
- **Forms**: React Hook Form with Zod for robust validation

## Design System
The frontend adheres to a strict "Zinc Dark Theme" ensuring a professional, sleek, and high-contrast environment suitable for forensic analysis and long hours of review.
- **Canvas & Background**: `bg-zinc-950` serves as the true dark gray base.
- **Typography**: Clean, sans-serif fonts with `text-zinc-100` for primary headings and `text-zinc-400` for secondary data.
- **Semantic Status Colors**:
  - **Emerald (`text-emerald-400`)**: Safe, verified, intact chain, or sealed evidence.
  - **Red (`text-red-400`)**: Risk, spoofed files, integrity violations, or broken chain of custody.
  - **Amber (`text-amber-400`)**: Warnings, pending states, or unverified items.
  - **Blue (`text-blue-400`)**: System indicators, informational highlights.

## Folder Structure
Inside the `src/` directory, the code is structured as follows:
- **`app/`**: Contains the Next.js App Router definitions.
  - **`(dashboard)/`**: All authenticated routes (e.g., cases, evidence, admin).
  - **`login/` & `register/`**: Public authentication routes.
- **`components/`**: Modular, reusable UI building blocks.
  - **`ui/`**: Core shared components like badges, modals, and tabs.
  - **`evidence/` & `cases/`**: Feature-specific complex components (e.g., Custody Timeline, AI Analysis Panel).
- **`lib/`**: Core utilities.
  - **`api/`**: Fetch/Axios wrappers interacting with the Spring Boot backend.
  - **`crypto.ts`**: Client-side cryptography tools (SHA-256 hashing, formatting).
- **`stores/`**: Zustand global stores (e.g., `auth-store.ts`).
- **`types/`**: TypeScript interfaces mapping to the backend DTOs.

## Page-by-Page Breakdown

### Dashboard (`/dashboard`)
- **Purpose**: The central command hub giving officers a bird's-eye view of recent case activities, flagged tampered evidence, and quick links to operational workbenches.
- **Endpoints**: Calls `casesApi.getAll()`, and aggregates evidence and custody data.
- **Key Components**: Features the Cryptographic Verification Banner (showing if the ledger is intact) and quick-action metrics.
- **RBAC**: Accessible to all logged-in roles.

### Cases Registry (`/cases` & `/cases/[id]`)
- **Purpose**: Where officers create, list, and dive into specific investigation dockets. The detail view shows the case's metadata and lists all associated evidence items.
- **Endpoints**: `casesApi.getAll()`, `casesApi.getById()`, `casesApi.create()`.
- **Key Components**: Uses `<details>` wrappers for progressive disclosure of case metadata and **CourtBundleModal** for compiling case dockets.
- **RBAC**: Lead Investigating Officers have write access; others have read-only access depending on assignment.

### Digital Evidence Vault (/evidence & /evidence/[id])
- **Purpose**: The core repository where an officer views sealed evidence files, verifies physical and digital integrity, and manages hardware specifics.
- **Endpoints**: evidenceApi.getByCaseId(), evidenceApi.getById(), evidenceApi.upload().
- **Key Components**: 
  - **Integrity Seal Card**: Visually indicates if the SHA-256 hash matches the server's record.
  - **Sandboxed Document Canvas**: A secure, blob-based PDF viewer utilizing HTML5 Canvas and pdfjs-dist to prevent right-click and selection, featuring a dynamic diagonal watermark of the logged-in user's name, ID, and live timestamp.
- **RBAC**: Uploads restricted to Forensic Analysts and Investigating Officers.

### Chain of Custody (`/custody`)
- **Purpose**: An immutable tracking view displaying how evidence moved from seizure to the forensic lab to the courtroom.
- **Endpoints**: `evidenceApi.getHistory()`, `evidenceApi.transfer()`.
- **Key Components**: **CustodyTimelineView** which visually maps each transfer event, the actor involved, and the timestamp.
- **RBAC**: Transfers can only be initiated by the current custodian of the evidence.

### Audit Ledger (`/ledger`)
- **Purpose**: A deep-dive into the raw, append-only cryptographic hash chain that underpins the entire E-Vault system. Used for strict compliance auditing.
- **Endpoints**: `ledgerApi.getEntries()`, `ledgerApi.verifyChain()`.
- **Key Components**: The **Verification Banner** (indicating chain validity) and the **Ledger Table**.
- **RBAC**: Primarily for Administrators and Auditors.

### AI Forensics Engine (`/ai-analysis`)
- **Purpose**: Where forensic teams run asynchronous neural pipelines for OCR text extraction, PII entity discovery, and auto-classification on evidence.
- **Endpoints**: `aiApi.getResults()`.
- **Key Components**: **AiAnalysisPanel** (showing pipeline status) and **PiiRedactorWorkbench** (for scrubbing sensitive data before court submission).
- **RBAC**: Restricted to Forensic Analysts and Lead Officers.

### Statutory Certificates (`/certificates`)
- **Purpose**: Where officers generate, view, and download court-admissible PDF certificates (e.g., BSA 2023 Â§63).
- **Endpoints**: `certificatesApi.getByEvidenceId()`, `certificatesApi.issue()`, `certificatesApi.download()`.
- **Key Components**: **BsaCertificatePreviewModal** which displays the statutory schedule and hash snapshots before final generation.
- **RBAC**: Only Lead Investigating Officers and Administrators can issue certificates.

### Versioned Legal Documents (`/documents`)
- **Purpose**: Immutable version control repository for FIRs, charge sheets, and legal filings.
- **Endpoints**: `documentsApi.getByCaseId()`, `documentsApi.create()`, `documentsApi.addVersion()`.
- **Key Components**: Revision History Drawer.
- **RBAC**: Lead Investigating Officers and Prosecutors.

### Reporting & Exports (`/reports`)
- **Purpose**: Generates formal CSV manifests of case dockets, evidence registries, and system actors for external review.
- **Endpoints**: Calls aggregation across `casesApi`, `evidenceApi`, and `usersApi`.
- **Key Components**: Specialized export cards triggering Blob downloads.
- **RBAC**: Typically restricted to Administrators and high-level Prosecutors.

### Admin Setup (/admin)
- **Purpose**: Role-based access control directory where administrators onboard new system actors and review cryptographic security posture.
- **Endpoints**: usersApi.getAll(), usersApi.create().
- **Key Components**: The **Security Specs Grid** detailing JWT, RBAC, and HMAC-SHA256 usage, alongside the user directory grid.
- **RBAC**: Strictly limited to Administrators.

### Hybrid Search (/search)
- **Purpose**: Centralized full-text and semantic search engine indexing cases, evidence metadata, and legal documents.
- **Endpoints**: searchApi.search().
- **Key Components**: Interactive filter chips for ALL, CASE, EVIDENCE, and DOCUMENT types with paginated, high-density result cards.
- **RBAC**: Accessible to all logged-in roles.

### Security Alarms (/alarms)
- **Purpose**: Real-time STOMP WebSocket subscriber page displaying critical integrity violations, unauthorized access attempts, and WORM storage alerts.
- **Endpoints**: larmsApi.listAlarms(), larmsApi.acknowledgeAlarm().
- **Key Components**: A live feed of categorized alerts (Critical, High, Medium, Info) with unacknowledged alarm filtering and notification badges.
- **RBAC**: Restricted to System Admin and Auditor roles.

## Key Shared Components
- **Modal**: A highly reusable wrapper for forms and previews (e.g., Transfer Evidence, Issue Certificate) utilizing a translucent backdrop and animated transitions.
- **ClassificationBadge / DocumentTypeBadge / CustodyStateBadge**: Standardized, color-coded semantic pills used globally to represent status (e.g., TOP SECRET, FIR, IN_LAB).
- **HashVerifier / IntegrityCard**: A standardized UI block that checks a file's client-side hash against the server-side ledger hash to prove non-repudiation.
- **TrustedTimestampSection**: Renders an RFC 3161 cryptographically verifiable time-seal block with expandable JSON evidence, proving temporal non-repudiation.
- **useStompSubscription (Hook)**: A React hook that initiates a persistent WebSocket/STOMP connection using sockjs-client and @stomp/stompjs to push live data (e.g., Alarms, Case Activity) to the UI.

## Client-Side Crypto
The `lib/crypto.ts` file houses pure client-side cryptographic functions, most notably generating SHA-256 hashes using the browser's native `crypto.subtle` API. 
**Why is this important?** 
Hashing must happen in the browser *before* the file is uploaded. This ensures that the raw digital evidence is fingerprinted by the user's machine, proving that the evidence was not tampered with or corrupted during transit to the Spring Boot server. This client-side hash becomes the permanent cryptographic seal verified in the court of law.

