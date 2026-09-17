'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  FileSearch,
  ArrowRightLeft,
  Send,
} from 'lucide-react';
import { clsx } from 'clsx';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { custodyApi } from '@/lib/api/custody';
import { usersApi } from '@/lib/api/users';
import { CustodyTimelineView } from '@/components/evidence/custody-timeline-view';
import { CustodyStateBadge } from '@/components/ui/badge';
import { Modal } from '@/components/ui/modal';
import type { EvidenceDetailResponse, CustodyTransferRequest } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export default function CustodyHubPage() {
  const queryClient = useQueryClient();
  const [selectedEvidenceId, setSelectedEvidenceId] = useState<string>('');
  const [isTransferOpen, setIsTransferOpen] = useState(false);
  const [targetUserId, setTargetUserId] = useState('');
  const [transferReason, setTransferReason] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch cases to load all evidence
  const { data: cases = [] } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  // Fetch all evidence
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

  // Fetch users
  const { data: users = [] } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getAll(),
  });

  // Automatically select first evidence item if none selected
  const activeEvidenceId = selectedEvidenceId || allEvidence[0]?.id || '';
  const activeEvidence = allEvidence.find((e) => e.id === activeEvidenceId);

  // Transfer mutation
  const transferMutation = useMutation({
    mutationFn: (req: CustodyTransferRequest) => custodyApi.transfer(activeEvidenceId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['custody-timeline', activeEvidenceId] });
      queryClient.invalidateQueries({ queryKey: ['all-evidence'] });
      setIsTransferOpen(false);
      setTargetUserId('');
      setTransferReason('');
      setFormError(null);
    },
    onError: (err: any) => {
      setFormError(err.message || 'Custody handoff failed');
    },
  });

  const handleTransferSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!targetUserId) {
      setFormError('Please select a recipient party');
      return;
    }
    if (!transferReason.trim()) {
      setFormError('Transfer reason is legally mandatory for chain of custody compliance');
      return;
    }
    transferMutation.mutate({
      toPartyId: targetUserId,
      reason: transferReason.trim(),
    });
  };

  return (
    <div className="space-y-6">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Chain of Custody Command Hub</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Statutory tracking of evidence possession, physical transfers, and forensic handoffs under Indian legal standards
          </p>
        </div>

        {activeEvidenceId && (
          <button
            onClick={() => setIsTransferOpen(true)}
            className="btn-primary shrink-0"
          >
            <ArrowRightLeft className="w-4 h-4" />
            <span>Transfer Custody</span>
          </button>
        )}
      </div>

      {/* Main Two-Column Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column: Evidence Picker */}
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-zinc-800">
            <h3 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
              <div className="w-6 h-6 rounded bg-zinc-100 text-zinc-950 flex items-center justify-center">
                <FileSearch className="w-3.5 h-3.5" />
              </div>
              <span>Evidence Registry ({allEvidence.length})</span>
            </h3>
          </div>

          {evidenceLoading ? (
            <div className="py-12 text-center text-xs text-zinc-500">Loading evidence items...</div>
          ) : allEvidence.length === 0 ? (
            <div className="py-12 text-center text-xs text-zinc-500">No evidence items in vault yet.</div>
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
                      <CustodyStateBadge state={ev.custodyState} />
                    </div>
                    <p className="text-xs font-semibold truncate text-zinc-200">{ev.title}</p>
                    <p className="text-[11px] text-zinc-500 mt-1 truncate font-mono">
                      {ev.deviceMake ? `${ev.deviceMake} ${ev.deviceModel || ''}` : 'Digital Asset'}
                    </p>
                  </button>
                );
              })}
            </div>
          )}
        </div>

        {/* Right Column: Custody Timeline Viewer for Active Item */}
        <div className="lg:col-span-2 space-y-6">
          {activeEvidence ? (
            <>
              {/* Selected Item Summary Bar */}
              <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-mono text-sm font-bold text-zinc-100">{activeEvidence.evidenceNumber}</span>
                    <CustodyStateBadge state={activeEvidence.custodyState} />
                  </div>
                  <h3 className="text-sm font-bold tracking-tight text-zinc-200">{activeEvidence.title}</h3>
                </div>

                <Link
                  href={`/evidence/${activeEvidence.id}`}
                  className="btn-secondary text-xs"
                >
                  <span>Open Full Dossier</span>
                </Link>
              </div>

              {/* Custody Timeline */}
              <CustodyTimelineView evidenceId={activeEvidence.id} canTransfer={true} />
            </>
          ) : (
            <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-16 text-center text-xs text-zinc-500">
              Select an evidence item on the left to view its signed chain of custody timeline.
            </div>
          )}
        </div>
      </div>

      {/* Transfer Modal */}
      {activeEvidence && (
        <Modal
          isOpen={isTransferOpen}
          onClose={() => setIsTransferOpen(false)}
          title={`Transfer Custody of ${activeEvidence.evidenceNumber}`}
          subtitle="Record formal custody handoff in the immutable audit ledger"
        >
          <form onSubmit={handleTransferSubmit} className="space-y-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Recipient Party (Officer / Analyst / Judge) <span className="text-red-400">*</span>
              </label>
              <select
                value={targetUserId}
                onChange={(e) => setTargetUserId(e.target.value)}
                className="select-field"
                required
              >
                <option value="">Select recipient...</option>
                {users.map((u) => (
                  <option key={u.id} value={u.id} className="bg-zinc-900 text-zinc-100">
                    {u.fullName} ({u.role.replace('_', ' ')}) [{u.badgeNumber || u.username}]
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Mandatory Legal Reason for Custody Transfer <span className="text-red-400">*</span>
              </label>
              <textarea
                value={transferReason}
                onChange={(e) => setTransferReason(e.target.value)}
                rows={3}
                placeholder="e.g. Evidence handoff to Forensic Science Laboratory for bit-level extraction..."
                className="input-field"
                required
              />
            </div>

            {formError && (
              <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
                {formError}
              </div>
            )}

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setIsTransferOpen(false)}
                className="btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={transferMutation.isPending}
                className="btn-primary"
              >
                <Send className="w-3.5 h-3.5" />
                <span>{transferMutation.isPending ? 'Signing Handoff...' : 'Record Signed Transfer'}</span>
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
}
