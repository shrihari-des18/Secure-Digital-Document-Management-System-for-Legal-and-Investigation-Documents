'use client';

import { useState, Suspense } from 'react';
import Link from 'next/link';
import {
  FileText,
  Plus,
  Search,
  Download,
  History,
  Upload,
  Clock,
  Briefcase,
} from 'lucide-react';
import { clsx } from 'clsx';
import { casesApi } from '@/lib/api/cases';
import { documentsApi } from '@/lib/api/documents';
import { formatDate, truncateHash, formatBytes } from '@/lib/crypto';
import { DocumentTypeBadge } from '@/components/ui/badge';
import { Modal } from '@/components/ui/modal';
import type {
  LegalDocumentResponse,
  CreateLegalDocumentRequest,
  AddDocumentVersionRequest,
} from '@/types';
import { LegalDocumentType } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

function DocumentsContent() {
  const queryClient = useQueryClient();

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedType, setSelectedType] = useState<LegalDocumentType | 'ALL'>('ALL');
  const [selectedCaseId, setSelectedCaseId] = useState<string>('ALL');

  // Create document modal state
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [createCaseId, setCreateCaseId] = useState('');
  const [docNumber, setDocNumber] = useState('');
  const [docTitle, setDocTitle] = useState('');
  const [docType, setDocType] = useState<LegalDocumentType>(LegalDocumentType.FIR);
  const [docDescription, setDocDescription] = useState('');
  const [createFile, setCreateFile] = useState<File | null>(null);
  const [createError, setCreateError] = useState<string | null>(null);

  // Add version modal state
  const [isVersionOpen, setIsVersionOpen] = useState(false);
  const [versionDocId, setVersionDocId] = useState<string>('');
  const [versionDocTitle, setVersionDocTitle] = useState('');
  const [changeSummary, setChangeSummary] = useState('');
  const [versionFile, setVersionFile] = useState<File | null>(null);
  const [versionError, setVersionError] = useState<string | null>(null);

  // History modal state
  const [isHistoryOpen, setIsHistoryOpen] = useState(false);
  const [historyDoc, setHistoryDoc] = useState<LegalDocumentResponse | null>(null);

  // Fetch cases
  const { data: cases = [] } = useQuery({
    queryKey: ['cases'],
    queryFn: () => casesApi.getAll(),
  });

  // Fetch all legal documents across cases
  const { data: allDocuments = [], isLoading } = useQuery({
    queryKey: ['all-documents', cases.map((c) => c.id)],
    queryFn: async () => {
      if (cases.length === 0) return [];
      const promises = cases.map((c) =>
        documentsApi.getByCaseId(c.id).catch(() => [] as LegalDocumentResponse[])
      );
      const results = await Promise.all(promises);
      return results.flat();
    },
    enabled: cases.length > 0,
  });

  // Fetch history for selected document in history modal
  const { data: revisionHistory = [], isLoading: historyLoading } = useQuery({
    queryKey: ['document-history', historyDoc?.id],
    queryFn: () => documentsApi.getHistory(historyDoc!.id),
    enabled: !!historyDoc?.id && isHistoryOpen,
  });

  // Create document v1 mutation
  const createMutation = useMutation({
    mutationFn: () => {
      if (!createFile) throw new Error('Please attach a document file');
      if (!createCaseId) throw new Error('Please select an investigation case');

      const meta: CreateLegalDocumentRequest = {
        caseId: createCaseId,
        documentNumber: docNumber.trim(),
        title: docTitle.trim(),
        documentType: docType,
        description: docDescription.trim() || undefined,
        changeSummary: 'Initial legal filing (v1)',
      };
      return documentsApi.create(meta, createFile);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['all-documents'] });
      setIsCreateOpen(false);
      setCreateCaseId('');
      setDocNumber('');
      setDocTitle('');
      setDocDescription('');
      setCreateFile(null);
      setCreateError(null);
    },
    onError: (err: any) => {
      setCreateError(err.message || 'Filing failed');
    },
  });

  // Add version v2+ mutation
  const versionMutation = useMutation({
    mutationFn: () => {
      if (!versionFile) throw new Error('Please attach an amended document file');
      const meta: AddDocumentVersionRequest = {
        changeSummary: changeSummary.trim(),
      };
      return documentsApi.addVersion(versionDocId, meta, versionFile);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['all-documents'] });
      queryClient.invalidateQueries({ queryKey: ['document-history', versionDocId] });
      setIsVersionOpen(false);
      setChangeSummary('');
      setVersionFile(null);
      setVersionError(null);
    },
    onError: (err: any) => {
      setVersionError(err.message || 'Version revision failed');
    },
  });

  const filteredDocuments = allDocuments.filter((doc) => {
    const matchSearch =
      doc.documentNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      doc.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (doc.description && doc.description.toLowerCase().includes(searchQuery.toLowerCase()));

    const matchType = selectedType === 'ALL' || doc.documentType === selectedType;
    const matchCase = selectedCaseId === 'ALL' || doc.caseId === selectedCaseId;

    return matchSearch && matchType && matchCase;
  });

  return (
    <div className="space-y-8">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Versioned Legal Documents</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Immutable version control, cryptographic revision chains, and statutory filing logs
          </p>
        </div>

        <button
          onClick={() => setIsCreateOpen(true)}
          className="btn-primary shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>File Legal Document</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl space-y-4">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          {/* Search */}
          <div className="relative w-full md:w-96">
            <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search document ID, title, summary..."
              className="input-field pl-9"
            />
          </div>

          {/* Case Filter */}
          <div className="w-full md:w-64">
            <select
              value={selectedCaseId}
              onChange={(e) => setSelectedCaseId(e.target.value)}
              className="select-field"
            >
              <option value="ALL">Filter by Case (All Cases)</option>
              {cases.map((c) => (
                <option key={c.id} value={c.id} className="bg-zinc-900 text-zinc-100">
                  {c.caseNumber} — {c.title}
                </option>
              ))}
            </select>
          </div>
        </div>

        {/* Type pills */}
        <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-zinc-800/80 font-mono text-xs">
          <span className="text-[11px] font-semibold text-zinc-500 uppercase">Type:</span>
          {(['ALL', ...Object.values(LegalDocumentType)] as const).map((t) => (
            <button
              key={t}
              onClick={() => setSelectedType(t)}
              className={clsx(
                'text-[11px] px-2.5 py-1 rounded-lg font-medium transition-colors',
                selectedType === t
                  ? 'bg-zinc-100 text-zinc-950 font-bold border border-zinc-100'
                  : 'bg-zinc-900/60 text-zinc-400 hover:text-zinc-200 border border-zinc-800'
              )}
            >
              {t === 'ALL' ? 'All Types' : t.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Documents List */}
      {isLoading ? (
        <div className="py-20 text-center text-xs text-zinc-500">Loading legal documents...</div>
      ) : filteredDocuments.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
          <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-500 mx-auto mb-3">
            <FileText className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold tracking-tight text-zinc-100">No legal documents found</h3>
          <p className="text-xs text-zinc-400 mt-1 max-w-sm mx-auto">
            {searchQuery || selectedType !== 'ALL'
              ? 'No documents match your query filters.'
              : 'File an initial FIR, charge sheet, or warrant under an investigation case.'}
          </p>
          <button
            onClick={() => setIsCreateOpen(true)}
            className="mt-4 btn-primary"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>File Legal Document</span>
          </button>
        </div>
      ) : (
        <div className="space-y-6">
          {filteredDocuments.map((doc) => {
            const parentCase = cases.find((c) => c.id === doc.caseId);

            return (
              <div
                key={doc.id}
                className="border border-zinc-800 bg-zinc-950 hover:border-zinc-700 rounded-xl p-6 transition-colors flex flex-col md:flex-row md:items-center justify-between gap-6"
              >
                <div>
                  <div className="flex flex-wrap items-center gap-2 mb-2">
                    <span className="font-mono text-xs font-bold text-zinc-200">{doc.documentNumber}</span>
                    <DocumentTypeBadge type={doc.documentType} />
                    <span className="text-xs font-mono px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800 text-zinc-200 font-bold">
                      v{doc.currentVersion}
                    </span>
                    {parentCase && (
                      <Link
                        href={`/cases/${parentCase.id}`}
                        className="text-[11px] text-zinc-400 hover:text-zinc-100 flex items-center gap-1 font-mono"
                      >
                        <Briefcase className="w-3 h-3" />
                        {parentCase.caseNumber}
                      </Link>
                    )}
                  </div>

                  <h3 className="text-sm font-bold tracking-tight text-zinc-100">{doc.title}</h3>
                  <p className="text-xs text-zinc-400 mt-1 max-w-2xl leading-relaxed">
                    {doc.description || 'No descriptive summary provided.'}
                  </p>

                  <div className="flex flex-wrap items-center gap-4 text-[11px] text-zinc-500 mt-3 font-mono">
                    <span className="flex items-center gap-1">
                      <Clock className="w-3 h-3" />
                      Created: {formatDate(doc.createdAt)}
                    </span>
                    {doc.latestVersion && (
                      <span className="text-emerald-400">
                        SHA-256: {truncateHash(doc.latestVersion.sha256)}
                      </span>
                    )}
                  </div>
                </div>

                <div className="flex flex-wrap items-center gap-2 shrink-0">
                  <button
                    onClick={() => {
                      setHistoryDoc(doc);
                      setIsHistoryOpen(true);
                    }}
                    className="btn-secondary text-xs"
                  >
                    <History className="w-3.5 h-3.5 text-zinc-300" />
                    <span>Revision History</span>
                  </button>

                  <button
                    onClick={() => {
                      setVersionDocId(doc.id);
                      setVersionDocTitle(doc.title);
                      setIsVersionOpen(true);
                    }}
                    className="btn-secondary text-xs"
                  >
                    <Upload className="w-3.5 h-3.5 text-zinc-300" />
                    <span>Upload v{doc.currentVersion + 1}</span>
                  </button>

                  <button
                    onClick={() =>
                      documentsApi.download(doc.id, doc.currentVersion, `${doc.documentNumber}_v${doc.currentVersion}.pdf`)
                    }
                    className="btn-primary text-xs"
                  >
                    <Download className="w-3.5 h-3.5" />
                    <span>Download v{doc.currentVersion}</span>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* File Document Modal (v1) */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="File Initial Legal Document"
        subtitle="Registers and cryptographically signs initial version (v1) of legal record"
        maxWidth="lg"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createMutation.mutate();
          }}
          className="space-y-6"
        >
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Investigation Case Docket <span className="text-red-400">*</span>
            </label>
            <select
              value={createCaseId}
              onChange={(e) => setCreateCaseId(e.target.value)}
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
              Document Identifier (e.g., FIR-2026-001) <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={docNumber}
              onChange={(e) => setDocNumber(e.target.value)}
              placeholder="FIR-2026-DELHI-001"
              required
              className="input-field font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Document Title <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={docTitle}
              onChange={(e) => setDocTitle(e.target.value)}
              placeholder="e.g., First Information Report (FIR)"
              required
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Document Type <span className="text-red-400">*</span>
            </label>
            <select
              value={docType}
              onChange={(e) => setDocType(e.target.value as LegalDocumentType)}
              className="select-field"
            >
              {Object.values(LegalDocumentType).map((t) => (
                <option key={t} value={t} className="bg-zinc-900 text-zinc-100">
                  {t.replace('_', ' ')}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Description
            </label>
            <textarea
              value={docDescription}
              onChange={(e) => setDocDescription(e.target.value)}
              rows={2}
              placeholder="Summary of document..."
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Document File (PDF) <span className="text-red-400">*</span>
            </label>
            <input
              type="file"
              onChange={(e) => setCreateFile(e.target.files?.[0] || null)}
              required
              className="w-full text-xs text-zinc-300 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-zinc-100 file:text-zinc-950 hover:file:bg-zinc-200 file:cursor-pointer"
            />
          </div>

          {createError && (
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
              {createError}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsCreateOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createMutation.isPending}
              className="btn-primary"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>{createMutation.isPending ? 'Filing Document...' : 'File Document'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Upload Revision Modal (v2+) */}
      <Modal
        isOpen={isVersionOpen}
        onClose={() => setIsVersionOpen(false)}
        title="Upload Amended Document Revision"
        subtitle={`Upload new version for "${versionDocTitle}"`}
        maxWidth="lg"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            versionMutation.mutate();
          }}
          className="space-y-6"
        >
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Change Summary & Reason for Amendment <span className="text-red-400">*</span>
            </label>
            <textarea
              value={changeSummary}
              onChange={(e) => setChangeSummary(e.target.value)}
              rows={3}
              placeholder="e.g., Added supplementary suspect statements and forensic lab references..."
              required
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Amended File (PDF / DOC) <span className="text-red-400">*</span>
            </label>
            <input
              type="file"
              onChange={(e) => setVersionFile(e.target.files?.[0] || null)}
              required
              className="w-full text-xs text-zinc-300 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-zinc-100 file:text-zinc-950 hover:file:bg-zinc-200 file:cursor-pointer"
            />
          </div>

          {versionError && (
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
              {versionError}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsVersionOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={versionMutation.isPending}
              className="btn-primary"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>{versionMutation.isPending ? 'Uploading Revision...' : 'Seal New Revision'}</span>
            </button>
          </div>
        </form>
      </Modal>

      {/* Revision History Drawer / Modal */}
      {historyDoc && (
        <Modal
          isOpen={isHistoryOpen}
          onClose={() => setIsHistoryOpen(false)}
          title={`Revision Chain: ${historyDoc.title}`}
          subtitle={`Document ${historyDoc.documentNumber} • ${historyDoc.documentType}`}
          maxWidth="xl"
        >
          <div className="space-y-4">
            {historyLoading ? (
              <div className="py-12 text-center text-xs text-zinc-500">Loading version timeline...</div>
            ) : revisionHistory.length === 0 ? (
              <div className="py-8 text-center text-xs text-zinc-500">No revisions found.</div>
            ) : (
              <div className="space-y-3">
                {revisionHistory.map((ver) => (
                  <div
                    key={ver.id}
                    className="p-4 rounded-xl bg-zinc-900/40 border border-zinc-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3"
                  >
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <span className="font-mono text-xs font-bold px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800 text-zinc-200">
                          Version {ver.versionNumber}
                        </span>
                        <span className="text-xs text-zinc-200 font-semibold">{ver.originalFileName}</span>
                        <span className="text-[11px] text-zinc-500 font-mono">({formatBytes(ver.sizeBytes)})</span>
                      </div>

                      <p className="text-xs text-zinc-300 mt-1">
                        <span className="text-zinc-500">Change memo: </span>
                        {ver.changeSummary || 'Initial filing.'}
                      </p>

                      <div className="flex flex-wrap items-center gap-3 text-[11px] text-zinc-500 mt-2 font-mono">
                        <span className="text-emerald-400">SHA-256: {truncateHash(ver.sha256)}</span>
                        <span>Sealed: {formatDate(ver.createdAt)}</span>
                      </div>
                    </div>

                    <button
                      onClick={() =>
                        documentsApi.download(
                          historyDoc.id,
                          ver.versionNumber,
                          `${historyDoc.documentNumber}_v${ver.versionNumber}.pdf`
                        )
                      }
                      className="btn-secondary text-xs shrink-0"
                    >
                      <Download className="w-3.5 h-3.5" />
                      <span>Download</span>
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
        </Modal>
      )}
    </div>
  );
}

export default function DocumentsPage() {
  return (
    <Suspense fallback={<div className="py-20 text-center text-xs text-zinc-500">Loading documents...</div>}>
      <DocumentsContent />
    </Suspense>
  );
}
