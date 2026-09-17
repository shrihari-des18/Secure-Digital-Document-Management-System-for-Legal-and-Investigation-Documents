'use client';

import { useState } from 'react';
import {
  FilePlus,
  ArrowRightLeft,
  Eye,
  Download,
  Brain,
  Award,
  Trash2,
  Clock,
  User,
  Shield,
  Send,
  Plus,
} from 'lucide-react';
import { clsx } from 'clsx';
import { custodyApi } from '@/lib/api/custody';
import { usersApi } from '@/lib/api/users';
import { formatDate } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import type { CustodyTransferRequest } from '@/types';
import { CustodyEventType } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

interface CustodyTimelineViewProps {
  evidenceId: string;
  canTransfer?: boolean;
}

const EVENT_CONFIGS: Record<
  CustodyEventType,
  { label: string; icon: React.ComponentType<{ className?: string }>; color: string; bg: string; dotBg: string }
> = {
  [CustodyEventType.INGEST]: {
    label: 'Initial Seizure & Ingestion',
    icon: FilePlus,
    color: 'text-emerald-400',
    bg: 'bg-emerald-500/10 border-emerald-500/30',
    dotBg: 'bg-emerald-400',
  },
  [CustodyEventType.TRANSFER]: {
    label: 'Custody Handoff',
    icon: ArrowRightLeft,
    color: 'text-blue-400',
    bg: 'bg-blue-500/10 border-blue-500/30',
    dotBg: 'bg-blue-400',
  },
  [CustodyEventType.VIEW]: {
    label: 'Evidence Inspection / View',
    icon: Eye,
    color: 'text-blue-400',
    bg: 'bg-blue-500/10 border-blue-500/30',
    dotBg: 'bg-blue-400',
  },
  [CustodyEventType.DOWNLOAD]: {
    label: 'Binary Content Export',
    icon: Download,
    color: 'text-zinc-300',
    bg: 'bg-zinc-800 border-zinc-700',
    dotBg: 'bg-zinc-300',
  },
  [CustodyEventType.ANALYZE]: {
    label: 'Forensic AI / ML Analysis',
    icon: Brain,
    color: 'text-amber-400',
    bg: 'bg-amber-500/10 border-amber-500/30',
    dotBg: 'bg-amber-400',
  },
  [CustodyEventType.CERTIFY]: {
    label: '§63 Certificate Generated',
    icon: Award,
    color: 'text-emerald-400',
    bg: 'bg-emerald-500/10 border-emerald-500/30',
    dotBg: 'bg-emerald-400',
  },
  [CustodyEventType.DISPOSE]: {
    label: 'Audited Disposal',
    icon: Trash2,
    color: 'text-red-400',
    bg: 'bg-red-500/10 border-red-500/30',
    dotBg: 'bg-red-400',
  },
};

