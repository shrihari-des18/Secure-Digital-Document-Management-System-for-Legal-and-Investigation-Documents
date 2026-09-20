import { apiClient } from '@/lib/api-client';
import type { CreateUserRequest, UserResponse } from '@/types';

export const usersApi = {
  /**
   * GET /api/v1/users
   * List all system users.
   */
  getAll(): Promise<UserResponse[]> {
    return apiClient.get<UserResponse[]>('/users');
  },

  /**
   * GET /api/v1/users/{id}
   * Get user details by UUID.
   */
  getById(id: string): Promise<UserResponse> {
    return apiClient.get<UserResponse>(`/users/${id}`);
  },

  /**
   * POST /api/v1/users
   * Create / register a new user in the system.
   */
  create(data: CreateUserRequest): Promise<UserResponse> {
    return apiClient.post<UserResponse>('/users', data);
  },
};
