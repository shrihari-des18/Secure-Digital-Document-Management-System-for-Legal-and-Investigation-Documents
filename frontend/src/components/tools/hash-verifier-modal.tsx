'use client';

import { useState } from 'react';
import {
  FileCheck,
  ShieldCheck,
  ShieldAlert,
  Hash,
  RefreshCw,
  HardDrive,
  ArrowRight,
} from 'lucide-react';
import { clsx } from 'clsx';
import Link from 'next/link';
import { computeSha256, computeSha512, formatBytes } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import type { EvidenceDetailResponse } from '@/types';

interface HashVerifierModalProps {
  isOpen: boolean;
  onClose: () => void;
  allEvidence: EvidenceDetailResponse[];
}

export function HashVerifierModal({ isOpen, onClose, allEvidence }: HashVerifierModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [isHashing, setIsHashing] = useState(false);
  const [computedSha256, setComputedSha256] = useState<string | null>(null);
  const [computedSha512, setComputedSha512] = useState<string | null>(null);

  const handleFileDrop = async (selectedFile: File | null) => {
    setFile(selectedFile);
    setComputedSha256(null);
    setComputedSha512(null);
    if (!selectedFile) return;

    setIsHashing(true);
    try {
      const sha256 = await computeSha256(selectedFile);
      const sha512 = await computeSha512(selectedFile);
      setComputedSha256(sha256);
      setComputedSha512(sha512);
    } catch (err) {
      console.error('Hashing failed', err);
    } finally {
      setIsHashing(false);
    }
  };

  const matchedEvidence = computedSha256
    ? allEvidence.find(
        (e) =>
          e.file?.sha256?.toLowerCase() === computedSha256.toLowerCase() ||
          e.file?.clientSha256?.toLowerCase() === computedSha256.toLowerCase()
      )
    : null;

  const handleReset = () => {
    setFile(null);
    setComputedSha256(null);
    setComputedSha512(null);
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Client-Side Cryptographic Evidence Verifier"
      subtitle="Verify file integrity locally using WebCrypto API without transmitting file bytes over the network"
      maxWidth="xl"
    >
      <div className="space-y-5">
        {/* Drop zone */}
        {!file ? (
          <div className="border border-dashed border-zinc-800 hover:border-zinc-700 rounded-xl p-8 text-center transition-colors bg-zinc-900/30">
            <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 mx-auto mb-3">
              <HardDrive className="w-5 h-5" />
            </div>
            <input
              type="file"
              id="verifier-file-input"
              onChange={(e) => handleFileDrop(e.target.files?.[0] || null)}
              className="hidden"
            />
            <label
              htmlFor="verifier-file-input"
              className="btn-primary cursor-pointer"
            >
              Select Local File to Verify
            </label>
            <p className="text-xs text-zinc-500 mt-2 font-mono">
              Drop any disk image, document, video, or log file to calculate hash client-side
            </p>
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-zinc-900/40 border border-zinc-800 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-zinc-100 text-zinc-950 flex items-center justify-center font-bold">
                <FileCheck className="w-5 h-5" />
              </div>
              <div>
                <h4 className="text-xs font-bold text-zinc-100">{file.name}</h4>
                <p className="text-[11px] text-zinc-500 font-mono">{formatBytes(file.size)}</p>
              </div>
            </div>
            <button
              onClick={handleReset}
              className="btn-secondary text-xs"
            >
              Change File
            </button>
          </div>
        )}

        {/* Hashing progress */}
        {isHashing && (
          <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs flex items-center gap-3 font-mono">
            <RefreshCw className="w-4 h-4 animate-spin" />
            <span>Computing SHA-256 and SHA-512 hashes via hardware WebCrypto engine...</span>
          </div>
        )}

        {/* Computed Hash Display */}
        {computedSha256 && (
          <div className="space-y-4 animate-fade-in">
            <div className="p-4 rounded-xl bg-zinc-900/40 border border-zinc-800 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-zinc-100 font-mono">
                <Hash className="w-4 h-4 text-zinc-300" />
                <span>Computed Local SHA-256 Digest:</span>
              </div>
              <p className="font-mono text-xs text-emerald-400 break-all select-all bg-zinc-950 p-2.5 rounded border border-zinc-800">
                {computedSha256}
              </p>
              {computedSha512 && (
                <div className="pt-2">
                  <span className="text-[10px] text-zinc-500 font-mono block mb-1">Extended SHA-512:</span>
                  <p className="font-mono text-[10px] text-zinc-400 break-all select-all bg-zinc-950 p-2 rounded border border-zinc-800">
                    {computedSha512}
                  </p>
                </div>
              )}
            </div>

            {/* Match Result Banner */}
            {matchedEvidence ? (
              <div className="p-5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 space-y-3">
                <div className="flex items-center gap-2 text-sm font-bold text-emerald-400">
                  <ShieldCheck className="w-5 h-5" />
                  <span>MATCH CONFIRMED: Authentic Sealed Evidence in E-Vault</span>
                </div>
                <p className="text-xs leading-relaxed text-emerald-200">
                  This file is cryptographically identical to evidence item{' '}
                  <span className="font-mono font-bold text-zinc-100">{matchedEvidence.evidenceNumber}</span> (
                  {matchedEvidence.title}). Zero byte alteration detected.
                </p>

                <div className="pt-2 flex items-center justify-between">
                  <span className="text-[11px] font-mono text-emerald-400">
                    Sealed at: {new Date(matchedEvidence.createdAt).toLocaleDateString()}
                  </span>
                  <Link
                    href={`/evidence/${matchedEvidence.id}`}
                    onClick={onClose}
                    className="inline-flex items-center gap-1 text-xs font-bold text-emerald-400 hover:underline font-mono"
                  >
                    <span>View Evidence Dossier</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </Link>
                </div>
              </div>
            ) : (
              <div className="p-5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 space-y-2">
                <div className="flex items-center gap-2 text-sm font-bold text-amber-400">
                  <ShieldAlert className="w-5 h-5" />
                  <span>No Matching Evidence Found in Vault</span>
                </div>
                <p className="text-xs leading-relaxed text-amber-200">
                  The computed SHA-256 does not match any sealed evidence asset in the current vault registry. This file
                  may be un-ingested, altered, or from another jurisdiction.
                </p>
              </div>
            )}
          </div>
        )}

        <div className="flex items-center justify-end pt-2">
          <button
            onClick={onClose}
            className="btn-secondary"
          >
            Close Verifier
          </button>
        </div>
      </div>
    </Modal>
  );
}
