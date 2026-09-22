'use client';

import { useState, useCallback } from 'react';
import { ShieldAlert, Check, Loader2 } from 'lucide-react';
import { clsx } from 'clsx';
import { alarmsApi } from '@/lib/api/alarms';
import { useStompSubscription } from '@/lib/ws/use-stomp-subscription';
import { useAuthStore } from '@/stores/auth-store';
import { RoleEnum, AlarmSeverity } from '@/types';
import type { SecurityAlarm } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { formatDate } from '@/lib/crypto';

interface SecurityActivityTabProps {
  caseId: string;
}

const SEVERITY_BADGE: Record<string, string> = {
  CRITICAL: 'bg-red-500/10 text-red-400 border-red-500/20',
  HIGH: 'bg-red-500/10 text-red-400 border-red-500/20',
  MEDIUM: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
  LOW: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  INFO: 'bg-zinc-800 text-zinc-300 border-zinc-700',
};

export function SecurityActivityTab({ caseId }: SecurityActivityTabProps) {
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);

  // Allow System Admin, Auditor, and Investigating Officer to see case-specific alarms
  const canViewAlarms = 
    user?.role === RoleEnum.SYSTEM_ADMIN || 
    user?.role === RoleEnum.AUDITOR || 
    user?.role === RoleEnum.INVESTIGATING_OFFICER;

  const { data: alarms = [], isLoading } = useQuery({
    queryKey: ['alarms', 'case', caseId],
    queryFn: () => alarmsApi.listAlarms({ caseId }),
    enabled: canViewAlarms,
    refetchInterval: 30000,
  });

  const ackMutation = useMutation({
    mutationFn: (id: string) => alarmsApi.acknowledgeAlarm(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['alarms'] });
    },
  });

  const handleAlarmPush = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: ['alarms', 'case', caseId] });
  }, [queryClient, caseId]);

  useStompSubscription('/topic/security/alarms', handleAlarmPush, canViewAlarms);

  if (!canViewAlarms) {
    return (
      <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-10 text-center">
        <ShieldAlert className="w-10 h-10 text-zinc-600 mx-auto mb-3" />
        <h2 className="text-sm font-bold tracking-tight text-zinc-100">Access Restricted</h2>
        <p className="text-xs text-zinc-400 mt-1">
          Security activity stream is restricted to authorized roles.
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h3 className="text-sm font-bold tracking-tight text-zinc-100">Live Security Activity Stream</h3>
      </div>

      {isLoading ? (
        <div className="py-12 text-center text-xs text-zinc-500">
          <Loader2 className="w-5 h-5 animate-spin mx-auto mb-2 text-zinc-400" />
          Loading activity stream...
        </div>
      ) : alarms.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-10 text-center">
          <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-500 mx-auto mb-2">
            <ShieldAlert className="w-5 h-5" />
          </div>
          <p className="text-xs text-zinc-400">No security alerts or tamper events recorded for this docket.</p>
          <p className="text-[11px] text-zinc-500 mt-1">System integrity is intact.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {alarms.map((alarm: SecurityAlarm) => {
            const badgeClass = SEVERITY_BADGE[alarm.severity] || SEVERITY_BADGE.INFO;

            return (
              <div
                key={alarm.id}
                className={clsx(
                  'p-4 rounded-xl border transition-colors flex flex-col sm:flex-row sm:items-start justify-between gap-4',
                  alarm.acknowledged
                    ? 'border-zinc-800 bg-zinc-950'
                    : 'border-zinc-700 bg-zinc-900/40'
                )}
              >
                <div className="flex-1 space-y-2">
                  <div className="flex items-center gap-2 flex-wrap mb-1">
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
                  <p className="text-sm text-zinc-200 leading-relaxed">
                    {alarm.message}
                  </p>
                  <div className="flex flex-wrap items-center gap-4 text-[10px] font-mono text-zinc-500">
                    <span>Time: {formatDate(alarm.createdAt)}</span>
                    {alarm.resourceRef && <span>Resource: <span className="text-zinc-300">{alarm.resourceRef}</span></span>}
                  </div>
                </div>

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
            );
          })}
        </div>
      )}
    </div>
  );
}
