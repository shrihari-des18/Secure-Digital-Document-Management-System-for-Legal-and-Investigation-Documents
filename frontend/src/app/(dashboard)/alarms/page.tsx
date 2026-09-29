'use client';

import { useState, useCallback } from 'react';
import {
  ShieldAlert,
  Check,
  Loader2,
  Filter,
} from 'lucide-react';
import { clsx } from 'clsx';
import { alarmsApi } from '@/lib/api/alarms';
import { useStompSubscription } from '@/lib/ws/use-stomp-subscription';
import { useAuthStore } from '@/stores/auth-store';
import { RoleEnum, AlarmSeverity } from '@/types';
import type { SecurityAlarm } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

const SEVERITY_CHIPS: { label: string; value: AlarmSeverity | 'ALL' }[] = [
  { label: 'All', value: 'ALL' },
  { label: 'Critical', value: AlarmSeverity.CRITICAL },
  { label: 'High', value: AlarmSeverity.HIGH },
  { label: 'Medium', value: AlarmSeverity.MEDIUM },
  { label: 'Low', value: AlarmSeverity.LOW },
  { label: 'Info', value: AlarmSeverity.INFO },
];

const SEVERITY_BADGE: Record<string, string> = {
  CRITICAL: 'bg-red-500/10 text-red-400 border-red-500/20',
  HIGH: 'bg-red-500/10 text-red-400 border-red-500/20',
  MEDIUM: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  LOW: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  INFO: 'bg-zinc-800 text-zinc-300 border-zinc-700',
};

