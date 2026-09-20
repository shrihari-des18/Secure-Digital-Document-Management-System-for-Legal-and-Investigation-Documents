'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { clsx } from 'clsx';
import {
  LayoutDashboard,
  Briefcase,
  FileSearch,
  Link2,
  BookOpen,
  Shield,
  Award,
  Users,
  Brain,
  FileText,
  ChevronLeft,
  ChevronRight,
  PieChart,
} from 'lucide-react';
import { useState } from 'react';
import { useAuthStore } from '@/stores/auth-store';
import { RoleEnum } from '@/types';

interface NavItem {
  label: string;
  href: string;
  icon: React.ComponentType<{ className?: string }>;
  roles?: RoleEnum[]; // undefined = all roles
}

const NAV_ITEMS: NavItem[] = [
  {
    label: 'Dashboard',
    href: '/dashboard',
    icon: LayoutDashboard,
  },
  {
    label: 'Cases',
    href: '/cases',
    icon: Briefcase,
  },
  {
    label: 'Evidence',
    href: '/evidence',
    icon: FileSearch,
  },
  {
    label: 'Custody',
    href: '/custody',
    icon: Link2,
  },
  {
    label: 'Documents',
    href: '/documents',
    icon: FileText,
  },
  {
    label: 'Audit Ledger',
    href: '/ledger',
    icon: BookOpen,
    roles: [RoleEnum.AUDITOR, RoleEnum.SYSTEM_ADMIN, RoleEnum.INVESTIGATING_OFFICER, RoleEnum.COURT, RoleEnum.PROSECUTOR, RoleEnum.RECORDS_CUSTODIAN],
  },
  {
    label: 'Certificates',
    href: '/certificates',
    icon: Award,
  },
  {
    label: 'AI Analysis',
    href: '/ai-analysis',
    icon: Brain,
    roles: [RoleEnum.INVESTIGATING_OFFICER, RoleEnum.FORENSIC_ANALYST, RoleEnum.PROSECUTOR, RoleEnum.COURT, RoleEnum.SYSTEM_ADMIN],
  },
  {
    label: 'Reports',
    href: '/reports',
    icon: PieChart,
    roles: [RoleEnum.SYSTEM_ADMIN, RoleEnum.AUDITOR, RoleEnum.COURT, RoleEnum.PROSECUTOR],
  },
  {
    label: 'Admin',
    href: '/admin',
    icon: Users,
    roles: [RoleEnum.SYSTEM_ADMIN],
  },
];

export function Sidebar() {
  const pathname = usePathname();
  const user = useAuthStore((s) => s.user);
  const [collapsed, setCollapsed] = useState(false);

  const visibleItems = NAV_ITEMS.filter(
    (item) => !item.roles || (user && item.roles.includes(user.role))
  );

  return (
    <aside
      className={clsx(
        'fixed left-0 top-0 h-screen bg-zinc-950 border-r border-zinc-800 flex flex-col z-40 transition-all duration-200 ease-in-out',
        collapsed ? 'w-[68px]' : 'w-[250px]'
      )}
    >
      {/* Logo */}
      <div className="flex items-center gap-3 px-4 h-16 border-b border-zinc-800 shrink-0 bg-zinc-950">
        <div className="w-9 h-9 rounded-lg bg-zinc-100 text-zinc-950 flex items-center justify-center shrink-0 shadow-sm">
          <Shield className="w-5 h-5" />
        </div>
        {!collapsed && (
          <div className="animate-fade-in">
            <h1 className="text-base font-bold text-zinc-100 leading-tight tracking-tight">E-Vault</h1>
            <p className="text-xs text-zinc-400 leading-tight font-mono">BSA 2023 §63</p>
          </div>
        )}

      </div>

      {/* Navigation */}
      <nav className="flex-1 overflow-y-auto py-3 px-2 stagger-children">
        {visibleItems.map((item) => {
          const isActive = pathname === item.href || pathname.startsWith(item.href + '/');
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={clsx(
                'flex items-center gap-3 px-3 py-2 rounded-lg mb-1 transition-all duration-150 group relative text-sm',
                isActive
                  ? 'bg-zinc-900 text-zinc-100 font-semibold border border-zinc-800/80 shadow-xs'
                  : 'text-zinc-400 hover:bg-zinc-900/40 hover:text-zinc-200 border border-transparent'
              )}
              title={collapsed ? item.label : undefined}
            >
              {/* Active indicator bar */}
              {isActive && (
                <div className="absolute left-0 top-1/2 -translate-y-1/2 w-[2.5px] h-4 bg-zinc-100 rounded-r-full" />
              )}

              <Icon className={clsx('w-4 h-4 shrink-0', isActive ? 'text-zinc-100' : 'text-zinc-400')} />

              {!collapsed && (
                <span className="truncate">{item.label}</span>
              )}

              {/* Tooltip for collapsed state */}
              {collapsed && (
                <div className="absolute left-full ml-2 px-2.5 py-1 bg-zinc-900 text-zinc-200 text-xs rounded-md border border-zinc-800 opacity-0 pointer-events-none group-hover:opacity-100 transition-opacity whitespace-nowrap z-50 shadow-xl">
                  {item.label}
                </div>
              )}
            </Link>
          );
        })}
      </nav>

      {/* Collapse Toggle */}
      <div className="border-t border-zinc-800 p-2 shrink-0 bg-zinc-950">
        <button
          onClick={() => setCollapsed(!collapsed)}
          className="flex items-center justify-center w-full p-2 rounded-lg text-zinc-400 hover:bg-zinc-900 hover:text-zinc-200 transition-colors"
          title={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}
        >
          {collapsed ? (
            <ChevronRight className="w-4 h-4" />
          ) : (
            <ChevronLeft className="w-4 h-4" />
          )}
        </button>
      </div>
    </aside>
  );
}
