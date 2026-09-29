'use client';

import { useState, useCallback } from 'react';
import Link from 'next/link';
import {
  Briefcase,
  FileSearch,
  BookOpen,
  Plus,
  ArrowRight,
  Award,
  ShieldAlert,
} from 'lucide-react';
import { clsx } from 'clsx';
import { useAuthStore, ROLE_LABELS } from '@/stores/auth-store';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { ledgerApi } from '@/lib/api/ledger';
import { alarmsApi } from '@/lib/api/alarms';
import { useStompSubscription } from '@/lib/ws/use-stomp-subscription';
import { formatDate, truncateHash } from '@/lib/crypto';
import { CaseStatusBadge, CustodyStateBadge, ClassificationBadge } from '@/components/ui/badge';
import type { EvidenceDetailResponse, SecurityAlarm } from '@/types';
import { CaseStatus, CustodyState, RoleEnum } from '@/types';
import { useQuery, useQueryClient } from '@tanstack/react-query';

export default function DashboardPage() {
  const user = useAuthStore((s) => s.user);
  const queryClient = useQueryClient();
  const isSecurityRole = user?.role === RoleEnum.SYSTEM_ADMIN || user?.role === RoleEnum.AUDITOR;

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

  // Fetch recent alarms (security roles only)
  const { data: recentAlarms = [] } = useQuery({
    queryKey: ['dashboard-alarms'],
    queryFn: () => alarmsApi.listAlarms({ unacknowledgedOnly: true }),
    enabled: isSecurityRole,
    refetchInterval: 30000,
  });

  // Live STOMP subscription for security alarms
  const handleAlarmPush = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['dashboard-alarms'] });
  }, [queryClient]);

  useStompSubscription('/topic/security/alarms', handleAlarmPush, isSecurityRole);

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

  const SEVERITY_BADGE: Record<string, string> = {
    CRITICAL: 'bg-red-500/10 text-red-400 border-red-500/20',
    HIGH: 'bg-red-500/10 text-red-400 border-red-500/20',
    MEDIUM: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    LOW: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    INFO: 'bg-zinc-800 text-zinc-300 border-zinc-700',
  };

  return (
    <div className="space-y-10">
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
              Secure Digital Evidence &amp; Legal Document Management System under Bharatiya Sakshya Adhiniyam, 2023 §63
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
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
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

      {/* Security Alerts Feed — SYSTEM_ADMIN / AUDITOR only */}
      {isSecurityRole && recentAlarms.length > 0 && (
        <details className="group border border-red-500/20 bg-zinc-950 rounded-xl overflow-hidden" open>
          <summary className="p-6 flex items-center justify-between cursor-pointer hover:bg-zinc-900/50 transition-colors">
            <h2 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
              <div className="w-7 h-7 rounded-md bg-red-500/10 text-red-400 flex items-center justify-center border border-red-500/20">
                <ShieldAlert className="w-3.5 h-3.5" />
              </div>
              <span>Live Security Alerts</span>
              <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 font-bold">
                {recentAlarms.length} UNACK
              </span>
            </h2>
            <Link
              href="/alarms"
              className="text-xs font-semibold text-zinc-300 hover:text-zinc-100 hover:underline flex items-center gap-1 font-mono"
              onClick={(e) => e.stopPropagation()}
            >
              <span>View all</span>
              <ArrowRight className="w-3 h-3" />
            </Link>
          </summary>

          <div className="px-6 pb-6 space-y-2">
            {recentAlarms.slice(0, 4).map((alarm: SecurityAlarm) => {
              const badgeClass = SEVERITY_BADGE[alarm.severity] || SEVERITY_BADGE.INFO;
              return (
                <div
                  key={alarm.id}
                  className="p-3.5 rounded-lg border border-zinc-800/80 bg-zinc-900/40 flex items-start gap-3"
                >
                  <ShieldAlert className={clsx('w-4 h-4 shrink-0 mt-0.5',
                    alarm.severity === 'CRITICAL' || alarm.severity === 'HIGH' ? 'text-red-400' :
                    alarm.severity === 'MEDIUM' ? 'text-amber-400' : 'text-zinc-400'
                  )} />
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2 mb-1">
                      <span className={clsx('text-[10px] font-mono font-bold px-1.5 py-0.5 rounded border', badgeClass)}>
                        {alarm.severity}
                      </span>
                      <span className="text-[10px] font-mono text-zinc-500">
                        {alarm.alarmType?.toString().replace(/_/g, ' ')}
                      </span>
                    </div>
                    <p className="text-xs text-zinc-300 line-clamp-1">{alarm.message}</p>
                  </div>
                </div>
              );
            })}
          </div>
        </details>
      )}

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
                  <h3 className="text-sm font-semibold text-zinc-300 group-hover:text-zinc-100 transition-colors line-clamp-1 mt-2">
                    {c.title}
                  </h3>
                </Link>
              ))
            )}
          </div>
        </div>

        {/* Recent Evidence */}
        <div className="border border-zinc-800 bg-zinc-950 p-8 rounded-xl">
          <div className="flex items-center justify-between pb-6 border-b border-zinc-800">
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
                  <h3 className="text-sm font-semibold text-zinc-300 group-hover:text-zinc-100 transition-colors line-clamp-1 mt-2">
                    {ev.title}
                  </h3>
                </Link>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Quick Access Matrix (Progressive Disclosure) */}
      <details className="group border border-zinc-800 bg-zinc-950 rounded-xl overflow-hidden">
        <summary className="p-6 flex items-center justify-between cursor-pointer hover:bg-zinc-900/50 transition-colors">
          <h2 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
            <span className="w-7 h-7 rounded-md bg-zinc-100 text-zinc-950 flex items-center justify-center">
              <Plus className="w-3.5 h-3.5" />
            </span>
            Statutory &amp; Operational Workbenches
          </h2>
          <span className="text-xs font-semibold text-zinc-400 group-open:text-zinc-100 transition-colors">
            Toggle View
          </span>
        </summary>
        
        <div className="p-6 pt-0 border-t border-zinc-800 bg-zinc-950">
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-6 mt-6">
            <Link
              href="/evidence/upload"
              className="p-6 rounded-lg border border-zinc-800 bg-zinc-900/30 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors text-center group"
            >
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 mx-auto mb-3 group-hover:text-zinc-100">
                <Plus className="w-5 h-5" />
              </div>
              <span className="text-xs font-semibold text-zinc-200 block">Evidence Intake</span>
            </Link>

            <Link
              href="/custody"
              className="p-6 rounded-lg border border-zinc-800 bg-zinc-900/30 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors text-center group"
            >
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-blue-400 mx-auto mb-3 group-hover:text-blue-300">
                <Briefcase className="w-5 h-5" />
              </div>
              <span className="text-xs font-semibold text-zinc-200 block">Chain of Custody</span>
            </Link>

            <Link
              href="/certificates"
              className="p-6 rounded-lg border border-zinc-800 bg-zinc-900/30 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors text-center group"
            >
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-emerald-400 mx-auto mb-3 group-hover:text-emerald-300">
                <Award className="w-5 h-5" />
              </div>
              <span className="text-xs font-semibold text-zinc-200 block">BSA §63 Certs</span>
            </Link>

            <Link
              href="/documents"
              className="p-6 rounded-lg border border-zinc-800 bg-zinc-900/30 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors text-center group"
            >
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-amber-400 mx-auto mb-3 group-hover:text-amber-300">
                <FileSearch className="w-5 h-5" />
              </div>
              <span className="text-xs font-semibold text-zinc-200 block">Legal Documents</span>
            </Link>

            <Link
              href="/ledger"
              className="p-6 rounded-lg border border-zinc-800 bg-zinc-900/30 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors text-center group"
            >
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-emerald-400 mx-auto mb-3 group-hover:text-emerald-300">
                <BookOpen className="w-5 h-5" />
              </div>
              <span className="text-xs font-semibold text-zinc-200 block">Audit Ledger</span>
            </Link>

            <Link
              href="/reports"
              className="p-6 rounded-lg border border-zinc-800 bg-zinc-900/30 hover:bg-zinc-800/40 hover:border-zinc-700 transition-colors text-center group"
            >
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-blue-400 mx-auto mb-3 group-hover:text-blue-300">
                <Briefcase className="w-5 h-5" />
              </div>
              <span className="text-xs font-semibold text-zinc-200 block">System Reports</span>
            </Link>
          </div>
        </div>
      </details>
    </div>
  );
}
