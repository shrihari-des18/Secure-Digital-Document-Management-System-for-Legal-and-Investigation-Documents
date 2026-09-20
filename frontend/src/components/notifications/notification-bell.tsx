'use client';

import { useState, useRef, useEffect } from 'react';
import {
  Bell,
  ShieldCheck,
  FileCheck,
  Award,
  ArrowRightLeft,
} from 'lucide-react';
import { clsx } from 'clsx';
import Link from 'next/link';

interface NotificationItem {
  id: string;
  title: string;
  description: string;
  time: string;
  type: 'integrity' | 'custody' | 'cert' | 'evidence';
  read: boolean;
  href?: string;
}

const INITIAL_NOTIFICATIONS: NotificationItem[] = [
  {
    id: '1',
    title: 'Cryptographic Ledger Verified',
    description: 'Automated block chain traversal confirmed 100% unbroken integrity.',
    time: 'Just now',
    type: 'integrity',
    read: false,
    href: '/ledger',
  },
  {
    id: '2',
    title: 'Evidence Ingested & Sealed',
    description: 'Hardware bit-stream dump sealed with ECDSA digital signature.',
    time: '5m ago',
    type: 'evidence',
    read: false,
    href: '/evidence',
  },
  {
    id: '3',
    title: 'BSA §63 Certificate Issued',
    description: 'Court admissibility schedule generated and digitally signed.',
    time: '25m ago',
    type: 'cert',
    read: true,
    href: '/certificates',
  },
];

export function NotificationBell() {
  const [isOpen, setIsOpen] = useState(false);
  const [notifications, setNotifications] = useState<NotificationItem[]>(INITIAL_NOTIFICATIONS);
  const containerRef = useRef<HTMLDivElement>(null);

  const unreadCount = notifications.filter((n) => !n.read).length;

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (containerRef.current && !containerRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const markAllAsRead = () => {
    setNotifications((prev) => prev.map((n) => ({ ...n, read: true })));
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 rounded-lg text-zinc-400 hover:text-zinc-100 hover:bg-zinc-900 transition-colors"
        title="Notifications"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-full mt-2 w-80 sm:w-96 rounded-xl bg-zinc-950 border border-zinc-800 shadow-2xl overflow-hidden z-50 animate-fade-in">
          <div className="flex items-center justify-between p-4 border-b border-zinc-800 bg-zinc-950">
            <div className="flex items-center gap-2">
              <h3 className="text-xs font-bold tracking-tight text-zinc-100">System Notifications & Audits</h3>
              {unreadCount > 0 && (
                <span className="text-[10px] font-mono px-1.5 py-0.5 rounded bg-zinc-800 text-zinc-200 border border-zinc-700 font-bold">
                  {unreadCount} NEW
                </span>
              )}
            </div>

            {unreadCount > 0 && (
              <button
                onClick={markAllAsRead}
                className="text-[11px] text-zinc-400 hover:text-zinc-100 font-semibold cursor-pointer"
              >
                Mark all read
              </button>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto divide-y divide-zinc-800/60">
            {notifications.map((n) => (
              <Link
                key={n.id}
                href={n.href || '#'}
                onClick={() => setIsOpen(false)}
                className={clsx(
                  'block p-4 transition-colors hover:bg-zinc-800/20',
                  !n.read && 'bg-zinc-900/30'
                )}
              >
                <div className="flex items-start gap-3">
                  <div className="mt-0.5">
                    {n.type === 'integrity' && <ShieldCheck className="w-4 h-4 text-emerald-400" />}
                    {n.type === 'evidence' && <FileCheck className="w-4 h-4 text-blue-400" />}
                    {n.type === 'cert' && <Award className="w-4 h-4 text-amber-400" />}
                    {n.type === 'custody' && <ArrowRightLeft className="w-4 h-4 text-blue-400" />}
                  </div>

                  <div className="flex-1 space-y-1">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold tracking-tight text-zinc-100">{n.title}</h4>
                      <span className="text-[10px] font-mono text-zinc-500">{n.time}</span>
                    </div>
                    <p className="text-xs text-zinc-400 leading-relaxed">{n.description}</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
