import { apiClient } from '@/lib/api-client';
import type { AuthResponse, LoginRequest, RegisterRequest, UserResponse } from '@/types';

export const authApi = {
  /**
   * POST /api/v1/auth/login
   * Exchange credentials for a JWT token.
   */
  login(data: LoginRequest): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>('/auth/login', data, { skipAuth: true });
  },

  /**
   * POST /api/v1/auth/register
   * Create a new user account and receive a JWT token.
   */
  register(data: RegisterRequest): Promise<AuthResponse> {
    return apiClient.post<AuthResponse>('/auth/register', data, { skipAuth: true });
  },

  /**
   * GET /api/v1/auth/me
   * Get the currently authenticated user's profile.
   */
  getMe(): Promise<UserResponse> {
    return apiClient.get<UserResponse>('/auth/me');
  },
};
