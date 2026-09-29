'use client';

import { useState, useRef, useEffect, useCallback } from 'react';
import {
  Bell,
  ShieldAlert,
  ShieldCheck,
  AlertTriangle,
  CheckCircle,
} from 'lucide-react';
import { clsx } from 'clsx';
import Link from 'next/link';
import { alarmsApi } from '@/lib/api/alarms';
import { useStompSubscription } from '@/lib/ws/use-stomp-subscription';
import { useAuthStore } from '@/stores/auth-store';
import { RoleEnum } from '@/types';
import type { SecurityAlarm, AlarmSeverity } from '@/types';

const SEVERITY_COLORS: Record<string, string> = {
  CRITICAL: 'text-red-400',
  HIGH: 'text-red-400',
  MEDIUM: 'text-amber-400',
  LOW: 'text-blue-400',
  INFO: 'text-zinc-400',
};

const SEVERITY_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  CRITICAL: AlertTriangle,
  HIGH: ShieldAlert,
  MEDIUM: ShieldAlert,
  LOW: ShieldCheck,
  INFO: CheckCircle,
};

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [unacknowledgedCount, setUnacknowledgedCount] = useState(0);
  const [recentAlarms, setRecentAlarms] = useState<SecurityAlarm[]>([]);
  const containerRef = useRef<HTMLDivElement>(null);
  const user = useAuthStore((s) => s.user);

  const isSecurityRole = user?.role === RoleEnum.SYSTEM_ADMIN || user?.role === RoleEnum.AUDITOR;

  // Fetch unacknowledged count on mount
  useEffect(() => {
    if (!isSecurityRole) return;
    alarmsApi.getUnacknowledgedCount()
      .then(setUnacknowledgedCount)
      .catch(() => setUnacknowledgedCount(0));
  }, [isSecurityRole]);

  // Fetch recent alarms when dropdown opens
  useEffect(() => {
    if (!isOpen || !isSecurityRole) return;
    alarmsApi.listAlarms({ unacknowledgedOnly: true })
      .then((alarms) => setRecentAlarms(alarms.slice(0, 5)))
      .catch(() => {});
  }, [isOpen, isSecurityRole]);

  // Subscribe to live alarm pushes
  const handleAlarmPush = useCallback((message: import('@stomp/stompjs').IMessage) => {
    try {
      const alarm = JSON.parse(message.body) as SecurityAlarm;
      
      setRecentAlarms((prev) => { if (prev.some(a => a.id === alarm.id)) return prev; setUnacknowledgedCount(c => c + 1); return [alarm, ...prev].slice(0, 5); });
    } catch {
      // Malformed message â ignore
    }
  }, []);

  useStompSubscription(
    '/topic/security/alarms',
    handleAlarmPush,
    isSecurityRole
  );

  // Click outside handler
  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const formatTime = (iso: string) => {
    try {
      const d = new Date(iso);
      const now = new Date();
      const diff = Math.floor((now.getTime() - d.getTime()) / 1000);
      if (diff < 60) return 'Just now';
      if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
      if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
      return d.toLocaleDateString();
    } catch {
      return '';
    }
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-lg text-zinc-400 hover:text-zinc-100 hover:bg-zinc-900 transition-colors"
        title="Security Notifications"
      >
        <Bell className="w-5 h-5" />
        {unacknowledgedCount > 0 && (
          <span className="absolute -top-0.5 -right-0.5 min-w-[18px] h-[18px] rounded-full bg-red-500 text-white text-[10px] font-bold flex items-center justify-center px-1 animate-pulse">
            {unacknowledgedCount > 99 ? '99+' : unacknowledgedCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 sm:w-96 rounded-xl bg-zinc-950 border border-zinc-800 shadow-2xl overflow-hidden z-50 animate-fade-in">
          <div className="flex items-center justify-between p-4 border-b border-zinc-800 bg-zinc-950">
            <div className="flex items-center gap-2">
              <h3 className="text-xs font-bold tracking-tight text-zinc-100">Security Alerts</h3>
              {unacknowledgedCount > 0 && (
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-red-500/10 text-red-400 border border-red-500/20 font-bold">
                  {unacknowledgedCount} UNACK
                </span>
              )}
            </div>

            {isSecurityRole && (
              <Link
                href="/alarms"
                onClick={() => setIsOpen(false)}
                className="text-[11px] text-zinc-400 hover:text-zinc-100 font-semibold cursor-pointer"
              >
                View all â
              </Link>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-zinc-800/60">
            {!isSecurityRole ? (
              <div className="p-6 text-center text-xs text-zinc-500">
                Security alerts are restricted to System Admin and Auditor roles.
              </div>
            ) : recentAlarms.length === 0 ? (
              <div className="p-6 text-center text-xs text-zinc-500">
                <ShieldCheck className="w-6 h-6 text-emerald-400 mx-auto mb-2" />
                No unacknowledged security alerts. System nominal.
              </div>
            ) : (
              recentAlarms.map((alarm) => {
                const Icon = SEVERITY_ICONS[alarm.severity] || ShieldAlert;
                const color = SEVERITY_COLORS[alarm.severity] || 'text-zinc-400';

                return (
                  <Link
                    key={alarm.id}
                    href="/alarms"
                    onClick={() => setIsOpen(false)}
                    className="block p-4 transition-colors hover:bg-zinc-800/20 bg-zinc-900/30"
                  >
                    <div className="flex items-start gap-3">
                      <div className="mt-0.5">
                        <Icon className={clsx('w-4 h-4', color)} />
                      </div>
                      <div className="flex-1 space-y-1">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-1.5">
                            <span className={clsx('text-[10px] font-mono font-bold px-1.5 py-0.5 rounded border',
                              alarm.severity === 'CRITICAL' ? 'bg-red-500/10 text-red-400 border-red-500/20' :
                              alarm.severity === 'HIGH' ? 'bg-red-500/10 text-red-400 border-red-500/20' :
                              alarm.severity === 'MEDIUM' ? 'bg-amber-500/10 text-amber-400 border-amber-500/20' :
                              'bg-zinc-800 text-zinc-300 border-zinc-700'
                            )}>
                              {alarm.severity}
                            </span>
                            <span className="text-[10px] font-mono text-zinc-500">
                              {alarm.alarmType?.toString().replace(/_/g, ' ')}
                            </span>
                          </div>
                          <span className="text-[10px] font-mono text-zinc-500">
                            {formatTime(alarm.createdAt)}
                          </span>
                        </div>
                        <p className="text-xs text-zinc-300 leading-relaxed line-clamp-2">
                          {alarm.message}
                        </p>
                      </div>
                    </div>
                  </Link>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
