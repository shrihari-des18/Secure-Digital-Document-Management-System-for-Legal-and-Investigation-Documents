'use client';

import {
  Scale,
  Printer,
  FileCheck,
  ShieldCheck,
  FileText,
} from 'lucide-react';
import { truncateHash } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import type { CaseResponse, EvidenceDetailResponse, LegalDocumentResponse } from '@/types';

interface CourtBundleModalProps {
  isOpen: boolean;
  onClose: () => void;
  caseData: CaseResponse;
  evidenceList: EvidenceDetailResponse[];
  documentList: LegalDocumentResponse[];
}

export function CourtBundleModal({
  isOpen,
  onClose,
  caseData,
  evidenceList,
  documentList,
}: CourtBundleModalProps) {
  const handlePrint = () => {
    window.print();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Court Admissibility Dossier & Evidence Schedule"
      subtitle={`Statutory Submission Package for Case ${caseData.caseNumber}`}
      maxWidth="4xl"
    >
      <div className="space-y-6">
        {/* Dossier Document Sheet */}
        <div className="bg-zinc-950 border border-zinc-800 rounded-xl p-8 text-zinc-100 shadow-2xl space-y-6 print:bg-white print:text-black print:border-none font-sans">
          {/* Header */}
          <div className="text-center pb-6 border-b border-zinc-800">
            <div className="flex items-center justify-center gap-2 text-zinc-400 text-xs font-mono font-bold tracking-widest uppercase mb-1">
              <Scale className="w-4 h-4 text-zinc-300" />
              <span>Court of Competent Jurisdiction • Admissibility Schedule</span>
            </div>
            <h2 className="text-xl font-bold tracking-tight uppercase text-zinc-100">
              Electronic Record & Evidence Submission Dossier
            </h2>
            <p className="text-xs text-zinc-400 mt-1">
              Pursuant to Bharatiya Sakshya Adhiniyam, 2023 §63 & Information Technology Act, 2000
            </p>
          </div>

          {/* Case Metadata */}
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 p-4 bg-zinc-900/60 rounded-lg border border-zinc-800 text-xs">
            <div>
              <span className="text-zinc-500 font-bold block text-[10px] uppercase font-mono">FIR / Case Docket:</span>
              <span className="font-mono text-zinc-100 font-bold text-sm">{caseData.caseNumber}</span>
            </div>
            <div>
              <span className="text-zinc-500 font-bold block text-[10px] uppercase font-mono">Title:</span>
              <span className="font-medium text-zinc-200">{caseData.title}</span>
            </div>
            <div>
              <span className="text-zinc-500 font-bold block text-[10px] uppercase font-mono">Jurisdiction:</span>
              <span className="font-medium text-zinc-200">{caseData.jurisdiction}</span>
            </div>
            <div>
              <span className="text-zinc-500 font-bold block text-[10px] uppercase font-mono">Current Status:</span>
              <span className="font-mono text-emerald-400 font-bold">{caseData.status}</span>
            </div>
          </div>

          {/* Evidence Schedule Table */}
          <div className="space-y-3 text-xs">
            <h3 className="font-bold tracking-tight text-zinc-200 uppercase text-xs border-b border-zinc-800 pb-1.5 flex items-center gap-2">
              <FileCheck className="w-4 h-4 text-zinc-400" />
              <span>Schedule of Ingested Digital Evidence Assets ({evidenceList.length})</span>
            </h3>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse">
                <thead>
                  <tr className="border-b border-zinc-800 text-zinc-500 text-[10px] uppercase font-mono">
                    <th className="py-2 px-3">Item #</th>
                    <th className="py-2 px-3">Title / Description</th>
                    <th className="py-2 px-3">Hardware Source</th>
                    <th className="py-2 px-3 font-mono">SHA-256 Digest</th>
                    <th className="py-2 px-3">Custody</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-zinc-800/60">
                  {evidenceList.map((ev) => (
                    <tr key={ev.id} className="text-zinc-300">
                      <td className="py-2.5 px-3 font-mono text-zinc-200 font-bold">{ev.evidenceNumber}</td>
                      <td className="py-2.5 px-3 font-medium text-zinc-200">{ev.title}</td>
                      <td className="py-2.5 px-3 text-zinc-400">
                        {ev.deviceMake ? `${ev.deviceMake} ${ev.deviceModel || ''}` : 'Binary Image'}
                      </td>
                      <td className="py-2.5 px-3 font-mono text-emerald-400 text-[11px]">
                        {truncateHash(ev.file?.sha256, 8, 8)}
                      </td>
                      <td className="py-2.5 px-3 text-xs font-mono">{ev.custodyState}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          {/* Filed Legal Documents */}
          <div className="space-y-3 text-xs">
            <h3 className="font-bold tracking-tight text-zinc-200 uppercase text-xs border-b border-zinc-800 pb-1.5 flex items-center gap-2">
              <FileText className="w-4 h-4 text-zinc-400" />
              <span>Filed Legal Documents & Revisions ({documentList.length})</span>
            </h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {documentList.map((doc) => (
                <div key={doc.id} className="p-3 rounded-lg bg-zinc-900/60 border border-zinc-800 space-y-1">
                  <div className="flex items-center justify-between">
                    <span className="font-mono text-zinc-200 font-bold">{doc.documentNumber}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-zinc-950 border border-zinc-800 text-zinc-400">
                      v{doc.currentVersion} • {doc.documentType}
                    </span>
                  </div>
                  <p className="text-zinc-200 font-medium">{doc.title}</p>
                </div>
              ))}
            </div>
          </div>

          {/* Statutory Attestation Footer */}
          <div className="pt-6 border-t border-zinc-800 flex flex-col sm:flex-row sm:items-end justify-between gap-4 text-xs">
            <div className="space-y-1 max-w-sm text-zinc-400 text-[11px]">
              <span className="font-bold text-zinc-200 block">Integrity Certification:</span>
              <p>
                All evidence items sealed in append-only storage with tamper-evident SHA-256 hash chains and ECDSA
                signatures.
              </p>
            </div>

            <div className="text-right space-y-1 font-mono">
              <span className="text-zinc-300 block text-xs">Lead Officer: {caseData.leadOfficerId}</span>
              <span className="text-[10px] text-emerald-400 flex items-center justify-end gap-1">
                <ShieldCheck className="w-3.5 h-3.5" /> Chain of Custody Sealed
              </span>
            </div>
          </div>
        </div>

        {/* Modal Controls */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            onClick={onClose}
            className="btn-secondary"
          >
            Close
          </button>
          <button
            onClick={handlePrint}
            className="btn-primary"
          >
            <Printer className="w-3.5 h-3.5" />
            <span>Print / Export Dossier</span>
          </button>
        </div>
      </div>
    </Modal>
  );
}
