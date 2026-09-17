import type { ApiResponse } from '@/types';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

// ============================================================
// Custom error for API failures
// ============================================================

export class ApiError extends Error {
  status: number;
  detail: string | null;

  constructor(status: number, message: string, detail: string | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.detail = detail;
  }
}

// ============================================================
// Token storage helpers (client-side only)
// ============================================================

export function getStoredToken(): string | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem('evault-auth');
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    return parsed?.state?.token ?? null;
  } catch {
    return null;
  }
}

// ============================================================
// Core fetch wrapper
// ============================================================

interface RequestOptions extends Omit<RequestInit, 'body'> {
  body?: unknown;
  skipAuth?: boolean;
  rawResponse?: boolean;
}

async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const { body, skipAuth = false, rawResponse = false, headers: customHeaders, ...rest } = options;

  const headers: HeadersInit = {
    ...(customHeaders as Record<string, string>),
  };

  // Only set Content-Type for JSON bodies (not FormData/multipart)
  if (body && !(body instanceof FormData)) {
    (headers as Record<string, string>)['Content-Type'] = 'application/json';
  }

  // Attach JWT token
  if (!skipAuth) {
    const token = getStoredToken();
    if (token) {
      (headers as Record<string, string>)['Authorization'] = `Bearer ${token}`;
    }
  }

  const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

  const response = await fetch(url, {
    headers,
    body: body instanceof FormData ? (body as BodyInit) : body ? JSON.stringify(body) : undefined,
    ...rest,
  });

  // Handle binary responses (PDF downloads, file streams)
  if (rawResponse) {
    return response as unknown as T;
  }

  // Handle 204 No Content
  if (response.status === 204) {
    return undefined as unknown as T;
  }

  // Parse JSON response
  const contentType = response.headers.get('content-type');

  if (!response.ok) {
    // Try to parse RFC 7807 problem+json or ApiResponse error
    if (contentType?.includes('application/json') || contentType?.includes('application/problem+json')) {
      const errorBody = await response.json();
      throw new ApiError(
        response.status,
        errorBody.message || errorBody.title || `HTTP ${response.status}`,
        errorBody.detail || null
      );
    }
    throw new ApiError(response.status, `HTTP ${response.status}: ${response.statusText}`);
  }

  if (!contentType?.includes('application/json')) {
    return undefined as unknown as T;
  }

  const json: ApiResponse<T> = await response.json();

  // Unwrap ApiResponse<T> envelope — return .data directly
  if (json && typeof json === 'object' && 'success' in json && 'data' in json) {
    if (!json.success) {
      throw new ApiError(response.status, json.message || 'Operation failed');
    }
    return json.data;
  }

  // If response isn't wrapped in ApiResponse, return as-is
  return json as unknown as T;
}

// ============================================================
// HTTP method shortcuts
// ============================================================

export const apiClient = {
  get<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'GET' });
  },

  post<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'POST', body });
  },

  put<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'PUT', body });
  },

  patch<T>(endpoint: string, body?: unknown, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'PATCH', body });
  },

  delete<T>(endpoint: string, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'DELETE' });
  },

  /** For multipart/form-data uploads (evidence, documents) */
  upload<T>(endpoint: string, formData: FormData, options?: RequestOptions): Promise<T> {
    return request<T>(endpoint, { ...options, method: 'POST', body: formData });
  },

  /** Direct authenticated download trigger */
  async downloadFile(endpoint: string, defaultFilename = 'download'): Promise<void> {
    const response = await request<Response>(endpoint, { rawResponse: true, method: 'GET' });
    if (!response.ok) {
      throw new ApiError(response.status, `Download failed: ${response.statusText}`);
    }
    const blob = await response.blob();
    const disposition = response.headers.get('content-disposition');
    let filename = defaultFilename;
    if (disposition && disposition.includes('filename=')) {
      const match = disposition.match(/filename="?([^"]+)"?/);
      if (match && match[1]) {
        filename = match[1];
      }
    }
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(blobUrl);
  },
};
