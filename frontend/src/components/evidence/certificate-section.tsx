'use client';

import { useState } from 'react';
import { Award, Download, Plus, Clock } from 'lucide-react';
import { clsx } from 'clsx';
import { certificatesApi } from '@/lib/api/certificates';
import { formatDate } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import type { CertificateResponse, IssueCertificateRequest } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

interface CertificateSectionProps {
  evidenceId: string;
}

export function CertificateSection({ evidenceId }: CertificateSectionProps) {
  const queryClient = useQueryClient();
  const [isIssueOpen, setIsIssueOpen] = useState(false);
  const [mannerOfProduction, setMannerOfProduction] = useState('');
  const [certifyingNotes, setCertifyingNotes] = useState('');
  const [formError, setFormError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  // Fetch certificates for this evidence
  const { data: certificates = [], isLoading } = useQuery({
    queryKey: ['certificates-evidence', evidenceId],
    queryFn: () => certificatesApi.getByEvidenceId(evidenceId),
  });

  // Issue certificate mutation
  const issueMutation = useMutation({
    mutationFn: (req: IssueCertificateRequest) => certificatesApi.issue(evidenceId, req),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['certificates-evidence', evidenceId] });
      queryClient.invalidateQueries({ queryKey: ['certificates'] });
      setIsIssueOpen(false);
      setMannerOfProduction('');
      setCertifyingNotes('');
      setFormError(null);
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to issue statutory certificate');
    },
  });

  const handleIssueSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    issueMutation.mutate({
      mannerOfProduction: mannerOfProduction.trim() || undefined,
      certifyingOfficerNotes: certifyingNotes.trim() || undefined,
    });
  };

  const handleDownload = async (cert: CertificateResponse) => {
    setDownloadingId(cert.id);
    try {
      await certificatesApi.download(cert.id, `${cert.certificateNumber || 'BSA_Section63_Certificate'}.pdf`);
    } catch (err: any) {
      alert(err.message || 'Download failed');
    } finally {
      setDownloadingId(null);
    }
  };

  return (
    <div className="card-zinc">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-zinc-800">
        <div className="flex items-center gap-3">
          <div className="icon-box-primary">
            <Award className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold text-zinc-100 tracking-tight">Statutory BSA 2023 §63 Court Certificates</h3>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">
                INDIAN EVIDENCE ACT REPLACEMENT
              </span>
            </div>
            <p className="text-xs text-zinc-400 mt-0.5">
              Court admissibility certificate binding device particulars, SHA-256 hashes, and custody summary
            </p>
          </div>
        </div>

        <button
          onClick={() => setIsIssueOpen(true)}
          className="btn-primary text-xs shrink-0"
        >
          <Plus className="w-3.5 h-3.5" />
          Issue §63 Certificate
        </button>
      </div>

      {/* Certificates List */}
      <div className="mt-6">
        {isLoading ? (
          <div className="py-12 text-center text-xs text-zinc-400">Loading statutory certificates...</div>
        ) : certificates.length === 0 ? (
          <div className="py-12 text-center">
            <Award className="w-8 h-8 text-zinc-600 mx-auto mb-2" />
            <p className="text-xs text-zinc-400">No Section 63 certificates issued yet for this evidence item.</p>
            <p className="text-[11px] text-zinc-500 mt-1">
              Issue a signed certificate for direct electronic admissibility in Indian courts.
            </p>
          </div>
        ) : (
          <div className="space-y-4">
            {certificates.map((cert) => (
              <div
                key={cert.id}
                className="p-5 rounded-xl bg-zinc-900/60 border border-zinc-800 hover:border-emerald-500/40 transition-all"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-mono text-sm font-bold text-zinc-100">{cert.certificateNumber}</span>
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/30 uppercase font-semibold">
                        {cert.standard || 'BSA §63 (2023)'}
                      </span>
                    </div>
                    <div className="flex flex-wrap items-center gap-4 text-xs text-zinc-400">
                      <span className="flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5" />
                        Issued: {formatDate(cert.issuedAt)}
                      </span>
                      <span>
                        Officer ID: <span className="font-mono text-zinc-200">{cert.issuedByOfficerId}</span>
                      </span>
                      <span>
                        Signing Key: <span className="font-mono text-zinc-200">{cert.signingKeyId}</span>
                      </span>
                    </div>
                  </div>

                  <button
                    onClick={() => handleDownload(cert)}
                    disabled={downloadingId === cert.id}
                    className="btn-secondary text-xs shrink-0"
                  >
                    <Download className={clsx('w-3.5 h-3.5 text-emerald-400', downloadingId === cert.id && 'animate-spin')} />
                    {downloadingId === cert.id ? 'Downloading...' : 'Download Signed PDF'}
                  </button>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-4 pt-3 border-t border-zinc-800 text-xs">
                  <div>
                    <span className="text-zinc-400">Certified SHA-256 Hash Snapshot:</span>
                    <p className="font-mono text-xs text-emerald-400 break-all select-all mt-0.5">
                      {cert.hashSnapshotSha256}
                    </p>
                  </div>
                  <div>
                    <span className="text-zinc-400">Manner of Production:</span>
                    <p className="text-zinc-200 mt-0.5">{cert.mannerOfProduction || 'Standard Ingest Bit-stream copy'}</p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Issue Certificate Modal */}
      <Modal
        isOpen={isIssueOpen}
        onClose={() => setIsIssueOpen(false)}
        title="Issue Statutory BSA §63 Court Certificate"
        subtitle="Generate and digitally sign court-admissible certificate under Bharatiya Sakshya Adhiniyam, 2023 §63"
        maxWidth="xl"
      >
        <form onSubmit={handleIssueSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
              Manner of Production & Technical Acquisition Method
            </label>
            <input
              type="text"
              value={mannerOfProduction}
              onChange={(e) => setMannerOfProduction(e.target.value)}
              placeholder="e.g., Direct bit-stream image preservation from Dell PowerEdge server controller via write-blocker..."
              className="input-field text-xs"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-zinc-400 mb-1.5">
              Certifying Officer Remarks & Admissibility Notes
            </label>
            <textarea
              value={certifyingNotes}
              onChange={(e) => setCertifyingNotes(e.target.value)}
              rows={3}
              placeholder="e.g., Electronic record produced in ordinary course of activity without system alteration..."
              className="input-field text-xs"
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
              onClick={() => setIsIssueOpen(false)}
              className="btn-secondary text-xs"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={issueMutation.isPending}
              className="btn-primary text-xs"
            >
              <Award className="w-3.5 h-3.5" />
              {issueMutation.isPending ? 'Generating & Signing Certificate...' : 'Generate & Digitally Sign PDF'}
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

