'use client';

import { useState } from 'react';
import { Clock, ShieldCheck, ShieldAlert, CheckCircle2 } from 'lucide-react';
import { clsx } from 'clsx';
import { timestampApi } from '@/lib/api/timestamp';
import { formatDate } from '@/lib/crypto';
import { useQuery } from '@tanstack/react-query';

interface TrustedTimestampSectionProps {
  resourceType: 'evidence' | 'certificate';
  resourceId: string;
}

export function TrustedTimestampSection({ resourceType, resourceId }: TrustedTimestampSectionProps) {
  const { data: timestampData, isLoading, error } = useQuery({
    queryKey: ['timestamp', resourceType, resourceId],
    queryFn: () => {
      if (resourceType === 'evidence') {
        return timestampApi.getEvidenceTimestamp(resourceId);
      }
      return timestampApi.getCertificateTimestamp(resourceId);
    },
    enabled: !!resourceId,
    retry: false, // Do not retry if timestamp is missing
  });

  if (isLoading) {
    return (
      <details className="group border border-zinc-800 bg-zinc-950 rounded-xl overflow-hidden mt-6">
        <summary className="p-4 flex items-center justify-between cursor-pointer hover:bg-zinc-900/50 transition-colors">
          <h3 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
            <Clock className="w-4 h-4 text-zinc-400" />
            RFC 3161 Trusted Timestamp Authority (TSA)
          </h3>
          <span className="text-xs font-semibold text-zinc-400 group-open:text-zinc-100 transition-colors font-mono">
            Loading...
          </span>
        </summary>
      </details>
    );
  }

  // If there's an error (e.g. no timestamp exists yet), just don't show the section or show a placeholder
  if (error || !timestampData) {
    return null;
  }

  const { valid, message, tsaName, genTime, serialNumber } = timestampData;

  return (
    <details className="group border border-zinc-800 bg-zinc-950 rounded-xl overflow-hidden mt-6">
      <summary className="p-4 flex items-center justify-between cursor-pointer hover:bg-zinc-900/50 transition-colors">
        <h3 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
          <Clock className="w-4 h-4 text-emerald-400" />
          RFC 3161 Trusted Timestamp Authority (TSA)
        </h3>
        <div className="flex items-center gap-3">
          <span className={clsx(
            'text-[10px] font-mono px-2 py-0.5 rounded font-bold border flex items-center gap-1',
            valid
              ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
              : 'bg-red-500/10 text-red-400 border-red-500/20'
          )}>
            {valid ? (
              <><ShieldCheck className="w-3 h-3" /> VALIDATED</>
            ) : (
              <><ShieldAlert className="w-3 h-3" /> INVALID</>
            )}
          </span>
          <span className="text-xs font-semibold text-zinc-400 group-open:text-zinc-100 transition-colors font-mono">
            View Details ▼
          </span>
        </div>
      </summary>
      
      <div className="p-4 pt-0 border-t border-zinc-800 bg-zinc-950">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mt-4">
          
          <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
            <div className="flex items-center gap-2 text-xs font-semibold text-zinc-400 mb-2 font-mono">
              <span>TSA Authority</span>
            </div>
            <p className="font-mono text-xs text-zinc-200">
              {tsaName || 'O=E-Vault TSA, C=IN'}
            </p>
          </div>

          <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
            <div className="flex items-center gap-2 text-xs font-semibold text-zinc-400 mb-2 font-mono">
              <span>GenTime (Cryptographic Time)</span>
            </div>
            <p className="font-mono text-xs text-emerald-400">
              {genTime ? formatDate(genTime) : '—'}
            </p>
          </div>

          <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800 md:col-span-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-zinc-400 mb-2 font-mono">
              <span>Message Imprint (Hashed Data)</span>
            </div>
            <p className="font-mono text-[11px] text-zinc-400 break-all select-all bg-zinc-950 p-2.5 rounded border border-zinc-800">
              Algorithm: SHA-256
            </p>
          </div>
          
          <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800 md:col-span-2">
            <div className="flex items-center gap-2 text-xs font-semibold text-zinc-400 mb-2 font-mono">
              <span>Token Serial Number</span>
            </div>
            <p className="font-mono text-xs text-zinc-400 break-all select-all">
              {serialNumber || '—'}
            </p>
          </div>

        </div>

        {valid && (
          <div className="mt-4 p-4 rounded-xl border border-emerald-500/20 bg-emerald-500/10 text-emerald-400 text-xs flex items-start gap-3 font-mono animate-fade-in">
            <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
            <div className="flex-1">
              <span className="font-bold block mb-1">TSA Verification Successful</span>
              {message || 'The RFC 3161 Time-Stamp Token successfully verified against the TSA certificate. The signed time is cryptographically bound to the data hash.'}
            </div>
          </div>
        )}
      </div>
    </details>
  );
}
