'use client';

import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuthStore } from '@/stores/auth-store';
import { Shield } from 'lucide-react';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const isAuthenticated = useAuthStore((s) => s.isAuthenticated);

  useEffect(() => {
    if (isAuthenticated) {
      router.replace('/dashboard');
    }
  }, [isAuthenticated, router]);

  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100 antialiased font-sans flex items-center justify-center relative overflow-hidden p-4">
      <div className="relative z-10 w-full max-w-md">
        {/* Logo / Branding */}
        <div className="text-center mb-6 animate-fade-in">
          <div className="inline-flex items-center justify-center w-12 h-12 rounded-xl bg-zinc-100 text-zinc-950 mb-3 shadow-md">
            <Shield className="w-6 h-6" />
          </div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">E-Vault</h1>
          <p className="text-xs text-zinc-400 mt-1 font-mono">
            Digital Evidence Management • BSA 2023 §63
          </p>
        </div>

        {children}
      </div>
    </div>
  );
}
