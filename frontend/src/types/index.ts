// ============================================================
// Enums â€” mirror backend Java enums exactly
// ============================================================

export enum RoleEnum {
  SYSTEM_ADMIN = 'SYSTEM_ADMIN',
  INVESTIGATING_OFFICER = 'INVESTIGATING_OFFICER',
  FORENSIC_ANALYST = 'FORENSIC_ANALYST',
  PROSECUTOR = 'PROSECUTOR',
  COURT = 'COURT',
  AUDITOR = 'AUDITOR',
  RECORDS_CUSTODIAN = 'RECORDS_CUSTODIAN',
}

export enum CaseStatus {
  OPEN = 'OPEN',
  UNDER_INVESTIGATION = 'UNDER_INVESTIGATION',
  IN_COURT = 'IN_COURT',
  CLOSED = 'CLOSED',
  ARCHIVED = 'ARCHIVED',
}

export enum CustodyState {
  SEIZED = 'SEIZED',
  INGESTED = 'INGESTED',
  UNDER_ANALYSIS = 'UNDER_ANALYSIS',
  TRANSFERRED = 'TRANSFERRED',
  IN_COURT = 'IN_COURT',
  CERTIFIED = 'CERTIFIED',
  ARCHIVED = 'ARCHIVED',
  DISPOSED = 'DISPOSED',
}

export enum ClassificationLevel {
  PUBLIC = 'PUBLIC',
  RESTRICTED = 'RESTRICTED',
  CONFIDENTIAL = 'CONFIDENTIAL',
  SECRET = 'SECRET',
}

export enum CustodyEventType {
  INGEST = 'INGEST',
  TRANSFER = 'TRANSFER',
  VIEW = 'VIEW',
  DOWNLOAD = 'DOWNLOAD',
  ANALYZE = 'ANALYZE',
  CERTIFY = 'CERTIFY',
  DISPOSE = 'DISPOSE',
}

export enum AnalysisStatus {
  PENDING = 'PENDING',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
}

export enum LegalDocumentType {
  FIR = 'FIR',
  CASE_DIARY = 'CASE_DIARY',
  SEARCH_WARRANT = 'SEARCH_WARRANT',
  CHARGE_SHEET = 'CHARGE_SHEET',
  SEIZURE_MEMO = 'SEIZURE_MEMO',
  FORENSIC_REPORT = 'FORENSIC_REPORT',
  BAIL_APPLICATION = 'BAIL_APPLICATION',
  COURT_ORDER = 'COURT_ORDER',
}

export enum IntegrityStatus {
  VERIFIED = 'VERIFIED',
  FAILED = 'FAILED',
}

// ============================================================
// Backend API envelope â€” ApiResponse<T>
// ============================================================

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

// ============================================================
// Auth DTOs
// ============================================================

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
  fullName: string;
  email: string;
  badgeNumber?: string;
  department?: string;
  role: RoleEnum;
}

export interface UserResponse {
  id: string;
  username: string;
  fullName: string;
  email: string;
  badgeNumber: string | null;
  department: string | null;
  role: RoleEnum;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  expiresInMs: number;
  user: UserResponse;
}

// ============================================================
// Case DTOs
// ============================================================

export interface CreateCaseRequest {
  caseNumber: string;
  title: string;
  description?: string;
  jurisdiction: string;
  leadOfficerId: string;
  assignedOfficerIds?: string[];
}

export interface UpdateCaseRequest {
  title?: string;
  description?: string;
  jurisdiction?: string;
  assignedOfficerIds?: string[];
}

export interface UpdateCaseStatusRequest {
  status: CaseStatus;
  reason?: string;
}

export interface CaseResponse {
  id: string;
  caseNumber: string;
  title: string;
  description: string | null;
  jurisdiction: string;
  status: CaseStatus;
  leadOfficerId: string;
  assignedOfficerIds: string[];
  createdAt: string;
  updatedAt: string;
}

// ============================================================
// Evidence DTOs
// ============================================================

export interface UploadEvidenceRequest {
  caseId: string;
  evidenceNumber: string;
  title: string;
  description?: string;
  classification: ClassificationLevel;
  deviceMake?: string;
  deviceModel?: string;
  deviceSerialNumber?: string;
  seizedLocation?: string;
  seizedAt?: string;
  seizedByOfficerId?: string;
  clientSha256?: string;
}

export interface EvidenceFileResponse {
  fileId: string;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  sha512: string | null;
  clientSha256: string | null;
  integrityStatus: IntegrityStatus;
  digitalSignature: string;
  signingKeyId: string;
  createdAt: string;
}

export interface EvidenceDetailResponse {
  id: string;
  caseId: string;
  evidenceNumber: string;
  title: string;
  description: string | null;
  classification: ClassificationLevel;
  custodyState: CustodyState;
  deviceMake: string | null;
  deviceModel: string | null;
  deviceSerialNumber: string | null;
  seizedLocation: string | null;
  seizedAt: string | null;
  seizedByOfficerId: string | null;
  currentCustodianId: string | null;
  createdBy: string;
  file: EvidenceFileResponse;
  createdAt: string;
  updatedAt: string;
}

export interface IntegrityVerificationResponse {
  evidenceId: string;
  evidenceNumber: string;
  recordedSha256: string;
  calculatedSha256: string;
  match: boolean;
  integrityStatus: IntegrityStatus;
  digitalSignature?: string;
  message: string;
  verifiedAt: string;
}

// ============================================================
// Custody DTOs
// ============================================================

export interface CustodyTransferRequest {
  toPartyId: string;
  reason: string;
}

