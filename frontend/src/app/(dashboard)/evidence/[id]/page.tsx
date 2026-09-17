'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import {
  ChevronLeft,
  Download,
  Briefcase,
  AlertTriangle,
  Brain,
  Award,
  Link2,
} from 'lucide-react';
import { clsx } from 'clsx';
import { evidenceApi } from '@/lib/api/evidence';
import { casesApi } from '@/lib/api/cases';
import { formatDate, formatBytes } from '@/lib/crypto';
import { ClassificationBadge, CustodyStateBadge } from '@/components/ui/badge';
import { Tabs } from '@/components/ui/tabs';
import { IntegritySealCard } from '@/components/evidence/integrity-seal-card';
import { CustodyTimelineView } from '@/components/evidence/custody-timeline-view';
import { AiAnalysisPanel } from '@/components/evidence/ai-analysis-panel';
import { CertificateSection } from '@/components/evidence/certificate-section';
import { useQuery } from '@tanstack/react-query';

export default function EvidenceDetailPage() {
  const params = useParams();
  const evidenceId = params?.id as string;

  const [activeTab, setActiveTab] = useState<'custody' | 'ai' | 'certificates'>('custody');
  const [downloading, setDownloading] = useState(false);

  // Fetch evidence details
  const { data: evidence, isLoading, error } = useQuery({
    queryKey: ['evidence', evidenceId],
    queryFn: () => evidenceApi.getById(evidenceId),
    enabled: !!evidenceId,
  });

  // Fetch parent case
  const { data: parentCase } = useQuery({
    queryKey: ['case', evidence?.caseId],
    queryFn: () => casesApi.getById(evidence!.caseId),
    enabled: !!evidence?.caseId,
  });

  const handleDownload = async () => {
    if (!evidence) return;
    setDownloading(true);
    try {
      await evidenceApi.download(evidence.id, evidence.file?.originalFileName || `${evidence.evidenceNumber}.bin`);
    } catch (err: any) {
      alert(err.message || 'Download failed');
    } finally {
      setDownloading(false);
    }
  };

  if (isLoading) {
    return <div className="py-20 text-center text-xs text-zinc-500">Loading sealed evidence dossier...</div>;
  }

  if (!evidence || error) {
    return (
      <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
        <AlertTriangle className="w-10 h-10 text-amber-400 mx-auto mb-3" />
        <h2 className="text-sm font-bold tracking-tight text-zinc-100">Evidence item not found or clearance restricted</h2>
        <p className="text-xs text-zinc-400 mt-1">
          The requested evidence identifier does not exist or you lack ABAC clearance to view it.
        </p>
        <Link
          href="/evidence"
          className="mt-4 btn-primary"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Back to Evidence Vault</span>
        </Link>
      </div>
    );
  }

  const tabs = [
    { id: 'custody', label: 'Chain of Custody Ledger', icon: Link2 },
    { id: 'ai', label: 'AI Forensics & OCR Extraction', icon: Brain },
    { id: 'certificates', label: 'BSA §63 Statutory Certificates', icon: Award },
  ];

  return (
    <div className="space-y-6">
      {/* Back button */}
      <div className="flex items-center justify-between">
        <Link
          href="/evidence"
          className="inline-flex items-center gap-1.5 text-xs text-zinc-400 hover:text-zinc-100 transition-colors font-mono"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Back to Evidence Vault</span>
        </Link>

        {parentCase && (
          <Link
            href={`/cases/${parentCase.id}`}
            className="inline-flex items-center gap-1.5 text-xs text-zinc-300 hover:text-zinc-100 hover:underline font-mono"
          >
            <Briefcase className="w-3.5 h-3.5" />
            <span>Case Docket: {parentCase.caseNumber}</span>
          </Link>
        )}
      </div>

      {/* Header card */}
      <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 pb-5 border-b border-zinc-800">
          <div>
            <div className="flex items-center gap-2.5 mb-2">
              <span className="font-mono text-xs font-bold text-zinc-200 px-2.5 py-0.5 rounded bg-zinc-900 border border-zinc-800">
                {evidence.evidenceNumber}
              </span>
              <ClassificationBadge level={evidence.classification} />
              <CustodyStateBadge state={evidence.custodyState} />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-zinc-100">{evidence.title}</h1>
            <p className="text-xs text-zinc-400 mt-1 leading-relaxed max-w-3xl">
              {evidence.description || 'No descriptive context recorded during seizure.'}
            </p>
          </div>

          <div className="flex items-center gap-2 shrink-0">
            <button
              onClick={handleDownload}
              disabled={downloading}
              className="btn-primary"
            >
              <Download className={clsx('w-4 h-4', downloading && 'animate-spin')} />
              <span>{downloading ? 'Streaming...' : 'Download Sealed Binary'}</span>
            </button>
          </div>
        </div>

        {/* Device particulars and seizure specs */}
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 pt-5 text-xs">
          <div>
            <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Hardware Source</span>
            <span className="text-zinc-200 font-medium mt-0.5 block">
              {evidence.deviceMake ? `${evidence.deviceMake} ${evidence.deviceModel || ''}` : 'Direct Upload'}
            </span>
            {evidence.deviceSerialNumber && (
              <span className="text-[10px] font-mono text-zinc-500">SN: {evidence.deviceSerialNumber}</span>
            )}
          </div>

          <div>
            <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Seizure Location</span>
            <span className="text-zinc-200 font-medium mt-0.5 block truncate">
              {evidence.seizedLocation || 'Unspecified Jurisdiction'}
            </span>
            <span className="text-[10px] text-zinc-500 font-mono">{formatDate(evidence.seizedAt)}</span>
          </div>

          <div>
            <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Binary File Specs</span>
            <span className="text-zinc-200 font-medium mt-0.5 block truncate font-mono">
              {evidence.file?.originalFileName || 'raw_data.bin'}
            </span>
            <span className="text-[10px] font-mono text-emerald-400">
              {evidence.file?.sizeBytes ? formatBytes(evidence.file.sizeBytes) : '—'} • {evidence.file?.contentType || 'binary'}
            </span>
          </div>

          <div>
            <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Custodian State</span>
            <span className="text-zinc-200 font-medium mt-0.5 block">
              {evidence.custodyState.replace('_', ' ')}
            </span>
            <span className="text-[10px] font-mono text-zinc-500">
              ID: {evidence.currentCustodianId || evidence.createdBy}
            </span>
          </div>
        </div>
      </div>

      {/* Cryptographic Integrity Card */}
      <IntegritySealCard evidence={evidence} />

      {/* Tabs */}
      <Tabs tabs={tabs} activeTab={activeTab} onChange={(id) => setActiveTab(id as any)} />

      {/* Tab 1: Chain of Custody */}
      {activeTab === 'custody' && <CustodyTimelineView evidenceId={evidence.id} />}

      {/* Tab 2: AI Forensics */}
      {activeTab === 'ai' && <AiAnalysisPanel evidenceId={evidence.id} />}

      {/* Tab 3: Statutory Certificates */}
      {activeTab === 'certificates' && <CertificateSection evidenceId={evidence.id} />}
    </div>
  );
}
