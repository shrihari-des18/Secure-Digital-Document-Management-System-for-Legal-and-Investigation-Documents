import { apiClient } from '@/lib/api-client';
import type { CustodyEventResponse, CustodyTransferRequest } from '@/types';

export const custodyApi = {
  /**
   * POST /api/v1/evidence/{evidenceId}/custody-transfer
   * Transfer physical/digital custody of evidence to another party with mandatory reason.
   */
  transfer(evidenceId: string, data: CustodyTransferRequest): Promise<CustodyEventResponse> {
    return apiClient.post<CustodyEventResponse>(`/evidence/${evidenceId}/custody-transfer`, data);
  },

  /**
   * GET /api/v1/evidence/{evidenceId}/custody
   * Retrieve chronological, digitally signed chain-of-custody timeline.
   */
  getTimeline(evidenceId: string): Promise<CustodyEventResponse[]> {
    return apiClient.get<CustodyEventResponse[]>(`/evidence/${evidenceId}/custody`);
  },
};
