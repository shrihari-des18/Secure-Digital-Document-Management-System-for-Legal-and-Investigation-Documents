'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { LogOut, User, FileCheck } from 'lucide-react';
import { useAuthStore, ROLE_LABELS, ROLE_COLORS } from '@/stores/auth-store';
import { NotificationBell } from '@/components/notifications/notification-bell';
import { HashVerifierModal } from '@/components/tools/hash-verifier-modal';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import type { EvidenceDetailResponse } from '@/types';
import { useQuery } from '@tanstack/react-query';

export function Topbar() {
  const router = useRouter();
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);
  const [isVerifierOpen, setIsVerifierOpen] = useState(false);

  // Fetch all evidence for quick hash verifier
  const { data: cases = [] } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  const { data: allEvidence = [] } = useQuery({
    queryKey: ['all-evidence', cases.map((c) => c.id)],
    queryFn: async () => {
      if (cases.length === 0) return [];
      const promises = cases.map((c) =>
        evidenceApi.getByCaseId(c.id).catch(() => [] as EvidenceDetailResponse[])
      );
      const results = await Promise.all(promises);
      return results.flat();
    },
    enabled: cases.length > 0 && isVerifierOpen,
  });

  const handleLogout = () => {
    logout();
    router.push('/login');
  };

  const roleLabel = user?.role ? ROLE_LABELS[user.role] ?? user.role : 'Officer';
  const roleColor = user?.role ? ROLE_COLORS[user.role] ?? 'bg-zinc-900 text-zinc-300 border-zinc-800' : '';

  return (
    <>
      <header className="h-16 bg-zinc-950/80 backdrop-blur-md border-b border-zinc-800 flex items-center justify-between px-6 sticky top-0 z-30">
        {/* Left: System status indicator */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2 text-sm text-zinc-400">
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
            <span className="font-mono text-zinc-100 font-semibold hidden sm:inline">E-Vault Core</span>
            <span className="text-zinc-600 hidden sm:inline">•</span>
            <span className="text-emerald-400 font-mono text-xs font-medium">WORM Storage Active</span>
          </div>
        </div>

        {/* Right: Actions, Notifications, User info, Logout */}
        <div className="flex items-center gap-3">
          {/* Quick client-side WebCrypto hash verification tool */}
          <button
            onClick={() => setIsVerifierOpen(true)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-zinc-900 hover:bg-zinc-800 border border-zinc-800 text-xs text-zinc-300 hover:text-zinc-100 transition-colors"
            title="Verify any local file against vault SHA-256"
          >
            <FileCheck className="w-3.5 h-3.5 text-zinc-300" />
            <span className="hidden md:inline font-medium">Verify Hash Tool</span>
          </button>

          {/* Notification bell */}
          <NotificationBell />

          <div className="h-4 w-px bg-zinc-800 mx-1" />

          {/* User badge */}
          {user && (
            <div className="flex items-center gap-2.5">
              <div className="w-9 h-9 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-200 font-bold text-xs shadow-xs">
                {user.fullName ? user.fullName.slice(0, 2).toUpperCase() : <User className="w-4 h-4" />}
              </div>
              <div className="hidden sm:block text-left">
                <p className="text-sm font-semibold text-zinc-100 leading-tight">{user.fullName || user.username}</p>
                <span className={`inline-block text-xs px-2 py-0.5 rounded font-mono font-medium leading-tight mt-0.5 border ${roleColor}`}>
                  {roleLabel}
                </span>
              </div>
            </div>
          )}


          {/* Logout */}
          <button
            onClick={handleLogout}
            className="p-2 rounded-lg text-zinc-400 hover:text-red-400 hover:bg-red-500/10 transition-colors ml-1"
            title="Sign out"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Global Hash Verifier Modal */}
      <HashVerifierModal
        isOpen={isVerifierOpen}
        onClose={() => setIsVerifierOpen(false)}
        allEvidence={allEvidence}
      />
    </>
  );
}
