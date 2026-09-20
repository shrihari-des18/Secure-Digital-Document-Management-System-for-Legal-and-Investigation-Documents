'use client';

import { useState, useEffect, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import Link from 'next/link';
import {
  Upload,
  ChevronLeft,
  ShieldCheck,
  HardDrive,
  Lock,
  Cpu,
  RefreshCw,
  AlertCircle,
} from 'lucide-react';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { computeSha256, computeSha512, formatBytes } from '@/lib/crypto';
import type { UploadEvidenceRequest } from '@/types';
import { ClassificationLevel } from '@/types';
import { useQuery, useMutation } from '@tanstack/react-query';

function EvidenceUploadContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const preselectedCaseId = searchParams?.get('caseId') || '';

  // Form states
  const [caseId, setCaseId] = useState(preselectedCaseId);
  const [evidenceNumber, setEvidenceNumber] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [classification, setClassification] = useState<ClassificationLevel>(ClassificationLevel.CONFIDENTIAL);
  const [deviceMake, setDeviceMake] = useState('');
  const [deviceModel, setDeviceModel] = useState('');
  const [deviceSerialNumber, setDeviceSerialNumber] = useState('');
  const [seizedLocation, setSeizedLocation] = useState('');
  const [seizedAt, setSeizedAt] = useState(new Date().toISOString().slice(0, 16));

  // File and client crypto state
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isHashing, setIsHashing] = useState(false);
  const [clientSha256, setClientSha256] = useState<string | null>(null);
  const [clientSha512, setClientSha512] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch available cases
  const { data: cases = [] } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  useEffect(() => {
    if (preselectedCaseId && !caseId) {
      setCaseId(preselectedCaseId);
    }
  }, [preselectedCaseId, caseId]);

  // Handle file selection and compute client hash
  const handleFileChange = async (file: File | null) => {
    setSelectedFile(file);
    setClientSha256(null);
    setClientSha512(null);
    if (!file) return;

    setIsHashing(true);
    try {
      const sha256 = await computeSha256(file);
      const sha512 = await computeSha512(file);
      setClientSha256(sha256);
      setClientSha512(sha512);
    } catch (err) {
      console.error('Client hashing failed', err);
    } finally {
      setIsHashing(false);
    }
  };

  // Upload and seal mutation
  const uploadMutation = useMutation({
    mutationFn: async () => {
      if (!selectedFile) throw new Error('Please select an evidence file to upload');
      if (!caseId) throw new Error('Please select a target investigation case');

      const metadata: UploadEvidenceRequest = {
        caseId,
        evidenceNumber: evidenceNumber.trim(),
        title: title.trim(),
        description: description.trim() || undefined,
        classification,
        deviceMake: deviceMake.trim() || undefined,
        deviceModel: deviceModel.trim() || undefined,
        deviceSerialNumber: deviceSerialNumber.trim() || undefined,
        seizedLocation: seizedLocation.trim() || undefined,
        seizedAt: seizedAt ? new Date(seizedAt).toISOString() : undefined,
        clientSha256: clientSha256 || undefined,
      };

      return evidenceApi.upload(metadata, selectedFile);
    },
    onSuccess: (data) => {
      router.push(`/evidence/${data.id}`);
    },
    onError: (err: any) => {
      setFormError(err.message || 'Evidence intake failed');
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    uploadMutation.mutate();
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto">
      {/* Back button */}
      <Link
        href="/evidence"
        className="inline-flex items-center gap-1.5 text-xs text-zinc-400 hover:text-zinc-100 transition-colors font-mono"
      >
        <ChevronLeft className="w-4 h-4" />
        <span>Back to Evidence Vault</span>
      </Link>

      {/* Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Evidence Intake & Sealing Protocol</h1>
        <p className="text-xs text-zinc-400 mt-1">
          Client-side WebCrypto SHA-256 pre-verification with server-side envelope sealing and immutable ledger commit
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Step 1: File Selection & Live WebCrypto Hash */}
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-zinc-800 text-sm font-bold tracking-tight text-zinc-100">
            <div className="w-6 h-6 rounded bg-zinc-100 text-zinc-950 flex items-center justify-center text-xs">1</div>
            <span>Digital Evidence Binary & Client Hashing</span>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-2">
              Select Raw Evidence File <span className="text-red-400">*</span>
            </label>
            <div className="border border-dashed border-zinc-800 hover:border-zinc-700 rounded-xl p-8 text-center transition-colors bg-zinc-900/30">
              <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 mx-auto mb-3">
                <Upload className="w-5 h-5" />
              </div>
              <input
                type="file"
                id="file-upload"
                onChange={(e) => handleFileChange(e.target.files?.[0] || null)}
                className="hidden"
                required
              />
              <label
                htmlFor="file-upload"
                className="btn-primary cursor-pointer"
              >
                Choose Evidence File
              </label>
              {selectedFile ? (
                <div className="mt-4 text-xs font-mono">
                  <span className="text-zinc-100 font-bold">{selectedFile.name}</span>
                  <span className="text-zinc-500 ml-2">({formatBytes(selectedFile.size)})</span>
                </div>
              ) : (
                <p className="text-xs text-zinc-500 mt-2">or drag and drop evidence file here</p>
              )}
            </div>
          </div>

          {/* Live WebCrypto Client Hashing status */}
          {isHashing && (
            <div className="p-4 rounded-lg bg-blue-500/10 border border-blue-500/20 text-blue-400 text-xs flex items-center gap-3 font-mono">
              <RefreshCw className="w-4 h-4 animate-spin" />
              <span>Computing client-side WebCrypto SHA-256 and SHA-512 hashes in browser...</span>
            </div>
          )}

          {clientSha256 && (
            <div className="space-y-3 p-4 rounded-lg bg-zinc-900/40 border border-zinc-800 animate-fade-in">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-xs font-bold text-emerald-400 font-mono">
                  <ShieldCheck className="w-4 h-4" />
                  <span>Pre-Upload Client Hash Verified (WebCrypto SHA-256)</span>
                </div>
                <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-semibold">
                  CLIENT COMPUTED
                </span>
              </div>
              <p className="font-mono text-xs text-emerald-400 break-all select-all bg-zinc-950 p-2.5 rounded border border-zinc-800">
                {clientSha256}
              </p>
              {clientSha512 && (
                <div>
                  <span className="text-[10px] font-semibold font-mono text-zinc-400 block mb-1">
                    SHA-512 Extended Fingerprint:
                  </span>
                  <p className="font-mono text-[10px] text-zinc-300 break-all select-all bg-zinc-950 p-2 rounded border border-zinc-800">
                    {clientSha512}
                  </p>
                </div>
              )}
            </div>
          )}
        </div>

        {/* Step 2: Evidence Metadata & Case Docket Binding */}
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-zinc-800 text-sm font-bold tracking-tight text-zinc-100">
            <div className="w-6 h-6 rounded bg-zinc-100 text-zinc-950 flex items-center justify-center text-xs">2</div>
            <span>Evidence Metadata & Chain of Custody Parameters</span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Target Investigation Case Docket <span className="text-red-400">*</span>
              </label>
              <select
                value={caseId}
                onChange={(e) => setCaseId(e.target.value)}
                required
                className="select-field"
              >
                <option value="">Select case docket...</option>
                {cases.map((c) => (
                  <option key={c.id} value={c.id} className="bg-zinc-900 text-zinc-100">
                    {c.caseNumber} — {c.title}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Evidence Item Identifier (e.g. EVID-DEL-2026-001) <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                value={evidenceNumber}
                onChange={(e) => setEvidenceNumber(e.target.value)}
                placeholder="EVID-DEL-2026-001"
                required
                className="input-field font-mono"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Evidence Title <span className="text-red-400">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g., Gateway Access Logs Archive"
                required
                className="input-field"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Security Classification Level <span className="text-red-400">*</span>
              </label>
              <select
                value={classification}
                onChange={(e) => setClassification(e.target.value as ClassificationLevel)}
                className="select-field"
              >
                {Object.values(ClassificationLevel).map((lvl) => (
                  <option key={lvl} value={lvl} className="bg-zinc-900 text-zinc-100">
                    {lvl}
                  </option>
                ))}
              </select>
            </div>

            <div className="md:col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Description & Seizure Context
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
                placeholder="Detailed notes regarding physical seizure, drive condition, write-blocker used..."
                className="input-field"
              />
            </div>
          </div>
        </div>

        {/* Step 3: Device Hardware & Seizure Particulars (for §63 Admissibility) */}
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 space-y-4">
          <div className="flex items-center gap-2 pb-3 border-b border-zinc-800 text-sm font-bold tracking-tight text-zinc-100">
            <div className="w-6 h-6 rounded bg-zinc-100 text-zinc-950 flex items-center justify-center text-xs">3</div>
            <span>Hardware Particulars & Seizure Location (BSA §63 Mandatory Fields)</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Device Make
              </label>
              <input
                type="text"
                value={deviceMake}
                onChange={(e) => setDeviceMake(e.target.value)}
                placeholder="e.g. Dell / Apple / Samsung"
                className="input-field"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Device Model
              </label>
              <input
                type="text"
                value={deviceModel}
                onChange={(e) => setDeviceModel(e.target.value)}
                placeholder="e.g. PowerEdge R740 Server"
                className="input-field"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Hardware Serial Number
              </label>
              <input
                type="text"
                value={deviceSerialNumber}
                onChange={(e) => setDeviceSerialNumber(e.target.value)}
                placeholder="e.g. SN-7798-DEL-2026"
                className="input-field font-mono"
              />
            </div>

            <div className="sm:col-span-2">
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Physical Seizure Location
              </label>
              <input
                type="text"
                value={seizedLocation}
                onChange={(e) => setSeizedLocation(e.target.value)}
                placeholder="e.g. Data Center Rack 4, Okhla Phase III, New Delhi"
                className="input-field"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
                Seizure Date & Time
              </label>
              <input
                type="datetime-local"
                value={seizedAt}
                onChange={(e) => setSeizedAt(e.target.value)}
                className="input-field font-mono"
              />
            </div>
          </div>
        </div>

        {formError && (
          <div className="p-4 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{formError}</span>
          </div>
        )}

        {/* Submit action */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <Link
            href="/evidence"
            className="btn-secondary"
          >
            Cancel
          </Link>

          <button
            type="submit"
            disabled={uploadMutation.isPending || isHashing}
            className="btn-primary"
          >
            <ShieldCheck className="w-4 h-4" />
            <span>{uploadMutation.isPending ? 'Ingesting & Cryptographically Sealing...' : 'Ingest & Seal Evidence'}</span>
          </button>
        </div>
      </form>
    </div>
  );
}

export default function EvidenceUploadPage() {
  return (
    <Suspense fallback={<div className="py-20 text-center text-xs text-zinc-500">Loading intake protocol...</div>}>
      <EvidenceUploadContent />
    </Suspense>
  );
}
