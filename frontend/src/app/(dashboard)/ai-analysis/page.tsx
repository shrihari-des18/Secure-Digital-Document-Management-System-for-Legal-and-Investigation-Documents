'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  FileSearch,
} from 'lucide-react';
import { clsx } from 'clsx';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { aiApi } from '@/lib/api/ai';
import { AiAnalysisPanel } from '@/components/evidence/ai-analysis-panel';
import { PiiRedactorWorkbench } from '@/components/evidence/pii-redactor-workbench';
import { ClassificationBadge } from '@/components/ui/badge';
import type { EvidenceDetailResponse } from '@/types';
import { useQuery } from '@tanstack/react-query';

export default function AiAnalysisHubPage() {
  const [selectedEvidenceId, setSelectedEvidenceId] = useState<string>('');

  // Fetch cases
  const { data: cases = [] } = useQuery({
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

  const activeEvidenceId = selectedEvidenceId || allEvidence[0]?.id || '';
  const activeEvidence = allEvidence.find((e) => e.id === activeEvidenceId);

  // Fetch AI analysis results for active evidence
  const { data: analysis } = useQuery({
    queryKey: ['ai-analysis', activeEvidenceId],
    queryFn: () => aiApi.getResults(activeEvidenceId),
    enabled: !!activeEvidenceId,
    retry: false,
  });

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">AI Forensics & Intelligence Engine</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Asynchronous neural pipelines for OCR text extraction, PII entity discovery, auto-classification, and pHash similarity
          </p>
        </div>
      </div>

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        {/* Left Column: Evidence List */}
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between pb-4 border-b border-zinc-800">
            <h3 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
              <div className="w-6 h-6 rounded bg-zinc-100 text-zinc-950 flex items-center justify-center">
                <FileSearch className="w-3.5 h-3.5" />
              </div>
              <span>Evidence Items ({allEvidence.length})</span>
            </h3>
          </div>

          {evidenceLoading ? (
            <div className="py-12 text-center text-xs text-zinc-500">Loading evidence items...</div>
          ) : allEvidence.length === 0 ? (
            <div className="py-12 text-center text-xs text-zinc-500">No evidence items available.</div>
          ) : (
            <div className="space-y-2 max-h-[600px] overflow-y-auto pr-1">
              {allEvidence.map((ev) => {
                const isSelected = ev.id === activeEvidenceId;

                return (
                  <button
                    key={ev.id}
                    onClick={() => setSelectedEvidenceId(ev.id)}
                    className={clsx(
                      'w-full text-left p-3.5 rounded-lg border transition-colors cursor-pointer',
                      isSelected
                        ? 'bg-zinc-900 border-zinc-700 text-zinc-100'
                        : 'bg-zinc-900/30 border-zinc-800 hover:bg-zinc-800/40 text-zinc-400 hover:text-zinc-200'
                    )}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="font-mono text-xs font-bold text-zinc-200">{ev.evidenceNumber}</span>
                      <ClassificationBadge level={ev.classification} />
                    </div>
                    <p className="text-xs font-semibold truncate text-zinc-200">{ev.title}</p>
                    <p className="text-[11px] text-zinc-500 mt-1 truncate font-mono">
                      {ev.deviceMake ? `${ev.deviceMake} ${ev.deviceModel || ''}` : 'Digital File'}
                    </p>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: AI Forensics Panel & PII Redactor Workbench */}
        <div className="lg:col-span-2 space-y-8">
          {activeEvidence ? (
            <>
              {/* Selected Item Summary Bar */}
              <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 flex flex-col sm:flex-row sm:items-center justify-between gap-6">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-mono text-sm font-bold text-zinc-100">{activeEvidence.evidenceNumber}</span>
                    <ClassificationBadge level={activeEvidence.classification} />
                  </div>
                  <h3 className="text-sm font-bold tracking-tight text-zinc-200">{activeEvidence.title}</h3>
                </div>

                <Link
                  href={`/evidence/${activeEvidence.id}`}
                  className="btn-secondary text-xs"
                >
                  <span>Open Full Evidence Dossier →</span>
                </Link>
              </div>

              {/* AI Analysis Panel */}
              <AiAnalysisPanel evidenceId={activeEvidence.id} />

              {/* PII Redactor Workbench */}
              {analysis && <PiiRedactorWorkbench analysis={analysis} />}
            </>
          ) : (
            <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-16 text-center text-xs text-zinc-500">
              Select an evidence item from the list on the left to run or inspect AI analysis.
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
