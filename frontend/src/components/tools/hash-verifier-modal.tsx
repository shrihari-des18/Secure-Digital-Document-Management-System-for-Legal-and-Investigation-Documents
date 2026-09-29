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
  Clock,
  CheckCircle2,
} from 'lucide-react';
import { clsx } from 'clsx';
import Link from 'next/link';
import { computeSha256, computeSha512, formatBytes, formatDate } from '@/lib/crypto';
import { Modal } from '@/components/ui/modal';
import { Tabs } from '@/components/ui/tabs';
import { timestampApi } from '@/lib/api/timestamp';
import type { EvidenceDetailResponse, TimestampVerifyResponse } from '@/types';
import { useMutation } from '@tanstack/react-query';

interface HashVerifierModalProps {
  isOpen: boolean;
  onClose: () => void;
  allEvidence: EvidenceDetailResponse[];
}

export function HashVerifierModal({ isOpen, onClose, allEvidence }: HashVerifierModalProps) {
  const [activeTab, setActiveTab] = useState<'local' | 'tsa'>('local');
  const [file, setFile] = useState<File | null>(null);
  const [isHashing, setIsHashing] = useState(false);
  const [computedSha256, setComputedSha256] = useState<string | null>(null);
  const [computedSha512, setComputedSha512] = useState<string | null>(null);

  // TSA state
  const [tsaTokenInput, setTsaTokenInput] = useState('');
  const [tsaVerifyResult, setTsaVerifyResult] = useState<TimestampVerifyResponse | null>(null);
  const [tsaStampToken, setTsaStampToken] = useState<string | null>(null);

  const handleFileDrop = async (selectedFile: File | null) => {
    setFile(selectedFile);
    setComputedSha256(null);
    setComputedSha512(null);
    setTsaVerifyResult(null);
    setTsaStampToken(null);
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
    setTsaVerifyResult(null);
    setTsaStampToken(null);
    setTsaTokenInput('');
  };

  // Stamp mutation
  const stampMutation = useMutation({
    mutationFn: () => timestampApi.stampHash({ sha256: computedSha256! }),
    onSuccess: (res) => {
      setTsaStampToken(res.tokenBase64);
      setTsaVerifyResult(null); // clear previous verify
    },
    onError: (err: any) => {
      alert(err.message || 'TSA Stamping failed');
    }
  });

  // Verify mutation
  const verifyMutation = useMutation({
    mutationFn: () => timestampApi.verifyTimestamp({
      expectedSha256: computedSha256!,
      tokenBase64: tsaTokenInput.trim()
    }),
    onSuccess: (res) => {
      setTsaVerifyResult(res);
    },
    onError: (err: any) => {
      alert(err.message || 'TSA Verification failed');
    }
  });

  const tabs = [
    { id: 'local', label: 'Local WebCrypto', icon: HardDrive },
    { id: 'tsa', label: 'RFC 3161 TSA Stamp / Verify', icon: Clock },
  ];

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Cryptographic Evidence Verifier"
      subtitle="Verify file integrity locally or stamp hashes via RFC 3161 Trusted Timestamp Authority (TSA)"
      maxWidth="xl"
    >
      <div className="space-y-5">
        <Tabs tabs={tabs} activeTab={activeTab} onChange={(id) => setActiveTab(id as any)} />

        {/* Common File Selection Header */}
        {!file ? (
          <div className="border border-dashed border-zinc-800 hover:border-zinc-700 rounded-xl p-8 text-center transition-colors bg-zinc-900/30 mt-4">
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
              Select Local File to Analyze
            </label>
            <p className="text-xs text-zinc-500 mt-2 font-mono">
              File stays in browser. Only SHA-256 hash is transmitted to TSA if requested.
            </p>
          </div>
        ) : (
          <div className="p-4 rounded-xl bg-zinc-900/40 border border-zinc-800 flex items-center justify-between mt-4">
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
              className="btn-secondary text-xs shrink-0"
            >
              Change File
            </button>
          </div>
        )}

        {isHashing && (
          <div className="p-4 rounded-xl bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs flex items-center gap-3 font-mono">
            <RefreshCw className="w-4 h-4 animate-spin" />
            <span>Computing SHA-256 hash locally via WebCrypto...</span>
          </div>
        )}

        {/* Computed Hash Display (Common) */}
        {computedSha256 && !isHashing && (
          <div className="p-4 rounded-xl bg-zinc-900/40 border border-zinc-800 space-y-2 animate-fade-in">
            <div className="flex items-center gap-2 text-xs font-bold text-zinc-100 font-mono">
              <Hash className="w-4 h-4 text-zinc-300" />
              <span>Target SHA-256 Digest:</span>
            </div>
            <p className="font-mono text-xs text-emerald-400 break-all select-all bg-zinc-950 p-2.5 rounded border border-zinc-800">
              {computedSha256}
            </p>
          </div>
        )}

        {/* Tab Content: Local Match */}
        {activeTab === 'local' && computedSha256 && !isHashing && (
          <div className="animate-fade-in space-y-4">
            {matchedEvidence ? (
              <div className="p-5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-300 space-y-3">
                <div className="flex items-center gap-2 text-sm font-bold text-emerald-400">
                  <ShieldCheck className="w-5 h-5" />
                  <span>MATCH CONFIRMED: Authentic Sealed Evidence in Vault</span>
                </div>
                <p className="text-xs leading-relaxed text-emerald-200">
                  This file is cryptographically identical to evidence item{' '}
                  <span className="font-mono font-bold text-zinc-100">{matchedEvidence.evidenceNumber}</span> (
                  {matchedEvidence.title}). Zero byte alteration detected.
                </p>

                <div className="pt-2 flex items-center justify-between">
                  <span className="text-[11px] font-mono text-emerald-400">
                    Sealed at: {formatDate(matchedEvidence.createdAt)}
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

        {/* Tab Content: TSA Stamp & Verify */}
        {activeTab === 'tsa' && computedSha256 && !isHashing && (
          <div className="animate-fade-in space-y-6">
            
            {/* Stamp Action */}
            <div className="p-5 rounded-xl border border-zinc-800 bg-zinc-950 space-y-4">
              <div>
                <h4 className="text-sm font-bold text-zinc-100 flex items-center gap-2">
                  <Clock className="w-4 h-4 text-emerald-400" />
                  Generate New RFC 3161 Timestamp Token
                </h4>
                <p className="text-xs text-zinc-400 mt-1 leading-relaxed">
                  Submit the SHA-256 hash to the Trusted Timestamp Authority (TSA). The TSA will return a signed token cryptographically binding this hash to the current atomic time.
                </p>
              </div>
              <button
                onClick={() => stampMutation.mutate()}
                disabled={stampMutation.isPending}
                className="btn-primary"
              >
                {stampMutation.isPending ? 'Requesting TSA Signature...' : 'Request TSA Stamp for Hash'}
              </button>

              {tsaStampToken && (
                <div className="mt-4 p-4 bg-emerald-500/10 border border-emerald-500/20 rounded-xl space-y-2 animate-fade-in">
                  <div className="flex items-center gap-2 text-xs font-bold text-emerald-400">
                    <CheckCircle2 className="w-4 h-4" />
                    <span>TSA Token Generated Successfully</span>
                  </div>
                  <p className="text-[10px] text-emerald-300 font-mono">
                    Base64 RFC 3161 Token (Save this to verify later):
                  </p>
                  <textarea
                    readOnly
                    value={tsaStampToken}
                    rows={4}
                    className="w-full text-[10px] font-mono p-2 rounded bg-zinc-950 border border-emerald-500/30 text-emerald-400 select-all outline-none"
                  />
                </div>
              )}
            </div>

            {/* Verify Action */}
            <div className="p-5 rounded-xl border border-zinc-800 bg-zinc-950 space-y-4">
              <div>
                <h4 className="text-sm font-bold text-zinc-100 flex items-center gap-2">
                  <ShieldCheck className="w-4 h-4 text-blue-400" />
                  Verify Existing RFC 3161 Timestamp Token
                </h4>
                <p className="text-xs text-zinc-400 mt-1 leading-relaxed">
                  Paste a Base64-encoded TSA token to cryptographically verify it against the local file's SHA-256 hash.
                </p>
              </div>
              
              <textarea
                value={tsaTokenInput}
                onChange={(e) => setTsaTokenInput(e.target.value)}
                placeholder="Paste Base64 TSA Token here..."
                rows={3}
                className="input-field font-mono text-[10px]"
              />

              <button
                onClick={() => verifyMutation.mutate()}
                disabled={verifyMutation.isPending || !tsaTokenInput.trim()}
                className="btn-secondary w-full"
              >
                {verifyMutation.isPending ? 'Verifying Signature...' : 'Verify Token Integrity'}
              </button>

              {tsaVerifyResult && (
                <div className={clsx(
                  'mt-4 p-4 rounded-xl border space-y-2 animate-fade-in',
                  tsaVerifyResult.valid ? 'bg-emerald-500/10 border-emerald-500/20' : 'bg-red-500/10 border-red-500/20'
                )}>
                  <div className={clsx(
                    'flex items-center gap-2 text-xs font-bold',
                    tsaVerifyResult.valid ? 'text-emerald-400' : 'text-red-400'
                  )}>
                    {tsaVerifyResult.valid ? <CheckCircle2 className="w-4 h-4" /> : <ShieldAlert className="w-4 h-4" />}
                    <span>{tsaVerifyResult.valid ? 'TSA VERIFICATION SUCCESSFUL' : 'TSA VERIFICATION FAILED'}</span>
                  </div>
                  
                  {tsaVerifyResult.valid && true && (
                    <div className="space-y-1 mt-2 text-[11px] font-mono text-zinc-300">
                      <p><span className="text-zinc-500">Authority:</span> {tsaVerifyResult.tsaName || 'O=E-Vault TSA'}</p>
                      <p><span className="text-zinc-500">Cryptographic Time:</span> {formatDate(tsaVerifyResult.genTime)}</p>
                      <p><span className="text-zinc-500">Serial Number:</span> {tsaVerifyResult.serialNumber}</p>
                    </div>
                  )}

                  {!tsaVerifyResult.valid && (
                    <p className="text-xs text-red-300">{tsaVerifyResult.message || 'The token does not match the file hash or the signature is invalid.'}</p>
                  )}
                </div>
              )}
            </div>

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

