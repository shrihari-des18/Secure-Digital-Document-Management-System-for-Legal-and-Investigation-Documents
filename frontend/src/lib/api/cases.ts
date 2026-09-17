import { apiClient } from '@/lib/api-client';
import type {
  CaseResponse,
  CaseStatus,
  CreateCaseRequest,
  UpdateCaseRequest,
  UpdateCaseStatusRequest,
} from '@/types';

export const casesApi = {
  /**
   * GET /api/v1/cases
   * List all cases, optionally filtered by status.
   */
  getAll(status?: CaseStatus): Promise<CaseResponse[]> {
    const query = status ? `?status=${status}` : '';
    return apiClient.get<CaseResponse[]>(`/cases${query}`);
  },

  /**
   * GET /api/v1/cases/{id}
   * Get case details by primary UUID.
   */
  getById(id: string): Promise<CaseResponse> {
    return apiClient.get<CaseResponse>(`/cases/${id}`);
  },

  /**
   * GET /api/v1/cases/number/{caseNumber}
   * Get case by case/FIR number.
   */
  getByNumber(caseNumber: string): Promise<CaseResponse> {
    return apiClient.get<CaseResponse>(`/cases/number/${encodeURIComponent(caseNumber)}`);
  },

  /**
   * POST /api/v1/cases
   * Initialize a new criminal or legal investigation case record.
   */
  create(data: CreateCaseRequest): Promise<CaseResponse> {
    return apiClient.post<CaseResponse>('/cases', data);
  },

  /**
   * PUT /api/v1/cases/{id}
   * Update case metadata (title, description, jurisdiction, assigned officers).
   */
  update(id: string, data: UpdateCaseRequest): Promise<CaseResponse> {
    return apiClient.put<CaseResponse>(`/cases/${id}`, data);
  },

  /**
   * PATCH /api/v1/cases/{id}/status
   * Update case lifecycle status (OPEN -> UNDER_INVESTIGATION -> IN_COURT -> CLOSED -> ARCHIVED).
   */
  updateStatus(id: string, status: CaseStatus, reason?: string): Promise<CaseResponse> {
    const payload: UpdateCaseStatusRequest = { status, reason };
    return apiClient.patch<CaseResponse>(`/cases/${id}/status`, payload);
  },
};
