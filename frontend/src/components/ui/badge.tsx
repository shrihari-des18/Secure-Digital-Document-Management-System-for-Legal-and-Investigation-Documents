import { clsx } from 'clsx';
import {
  CaseStatus,
  ClassificationLevel,
  CustodyState,
  IntegrityStatus,
  LegalDocumentType,
} from '@/types';

export interface BadgeProps {
  children: React.ReactNode;
  variant?: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'neutral' | 'emerald' | 'amber' | 'red' | 'blue';
  className?: string;
  size?: 'sm' | 'md' | 'lg';
}

export function Badge({ children, variant = 'default', size = 'sm', className }: BadgeProps) {
  const sizeClasses = {
    sm: 'text-xs px-2.5 py-0.5',
    md: 'text-sm px-3 py-1',
    lg: 'text-base px-3.5 py-1.5',
  };


  const variantClasses: Record<string, string> = {
    default: 'bg-zinc-800/60 text-zinc-300 border-zinc-700/50',
    neutral: 'bg-zinc-900 text-zinc-400 border-zinc-800',
    // Emerald: safe / verified / sealed / chain intact
    success: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    emerald: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    // Amber: warnings / pending states
    warning: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    amber: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    // Red: risk / spoofed / integrity violation / broken chain
    danger: 'bg-red-500/10 text-red-400 border-red-500/20',
    red: 'bg-red-500/10 text-red-400 border-red-500/20',
    // Blue: neutral accents / informational tags
    info: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
    blue: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  };

  return (
    <span
      className={clsx(
        'inline-flex items-center gap-1 font-medium rounded-md border font-mono tracking-tight',
        sizeClasses[size],
        variantClasses[variant] || variantClasses.default,
        className
      )}
    >
      {children}
    </span>
  );
}

export function CaseStatusBadge({ status }: { status: CaseStatus }) {
  const configs: Record<CaseStatus, { label: string; variant: BadgeProps['variant'] }> = {
    [CaseStatus.OPEN]: { label: 'Open', variant: 'blue' },
    [CaseStatus.UNDER_INVESTIGATION]: { label: 'Under Investigation', variant: 'amber' },
    [CaseStatus.IN_COURT]: { label: 'In Court', variant: 'amber' },
    [CaseStatus.CLOSED]: { label: 'Closed', variant: 'emerald' },
    [CaseStatus.ARCHIVED]: { label: 'Archived', variant: 'neutral' },
  };
  const c = configs[status] || { label: status, variant: 'neutral' };
  return <Badge variant={c.variant}>{c.label}</Badge>;
}

export function CustodyStateBadge({ state }: { state: CustodyState }) {
  const configs: Record<CustodyState, { label: string; variant: BadgeProps['variant'] }> = {
    [CustodyState.SEIZED]: { label: 'Seized', variant: 'amber' },
    [CustodyState.INGESTED]: { label: 'Ingested & Sealed', variant: 'emerald' },
    [CustodyState.UNDER_ANALYSIS]: { label: 'Under Analysis', variant: 'amber' },
    [CustodyState.TRANSFERRED]: { label: 'Transferred', variant: 'blue' },
    [CustodyState.IN_COURT]: { label: 'In Court', variant: 'amber' },
    [CustodyState.CERTIFIED]: { label: '§63 Certified', variant: 'emerald' },
    [CustodyState.ARCHIVED]: { label: 'Archived', variant: 'neutral' },
    [CustodyState.DISPOSED]: { label: 'Disposed', variant: 'red' },
  };
  const c = configs[state] || { label: state, variant: 'neutral' };
  return <Badge variant={c.variant}>{c.label}</Badge>;
}

export function ClassificationBadge({ level }: { level: ClassificationLevel }) {
  const configs: Record<ClassificationLevel, { label: string; variant: BadgeProps['variant'] }> = {
    [ClassificationLevel.PUBLIC]: { label: 'Public', variant: 'neutral' },
    [ClassificationLevel.RESTRICTED]: { label: 'Restricted', variant: 'blue' },
    [ClassificationLevel.CONFIDENTIAL]: { label: 'Confidential', variant: 'amber' },
    [ClassificationLevel.SECRET]: { label: 'Secret (Top Clearance)', variant: 'red' },
  };
  const c = configs[level] || { label: level, variant: 'neutral' };
  return <Badge variant={c.variant}>{c.label}</Badge>;
}

export function IntegrityBadge({ status }: { status: IntegrityStatus }) {
  if (status === IntegrityStatus.VERIFIED) {
    return (
      <Badge variant="emerald">
        <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block mr-0.5" />
        Intact & Verified
      </Badge>
    );
  }
  return (
    <Badge variant="red">
      <span className="w-1.5 h-1.5 rounded-full bg-red-400 inline-block mr-0.5" />
      Tamper / Mismatch
    </Badge>
  );
}

export function DocumentTypeBadge({ type }: { type: LegalDocumentType }) {
  const configs: Record<LegalDocumentType, { label: string; variant: BadgeProps['variant'] }> = {
    [LegalDocumentType.FIR]: { label: 'FIR', variant: 'red' },
    [LegalDocumentType.CASE_DIARY]: { label: 'Case Diary', variant: 'blue' },
    [LegalDocumentType.SEARCH_WARRANT]: { label: 'Search Warrant', variant: 'amber' },
    [LegalDocumentType.CHARGE_SHEET]: { label: 'Charge Sheet', variant: 'blue' },
    [LegalDocumentType.SEIZURE_MEMO]: { label: 'Seizure Memo', variant: 'emerald' },
    [LegalDocumentType.FORENSIC_REPORT]: { label: 'Forensic Lab Report', variant: 'emerald' },
    [LegalDocumentType.BAIL_APPLICATION]: { label: 'Bail Application', variant: 'neutral' },
    [LegalDocumentType.COURT_ORDER]: { label: 'Court Order', variant: 'amber' },
  };
  const c = configs[type] || { label: type, variant: 'neutral' };
  return <Badge variant={c.variant}>{c.label}</Badge>;
}
