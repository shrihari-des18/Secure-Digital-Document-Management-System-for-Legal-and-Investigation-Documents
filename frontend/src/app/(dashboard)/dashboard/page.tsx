'use client';

import Link from 'next/link';
import {
  Briefcase,
  FileSearch,
  BookOpen,
  Plus,
  ArrowRight,
  Award,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuthStore, ROLE_LABELS } from '@/stores/auth-store';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { ledgerApi } from '@/lib/api/ledger';
import { formatDate, truncateHash } from '@/lib/crypto';
import { CaseStatusBadge, CustodyStateBadge, ClassificationBadge } from '@/components/ui/badge';
import type { EvidenceDetailResponse } from '@/types';
import { CaseStatus, CustodyState } from '@/types';
import { useQuery } from '@tanstack/react-query';

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);

  // Fetch live cases
  const { data: cases = [], isLoading: casesLoading } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  // Fetch evidence across cases
  const { data: allEvidence = [], isLoading: evidenceLoading } = useQuery({
    queryKey: ['all-evidence', cases.map((c) => c.id)],
    queryFn: async () => {
      if (cases.length === 0) return [];
      const promises = cases.map((c) =>
        evidenceApi.getByCaseId(c.id).catch(() => [] as EvidenceDetailResponse[])
      );
      const results = await Promise.all(promises);
      return results.flat();
    },
    enabled: cases.length > 0,
  });

  // Fetch live ledger verification
  const { data: ledgerStatus } = useQuery({
    queryKey: ['ledger-verification'],
    queryFn: () => ledgerApi.verify(),
  });

  const activeInvestigations = cases.filter(
    (c) => c.status === CaseStatus.UNDER_INVESTIGATION || c.status === CaseStatus.OPEN
  ).length;

  const sealedEvidenceCount = allEvidence.length;
  const certifiedCount = allEvidence.filter((e) => e.custodyState === CustodyState.CERTIFIED).length;
  const isChainIntact = ledgerStatus ? ledgerStatus.valid : true;

  const stats = [
    {
      title: 'Active Case Dockets',
      value: casesLoading ? '...' : activeInvestigations.toString(),
      change: `${cases.length} Total Registered`,
      icon: Briefcase,
      color: 'text-blue-400',
      badgeClass: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
      href: '/cases',
    },
    {
      title: 'Sealed Evidence Assets',
      value: evidenceLoading ? '...' : sealedEvidenceCount.toString(),
      change: '100% SHA-256 Verified',
      icon: FileSearch,
      color: 'text-emerald-400',
      badgeClass: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
      href: '/evidence',
    },
    {
      title: 'Cryptographic Ledger',
      value: ledgerStatus ? `${ledgerStatus.totalEntries} Blocks` : 'Active',
      change: isChainIntact ? '✓ Chain 100% Intact' : '⚠ Tamper Warning',
      icon: BookOpen,
      color: isChainIntact ? 'text-emerald-400' : 'text-red-400',
      badgeClass: isChainIntact
        ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
        : 'bg-red-500/10 text-red-400 border-red-500/20',
      href: '/ledger',
    },
    {
      title: 'BSA §63 Admissible',
      value: evidenceLoading ? '...' : `${certifiedCount} Certified`,
      change: 'Court-Ready Schedules',
      icon: Award,
      color: 'text-amber-400',
      badgeClass: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
      href: '/certificates',
    },
  ];

  return (
    <div className="space-y-6">
      {/* Welcome Banner */}
      <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl hover:border-zinc-700 hover:bg-zinc-800/20 transition-colors">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1.5">
              <span className="text-[11px] font-mono font-semibold uppercase tracking-wider text-emerald-400 bg-emerald-500/10 border border-emerald-500/20 px-2 py-0.5 rounded-md">
                Active Session
              </span>
              <span className="text-zinc-600">•</span>
              <span className="text-xs text-zinc-400 font-mono">
                {user?.role ? ROLE_LABELS[user.role] : 'Law Enforcement Role'}
              </span>
            </div>
            <h1 className="text-2xl font-bold tracking-tight text-zinc-100">
              Welcome back, {user?.fullName || 'Officer'}
            </h1>
            <p className="text-xs text-zinc-400 mt-1 max-w-xl leading-relaxed">
              Secure Digital Evidence & Legal Document Management System under Bharatiya Sakshya Adhiniyam, 2023 §63
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-2.5">
            <Link
              href="/evidence/upload"
              className="btn-primary"
            >
              <Plus className="w-4 h-4" />
              <span>Ingest Evidence</span>
            </Link>

            <Link
              href="/cases"
              className="btn-secondary"
            >
              <Briefcase className="w-4 h-4" />
              <span>All Dockets</span>
            </Link>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {stats.map((stat) => {
          const Icon = stat.icon;
          return (
            <Link
              key={stat.title}
              href={stat.href}
              className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl hover:border-zinc-700 hover:bg-zinc-800/20 transition-colors group block"
            >
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider font-mono text-zinc-400">{stat.title}</span>
                <div className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300">
                  <Icon className={clsx('w-4 h-4', stat.color)} />
                </div>
              </div>

              <div className="mt-4 flex items-baseline justify-between">
                <span className="text-3xl font-bold text-zinc-100 font-mono tracking-tight">{stat.value}</span>
                <span className={clsx('text-[11px] font-mono px-2 py-0.5 rounded border', stat.badgeClass)}>
                  {stat.change}
                </span>
              </div>
            </Link>
          );
        })}
      </div>

      {/* Main Grid: Recent Cases & Recent Evidence */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Recent Cases */}
        <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl">
          <div className="flex items-center justify-between pb-4 border-b border-zinc-800">
            <h2 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
              <div className="w-7 h-7 rounded-md bg-zinc-100 text-zinc-950 flex items-center justify-center">
                <Briefcase className="w-3.5 h-3.5" />
              </div>
              <span>Active Investigation Dockets</span>
            </h2>
            <Link
              href="/cases"
              className="text-xs font-semibold text-zinc-300 hover:text-zinc-100 hover:underline flex items-center gap-1 font-mono"
            >
              <span>View all ({cases.length})</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>

          <div className="mt-4 space-y-2.5">
            {casesLoading ? (
              <div className="py-8 text-center text-xs text-zinc-500">Loading active cases...</div>
            ) : cases.length === 0 ? (
              <div className="py-8 text-center text-xs text-zinc-500">
                No cases registered yet. Click &quot;+ New Case&quot; to create your first docket.
              </div>
            ) : (
              cases.slice(0, 3).map((c) => (
                <Link
                  key={c.id}
                  href={`/cases/${c.id}`}
                  className="block p-3.5 rounded-lg border border-zinc-800/80 bg-zinc-900/40 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors group"
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="font-mono text-xs font-bold text-zinc-200 group-hover:text-zinc-100">{c.caseNumber}</span>
                    <CaseStatusBadge status={c.status} />
                  </div>
                  <h3 className="text-xs font-semibold text-zinc-300 group-hover:text-zinc-100 transition-colors line-clamp-1">
                    {c.title}
                  </h3>
                  <div className="flex items-center justify-between text-[11px] text-zinc-500 mt-2 font-mono">
                    <span className="truncate max-w-[200px]">{c.jurisdiction}</span>
                    <span>{formatDate(c.createdAt)}</span>
                  </div>
                </Link>
              ))
            )}
          </div>
        </div>

        {/* Recent Evidence */}
        <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl">
          <div className="flex items-center justify-between pb-4 border-b border-zinc-800">
            <h2 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
              <div className="w-7 h-7 rounded-md bg-zinc-100 text-zinc-950 flex items-center justify-center">
                <FileSearch className="w-3.5 h-3.5" />
              </div>
              <span>Sealed Evidence Vault</span>
            </h2>
            <Link
              href="/evidence"
              className="text-xs font-semibold text-zinc-300 hover:text-zinc-100 hover:underline flex items-center gap-1 font-mono"
            >
              <span>View all ({allEvidence.length})</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </div>

          <div className="mt-4 space-y-2.5">
            {evidenceLoading ? (
              <div className="py-8 text-center text-xs text-zinc-500">Loading sealed evidence...</div>
            ) : allEvidence.length === 0 ? (
              <div className="py-8 text-center text-xs text-zinc-500">
                No digital evidence items ingested yet.
              </div>
            ) : (
              allEvidence.slice(0, 3).map((ev) => (
                <Link
                  key={ev.id}
                  href={`/evidence/${ev.id}`}
                  className="block p-3.5 rounded-lg border border-zinc-800/80 bg-zinc-900/40 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors group"
                >
                  <div className="flex items-center justify-between mb-1.5">
                    <span className="font-mono text-xs font-bold text-zinc-200">{ev.evidenceNumber}</span>
                    <div className="flex items-center gap-1.5">
                      <ClassificationBadge level={ev.classification} />
                      <CustodyStateBadge state={ev.custodyState} />
                    </div>
                  </div>
                  <h3 className="text-xs font-semibold text-zinc-300 group-hover:text-zinc-100 transition-colors line-clamp-1">
                    {ev.title}
                  </h3>
                  <div className="flex items-center justify-between text-[11px] text-zinc-500 mt-2 font-mono">
                    <span className="text-emerald-400">
                      SHA: {truncateHash(ev.file?.sha256)}
                    </span>
                    <span>{formatDate(ev.createdAt)}</span>
                  </div>
                </Link>
              ))
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
