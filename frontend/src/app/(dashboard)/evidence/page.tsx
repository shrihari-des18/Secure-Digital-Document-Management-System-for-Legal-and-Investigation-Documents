'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  FileSearch,
  Plus,
  Search,
  ArrowRight,
} from 'lucide-react';
import { clsx } from 'clsx';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { formatDate, truncateHash, formatBytes } from '@/lib/crypto';
import { ClassificationBadge, CustodyStateBadge } from '@/components/ui/badge';
import type { EvidenceDetailResponse } from '@/types';
import { ClassificationLevel, CustodyState } from '@/types';
import { useQuery } from '@tanstack/react-query';

export default function EvidenceVaultPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedClassification, setSelectedClassification] = useState<ClassificationLevel | 'ALL'>('ALL');
  const [selectedCustodyState, setSelectedCustodyState] = useState<CustodyState | 'ALL'>('ALL');
  const [selectedCaseId, setSelectedCaseId] = useState<string>('ALL');

  // Fetch all cases to query evidence
  const { data: cases = [] } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  // Fetch all evidence across cases
  const { data: allEvidence = [], isLoading } = useQuery({
    queryKey: ['all-evidence', cases.map((c) => c.id)],
    queryFn: async () => {
      if (cases.length === 0) return [];
      const evidencePromises = cases.map((c) =>
        evidenceApi.getByCaseId(c.id).catch(() => [] as EvidenceDetailResponse[])
      );
      const results = await Promise.all(evidencePromises);
      return results.flat();
    },
    enabled: cases.length > 0,
  });

  const filteredEvidence = allEvidence.filter((ev) => {
    const matchSearch =
      ev.evidenceNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      ev.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (ev.deviceMake && ev.deviceMake.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (ev.deviceModel && ev.deviceModel.toLowerCase().includes(searchQuery.toLowerCase())) ||
      (ev.seizedLocation && ev.seizedLocation.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchClass =
      selectedClassification === 'ALL' || ev.classification === selectedClassification;

    const matchCustody =
      selectedCustodyState === 'ALL' || ev.custodyState === selectedCustodyState;

    const matchCase = selectedCaseId === 'ALL' || ev.caseId === selectedCaseId;

    return matchSearch && matchClass && matchCustody && matchCase;
  });

  return (
    <div className="space-y-6">
      {/* Top Banner & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Digital Evidence Vault</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Tamper-evident, cryptographically sealed digital evidence registry with SHA-256 baseline
          </p>
        </div>

        <Link
          href="/evidence/upload"
          className="btn-primary shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Ingest & Seal Evidence</span>
        </Link>
      </div>

      {/* Search and Filters */}
      <div className="border border-zinc-800 bg-zinc-950 p-4 rounded-xl space-y-3">
        <div className="flex flex-col md:flex-row items-center justify-between gap-3">
          {/* Search */}
          <div className="relative w-full md:w-96">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search by evidence ID, title, device make/model..."
              className="input-field pl-9"
            />
          </div>

          {/* Case selector filter */}
          <div className="w-full md:w-64">
            <select
              value={selectedCaseId}
              onChange={(e) => setSelectedCaseId(e.target.value)}
              className="select-field"
            >
              <option value="ALL">Filter by Case (All Cases)</option>
              {cases.map((c) => (
                <option key={c.id} value={c.id} className="bg-zinc-900 text-zinc-100">
                  {c.caseNumber} — {c.title}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Classification and Custody Pills */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-zinc-800/80 font-mono text-xs">
          <span className="text-[11px] font-semibold text-zinc-500 uppercase">Classification:</span>
          {(['ALL', ...Object.values(ClassificationLevel)] as const).map((lvl) => (
            <button
              key={lvl}
              onClick={() => setSelectedClassification(lvl)}
              className={clsx(
                'text-[11px] px-2.5 py-1 rounded-lg font-medium transition-colors',
                selectedClassification === lvl
                  ? 'bg-zinc-100 text-zinc-950 font-bold border border-zinc-100'
                  : 'bg-zinc-900/60 text-zinc-400 hover:text-zinc-200 border border-zinc-800'
              )}
            >
              {lvl}
            </button>
          ))}

          <div className="h-4 w-px bg-zinc-800 mx-2 hidden sm:block" />

          <span className="text-[11px] font-semibold text-zinc-500 uppercase">State:</span>
          {(['ALL', ...Object.values(CustodyState)] as const).map((st) => (
            <button
              key={st}
              onClick={() => setSelectedCustodyState(st)}
              className={clsx(
                'text-[11px] px-2.5 py-1 rounded-lg font-medium transition-colors',
                selectedCustodyState === st
                  ? 'bg-zinc-100 text-zinc-950 font-bold border border-zinc-100'
                  : 'bg-zinc-900/60 text-zinc-400 hover:text-zinc-200 border border-zinc-800'
              )}
            >
              {st.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Evidence items grid */}
      {isLoading ? (
        <div className="py-20 text-center text-xs text-zinc-500">Loading evidence registry...</div>
      ) : filteredEvidence.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
          <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-500 mx-auto mb-3">
            <FileSearch className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold tracking-tight text-zinc-100">No digital evidence items found</h3>
          <p className="text-xs text-zinc-400 mt-1 max-w-sm mx-auto">
            {searchQuery || selectedClassification !== 'ALL' || selectedCustodyState !== 'ALL'
              ? 'No evidence items match your filters. Try resetting your search criteria.'
              : 'Upload and cryptographically seal an evidence file to begin chain of custody tracking.'}
          </p>
          <Link
            href="/evidence/upload"
            className="mt-4 btn-primary"
          >
            <Plus className="w-4 h-4" />
            <span>Ingest First Evidence Item</span>
          </Link>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredEvidence.map((ev) => {
            const parentCase = cases.find((c) => c.id === ev.caseId);

            return (
              <Link
                key={ev.id}
                href={`/evidence/${ev.id}`}
                className="group border border-zinc-800 bg-zinc-950 p-6 rounded-xl hover:border-zinc-700 hover:bg-zinc-800/20 transition-colors flex flex-col justify-between"
              >
                <div>
                  <div className="flex items-start justify-between gap-2 mb-3">
                    <span className="font-mono text-xs font-bold text-zinc-200 px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800">
                      {ev.evidenceNumber}
                    </span>
                    <CustodyStateBadge state={ev.custodyState} />
                  </div>

                  <h3 className="text-sm font-bold tracking-tight text-zinc-100 group-hover:text-zinc-200 transition-colors line-clamp-1">
                    {ev.title}
                  </h3>
                  <p className="text-xs text-zinc-400 line-clamp-2 mt-1.5 leading-relaxed">
                    {ev.description || 'No description provided for this evidence item.'}
                  </p>

                  <div className="flex items-center gap-2 mt-3">
                    <ClassificationBadge level={ev.classification} />
                    {ev.file?.sizeBytes && (
                      <span className="text-[11px] font-mono text-zinc-400 px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800">
                        {formatBytes(ev.file.sizeBytes)}
                      </span>
                    )}
                  </div>
                </div>

                <div className="mt-5 pt-4 border-t border-zinc-800 space-y-2">
                  <div className="flex items-center justify-between text-xs text-zinc-400 font-mono">
                    <span className="text-[11px] text-emerald-400">
                      SHA: {truncateHash(ev.file?.sha256)}
                    </span>
                    <span className="text-[11px] text-zinc-500">{formatDate(ev.createdAt)}</span>
                  </div>

                  {parentCase && (
                    <div className="text-[11px] text-zinc-500 truncate font-mono">
                      Case: <span className="text-zinc-300 font-medium">{parentCase.caseNumber}</span>
                    </div>
                  )}

                  <div className="flex items-center justify-end text-zinc-200 font-semibold text-[11px] group-hover:translate-x-0.5 transition-transform pt-1 font-mono">
                    <span>Inspect Evidence</span>
                    <ArrowRight className="w-3.5 h-3.5 ml-1" />
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
