import { apiClient } from '@/lib/api-client';
import type { AiAnalysisResponse, TriggerAnalysisRequest } from '@/types';

export const aiApi = {
  /**
   * POST /api/v1/evidence/{evidenceId}/analyze
   * Dispatches evidence item to AI/ML microservice pipeline for OCR, PII detection, and classification.
   */
  trigger(evidenceId: string, request?: TriggerAnalysisRequest): Promise<AiAnalysisResponse> {
    const payload: TriggerAnalysisRequest = {
      extractOcr: true,
      detectPii: true,
      generateTags: true,
      ...request,
    };
    return apiClient.post<AiAnalysisResponse>(`/evidence/${evidenceId}/analyze`, payload);
  },

  /**
   * GET /api/v1/evidence/{evidenceId}/analysis
   * Retrieves extracted OCR text, detected PII entities, classification tags, perceptual hashes, and confidence score.
   */
  getResults(evidenceId: string): Promise<AiAnalysisResponse> {
    return apiClient.get<AiAnalysisResponse>(`/evidence/${evidenceId}/analysis`);
  },
};
