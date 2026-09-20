'use client';

import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import type { UserResponse } from '@/types';
import { RoleEnum } from '@/types';

interface AuthState {
  token: string | null;
  user: UserResponse | null;
  isAuthenticated: boolean;

  // Actions
  setAuth: (token: string, user: UserResponse) => void;
  setUser: (user: UserResponse) => void;
  logout: () => void;
  hasRole: (role: RoleEnum | RoleEnum[]) => boolean;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      token: null,
      user: null,
      isAuthenticated: false,

      setAuth: (token: string, user: UserResponse) => {
        set({ token, user, isAuthenticated: true });
      },

      setUser: (user: UserResponse) => {
        set({ user });
      },

      logout: () => {
        set({ token: null, user: null, isAuthenticated: false });
      },

      hasRole: (role: RoleEnum | RoleEnum[]) => {
        const { user } = get();
        if (!user) return false;
        if (Array.isArray(role)) {
          return role.includes(user.role);
        }
        return user.role === role;
      },
    }),
    {
      name: 'evault-auth',
      partialize: (state) => ({
        token: state.token,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);

// ============================================================
// Role display helpers
// ============================================================

export const ROLE_LABELS: Record<RoleEnum, string> = {
  [RoleEnum.SYSTEM_ADMIN]: 'System Admin',
  [RoleEnum.INVESTIGATING_OFFICER]: 'Investigating Officer',
  [RoleEnum.FORENSIC_ANALYST]: 'Forensic Analyst',
  [RoleEnum.PROSECUTOR]: 'Prosecutor',
  [RoleEnum.COURT]: 'Court',
  [RoleEnum.AUDITOR]: 'Auditor',
  [RoleEnum.RECORDS_CUSTODIAN]: 'Records Custodian',
};

export const ROLE_COLORS: Record<RoleEnum, string> = {
  [RoleEnum.SYSTEM_ADMIN]: 'bg-red-500/10 text-red-400 border-red-500/20',
  [RoleEnum.INVESTIGATING_OFFICER]: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  [RoleEnum.FORENSIC_ANALYST]: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  [RoleEnum.PROSECUTOR]: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  [RoleEnum.COURT]: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  [RoleEnum.AUDITOR]: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  [RoleEnum.RECORDS_CUSTODIAN]: 'bg-zinc-800/80 text-zinc-300 border-zinc-700',
};
