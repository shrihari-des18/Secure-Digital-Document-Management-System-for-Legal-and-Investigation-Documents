import { apiClient } from '@/lib/api-client';
import type { SearchResponse } from '@/types';

export interface SearchParams {
  query?: string;
  type?: 'ALL' | 'CASE' | 'EVIDENCE' | 'DOCUMENT';
  page?: number;
  size?: number;
}

export const searchApi = {
  /**
   * GET /api/v1/search
   * Hybrid full-text & semantic vector search across cases, evidence, and documents.
   */
  search(params: SearchParams = {}): Promise<SearchResponse> {
    const searchParams = new URLSearchParams();
    if (params.query) searchParams.set('query', params.query);
    if (params.type && params.type !== 'ALL') searchParams.set('type', params.type);
    if (params.page !== undefined) searchParams.set('page', params.page.toString());
    if (params.size !== undefined) searchParams.set('size', params.size.toString());
    const qs = searchParams.toString();
    return apiClient.get<SearchResponse>(`/search${qs ? `?${qs}` : ''}`);
  },
};
