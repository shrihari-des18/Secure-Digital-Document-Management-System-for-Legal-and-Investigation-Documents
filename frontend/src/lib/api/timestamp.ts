import { apiClient } from '@/lib/api-client';
import type {
  TimestampResponse,
  TimestampVerifyResponse,
  StampHashRequest,
  VerifyTimestampRequest,
} from '@/types';

export const timestampApi = {
  /**
   * POST /api/v1/timestamp/stamp-hash
   * Submit a SHA-256 hash to the RFC 3161 TSA and receive a signed timestamp token.
   */
  stampHash(payload: StampHashRequest): Promise<TimestampResponse> {
    return apiClient.post<TimestampResponse>('/timestamp/stamp-hash', payload);
  },

  /**
   * POST /api/v1/timestamp/verify
   * Verify a Base64-encoded RFC 3161 Time-Stamp Token against an expected SHA-256 hash.
   */
  verifyTimestamp(payload: VerifyTimestampRequest): Promise<TimestampVerifyResponse> {
    return apiClient.post<TimestampVerifyResponse>('/timestamp/verify', payload);
  },

  /**
   * GET /api/v1/timestamp/evidence/{evidenceId}
   * Retrieve and verify the RFC 3161 timestamp for a sealed evidence item.
   */
  getEvidenceTimestamp(evidenceId: string): Promise<TimestampVerifyResponse> {
    return apiClient.get<TimestampVerifyResponse>(`/timestamp/evidence/${evidenceId}`);
  },

  /**
   * GET /api/v1/timestamp/certificate/{certificateId}
   * Retrieve and verify the RFC 3161 timestamp for a BSA section 63 certificate.
   */
  getCertificateTimestamp(certificateId: string): Promise<TimestampVerifyResponse> {
    return apiClient.get<TimestampVerifyResponse>(`/timestamp/certificate/${certificateId}`);
  },
};
