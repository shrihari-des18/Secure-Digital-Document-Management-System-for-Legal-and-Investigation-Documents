'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { authApi } from '@/lib/api/auth';
import { Sidebar } from '@/components/sidebar';
import { Topbar } from '@/components/topbar';
import { Loader2 } from 'lucide-react';

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { isAuthenticated, token, setUser, logout } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // Validate token on mount by calling /auth/me
  useEffect(() => {
    async function validateSession() {
      if (!isAuthenticated || !token) {
        router.replace('/login');
        return;
      }

      try {
        const user = await authApi.getMe();
        setUser(user);
      } catch {
        // Token invalid/expired — force re-login
        logout();
        router.replace('/login');
        return;
      }

      setLoading(false);
    }

    validateSession();
  }, [isAuthenticated, token, router, setUser, logout]);

  // Listen for sidebar collapse events via CSS variable
  useEffect(() => {
    const handleResize = () => {
      const sidebar = document.querySelector('aside');
      if (sidebar) {
        setSidebarCollapsed(sidebar.clientWidth < 100);
      }
    };

    const observer = new MutationObserver(handleResize);
    const sidebar = document.querySelector('aside');
    if (sidebar) {
      observer.observe(sidebar, { attributes: true, attributeFilter: ['class'] });
      handleResize();
    }

    return () => observer.disconnect();
  }, [loading]);

  if (loading) {
    return (
      <div className="min-h-screen bg-zinc-950 flex items-center justify-center">
        <div className="flex flex-col items-center gap-3">
          <Loader2 className="w-8 h-8 text-zinc-100 animate-spin" />
          <p className="text-sm text-zinc-400">Validating session...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 antialiased font-sans">
      <Sidebar />
      <div
        className="transition-all duration-200 ease-in-out bg-zinc-950"
        style={{ marginLeft: sidebarCollapsed ? '68px' : '250px' }}
      >
        <Topbar />
        <main className="p-6 bg-zinc-950">
          {children}
        </main>
      </div>
    </div>
  );
}
