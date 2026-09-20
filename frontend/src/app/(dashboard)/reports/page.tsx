'use client';

import { useState } from 'react';
import { 
  Download, 
  Briefcase, 
  FileSearch, 
  Users 
} from 'lucide-react';
import { casesApi } from '@/lib/api/cases';
import { evidenceApi } from '@/lib/api/evidence';
import { usersApi } from '@/lib/api/users';
import { useQuery } from '@tanstack/react-query';
import { formatDate } from '@/lib/crypto';
import type { EvidenceDetailResponse } from '@/types';

export default function ReportsPage() {
  const [exportingType, setExportingType] = useState<string | null>(null);

  // Pre-fetch data for exports
  const { data: cases = [] } = useQuery({
    queryKey: ['cases-export'],
    queryFn: () => casesApi.getAll(),
  });

  const { data: users = [] } = useQuery({
    queryKey: ['users-export'],
    queryFn: () => usersApi.getAll(),
  });

  const handleDownload = (filename: string, csvContent: string) => {
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);
    link.setAttribute('href', url);
    link.setAttribute('download', filename);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  const escapeCSV = (str: string | undefined | null) => {
    if (!str) return '""';
    const escaped = String(str).replace(/"/g, '""');
    return `"${escaped}"`;
  };

  const exportCases = () => {
    setExportingType('cases');
    try {
      const headers = ['ID', 'Case Number', 'Title', 'Status', 'Jurisdiction', 'Lead Officer ID', 'Created At', 'Description'];
      const rows = cases.map(c => [
        escapeCSV(c.id),
        escapeCSV(c.caseNumber),
        escapeCSV(c.title),
        escapeCSV(c.status),
        escapeCSV(c.jurisdiction),
        escapeCSV(c.leadOfficerId),
        escapeCSV(formatDate(c.createdAt)),
        escapeCSV(c.description),
      ]);
      const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      handleDownload(`Cases_Export_${new Date().toISOString().split('T')[0]}.csv`, csvContent);
    } finally {
      setExportingType(null);
    }
  };

  const exportEvidence = async () => {
    setExportingType('evidence');
    try {
      // Need to fetch evidence for all cases since we don't have a global evidence endpoint in API
      const evidencePromises = cases.map(c => evidenceApi.getByCaseId(c.id).catch(() => [] as EvidenceDetailResponse[]));
      const results = await Promise.all(evidencePromises);
      const allEvidence = results.flat();

      const headers = ['ID', 'Evidence Number', 'Case ID', 'Title', 'Classification', 'Custody State', 'Created At', 'Description'];
      const rows = allEvidence.map(e => [
        escapeCSV(e.id),
        escapeCSV(e.evidenceNumber),
        escapeCSV(e.caseId),
        escapeCSV(e.title),
        escapeCSV(e.classification),
        escapeCSV(e.custodyState),
        escapeCSV(formatDate(e.createdAt)),
        escapeCSV(e.description),
      ]);
      const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      handleDownload(`Evidence_Manifest_${new Date().toISOString().split('T')[0]}.csv`, csvContent);
    } catch (e) {
      console.error(e);
      alert('Failed to export evidence data');
    } finally {
      setExportingType(null);
    }
  };

  const exportUsers = () => {
    setExportingType('users');
    try {
      const headers = ['ID', 'Username', 'Full Name', 'Role', 'Badge Number', 'Department', 'Created At'];
      const rows = users.map(u => [
        escapeCSV(u.id),
        escapeCSV(u.username),
        escapeCSV(u.fullName),
        escapeCSV(u.role),
        escapeCSV(u.badgeNumber),
        escapeCSV(u.department),
        escapeCSV(formatDate(u.createdAt)),
      ]);
      const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      handleDownload(`System_Actors_Export_${new Date().toISOString().split('T')[0]}.csv`, csvContent);
    } finally {
      setExportingType(null);
    }
  };

  return (
    <div className="space-y-8">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-zinc-100">Reporting & Exports</h1>
          <p className="text-xs text-zinc-400 mt-1">
            Generate formal CSV manifests of case dockets, evidence registries, and system actors
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Cases Export Card */}
        <div className="border border-zinc-800 bg-zinc-950 p-8 rounded-xl hover:border-zinc-700 transition-colors flex flex-col justify-between">
          <div>
            <div className="w-10 h-10 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 mb-4">
              <Briefcase className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold tracking-tight text-zinc-100">Case Docket Registry</h3>
            <p className="text-xs text-zinc-400 mt-2 leading-relaxed">
              Export all formal investigation dockets, their legal statuses, jurisdictions, and lead officers.
            </p>
          </div>
          <div className="mt-6 pt-4 border-t border-zinc-800">
            <button
              onClick={exportCases}
              disabled={exportingType !== null}
              className="w-full btn-primary flex justify-center"
            >
              <Download className="w-4 h-4 mr-2" />
              {exportingType === 'cases' ? 'Generating...' : 'Export to CSV'}
            </button>
          </div>
        </div>

        {/* Evidence Export Card */}
        <div className="border border-zinc-800 bg-zinc-950 p-8 rounded-xl hover:border-zinc-700 transition-colors flex flex-col justify-between">
          <div>
            <div className="w-10 h-10 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 mb-4">
              <FileSearch className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold tracking-tight text-zinc-100">Evidence Manifest</h3>
            <p className="text-xs text-zinc-400 mt-2 leading-relaxed">
              Export the full registry of all cryptographically sealed evidence items across all dockets.
            </p>
          </div>
          <div className="mt-6 pt-4 border-t border-zinc-800">
            <button
              onClick={exportEvidence}
              disabled={exportingType !== null}
              className="w-full btn-primary flex justify-center"
            >
              <Download className="w-4 h-4 mr-2" />
              {exportingType === 'evidence' ? 'Generating...' : 'Export to CSV'}
            </button>
          </div>
        </div>

        {/* Users Export Card */}
        <div className="border border-zinc-800 bg-zinc-950 p-8 rounded-xl hover:border-zinc-700 transition-colors flex flex-col justify-between">
          <div>
            <div className="w-10 h-10 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300 mb-4">
              <Users className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold tracking-tight text-zinc-100">System Actors</h3>
            <p className="text-xs text-zinc-400 mt-2 leading-relaxed">
              Export directory of registered officers, analysts, prosecutors, and administrators.
            </p>
          </div>
          <div className="mt-6 pt-4 border-t border-zinc-800">
            <button
              onClick={exportUsers}
              disabled={exportingType !== null}
              className="w-full btn-primary flex justify-center"
            >
              <Download className="w-4 h-4 mr-2" />
              {exportingType === 'users' ? 'Generating...' : 'Export to CSV'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
