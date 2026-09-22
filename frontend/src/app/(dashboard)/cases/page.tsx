'use client';

import { useState } from 'react';
import Link from 'next/link';
import {
  Briefcase,
  Plus,
  Search,
  ArrowRight,
  MapPin,
  Calendar,
} from 'lucide-react';
import { clsx } from 'clsx';
import { casesApi } from '@/lib/api/cases';
import { usersApi } from '@/lib/api/users';
import { formatDate } from '@/lib/crypto';
import { CaseStatusBadge } from '@/components/ui/badge';
import { Modal } from '@/components/ui/modal';
import type { CreateCaseRequest } from '@/types';
import { CaseStatus } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

export default function CasesPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedStatus, setSelectedStatus] = useState<CaseStatus | 'ALL'>('ALL');
  const [isCreateOpen, setIsCreateOpen] = useState(false);

  // Form states
  const [caseNumber, setCaseNumber] = useState('');
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [jurisdiction, setJurisdiction] = useState('');
  const [leadOfficerId, setLeadOfficerId] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  // Fetch cases
  const { data: cases = [], isLoading } = useQuery({
    queryKey: ['cases', selectedStatus],
    queryFn: () => casesApi.getAll(selectedStatus === 'ALL' ? undefined : selectedStatus),
  });

  // Fetch users for lead officer picker
  const { data: users = [] } = useQuery({
    queryKey: ['users'],
    queryFn: () => usersApi.getAll(),
    enabled: isCreateOpen,
  });

  // Create case mutation
  const createMutation = useMutation({
    mutationFn: (data: CreateCaseRequest) => casesApi.create(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['cases'] });
      setIsCreateOpen(false);
      setCaseNumber('');
      setTitle('');
      setDescription('');
      setJurisdiction('');
      setLeadOfficerId('');
      setFormError(null);
    },
    onError: (err: any) => {
      setFormError(err.message || 'Failed to create case');
    },
  });

  const handleCreateSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!leadOfficerId) {
      setFormError('Please select a lead investigating officer');
      return;
    }
    createMutation.mutate({
      caseNumber: caseNumber.trim(),
      title: title.trim(),
      description: description.trim() || undefined,
      jurisdiction: jurisdiction.trim(),
      leadOfficerId,
      assignedOfficerIds: [leadOfficerId],
    });
  };

  const filteredCases = cases.filter((c) => {
    const matchQuery =
      c.caseNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      c.jurisdiction.toLowerCase().includes(searchQuery.toLowerCase());
    return matchQuery;
  });

  return (
    <div className="space-y-8">
      {/* Top Banner & Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Case Docket Registry</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Formal legal investigation dockets, FIR records, and chain-of-custody containers
          </p>
        </div>

        <button
          onClick={() => setIsCreateOpen(true)}
          className="btn-primary shrink-0"
        >
          <Plus className="w-4 h-4" />
          <span>New Case Docket</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="border border-zinc-800 bg-zinc-950 p-6 rounded-xl flex flex-col md:flex-row items-center justify-between gap-6">
        {/* Search */}
        <div className="relative w-full md:w-96">
          <Search className="w-4 h-4 text-zinc-500 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search FIR / Case number, title..."
            className="input-field !pl-10"
          />
        </div>

        {/* Status Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto w-full md:w-auto pb-1 md:pb-0">
          {(['ALL', ...Object.values(CaseStatus)] as const).map((st) => (
            <button
              key={st}
              onClick={() => setSelectedStatus(st)}
              className={clsx(
                'text-xs px-3 py-1.5 rounded-lg font-medium transition-colors whitespace-nowrap',
                selectedStatus === st
                  ? 'bg-zinc-100 text-zinc-950 font-bold border border-zinc-100'
                  : 'bg-zinc-900/60 text-zinc-400 hover:text-zinc-200 border border-zinc-800'
              )}
            >
              {st === 'ALL' ? 'All Cases' : st.replace('_', ' ')}
            </button>
          ))}
        </div>
      </div>

      {/* Case Grid */}
      {isLoading ? (
        <div className="py-20 text-center text-xs text-zinc-500">Loading case dockets from vault...</div>
      ) : filteredCases.length === 0 ? (
        <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-12 text-center">
          <div className="w-12 h-12 rounded-full bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-400 mx-auto mb-3">
            <Briefcase className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold tracking-tight text-zinc-100">No investigation cases found</h3>
          <p className="text-xs text-zinc-400 mt-1 max-w-sm mx-auto">
            {searchQuery
              ? 'No cases match your search query. Try resetting your search filters.'
              : 'Create a new case docket to begin evidence intake and legal document filing.'}
          </p>
          {!searchQuery && (
            <button
              onClick={() => setIsCreateOpen(true)}
              className="mt-4 btn-primary"
            >
              <Plus className="w-4 h-4" />
              <span>Initialize Case Docket</span>
            </button>
          )}
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredCases.map((c) => (
            <Link
              key={c.id}
              href={`/cases/${c.id}`}
              className="group border border-zinc-800 bg-zinc-950 p-6 rounded-xl hover:border-zinc-700 hover:bg-zinc-800/20 transition-colors flex flex-col justify-between"
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-3">
                  <span className="font-mono text-xs font-bold text-zinc-200 px-2 py-0.5 rounded bg-zinc-900 border border-zinc-800">
                    {c.caseNumber}
                  </span>
                  <CaseStatusBadge status={c.status} />
                </div>

                <h3 className="text-sm font-bold tracking-tight text-zinc-100 group-hover:text-zinc-200 transition-colors line-clamp-1">
                  {c.title}
                </h3>
                <p className="text-xs text-zinc-400 line-clamp-2 mt-1.5 leading-relaxed">
                  {c.description || 'No description provided for this case docket.'}
                </p>
              </div>

              <div className="mt-5 pt-4 border-t border-zinc-800/80 space-y-2">
                <div className="flex items-center gap-2 text-xs text-zinc-400">
                  <MapPin className="w-3.5 h-3.5 text-zinc-500 shrink-0" />
                  <span className="truncate">{c.jurisdiction}</span>
                </div>
                <div className="flex items-center justify-between text-xs text-zinc-400 font-mono">
                  <span className="flex items-center gap-1.5">
                    <Calendar className="w-3.5 h-3.5 text-zinc-500" />
                    {formatDate(c.createdAt)}
                  </span>
                  <div className="flex items-center gap-1 text-zinc-200 font-semibold text-[11px] group-hover:translate-x-0.5 transition-transform">
                    <span>Open Docket</span>
                    <ArrowRight className="w-3.5 h-3.5" />
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}

      {/* Create Case Modal */}
      <Modal
        isOpen={isCreateOpen}
        onClose={() => setIsCreateOpen(false)}
        title="Initialize New Investigation Case Docket"
        subtitle="Registers a formal case container with assigned jurisdiction and lead officer"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateSubmit} className="space-y-8">
          {/* Primary Details Section */}
          <div className="space-y-6">
            <h3 className="text-sm font-bold text-zinc-100 border-b border-zinc-800 pb-2">Primary Details</h3>
            
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-400 mb-1.5">
                  Case Number <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={caseNumber}
                  onChange={(e) => setCaseNumber(e.target.value)}
                  placeholder="FIR-2026-DELHI-101"
                  required
                  className="input-field font-mono"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-400 mb-1.5">
                  Investigation Title <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={title}
                  onChange={(e) => setTitle(e.target.value)}
                  placeholder="e.g., Operation Cyber Vault"
                  required
                  className="input-field"
                />
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-400 mb-1.5">
                Case Summary / Description
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={3}
                placeholder="Brief operational context, preliminary allegations, and initial findings..."
                className="input-field"
              />
            </div>
          </div>

          {/* Operational Details Section */}
          <div className="space-y-6">
            <h3 className="text-sm font-bold text-zinc-100 border-b border-zinc-800 pb-2">Operational Details</h3>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-400 mb-1.5">
                  Jurisdiction <span className="text-red-400">*</span>
                </label>
                <input
                  type="text"
                  value={jurisdiction}
                  onChange={(e) => setJurisdiction(e.target.value)}
                  placeholder="Delhi NCT Cyber Crime Cell"
                  required
                  className="input-field"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold uppercase tracking-wider font-mono text-zinc-400 mb-1.5">
                  Lead Officer <span className="text-red-400">*</span>
                </label>
                <select
                  value={leadOfficerId}
                  onChange={(e) => setLeadOfficerId(e.target.value)}
                  required
                  className="select-field"
                >
                  <option value="">Select lead officer...</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id} className="bg-zinc-900 text-zinc-100">
                      {u.fullName} ({u.role.replace('_', ' ')})
                    </option>
                  ))}
                </select>
              </div>
            </div>
          </div>
            {formError && (
            <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs">
              {formError}
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
              <Plus className="w-4 h-4" />
              <span>{createMutation.isPending ? 'Creating Docket...' : 'Create Case Docket'}</span>
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
}
