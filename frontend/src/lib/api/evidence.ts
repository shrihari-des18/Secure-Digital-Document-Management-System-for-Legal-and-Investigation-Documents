import { apiClient } from '@/lib/api-client';
import type {
  EvidenceDetailResponse,
  IntegrityVerificationResponse,
  UploadEvidenceRequest,
} from '@/types';

export const evidenceApi = {
  /**
   * POST /api/v1/evidence/upload
   * Upload and cryptographically seal digital evidence file with SHA-256/512 and digital signature.
   */
  async upload(metadata: UploadEvidenceRequest, file: File): Promise<EvidenceDetailResponse> {
    const formData = new FormData();
    const metadataBlob = new Blob([JSON.stringify(metadata)], { type: 'application/json' });
    formData.append('metadata', metadataBlob);
    formData.append('file', file);

    return apiClient.upload<EvidenceDetailResponse>('/evidence/upload', formData);
  },

  /**
   * GET /api/v1/evidence/{id}
   * Get evidence details, file hashes, and custody status by primary UUID.
   */
  getById(id: string): Promise<EvidenceDetailResponse> {
    return apiClient.get<EvidenceDetailResponse>(`/evidence/${id}`);
  },

  /**
   * GET /api/v1/evidence/case/{caseId}
   * List all digital evidence items belonging to an investigation case.
   */
  getByCaseId(caseId: string): Promise<EvidenceDetailResponse[]> {
    return apiClient.get<EvidenceDetailResponse[]>(`/evidence/case/${caseId}`);
  },

  /**
   * GET /api/v1/evidence/{id}/download
   * Download the sealed evidence binary file.
   */
  download(id: string, defaultFilename = 'evidence_file'): Promise<void> {
    return apiClient.downloadFile(`/evidence/${id}/download`, defaultFilename);
  },

  /**
   * POST /api/v1/evidence/{id}/verify-integrity
   * Recomputes raw storage SHA-256 and compares to sealed baseline.
   */
  verifyIntegrity(id: string): Promise<IntegrityVerificationResponse> {
    return apiClient.post<IntegrityVerificationResponse>(`/evidence/${id}/verify-integrity`);
  },
};
