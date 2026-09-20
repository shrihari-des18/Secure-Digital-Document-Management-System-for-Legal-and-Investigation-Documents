import { apiClient } from '@/lib/api-client';
import type { CertificateResponse, IssueCertificateRequest } from '@/types';

export const certificatesApi = {
  /**
   * POST /api/v1/certificates/evidence/{evidenceId}
   * Issues statutory BSA 2023 §63 court certificate with digital signature.
   */
  issue(evidenceId: string, request?: IssueCertificateRequest): Promise<CertificateResponse> {
    return apiClient.post<CertificateResponse>(`/certificates/evidence/${evidenceId}`, request || {});
  },

  /**
   * GET /api/v1/certificates/{id}
   * Retrieves certificate metadata and cryptographic hashes.
   */
  getById(id: string): Promise<CertificateResponse> {
    return apiClient.get<CertificateResponse>(`/certificates/${id}`);
  },

  /**
   * GET /api/v1/certificates/evidence/{evidenceId}
   * Lists all certificates issued for an evidence item.
   */
  getByEvidenceId(evidenceId: string): Promise<CertificateResponse[]> {
    return apiClient.get<CertificateResponse[]>(`/certificates/evidence/${evidenceId}`);
  },

  /**
   * GET /api/v1/certificates/{id}/download
   * Streams the digitally signed court-ready BSA §63 certificate PDF.
   */
  download(id: string, filename = 'BSA_Section63_Certificate.pdf'): Promise<void> {
    return apiClient.downloadFile(`/certificates/${id}/download`, filename);
  },
};