export function CustodyTimelineView({ evidenceId, canTransfer = true }: CustodyTimelineViewProps) {
  const queryClient = useQueryClient();
  const [isTransferOpen, setIsTransferOpen] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState('');
  const [reason, setReason] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch timeline
  const { data: timeline = [], isLoading } = useQuery({
    queryKey: ['custody-timeline', evidenceId],
    queryFn: () => custodyApi.getTimeline(evidenceId),
  });

  // Fetch users for transfer dropdown
  const { data: users = [] } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getAll(),
    enabled: isTransferOpen,
  });

  // Transfer mutation
  const transferMutation = useMutation({
    mutationFn: (req: CustodyTransferRequest) => custodyApi.transfer(evidenceId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['custody-timeline', evidenceId] });
      queryClient.invalidateQueries({ queryKey: ['evidence', evidenceId] });
      setIsTransferOpen(false);
      setSelectedUserId('');
      setReason('');
      setFormError(null);
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to transfer custody');
    },
  });

  const handleTransferSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserId) {
      setFormError('Please select a recipient officer or analyst');
      return;
    }
    if (!reason.trim()) {
      setFormError('Transfer reason is legally mandatory under chain of custody rules');
      return;
    }
    transferMutation.mutate({
      toPartyId: selectedUserId,
      reason: reason.trim(),
    });
  };

  return (
    <div className="card-zinc">
      {/* Header */}
      <div className="flex items-center justify-between pb-5 border-b border-zinc-800">
        <div className="flex items-center gap-3">
          <div className="icon-box-primary">
            <Shield className="w-5 h-5" />
          </div>
          <div>
            <h3 className="text-base font-bold text-zinc-100 tracking-tight">
              Chain of Custody Ledger & Audit Timeline
            </h3>
            <p className="text-xs text-zinc-400 mt-0.5">
              Immutable, signed record of every physical handoff and digital interaction
            </p>
          </div>
        </div>

        {canTransfer && (
          <button
            onClick={() => setIsTransferOpen(true)}
            className="btn-primary text-xs"
          >
            <Plus className="w-3.5 h-3.5" />
            Transfer Custody
          </button>
        )}
      </div>

      {/* Timeline view */}
      <div className="mt-6 relative">
        {isLoading ? (
          <div className="py-12 text-center text-xs text-zinc-400">Loading custody timeline...</div>
        ) : timeline.length === 0 ? (
          <div className="py-12 text-center text-xs text-zinc-400">No custody events recorded yet.</div>
        ) : (
          <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-zinc-800">
            {timeline.map((event, idx) => {
              const cfg = EVENT_CONFIGS[event.eventType] || {
                label: event.eventType,
                icon: Clock,
                color: 'text-zinc-300',
                bg: 'bg-zinc-900 border-zinc-800',
                dotBg: 'bg-zinc-400',
              };
              const Icon = cfg.icon;

              return (
                <div key={event.id || idx} className="relative group">
                  {/* Timeline dot */}
                  <div
                    className={clsx(
                      'absolute -left-6 top-1.5 w-5 h-5 rounded-full border flex items-center justify-center -translate-x-1/2 shadow-sm',
                      cfg.bg
                    )}
                  >
                    <div className={clsx('w-2 h-2 rounded-full', cfg.dotBg)} />
                  </div>

                  {/* Event content box */}
                  <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800 group-hover:border-zinc-700 hover:bg-zinc-800/20 transition-colors">
                    <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-1 mb-2">
                      <div className="flex items-center gap-2">
                        <Icon className={clsx('w-4 h-4', cfg.color)} />
                        <span className="text-xs font-bold text-zinc-100">{cfg.label}</span>
                        <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-zinc-900 border border-zinc-800 text-zinc-400">
                          {event.eventType}
                        </span>
                      </div>
                      <span className="text-[11px] text-zinc-400 font-mono flex items-center gap-1">
                        <Clock className="w-3 h-3" />
                        {formatDate(event.occurredAt)}
                      </span>
                    </div>

                    {/* Parties info */}
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs text-zinc-400 my-2">
                      {event.actorId && (
                        <div className="flex items-center gap-1.5">
                          <User className="w-3.5 h-3.5 text-zinc-500" />
                          <span>Actor ID: </span>
                          <span className="font-mono text-zinc-200">{event.actorId}</span>
                        </div>
                      )}
                      {event.toParty && (
                        <div className="flex items-center gap-1.5">
                          <ArrowRightLeft className="w-3.5 h-3.5 text-blue-400" />
                          <span>Transferred To: </span>
                          <span className="font-mono text-blue-300">{event.toParty}</span>
                        </div>
                      )}
                    </div>

                    {/* Reason statement */}
                    {event.reason && (
                      <div className="mt-2 text-xs text-zinc-200 bg-zinc-950 p-2.5 rounded-lg border border-zinc-800">
                        <span className="text-zinc-400 font-semibold">Legal Reason / Remarks: </span>
                        {event.reason}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Custody Transfer Modal */}
      <Modal
        isOpen={isTransferOpen}
        onClose={() => setIsTransferOpen(false)}
        title="Transfer Evidence Custody"
        subtitle="Record a formal custody handoff in the immutable audit ledger"
      >
        <form onSubmit={handleTransferSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
              Recipient Party (Officer / Forensic Analyst / Court)*
            </label>
            <select
              value={selectedUserId}
              onChange={(e) => setSelectedUserId(e.target.value)}
              className="select-field text-xs"
              required
            >
              <option value="">Select recipient user...</option>
              {users.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.fullName} ({u.role.replace('_', ' ')}) — {u.department || 'General'} [{u.badgeNumber || u.username}]
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
              Mandatory Legal Reason & Purpose of Transfer*
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              rows={3}
              placeholder="e.g., Handover to Cyber Forensics Division for bit-stream analysis and artifact extraction..."
              className="input-field text-xs"
              required
            />
          </div>

          {formError && (
            <div className="p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
              {formError}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsTransferOpen(false)}
              className="btn-secondary text-xs"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={transferMutation.isPending}
              className="btn-primary text-xs"
            >
              <Send className="w-3.5 h-3.5" />
              {transferMutation.isPending ? 'Signing Handoff...' : 'Record Signed Transfer'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

