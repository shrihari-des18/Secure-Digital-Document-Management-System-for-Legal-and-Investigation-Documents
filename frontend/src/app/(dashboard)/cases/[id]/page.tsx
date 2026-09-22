'use client';

import { useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import {
  ChevronLeft,
  FileSearch,
  FileText,
  Users,
  Plus,
  MapPin,
  Calendar,
  ArrowRight,
  Download,
  AlertTriangle,
  Upload,
  Scale,
  Settings,
  ShieldAlert,
} from 'lucide-react';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { documentsApi } from '@/lib/api/documents';
import { usersApi } from '@/lib/api/users';
import { formatDate, truncateHash } from '@/lib/crypto';
import {
  CaseStatusBadge,
  CustodyStateBadge,
  ClassificationBadge,
  DocumentTypeBadge,
} from '@/components/ui/badge';
import { Tabs } from '@/components/ui/tabs';
import { Modal } from '@/components/ui/modal';
import { CourtBundleModal } from '@/components/cases/court-bundle-modal';
import { SecurityActivityTab } from '@/components/cases/security-activity-tab';
import { useAuthStore } from '@/stores/auth-store';
import { RoleEnum } from '@/types';
import type {
  CreateLegalDocumentRequest,
} from '@/types';
import { CaseStatus, LegalDocumentType } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export default function CaseDetailPage() {
  const params = useParams();
  const caseId = params?.id as string;
  const queryClient = useQueryClient();
  const user = useAuthStore((s) => s.user);

  const [activeTab, setActiveTab] = useState<string>('evidence');
  const [isStatusModalOpen, setIsStatusModalOpen] = useState(false);
  const [newStatus, setNewStatus] = useState<CaseStatus>(CaseStatus.UNDER_INVESTIGATION);
  const [statusReason, setStatusReason] = useState('');
  const [isCourtBundleOpen, setIsCourtBundleOpen] = useState(false);

  // Edit case modal state
  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [editTitle, setEditTitle] = useState('');
  const [editDescription, setEditDescription] = useState('');
  const [editJurisdiction, setEditJurisdiction] = useState('');

  // Legal document filing modal state
  const [isDocModalOpen, setIsDocModalOpen] = useState(false);
  const [docNumber, setDocNumber] = useState('');
  const [docTitle, setDocTitle] = useState('');
  const [docType, setDocType] = useState<LegalDocumentType>(LegalDocumentType.FIR);
  const [docDescription, setDocDescription] = useState('');
  const [docFile, setDocFile] = useState<File | null>(null);
  const [docError, setDocError] = useState<string | null>(null);

  // Fetch case details
  const { data: caseData, isLoading: caseLoading, error: caseError } = useQuery({
    queryKey: ['case', caseId],
    queryFn: () => casesApi.getById(caseId),
    enabled: !!caseId,
  });

  // Fetch linked evidence items
  const { data: evidenceList = [], isLoading: evidenceLoading } = useQuery({
    queryKey: ['evidence-case', caseId],
    queryFn: () => evidenceApi.getByCaseId(caseId),
    enabled: !!caseId,
  });

  // Fetch linked legal documents
  const { data: documentList = [], isLoading: docLoading } = useQuery({
    queryKey: ['documents-case', caseId],
    queryFn: () => documentsApi.getByCaseId(caseId),
    enabled: !!caseId,
  });

  // Fetch users for assigned team details
  const { data: allUsers = [] } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getAll(),
  });

  // Status transition mutation
  const statusMutation = useMutation({
    mutationFn: () => casesApi.updateStatus(caseId, newStatus, statusReason.trim() || undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['case', caseId] });
      queryClient.invalidateQueries({ queryKey: ['cases'] });
      setIsStatusModalOpen(false);
      setStatusReason('');
    },
    onError: (err: any) => {
      alert(err.message || 'Status update failed');
    },
  });

  // Edit case mutation
  const updateCaseMutation = useMutation({
    mutationFn: () => casesApi.update(caseId, {
      title: editTitle.trim(),
      description: editDescription.trim() || undefined,
      jurisdiction: editJurisdiction.trim(),
      assignedOfficerIds: caseData?.assignedOfficerIds || [],
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['case', caseId] });
      queryClient.invalidateQueries({ queryKey: ['cases'] });
      setIsEditModalOpen(false);
    },
    onError: (err: any) => {
      alert(err.message || 'Failed to update case');
    },
  });

  // Legal document upload mutation
  const createDocMutation = useMutation({
    mutationFn: () => {
      if (!docFile) throw new Error('Please select a document file to attach');
      const meta: CreateLegalDocumentRequest = {
        caseId,
        documentNumber: docNumber.trim(),
        title: docTitle.trim(),
        documentType: docType,
        description: docDescription.trim() || undefined,
        changeSummary: 'Initial legal document filing under case docket',
      };
      return documentsApi.create(meta, docFile);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['documents-case', caseId] });
      setIsDocModalOpen(false);
      setDocNumber('');
      setDocTitle('');
      setDocDescription('');
      setDocFile(null);
      setDocError(null);
    },
    onError: (err: any) => {
      setDocError(err.message || 'Failed to file legal document');
    },
  });

  if (caseLoading) {
    return <div className="py-20 text-center text-xs text-zinc-500">Loading case workspace...</div>;
  }

  if (!caseData || caseError) {
    return (
      <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
        <AlertTriangle className="w-10 h-10 text-amber-400 mx-auto mb-3" />
        <h2 className="text-sm font-bold tracking-tight text-zinc-100">Case docket not found or access restricted</h2>
        <p className="text-xs text-zinc-400 mt-1">
          The requested case ID does not exist or you lack clearance to inspect this docket.
        </p>
        <Link
          href="/cases"
          className="mt-4 btn-primary"
        >
          <ChevronLeft className="w-4 h-4" />
          <span>Back to Case Registry</span>
        </Link>
      </div>
    );
  }

  const canViewActivity = user?.role === RoleEnum.SYSTEM_ADMIN || user?.role === RoleEnum.AUDITOR || user?.role === RoleEnum.INVESTIGATING_OFFICER;
  const tabs = [
    { id: 'evidence', label: 'Digital Evidence Items', icon: FileSearch, count: evidenceList.length },
    { id: 'documents', label: 'Legal Documents & Filings', icon: FileText, count: documentList.length },
    { id: 'team', label: 'Assigned Team & Officers', icon: Users, count: caseData.assignedOfficerIds?.length || 1 },
    ...(canViewActivity ? [{ id: 'activity', label: 'Security Activity', icon: ShieldAlert }] : []),
  ];

  const leadUser = allUsers.find((u) => u.id === caseData.leadOfficerId);

  return (
    <div className="space-y-8">
      {/* Back button */}
      <Link
        href="/cases"
        className="inline-flex items-center gap-1.5 text-xs text-zinc-400 hover:text-zinc-100 transition-colors font-mono"
      >
        <ChevronLeft className="w-4 h-4" />
        <span>Back to Case Registry</span>
      </Link>

      {/* Case Header Card */}
      <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-8 relative overflow-hidden">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-6 pb-6 border-b border-zinc-800">
          <div>
            <div className="flex items-center gap-3 mb-3">
              <span className="font-mono text-xs font-bold text-zinc-200 px-2.5 py-0.5 rounded bg-zinc-900 border border-zinc-800">
                {caseData.caseNumber}
              </span>
              <CaseStatusBadge status={caseData.status} />
            </div>
            <h1 className="text-xl font-bold tracking-tight text-zinc-100">{caseData.title}</h1>
            <p className="text-xs text-zinc-400 mt-2 leading-relaxed max-w-3xl">
              {caseData.description || 'No description entered for this investigation docket.'}
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3 shrink-0">
            <button
              onClick={() => setIsCourtBundleOpen(true)}
              className="btn-secondary text-amber-400 border-amber-500/20 hover:border-amber-500/40"
            >
              <Scale className="w-4 h-4 text-amber-400" />
              <span>Court Dossier</span>
            </button>

            <button
              onClick={() => {
                setEditTitle(caseData.title);
                setEditDescription(caseData.description || '');
                setEditJurisdiction(caseData.jurisdiction);
                setIsEditModalOpen(true);
              }}
              className="btn-secondary"
            >
              <Settings className="w-4 h-4" />
              <span>Edit</span>
            </button>

            <button
              onClick={() => {
                setNewStatus(caseData.status);
                setIsStatusModalOpen(true);
              }}
              className="btn-secondary"
            >
              Update Status
            </button>

            <Link
              href={`/evidence/upload?caseId=${caseData.id}`}
              className="btn-primary"
            >
              <Plus className="w-4 h-4" />
              <span>Ingest Evidence</span>
            </Link>
          </div>
        </div>

        {/* Metadata stats row */}
        <details className="group mt-6">
          <summary className="text-xs font-mono font-semibold text-zinc-400 uppercase tracking-wider cursor-pointer hover:text-zinc-200 transition-colors inline-flex items-center gap-2">
            <span>View Docket Metadata</span>
            <span className="group-open:rotate-180 transition-transform">â–¼</span>
          </summary>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6 pt-4 text-xs">
            <div className="flex items-center gap-3 text-zinc-400">
              <div className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300">
                <MapPin className="w-4 h-4" />
              </div>
              <div>
                <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Jurisdiction</span>
                <span className="text-zinc-200 font-medium">{caseData.jurisdiction}</span>
              </div>
            </div>

            <div className="flex items-center gap-3 text-zinc-400">
              <div className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-amber-400">
                <Users className="w-4 h-4" />
              </div>
              <div>
                <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Lead Officer</span>
                <span className="text-zinc-200 font-medium">
                  {leadUser ? `${leadUser.fullName} (${leadUser.badgeNumber || 'DL'})` : caseData.leadOfficerId}
                </span>
              </div>
            </div>

            <div className="flex items-center gap-3 text-zinc-400">
              <div className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-emerald-400">
                <Calendar className="w-4 h-4" />
              </div>
              <div>
                <span className="text-[10px] uppercase font-semibold font-mono text-zinc-500 block">Created Date</span>
                <span className="text-zinc-200 font-medium font-mono">{formatDate(caseData.createdAt)}</span>
              </div>
            </div>
          </div>
        </details>
      </div>

      {/* Tabs Switcher */}
      <Tabs tabs={tabs} activeTab={activeTab} onChange={(id) => setActiveTab(id as any)} />

      {/* Tab 1: Linked Digital Evidence */}
      {activeTab === 'evidence' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold tracking-tight text-zinc-100">Ingested Evidence Vault</h3>
            <Link
              href={`/evidence/upload?caseId=${caseData.id}`}
              className="btn-secondary text-xs py-1.5"
            >
              <Plus className="w-3.5 h-3.5 text-zinc-300" />
              <span>Upload New Evidence</span>
            </Link>
          </div>

          {evidenceLoading ? (
            <div className="py-12 text-center text-xs text-zinc-500">Loading evidence items...</div>
          ) : evidenceList.length === 0 ? (
            <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-10 text-center">
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-500 mx-auto mb-2">
                <FileSearch className="w-5 h-5" />
              </div>
              <p className="text-xs text-zinc-400">No digital evidence ingested for this case docket yet.</p>
              <Link
                href={`/evidence/upload?caseId=${caseData.id}`}
                className="mt-3 btn-primary"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Ingest First Evidence Item</span>
              </Link>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {evidenceList.map((ev) => (
                <Link
                  key={ev.id}
                  href={`/evidence/${ev.id}`}
                  className="group border border-zinc-800 bg-zinc-950 p-5 rounded-xl hover:border-zinc-700 hover:bg-zinc-800/20 transition-colors flex flex-col justify-between"
                >
                  <div>
                    <div className="flex items-center justify-between gap-2 mb-2">
                      <span className="font-mono text-xs font-bold text-zinc-200">{ev.evidenceNumber}</span>
                      <div className="flex items-center gap-1.5">
                        <ClassificationBadge level={ev.classification} />
                        <CustodyStateBadge state={ev.custodyState} />
                      </div>
                    </div>
                    <h4 className="text-sm font-bold tracking-tight text-zinc-100 group-hover:text-zinc-200 transition-colors line-clamp-1">
                      {ev.title}
                    </h4>
                    <p className="text-xs text-zinc-400 line-clamp-1 mt-1">
                      {ev.description || 'No description provided.'}
                    </p>
                  </div>

                  <div className="mt-4 pt-3 border-t border-zinc-800 flex items-center justify-between text-xs text-zinc-400 font-mono">
                    <span className="text-[11px] text-emerald-400">
                      SHA: {truncateHash(ev.file?.sha256)}
                    </span>
                    <span className="flex items-center gap-1 text-zinc-200 text-[11px] font-semibold group-hover:translate-x-0.5 transition-transform">
                      Inspect
                      <ArrowRight className="w-3 h-3" />
                    </span>
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 2: Legal Documents */}
      {activeTab === 'documents' && (
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="text-sm font-bold tracking-tight text-zinc-100">Case Legal Filings & Documents</h3>
            <button
              onClick={() => setIsDocModalOpen(true)}
              className="btn-secondary text-xs py-1.5"
            >
              <Plus className="w-3.5 h-3.5 text-zinc-300" />
              <span>File Legal Document</span>
            </button>
          </div>

          {docLoading ? (
            <div className="py-12 text-center text-xs text-zinc-500">Loading filed documents...</div>
          ) : documentList.length === 0 ? (
            <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-10 text-center">
              <div className="w-10 h-10 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-500 mx-auto mb-2">
                <FileText className="w-5 h-5" />
              </div>
              <p className="text-xs text-zinc-400">No legal documents filed for this case yet.</p>
              <button
                onClick={() => setIsDocModalOpen(true)}
                className="mt-3 btn-primary"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>File Initial FIR / Document</span>
              </button>
            </div>
          ) : (
            <div className="space-y-3">
              {documentList.map((doc) => (
                <div
                  key={doc.id}
                  className="border border-zinc-800 bg-zinc-950 rounded-xl p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                >
                  <div>
                    <div className="flex items-center gap-2 mb-1">
                      <span className="font-mono text-xs font-bold text-zinc-200">{doc.documentNumber}</span>
                      <DocumentTypeBadge type={doc.documentType} />
                      <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800 text-zinc-400">
                        v{doc.currentVersion}
                      </span>
                    </div>
                    <h4 className="text-sm font-bold tracking-tight text-zinc-100">{doc.title}</h4>
                    <p className="text-xs text-zinc-400 mt-0.5">
                      {doc.description || 'Initial filed document record.'}
                    </p>
                  </div>

                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => documentsApi.download(doc.id, doc.currentVersion, `${doc.documentNumber}.pdf`)}
                      className="btn-secondary text-xs"
                    >
                      <Download className="w-3.5 h-3.5 text-zinc-300" />
                      <span>Download v{doc.currentVersion}</span>
                    </button>
                    <Link
                      href={`/documents?id=${doc.id}`}
                      className="btn-primary text-xs"
                    >
                      View Revisions
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab 3: Assigned Team & Officers */}
      {activeTab === 'team' && (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6">
          <h3 className="text-sm font-bold tracking-tight text-zinc-100 mb-4">Assigned Investigation Team</h3>
          <div className="space-y-3">
            {caseData.assignedOfficerIds?.map((officerId) => {
              const u = allUsers.find((usr) => usr.id === officerId);
              const isLead = officerId === caseData.leadOfficerId;

              return (
                <div
                  key={officerId}
                  className="p-3.5 rounded-lg border border-zinc-800/80 bg-zinc-900/40 flex items-center justify-between"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-200 font-bold text-xs font-mono">
                      {u ? u.fullName.slice(0, 2).toUpperCase() : 'IO'}
                    </div>
                    <div>
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-zinc-100">
                          {u ? u.fullName : `Officer ID: ${officerId}`}
                        </span>
                        {isLead && (
                          <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 font-semibold">
                            LEAD INVESTIGATOR
                          </span>
                        )}
                      </div>
                      <p className="text-[11px] text-zinc-500 font-mono">
                        {u ? `${u.department || 'Police Department'} â€¢ Badge: ${u.badgeNumber || 'N/A'}` : officerId}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Tab 4: Security Activity */}
      {activeTab === 'activity' && canViewActivity && (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6">
          <SecurityActivityTab caseId={caseId} />
        </div>
      )}

      {/* Court Bundle Dossier Modal */}
      <CourtBundleModal
        isOpen={isCourtBundleOpen}
        onClose={() => setIsCourtBundleOpen(false)}
        caseData={caseData}
        evidenceList={evidenceList}
        documentList={documentList}
      />

      {/* Update Case Status Modal */}
      <Modal
        isOpen={isStatusModalOpen}
        onClose={() => setIsStatusModalOpen(false)}
        title="Transition Investigation Case Lifecycle"
        subtitle="Update the formal legal status of this docket"
      >
        <div className="space-y-6">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Select New Status
            </label>
            <select
              value={newStatus}
              onChange={(e) => setNewStatus(e.target.value as CaseStatus)}
              className="select-field"
            >
              {Object.values(CaseStatus).map((st) => (
                <option key={st} value={st} className="bg-zinc-900 text-zinc-100">
                  {st.replace('_', ' ')}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Reason / Legal Transition Memo
            </label>
            <textarea
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              rows={3}
              placeholder="e.g., Primary digital evidence seized and charge sheet prepared for court submission..."
              className="input-field"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsStatusModalOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              onClick={() => statusMutation.mutate()}
              disabled={statusMutation.isPending}
              className="btn-primary"
            >
              {statusMutation.isPending ? 'Updating...' : 'Confirm Status Update'}
            </button>
          </div>
        </div>
      </Modal>

      {/* Edit Case Metadata Modal */}
      <Modal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        title="Edit Case Docket"
        subtitle="Update the metadata for this legal investigation container"
      >
        <div className="space-y-6">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Investigation Title / Subject
            </label>
            <input
              type="text"
              value={editTitle}
              onChange={(e) => setEditTitle(e.target.value)}
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Jurisdiction
            </label>
            <input
              type="text"
              value={editJurisdiction}
              onChange={(e) => setEditJurisdiction(e.target.value)}
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Case Summary / Description
            </label>
            <textarea
              value={editDescription}
              onChange={(e) => setEditDescription(e.target.value)}
              rows={4}
              className="input-field"
            />
          </div>

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsEditModalOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              onClick={() => updateCaseMutation.mutate()}
              disabled={updateCaseMutation.isPending}
              className="btn-primary"
            >
              {updateCaseMutation.isPending ? 'Saving...' : 'Save Changes'}
            </button>
          </div>
        </div>
      </Modal>

      {/* File Legal Document Modal */}
      <Modal
        isOpen={isDocModalOpen}
        onClose={() => setIsDocModalOpen(false)}
        title="File Initial Legal Document"
        subtitle="Upload and cryptographically register an investigation legal document (FIR, Warrant, Charge Sheet)"
        maxWidth="lg"
      >
        <form
          onSubmit={(e) => {
            e.preventDefault();
            createDocMutation.mutate();
          }}
          className="space-y-4"
        >
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Document Identifier (e.g., FIR-DEL-01) <span className="text-red-400">*</span>
            </label>
            <input
              type="text"
              value={docNumber}
              onChange={(e) => setDocNumber(e.target.value)}
              placeholder="FIR-DELHI-2026-001"
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
              placeholder="e.g., First Information Report (FIR) under Â§66 ITA"
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
              Description / Remarks
            </label>
            <textarea
              value={docDescription}
              onChange={(e) => setDocDescription(e.target.value)}
              rows={2}
              placeholder="Summary of legal document contents..."
              className="input-field"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-300 mb-1.5">
              Document File (PDF / DOC) <span className="text-red-400">*</span>
            </label>
            <input
              type="file"
              onChange={(e) => setDocFile(e.target.files?.[0] || null)}
              required
              className="w-full text-xs text-zinc-300 file:mr-4 file:py-2 file:px-4 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-zinc-100 file:text-zinc-950 hover:file:bg-zinc-200 file:cursor-pointer"
            />
          </div>

          {docError && (
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
              {docError}
            </div>
          )}

          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => setIsDocModalOpen(false)}
              className="btn-secondary"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={createDocMutation.isPending}
              className="btn-primary"
            >
              <Upload className="w-3.5 h-3.5" />
              <span>{createDocMutation.isPending ? 'Filing Document...' : 'File Document'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}

