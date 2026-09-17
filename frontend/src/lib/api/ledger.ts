import { apiClient } from '@/lib/api-client';
import type { AuditLedgerResponse, LedgerVerificationResult } from '@/types';

export const ledgerApi = {
  /**
   * GET /api/v1/ledger/verify
   * Traverses and verifies SHA-256 block chain linkages from genesis to head.
   */
  verify(): Promise<LedgerVerificationResult> {
    return apiClient.get<LedgerVerificationResult>('/ledger/verify');
  },

  /**
   * GET /api/v1/ledger/entries
   * Retrieve all hash-chained transaction ledger records.
   */
  getAllEntries(): Promise<AuditLedgerResponse[]> {
    return apiClient.get<AuditLedgerResponse[]>('/ledger/entries');
  },
};
