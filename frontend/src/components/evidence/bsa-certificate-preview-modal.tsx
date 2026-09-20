'use client';

import { useState } from 'react';
import { Download, ShieldCheck, Check, Copy, Scale } from 'lucide-react';
import { clsx } from 'clsx';
import { formatDate } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import type { CertificateResponse, EvidenceDetailResponse, CaseResponse } from '@/types';
import { certificatesApi } from '@/lib/api/certificates';

interface BsaCertificatePreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  certificate: CertificateResponse;
  evidence?: EvidenceDetailResponse;
  caseData?: CaseResponse;
}

export function BsaCertificatePreviewModal({
  isOpen,
  onClose,
  certificate,
  evidence,
  caseData,
}: BsaCertificatePreviewModalProps) {
  const [downloading, setDownloading] = useState(false);
  const [copied, setCopied] = useState(false);

  const handleDownloadPdf = async () => {
    setDownloading(true);
    try {
      await certificatesApi.download(
        certificate.id,
        `${certificate.certificateNumber || 'BSA_Section63_Certificate'}.pdf`
      );
    } catch (err: any) {
      alert(err.message || 'PDF Download failed');
    } finally {
      setDownloading(false);
    }
  };

  const handleCopyDeclaration = () => {
    const text = `I hereby certify under Section 63 of the Bharatiya Sakshya Adhiniyam, 2023 that the electronic record identified as ${
      evidence?.evidenceNumber || certificate.evidenceId
    } was produced by a digital device operating in regular course of activity, and its cryptographic hash ${
      certificate.hashSnapshotSha256
    } remains unaltered. Certificate No: ${certificate.certificateNumber}`;
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Statutory Evidence Certificate (§63 BSA 2023)"
      subtitle={`Official Admissibility Schedule • ${certificate.certificateNumber}`}
      maxWidth="2xl"
    >
      <div className="space-y-6">
        {/* Certificate Paper Sheet (Legal Courtroom Document Styling) */}
        <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-6 sm:p-8 text-zinc-100 shadow-2xl relative overflow-hidden font-serif">
          {/* Watermark */}
          <div className="absolute inset-0 flex items-center justify-center opacity-5 pointer-events-none select-none">
            <Scale className="w-96 h-96 text-zinc-400" />
          </div>

          {/* Header */}
          <div className="text-center pb-6 border-b border-zinc-800 space-y-1">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-amber-500/10 border border-amber-500/20 text-amber-400 text-xs font-mono font-bold tracking-widest uppercase mb-2">
              <Scale className="w-3.5 h-3.5" />
              <span>Bharatiya Sakshya Adhiniyam, 2023</span>
            </div>
            <h2 className="text-lg font-bold tracking-tight text-zinc-100 uppercase tracking-wider">
              Certificate of Authenticity for Electronic Records
            </h2>
            <p className="text-xs text-zinc-400 font-sans">
              [Issued pursuant to Section 63(4) of the Bharatiya Sakshya Adhiniyam, 2023 (Successor to Section 65B of
              the Indian Evidence Act, 1872)]
            </p>
          </div>

          {/* Certificate Metadata Bar */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 my-5 py-3 px-4 bg-zinc-900/60 rounded-lg border border-zinc-800 text-xs font-sans">
            <div>
              <span className="text-zinc-500 text-[10px] uppercase font-bold font-mono block">Certificate No:</span>
              <span className="font-mono text-zinc-100 font-bold">{certificate.certificateNumber}</span>
            </div>
            <div>
              <span className="text-zinc-500 text-[10px] uppercase font-bold font-mono block">Date of Issuance:</span>
              <span className="font-medium text-zinc-300 font-mono">{formatDate(certificate.issuedAt)}</span>
            </div>
            <div>
              <span className="text-zinc-500 text-[10px] uppercase font-bold font-mono block">Case / FIR Docket:</span>
              <span className="font-mono text-zinc-100 font-bold">{caseData?.caseNumber || 'CASE-REGISTRY'}</span>
            </div>
          </div>

          {/* Part A: Particulars of Electronic Record */}
          <div className="space-y-3 font-sans text-xs">
            <h3 className="text-xs font-bold text-zinc-100 uppercase tracking-wider font-serif border-b border-zinc-800 pb-1">
              Part A — Description & Production Particulars
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-zinc-300">
              <div>
                <span className="text-zinc-500 font-semibold block">Evidence Item Identifier:</span>
                <span className="font-mono text-zinc-200">{evidence?.evidenceNumber || certificate.evidenceId}</span>
              </div>
              <div>
                <span className="text-zinc-500 font-semibold block">Description / Title:</span>
                <span className="text-zinc-200">{evidence?.title || 'Seized Digital Evidence Binary'}</span>
              </div>
              <div>
                <span className="text-zinc-500 font-semibold block">Device / Hardware Source:</span>
                <span className="text-zinc-200">
                  {evidence?.deviceMake ? `${evidence.deviceMake} ${evidence.deviceModel || ''}` : 'Digital Asset'}
                </span>
              </div>
              <div>
                <span className="text-zinc-500 font-semibold block">Seizure Jurisdiction / Officer:</span>
                <span className="text-zinc-200">{caseData?.jurisdiction || 'Investigation Agency'}</span>
              </div>
            </div>

            <div>
              <span className="text-zinc-500 font-semibold block">Manner of Production & Acquisition:</span>
              <p className="mt-1 p-2.5 rounded-lg bg-zinc-900/60 border border-zinc-800 text-zinc-300 leading-relaxed italic font-serif">
                &ldquo;{certificate.mannerOfProduction || 'Direct bit-stream byte image produced by write-blocked storage controller in regular course of activity.'}&rdquo;
              </p>
            </div>
          </div>

          {/* Part B: Cryptographic Hash Verification Schedule */}
          <div className="space-y-3 font-sans text-xs mt-6">
            <h3 className="text-xs font-bold text-zinc-100 uppercase tracking-wider font-serif border-b border-zinc-800 pb-1">
              Part B — Cryptographic Hash Schedule & Integrity Assertion
            </h3>

            <div className="p-3.5 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-2">
              <div className="flex items-center gap-2 text-emerald-400 font-bold font-mono">
                <ShieldCheck className="w-4 h-4" />
                <span>Certified Cryptographic SHA-256 Digest Snapshot:</span>
              </div>
              <p className="font-mono text-xs text-emerald-400 break-all select-all bg-zinc-950 p-2 rounded border border-zinc-800">
                {certificate.hashSnapshotSha256}
              </p>
              {certificate.hashSnapshotSha512 && (
                <div>
                  <span className="text-[10px] text-zinc-500 font-mono block mb-1">SHA-512 Extended Digest:</span>
                  <p className="font-mono text-[10px] text-zinc-400 break-all select-all bg-zinc-950 p-1.5 rounded border border-zinc-800">
                    {certificate.hashSnapshotSha512}
                  </p>
                </div>
              )}
            </div>
          </div>

          {/* Statutory Declaration & Signature Seal */}
          <div className="mt-8 pt-5 border-t border-zinc-800 flex flex-col sm:flex-row sm:items-end justify-between gap-4 font-sans text-xs">
            <div className="space-y-1 max-w-sm text-[11px] text-zinc-400">
              <span className="font-bold text-zinc-300 block">Statutory Declaration:</span>
              <p className="leading-relaxed">
                Certified that the device was in lawful custody, operating properly without unauthorized alteration.
              </p>
            </div>

            <div className="p-3 rounded-lg bg-zinc-900/60 border border-zinc-800 text-center space-y-1 shrink-0">
              <span className="text-[10px] font-mono text-zinc-400 uppercase font-bold block">
                Digital Signature Verification
              </span>
              <span className="font-mono text-xs text-zinc-200 block font-bold">
                Key ID: {certificate.signingKeyId || 'KMS-P256-V1'}
              </span>
              <span className="text-[10px] text-emerald-400 flex items-center justify-center gap-1 font-mono">
                <Check className="w-3 h-3" /> Digitally Signed by System
              </span>
            </div>
          </div>
        </div>

        {/* Action Controls */}
        <div className="flex flex-wrap items-center justify-between gap-3 pt-2">
          <button
            onClick={handleCopyDeclaration}
            className="btn-secondary text-xs"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
            <span>{copied ? 'Affidavit Text Copied' : 'Copy Affidavit Statement'}</span>
          </button>

          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="btn-secondary text-xs"
            >
              Close
            </button>

            <button
              onClick={handleDownloadPdf}
              disabled={downloading}
              className="btn-primary text-xs"
            >
              <Download className={clsx('w-3.5 h-3.5', downloading && 'animate-spin')} />
              <span>{downloading ? 'Downloading...' : 'Download Official PDF'}</span>
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
