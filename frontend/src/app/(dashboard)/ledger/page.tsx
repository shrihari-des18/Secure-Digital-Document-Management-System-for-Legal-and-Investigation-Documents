'use client';

import { useState } from 'react';
import {
  ShieldCheck,
  ShieldAlert,
  RefreshCw,
  Search,
  ChevronDown,
  ChevronRight,
} from 'lucide-react';
import { clsx } from 'clsx';
import { ledgerApi } from '@/lib/api/ledger';
import { formatDate, truncateHash } from '@/lib/crypto';
import { useQuery } from '@tanstack/react-query';

export default function LedgerPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [expandedRows, setExpandedRows] = useState<Record<number, boolean>>({});

  // Fetch all ledger entries
  const { data: entries = [], isLoading: entriesLoading } = useQuery({
    queryKey: ['ledger-entries'],
    queryFn: () => ledgerApi.getAllEntries(),
  });

  // Verify ledger mutation / query
  const {
    data: verification,
    isLoading: verifying,
    refetch: triggerVerification,
  } = useQuery({
    queryKey: ['ledger-verification'],
    queryFn: () => ledgerApi.verify(),
  });

  const toggleRow = (seq: number) => {
    setExpandedRows((prev) => ({ ...prev, [seq]: !prev[seq] }));
  };

  const filteredEntries = entries.filter((e) => {
    const q = searchQuery.toLowerCase();
    return (
      e.action.toLowerCase().includes(q) ||
      e.resourceRef.toLowerCase().includes(q) ||
      (e.actorId && e.actorId.toLowerCase().includes(q)) ||
      e.entryHash.toLowerCase().includes(q)
    );
  });

  const isChainIntact = verification ? verification.valid : true;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Cryptographic Audit Ledger</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Append-only SHA-256 hash-chained transaction ledger with verifiable cryptographic linkages
          </p>
        </div>

        <button
          onClick={() => triggerVerification()}
          disabled={verifying}
          className="btn-primary shrink-0"
        >
          <RefreshCw className={clsx('w-4 h-4', verifying && 'animate-spin')} />
          <span>{verifying ? 'Traversing Block Chain...' : 'Verify Full Hash Chain'}</span>
        </button>
      </div>

      {/* Whole-Ledger Cryptographic Verification Banner */}
      <div
        className={clsx(
          'p-8 rounded-xl border transition-colors',
          isChainIntact
            ? 'bg-zinc-950 border-zinc-800'
            : 'bg-red-500/10 border-red-500/30 text-red-200'
        )}
      >
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6">
          <div className="flex items-start gap-4">
            <div
              className={clsx(
                'w-12 h-12 rounded-xl flex items-center justify-center shrink-0 border',
                isChainIntact
                  ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                  : 'bg-red-500/20 border-red-500/40 text-red-400'
              )}
            >
              {isChainIntact ? <ShieldCheck className="w-6 h-6" /> : <ShieldAlert className="w-6 h-6" />}
            </div>

            <div>
              <div className="flex items-center gap-2 mb-1">
                <h2 className="text-base font-bold tracking-tight text-zinc-100">
                  {isChainIntact ? 'Audit Ledger Cryptographically Intact' : 'Tampering Detected in Ledger!'}
                </h2>
                <span
                  className={clsx(
                    'text-[10px] font-mono px-2 py-0.5 rounded font-semibold uppercase border',
                    isChainIntact
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20'
                      : 'bg-red-500/20 text-red-400 border-red-500/30'
                  )}
                >
                  {isChainIntact ? '100% UNBROKEN CHAIN' : 'BROKEN SEQUENCE'}
                </span>
              </div>
              <p className="text-xs text-zinc-400 max-w-2xl leading-relaxed">
                {verification?.message ||
                  'Every state change is sealed into an append-only hash chain where row N commits to SHA-256(row N-1).'}
              </p>
            </div>
          </div>

          {/* Verification Stats */}
          {verification && (
            <div className="flex flex-wrap items-center gap-4 text-xs font-mono pt-3 lg:pt-0 border-t lg:border-t-0 border-zinc-800">
              <div className="bg-zinc-900/40 p-3 rounded-lg border border-zinc-800">
                <span className="text-[10px] text-zinc-500 uppercase block font-sans">Blocks Verified</span>
                <span className="text-emerald-400 font-bold text-base">{verification.totalEntries}</span>
              </div>
              <div className="bg-zinc-900/40 p-3 rounded-lg border border-zinc-800">
                <span className="text-[10px] text-zinc-500 uppercase block font-sans">Head Hash</span>
                <span className="text-zinc-200 font-bold text-xs">{truncateHash(verification.headHash)}</span>
              </div>
              <div className="bg-zinc-900/40 p-3 rounded-lg border border-zinc-800">
                <span className="text-[10px] text-zinc-500 uppercase block font-sans">Verified At</span>
                <span className="text-zinc-200 text-xs">{formatDate(verification.verifiedAt)}</span>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl flex items-center justify-between gap-6">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search action, actor ID, resource ref, entry hash..."
            className="input-field pl-9"
          />
        </div>

        <span className="text-xs text-zinc-500 font-mono hidden sm:inline">
          Showing {filteredEntries.length} of {entries.length} blocks
        </span>
      </div>

      {/* Hash-Chain Table */}
      {entriesLoading ? (
        <div className="py-20 text-center text-xs text-zinc-500">Loading audit ledger blocks...</div>
      ) : filteredEntries.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center text-xs text-zinc-500">
          No audit ledger entries found matching your query.
        </div>
      ) : (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs border-collapse">
              <thead>
                <tr className="border-b border-zinc-800 bg-zinc-900/60 text-zinc-400 font-semibold text-[11px] uppercase tracking-wider font-mono">
                  <th className="py-3 px-4">Seq #</th>
                  <th className="py-3 px-4">Action</th>
                  <th className="py-3 px-4">Resource Ref</th>
                  <th className="py-3 px-4">Actor</th>
                  <th className="py-3 px-4 font-mono">Block Entry Hash</th>
                  <th className="py-3 px-4 font-mono">Previous Hash</th>
                  <th className="py-3 px-4">Timestamp</th>
                  <th className="py-3 px-4 text-right">Details</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-zinc-800/80">
                {filteredEntries.map((entry) => {
                  const isExpanded = !!expandedRows[entry.seq];

                  return (
                    <tr key={entry.seq} className="hover:bg-zinc-800/20 transition-colors">
                      <td className="py-3 px-4 font-mono font-bold text-zinc-200">
                        #{entry.seq}
                      </td>

                      <td className="py-3 px-4 font-semibold text-zinc-200">
                        <span className="px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800 font-mono text-[11px] text-zinc-200">
                          {entry.action}
                        </span>
                      </td>

                      <td className="py-3 px-4 font-mono text-zinc-300 truncate max-w-xs">
                        {entry.resourceRef}
                      </td>

                      <td className="py-3 px-4 font-mono text-zinc-400 truncate max-w-[120px]">
                        {entry.actorId || 'SYSTEM'}
                      </td>

                      <td className="py-3 px-4 font-mono text-emerald-400">
                        {truncateHash(entry.entryHash, 6, 6)}
                      </td>

                      <td className="py-3 px-4 font-mono text-zinc-500">
                        {truncateHash(entry.prevHash, 6, 6)}
                      </td>

                      <td className="py-3 px-4 text-zinc-400 whitespace-nowrap font-mono">
                        {formatDate(entry.occurredAt)}
                      </td>

                      <td className="py-3 px-4 text-right">
                        <button
                          onClick={() => toggleRow(entry.seq)}
                          className="btn-secondary text-[11px] py-1 px-2.5"
                        >
                          {isExpanded ? <ChevronDown className="w-3.5 h-3.5" /> : <ChevronRight className="w-3.5 h-3.5" />}
                          <span>{isExpanded ? 'Hide' : 'Inspect'}</span>
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