export default function AlarmsPage() {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);
  const [severityFilter, setSeverityFilter] = useState<AlarmSeverity | 'ALL'>('ALL');
  const [unacknowledgedOnly, setUnacknowledgedOnly] = useState(false);

  const isSecurityRole = user?.role === RoleEnum.SYSTEM_ADMIN || user?.role === RoleEnum.AUDITOR;

  // Fetch alarms
  const { data: alarms = [], isLoading } = useQuery({
    queryKey: ['alarms', severityFilter, unacknowledgedOnly],
    queryFn: () =>
      alarmsApi.listAlarms({
        severity: severityFilter === 'ALL' ? undefined : severityFilter,
        unacknowledgedOnly: unacknowledgedOnly || undefined,
      }),
    enabled: isSecurityRole,
    refetchInterval: 30000, // poll every 30s as fallback
  });

  // Acknowledge mutation
  const ackMutation = useMutation({
    mutationFn: (id: string) => alarmsApi.acknowledgeAlarm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alarms'] });
    },
  });

  // Live STOMP subscription — prepend new alarms
  const handleAlarmPush = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['alarms'] });
  }, [queryClient]);

  useStompSubscription('/topic/security/alarms', handleAlarmPush, isSecurityRole);

  const formatDate = (iso: string) => {
    try {
      return new Date(iso).toLocaleString();
    } catch {
      return iso;
    }
  };

  if (!isSecurityRole) {
    return (
      <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
        <ShieldAlert className="w-10 h-10 text-zinc-600 mx-auto mb-3" />
        <h2 className="text-sm font-bold text-zinc-100">Access Restricted</h2>
        <p className="text-xs text-zinc-400 mt-1">
          Security alarms are restricted to System Admin and Auditor roles.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-8 max-w-5xl">
      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-zinc-100 flex items-center gap-3">
          <div className="icon-box-primary">
            <ShieldAlert className="w-5 h-5" />
          </div>
          Security Alarms
        </h1>
        <p className="text-xs text-zinc-400 mt-2 leading-relaxed max-w-lg">
          Real-time tamper detection, integrity violations, and security incident alerts dispatched by the WORM storage subsystem.
        </p>
      </div>

      {/* Filters */}
      <div className="flex flex-wrap items-center gap-3">
        {/* Severity chips */}
        {SEVERITY_CHIPS.map((chip) => (
          <button
            key={chip.value}
            onClick={() => setSeverityFilter(chip.value)}
            className={clsx(
              'px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer',
              severityFilter === chip.value
                ? 'bg-zinc-100 text-zinc-950 border-zinc-100'
                : 'bg-zinc-900 text-zinc-400 border-zinc-800 hover:bg-zinc-800 hover:text-zinc-200 hover:border-zinc-700'
            )}
          >
            {chip.label}
          </button>
        ))}

        <div className="h-4 w-px bg-zinc-800 mx-1" />

        {/* Unacknowledged toggle */}
        <button
          onClick={() => setUnacknowledgedOnly(!unacknowledgedOnly)}
          className={clsx(
            'px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer flex items-center gap-1.5',
            unacknowledgedOnly
              ? 'bg-red-500/10 text-red-400 border-red-500/20'
              : 'bg-zinc-900 text-zinc-400 border-zinc-800 hover:bg-zinc-800 hover:text-zinc-200'
          )}
        >
          <Filter className="w-3 h-3" />
          Unacknowledged Only
        </button>
      </div>

      {/* Alarms List */}
      {isLoading ? (
        <div className="py-16 text-center">
          <Loader2 className="w-6 h-6 text-zinc-400 animate-spin mx-auto" />
          <p className="text-xs text-zinc-500 mt-3">Loading security alarms...</p>
        </div>
      ) : alarms.length === 0 ? (
        <div className="py-16 text-center border border-zinc-800 bg-zinc-950 rounded-xl">
          <ShieldAlert className="w-8 h-8 text-zinc-700 mx-auto mb-3" />
          <p className="text-sm text-zinc-400">No alarms match the current filters.</p>
          <p className="text-xs text-zinc-500 mt-1">System integrity nominal.</p>
        </div>
      ) : (
        <div className="space-y-3 stagger-children">
          {alarms.map((alarm: SecurityAlarm) => {
            const badgeClass = SEVERITY_BADGE[alarm.severity] || SEVERITY_BADGE.INFO;

            return (
              <div
                key={alarm.id}
                className={clsx(
                  'p-5 rounded-xl border transition-colors',
                  alarm.acknowledged
                    ? 'border-zinc-800 bg-zinc-950'
                    : 'border-zinc-700 bg-zinc-900/40'
                )}
              >
                <div className="flex flex-col sm:flex-row items-start sm:justify-between gap-4">
                  <div className="flex-1 space-y-2">
                    {/* Severity + type badges */}
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className={clsx('text-[10px] font-mono font-bold px-2 py-0.5 rounded border', badgeClass)}>
                        {alarm.severity}
                      </span>
                      <span className="text-[10px] font-mono text-zinc-500 bg-zinc-900 px-2 py-0.5 rounded border border-zinc-800">
                        {alarm.alarmType?.toString().replace(/_/g, ' ')}
                      </span>
                      {alarm.acknowledged && (
                        <span className="text-[10px] font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
                          ✓ ACKNOWLEDGED
                        </span>
                      )}
                    </div>

                    {/* Message */}
                    <p className="text-sm text-zinc-200 leading-relaxed">
                      {alarm.message}
                    </p>

                    {/* Metadata */}
                    <div className="flex flex-wrap items-center gap-4 text-[10px] font-mono text-zinc-500">
                      <span>Time: {formatDate(alarm.createdAt)}</span>
                      {alarm.resourceRef && <span>Resource: <span className="text-zinc-300">{alarm.resourceRef}</span></span>}
                      {alarm.caseId && <span>Case: <span className="text-zinc-300">{alarm.caseId.slice(0, 8)}...</span></span>}
                      {alarm.acknowledgedBy && <span>Acked by: <span className="text-zinc-300">{alarm.acknowledgedBy.slice(0, 8)}...</span></span>}
                    </div>
                  </div>

                  {/* Acknowledge action */}
                  {!alarm.acknowledged && (
                    <button
                      onClick={() => ackMutation.mutate(alarm.id)}
                      disabled={ackMutation.isPending}
                      className="btn-secondary text-xs shrink-0"
                    >
                      <Check className="w-3.5 h-3.5 text-emerald-400" />
                      <span>Acknowledge</span>
                    </button>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}
