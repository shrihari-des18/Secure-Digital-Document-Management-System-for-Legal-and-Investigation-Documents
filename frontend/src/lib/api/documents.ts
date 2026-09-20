import { apiClient } from '@/lib/api-client';
import type {
  AddDocumentVersionRequest,
  CreateLegalDocumentRequest,
  DocumentVersionResponse,
  LegalDocumentResponse,
} from '@/types';

export const documentsApi = {
  /**
   * POST /api/v1/documents
   * Create new legal document record and upload v1 file.
   */
  async create(metadata: CreateLegalDocumentRequest, file: File): Promise<LegalDocumentResponse> {
    const formData = new FormData();
    const metadataBlob = new Blob([JSON.stringify(metadata)], { type: 'application/json' });
    formData.append('metadata', metadataBlob);
    formData.append('file', file);

    return apiClient.upload<LegalDocumentResponse>('/documents', formData);
  },

  /**
   * POST /api/v1/documents/{id}/versions
   * Upload amended document revision (v2+).
   */
  async addVersion(
    documentId: string,
    metadata: AddDocumentVersionRequest,
    file: File
  ): Promise<DocumentVersionResponse> {
    const formData = new FormData();
    const metadataBlob = new Blob([JSON.stringify(metadata)], { type: 'application/json' });
    formData.append('metadata', metadataBlob);
    formData.append('file', file);

    return apiClient.upload<DocumentVersionResponse>(`/documents/${documentId}/versions`, formData);
  },

  /**
   * GET /api/v1/documents/{id}
   * Get legal document metadata and latest version info.
   */
  getById(id: string): Promise<LegalDocumentResponse> {
    return apiClient.get<LegalDocumentResponse>(`/documents/${id}`);
  },

  /**
   * GET /api/v1/documents/case/{caseId}
   * List all legal documents filed for a case.
   */
  getByCaseId(caseId: string): Promise<LegalDocumentResponse[]> {
    return apiClient.get<LegalDocumentResponse[]>(`/documents/case/${caseId}`);
  },

  /**
   * GET /api/v1/documents/{id}/versions
   * Get full revision history and SHA-256 hashes of all versions.
   */
  getHistory(id: string): Promise<DocumentVersionResponse[]> {
    return apiClient.get<DocumentVersionResponse[]>(`/documents/${id}/versions`);
  },

  /**
   * GET /api/v1/documents/{id}/download
   * Download specific or latest document version file.
   */
  download(id: string, version?: number, filename = 'document.pdf'): Promise<void> {
    const query = version ? `?version=${version}` : '';
    return apiClient.downloadFile(`/documents/${id}/download${query}`, filename);
  },
};