export interface CustodyEventResponse {
  id: string;
  evidenceId: string;
  eventType: CustodyEventType;
  actorId: string;
  fromParty: string | null;
  toParty: string | null;
  reason: string | null;
  occurredAt: string;
}

// ============================================================
// Audit Ledger DTOs
// ============================================================

export interface AuditLedgerResponse {
  seq: number;
  occurredAt: string;
  actorId: string | null;
  action: string;
  resourceRef: string;
  details: Record<string, unknown> | null;
  prevHash: string;
  entryHash: string;
}

export interface LedgerVerificationResult {
  valid: boolean;
  totalEntries: number;
  lastVerifiedSeq: number | null;
  headHash: string | null;
  brokenSeq: number | null;
  failureReason: string | null;
  message: string | null;
  verifiedAt: string;
}

// ============================================================
// Certificate DTOs
// ============================================================

export interface IssueCertificateRequest {
  mannerOfProduction?: string;
  certifyingOfficerNotes?: string;
}

export interface CertificateResponse {
  id: string;
  evidenceId: string;
  certificateNumber: string;
  standard: string;
  hashSnapshotSha256: string;
  hashSnapshotSha512: string | null;
  mannerOfProduction: string | null;
  deviceParticulars: string | null;
  issuedByOfficerId: string;
  issuedAt: string;
  digitalSignature: string;
  signingKeyId: string;
}

// ============================================================
// Legal Document DTOs
// ============================================================

export interface CreateLegalDocumentRequest {
  caseId: string;
  documentNumber: string;
  title: string;
  documentType: LegalDocumentType;
  description?: string;
  changeSummary?: string;
}

export interface AddDocumentVersionRequest {
  changeSummary: string;
}

export interface DocumentVersionResponse {
  id: string;
  documentId: string;
  versionNumber: number;
  originalFileName: string;
  contentType: string;
  sizeBytes: number;
  sha256: string;
  changeSummary: string | null;
  uploadedBy: string;
  digitalSignature: string;
  createdAt: string;
}

export interface LegalDocumentResponse {
  id: string;
  caseId: string;
  documentNumber: string;
  title: string;
  documentType: LegalDocumentType;
  description: string | null;
  currentVersion: number;
  createdBy: string;
  latestVersion: DocumentVersionResponse | null;
  createdAt: string;
  updatedAt: string;
}

// ============================================================
// AI Analysis DTOs
// ============================================================

export interface TriggerAnalysisRequest {
  extractOcr?: boolean;
  detectPii?: boolean;
  generateTags?: boolean;
  forceReanalysis?: boolean;
}

export interface AiAnalysisResponse {
  id: string;
  evidenceId: string;
  status: AnalysisStatus;
  ocrText: string | null;
  detectedPii: string | null;
  classificationTags: string | null;
  perceptualHash: string | null;
  confidenceScore: number | null;
  summary: string | null;
  failureReason: string | null;
  createdAt: string;
  updatedAt: string;
}

// ============================================================
// User DTOs
// ============================================================

export interface CreateUserRequest {
  username: string;
  password: string;
  fullName: string;
  email: string;
  badgeNumber?: string;
  department?: string;
  role: RoleEnum;
}

// ============================================================
// Search DTOs
// ============================================================

export enum SearchEntityType {
  ALL = 'ALL',
  CASE = 'CASE',
  EVIDENCE = 'EVIDENCE',
  DOCUMENT = 'DOCUMENT',
}

export interface SearchResultItem {
  id: string;
  entityType: 'CASE' | 'EVIDENCE' | 'DOCUMENT';
  title: string;
  snippet: string | null;
  score: number | null;
  metadata: Record<string, unknown> | null;
}

export interface SearchResponse {
  hits: SearchResultItem[];
  query: string;
  type?: string;
  page: number;
  size: number;
  totalHits: number;
  totalPages: number;
}

// ============================================================
// Security Alarm DTOs
// ============================================================

export enum AlarmSeverity {
  INFO = 'INFO',
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL',
}

export enum AlarmType {
  TAMPER_DETECTED = 'TAMPER_DETECTED',
  HASH_MISMATCH = 'HASH_MISMATCH',
  UNAUTHORIZED_ACCESS_ATTEMPT = 'UNAUTHORIZED_ACCESS_ATTEMPT',
  SEQUENCE_BREAK = 'SEQUENCE_BREAK',
  SUSPICIOUS_CUSTODY_TRANSFER = 'SUSPICIOUS_CUSTODY_TRANSFER',
  SECURITY_DRILL = 'SECURITY_DRILL',
}

export interface SecurityAlarm {
  id: string;
  alarmType: AlarmType | string;
  severity: AlarmSeverity;
  resourceRef: string | null;
  message: string;
  details: Record<string, unknown> | null;
  caseId: string | null;
  acknowledged: boolean;
  acknowledgedBy: string | null;
  acknowledgedAt: string | null;
  createdAt: string;
}

export interface TriggerTestAlarmRequest {
  alarmType: string;
  severity: string;
  resourceRef?: string;
  message: string;
  details?: Record<string, unknown>;
  caseId?: string;
}

// ============================================================
// RFC 3161 Timestamp DTOs
// ============================================================

export interface StampHashRequest {
  sha256: string;
  description?: string;
}

export interface VerifyTimestampRequest {
  tokenBase64: string;
  expectedSha256: string;
}

export interface TimestampResponse {
  tokenBase64: string;
  genTime: string;
  tsaName: string;
  serialNumber: string;
  policyOid: string;
  hashAlgorithm: string;
  imprintHash: string;
}

export interface TimestampVerifyResponse {
  valid: boolean;
  hashMatches: boolean;
  signatureVerified: boolean;
  genTime: string;
  tsaName: string;
  serialNumber: string;
  message: string;
}


