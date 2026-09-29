'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  Award,
  Download,
  Search,
  Plus,
  Clock,
  FileSearch,
  Eye,
} from 'lucide-react';
import { clsx } from 'clsx';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { certificatesApi } from '@/lib/api/certificates';
import { formatDate, truncateHash } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import { BsaCertificatePreviewModal } from '@/components/evidence/bsa-certificate-preview-modal';
import type { CertificateResponse, EvidenceDetailResponse, IssueCertificateRequest } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export default function CertificatesHubPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [isIssueOpen, setIsIssueOpen] = useState(false);
  const [selectedEvidenceId, setSelectedEvidenceId] = useState('');
  const [mannerOfProduction, setMannerOfProduction] = useState('');
  const [certifyingNotes, setCertifyingNotes] = useState('');
  const [issueError, setIssueError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  // Preview modal state
  const [previewCert, setPreviewCert] = useState<CertificateResponse | null>(null);
  const [isPreviewOpen, setIsPreviewOpen] = useState(false);

  // Fetch cases
  const { data: cases = [] } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  // Fetch all evidence
  const { data: allEvidence = [] } = useQuery({
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

  // Fetch all certificates across evidence items
  const { data: allCertificates = [], isLoading } = useQuery({
    queryKey: ['all-certificates', allEvidence.map((e) => e.id)],
    queryFn: async () => {
      if (allEvidence.length === 0) return [];
      const promises = allEvidence.map((e) =>
        certificatesApi.getByEvidenceId(e.id).catch(() => [] as CertificateResponse[])
      );
      const results = await Promise.all(promises);
      return results.flat();
    },
    enabled: allEvidence.length > 0,
  });

  // Issue mutation
  const issueMutation = useMutation({
    mutationFn: () => {
      if (!selectedEvidenceId) throw new Error('Please select an evidence item to certify');
      const req: IssueCertificateRequest = {
        mannerOfProduction: mannerOfProduction.trim() || undefined,
        certifyingOfficerNotes: certifyingNotes.trim() || undefined,
      };
      return certificatesApi.issue(selectedEvidenceId, req);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['all-certificates'] });
      setIsIssueOpen(false);
      setSelectedEvidenceId('');
      setMannerOfProduction('');
      setCertifyingNotes('');
      setIssueError(null);
    },
    onError: (err: any) => {
      setIssueError(err.message || 'Certificate generation failed');
    },
  });

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

  const filteredCertificates = allCertificates.filter((c) => {
    const q = searchQuery.toLowerCase();
    return (
      c.certificateNumber.toLowerCase().includes(q) ||
      c.standard.toLowerCase().includes(q) ||
      (c.mannerOfProduction && c.mannerOfProduction.toLowerCase().includes(q)) ||
      c.hashSnapshotSha256.toLowerCase().includes(q)
    );
  });

  const previewEvidence = previewCert ? allEvidence.find((e) => e.id === previewCert.evidenceId) : undefined;
  const previewCase = previewEvidence ? cases.find((c) => c.id === previewEvidence.caseId) : undefined;

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">
            BSA 2023 Section 63 Court Admissibility Certificates
          </h1>
          <p className="text-xs text-zinc-400 mt-1">
            Statutory electronic record certificates required for court admissibility under Bharatiya Sakshya Adhiniyam, 2023 Section 63
          </p>
        </div>

        <button
          onClick={() => setIsIssueOpen(true)}
          className="btn-primary shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>Issue Section 63 Certificate</span>
        </button>
      </div>

      {/* Search Bar */}
      <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl flex items-center justify-between gap-6">
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search certificate number, manner of production, hash..."
            className="input-field !pl-10"
          />
        </div>

        <span className="text-xs text-zinc-500 font-mono hidden sm:inline">
          {filteredCertificates.length} Issued Certificates
        </span>
      </div>

      {/* Certificates List */}
      {isLoading ? (
        <div className="py-20 text-center text-xs text-zinc-500">Loading statutory certificates...</div>
      ) : filteredCertificates.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
          <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-500 mx-auto mb-3">
            <Award className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold tracking-tight text-zinc-100">No Section 63 certificates issued yet</h3>
          <p className="text-xs text-zinc-400 mt-1 max-w-sm mx-auto">
            Generate and digitally sign court-admissible certificate PDFs for any ingested digital evidence item.
          </p>
          <button
            onClick={() => setIsIssueOpen(true)}
            className="mt-4 btn-primary"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Issue Court Certificate</span>
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {filteredCertificates.map((cert) => {
            const ev = allEvidence.find((e) => e.id === cert.evidenceId);

            return (
              <div
                key={cert.id}
                className="border border-zinc-800 bg-zinc-950 hover:border-zinc-700 rounded-xl p-6 transition-colors flex flex-col md:flex-row md:items-center justify-between gap-6"
              >
                <div>
                  <div className="flex flex-wrap items-center gap-2 mb-2">
                    <span className="font-mono text-sm font-bold text-zinc-100">{cert.certificateNumber}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 uppercase font-semibold">
                      {cert.standard || 'BSA Section 63'}
                    </span>
                    {ev && (
                      <Link
                        href={`/evidence/${ev.id}`}
                        className="text-[11px] text-zinc-400 hover:text-zinc-100 flex items-center gap-1 font-mono"
                      >
                        <FileSearch className="w-3 h-3" />
                        {ev.evidenceNumber}
                      </Link>
                    )}
                  </div>

                  <h3 className="text-sm font-bold tracking-tight text-zinc-200">
                    {ev ? `Evidence: ${ev.title}` : 'Digital Evidence Certificate'}
                  </h3>

                  <p className="text-xs text-zinc-400 mt-1 max-w-2xl leading-relaxed">
                    <span className="font-semibold text-zinc-300">Manner of Production: </span>
                    {cert.mannerOfProduction || 'Direct bit-stream copy from seized hardware.'}
                  </p>

                  <div className="flex flex-wrap items-center gap-4 text-[11px] text-zinc-500 mt-3 font-mono">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      Issued: {formatDate(cert.issuedAt)}
                    </span>
                    <span className="text-emerald-400">
                      SHA-256: {truncateHash(cert.hashSnapshotSha256)}
                    </span>
                    <span>Officer: {cert.issuedByOfficerId}</span>
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2 shrink-0">
                  <button
                    onClick={() => {
                      setPreviewCert(cert);
                      setIsPreviewOpen(true);
                    }}
                    className="btn-secondary text-xs"
                  >
                    <Eye className="w-3.5 h-3.5 text-zinc-300" />
                    <span>View Schedule</span>
                  </button>

                  <button
                    onClick={() => handleDownload(cert)}
                    disabled={downloadingId === cert.id}
                    className="btn-primary text-xs"
                  >
                    <Download className={clsx('w-3.5 h-3.5', downloadingId === cert.id && 'animate-spin')} />
                    <span>{downloadingId === cert.id ? 'Streaming...' : 'Download Signed PDF'}</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Issue Certificate Modal */}
      <Modal
        isOpen={isIssueOpen}
        onClose={() => setIsIssueOpen(false)}
        title="Issue Statutory BSA Section 63 Certificate"
        subtitle="Generate signed certificate with device particulars and hash snapshot"
        maxWidth="lg"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            issueMutation.mutate();
          }}
          className="space-y-6"
        >
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Select Evidence Item to Certify <span className="text-red-400">*</span>
            </label>
            <select
              value={selectedEvidenceId}
              onChange={(e) => setSelectedEvidenceId(e.target.value)}
              required
              className="select-field"
            >
              <option value="">Select digital evidence...</option>
              {allEvidence.map((ev) => (
                <option key={ev.id} value={ev.id} className="bg-zinc-900 text-zinc-100">
                  {ev.evidenceNumber} — {ev.title} ({ev.classification})
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Manner of Production & Acquisition Method
            </label>
            <input
              type="text"
              value={mannerOfProduction}
              onChange={(e) => setMannerOfProduction(e.target.value)}
              placeholder="e.g., Direct bit-stream image preservation from Dell PowerEdge server controller..."
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Certifying Officer Remarks
            </label>
            <textarea
              value={certifyingNotes}
              onChange={(e) => setCertifyingNotes(e.target.value)}
              rows={3}
              placeholder="e.g., Evidence seized and sealed in ordinary course of investigation without electronic tampering..."
              className="input-field"
            />
          </div>

          {issueError && (
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
              {issueError}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsIssueOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={issueMutation.isPending}
              className="btn-primary"
            >
              <Award className="w-3.5 h-3.5" />
              <span>{issueMutation.isPending ? 'Generating...' : 'Issue & Sign Certificate'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Interactive Certificate Schedule Preview Modal */}
      {previewCert && (
        <BsaCertificatePreviewModal
          isOpen={isPreviewOpen}
          onClose={() => setIsPreviewOpen(false)}
          certificate={previewCert}
          evidence={previewEvidence}
          caseData={previewCase}
        />
      )}
    </div>
  );
}
