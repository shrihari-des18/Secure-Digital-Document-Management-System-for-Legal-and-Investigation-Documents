'use client';

import { useState } from 'react';
import {
  Brain,
  FileText,
  AlertTriangle,
  Tag,
  Hash,
  Sparkles,
  Copy,
  Check,
  RefreshCw,
  ShieldCheck,
} from 'lucide-react';
import { clsx } from 'clsx';
import { aiApi } from '@/lib/api/ai';
import { formatDate } from '@/lib/crypto';
import { AnalysisStatus } from '@/types';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';

interface AiAnalysisPanelProps {
  evidenceId: string;
}

export function AiAnalysisPanel({ evidenceId }: AiAnalysisPanelProps) {
  const queryClient = useQueryClient();
  const [copied, setCopied] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  // Fetch analysis results
  const { data: analysis, isLoading } = useQuery({
    queryKey: ['ai-analysis', evidenceId],
    queryFn: () => aiApi.getResults(evidenceId),
    retry: false,
  });

  // Trigger analysis mutation
  const triggerMutation = useMutation({
    mutationFn: () => aiApi.trigger(evidenceId, { forceReanalysis: true }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ai-analysis', evidenceId] });
      setActionError(null);
    },
    onError: (err: any) => {
      setActionError(err.message || 'Failed to trigger AI analysis pipeline');
    },
  });

  const handleCopyOcr = () => {
    if (analysis?.ocrText) {
      navigator.clipboard.writeText(analysis.ocrText);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  // Parse PII entities safely
  let piiList: Array<{ type: string; value?: string; span?: [number, number]; action?: string }> = [];
  if (analysis?.detectedPii) {
    try {
      const parsed = JSON.parse(analysis.detectedPii);
      if (Array.isArray(parsed)) {
        piiList = parsed;
      }
    } catch {
      piiList = analysis.detectedPii.split(',').map((p) => ({ type: p.trim() }));
    }
  }

  // Parse classification tags safely
  let tagsList: string[] = [];
  if (analysis?.classificationTags) {
    try {
      const parsed = JSON.parse(analysis.classificationTags);
      if (Array.isArray(parsed)) {
        tagsList = parsed;
      } else {
        tagsList = analysis.classificationTags.split(',').map((t) => t.trim());
      }
    } catch {
      tagsList = analysis.classificationTags.split(',').map((t) => t.trim());
    }
  }

  const confidencePct = analysis?.confidenceScore ? Math.round(analysis.confidenceScore * 100) : 0;

  return (
    <div className="card-zinc">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-5 border-b border-zinc-800">
        <div className="flex items-center gap-3">
          <div className="icon-box-primary">
            <Brain className="w-5 h-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-base font-bold text-zinc-100 tracking-tight">AI Forensics & Evidence Intelligence</h3>
              <span className="text-[10px] px-2 py-0.5 rounded-full bg-blue-500/10 text-blue-400 border border-blue-500/20 font-mono">
                ML MICROSERVICE
              </span>
            </div>
            <p className="text-xs text-zinc-400 mt-0.5">
              OCR text extraction, PII entity detection, auto-classification, and pHash similarity
            </p>
          </div>
        </div>

        <button
          onClick={() => triggerMutation.mutate()}
          disabled={triggerMutation.isPending}
          className="btn-primary text-xs shrink-0"
        >
          <RefreshCw className={clsx('w-3.5 h-3.5', triggerMutation.isPending && 'animate-spin')} />
          {triggerMutation.isPending ? 'Running AI Pipeline...' : 'Trigger AI Analysis'}
        </button>
      </div>

      {actionError && (
        <div className="mt-4 p-3 rounded-xl bg-red-500/10 border border-red-500/30 text-red-400 text-xs">
          {actionError}
        </div>
      )}

      {isLoading ? (
        <div className="py-12 text-center text-xs text-zinc-400">Loading AI forensics records...</div>
      ) : !analysis ? (
        <div className="py-12 text-center">
          <Sparkles className="w-8 h-8 text-zinc-600 mx-auto mb-2" />
          <p className="text-xs text-zinc-400">No AI analysis results available yet for this evidence item.</p>
          <p className="text-[11px] text-zinc-500 mt-1">
            Click &quot;Trigger AI Analysis&quot; to dispatch OCR, PII scanning, and tagging.
          </p>
        </div>
      ) : (
        <div className="space-y-6 mt-6">
          {/* Status & Confidence Grid */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
              <span className="text-xs text-zinc-400">Pipeline Status</span>
              <div className="mt-1 flex items-center gap-2">
                <span
                  className={clsx(
                    'text-xs font-bold font-mono px-2 py-0.5 rounded-md uppercase border',
                    analysis.status === AnalysisStatus.COMPLETED
                      ? 'bg-emerald-500/10 text-emerald-400 border-emerald-500/30'
                      : analysis.status === AnalysisStatus.PROCESSING
                      ? 'bg-amber-500/10 text-amber-400 border-amber-500/30 animate-pulse'
                      : 'bg-zinc-800 text-zinc-300 border-zinc-700'
                  )}
                >
                  {analysis.status}
                </span>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
              <span className="text-xs text-zinc-400">ML Confidence Score</span>
              <div className="mt-1 flex items-center gap-2">
                <span className="text-base font-bold text-zinc-100 font-mono">{confidencePct}%</span>
                <div className="flex-1 h-2 bg-zinc-950 rounded-full overflow-hidden border border-zinc-800">
                  <div
                    className="h-full bg-zinc-100 rounded-full transition-all duration-500"
                    style={{ width: `${confidencePct}%` }}
                  />
                </div>
              </div>
            </div>

            <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
              <span className="text-xs text-zinc-400">Analyzed Timestamp</span>
              <p className="text-xs font-medium text-zinc-200 mt-1">{formatDate(analysis.createdAt)}</p>
            </div>
          </div>

          {/* Detected PII Entities */}
          <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
            <div className="flex items-center gap-2 text-xs font-bold text-zinc-100 mb-3">
              <AlertTriangle className="w-4 h-4 text-amber-400" />
              <span>Detected PII / Sensitive Entities for Redaction</span>
            </div>
            {piiList.length === 0 ? (
              <p className="text-xs text-emerald-400 flex items-center gap-1.5">
                <ShieldCheck className="w-4 h-4" /> No sensitive PII entities detected in this document.
              </p>
            ) : (
              <div className="flex flex-wrap gap-2">
                {piiList.map((pii, i) => (
                  <span
                    key={i}
                    className="inline-flex items-center gap-1.5 text-xs px-2.5 py-1 rounded-lg bg-amber-500/10 text-amber-400 border border-amber-500/30 font-mono"
                  >
                    <span className="font-bold text-amber-300">{pii.type}:</span>
                    <span>{pii.value || pii.action || 'Flagged for Redaction'}</span>
                  </span>
                ))}
              </div>
            )}
          </div>

          {/* Classification Tags */}
          {tagsList.length > 0 && (
            <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
              <div className="flex items-center gap-2 text-xs font-bold text-zinc-100 mb-3">
                <Tag className="w-4 h-4 text-blue-400" />
                <span>Auto-Generated Classification Tags</span>
              </div>
              <div className="flex flex-wrap gap-2">
                {tagsList.map((tag, i) => (
                  <span
                    key={i}
                    className="text-xs px-2.5 py-1 rounded-lg bg-blue-500/10 text-blue-400 border border-blue-500/30 font-medium"
                  >
                    #{tag}
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Perceptual Hash */}
          {analysis.perceptualHash && (
            <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
              <div className="flex items-center gap-2 text-xs font-bold text-zinc-100 mb-2">
                <Hash className="w-4 h-4 text-zinc-400" />
                <span>Perceptual Hash (pHash for Image/Media De-duplication)</span>
              </div>
              <p className="font-mono text-xs text-zinc-300 bg-zinc-950 p-2.5 rounded-lg border border-zinc-800 select-all break-all">
                {analysis.perceptualHash}
              </p>
            </div>
          )}

          {/* Extracted OCR Text */}
          <div className="p-4 rounded-xl bg-zinc-900/60 border border-zinc-800">
            <div className="flex items-center justify-between gap-2 mb-2">
              <div className="flex items-center gap-2 text-xs font-bold text-zinc-100">
                <FileText className="w-4 h-4 text-emerald-400" />
                <span>Extracted OCR Full Text</span>
              </div>
              {analysis.ocrText && (
                <button
                  onClick={handleCopyOcr}
                  className="flex items-center gap-1 text-xs text-zinc-400 hover:text-zinc-100 px-2 py-1 rounded-md hover:bg-zinc-800 transition-colors"
                >
                  {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5" />}
                  <span>{copied ? 'Copied' : 'Copy Text'}</span>
                </button>
              )}
            </div>
            <div className="bg-zinc-950 p-3.5 rounded-lg border border-zinc-800 max-h-60 overflow-y-auto">
              <pre className="text-xs text-zinc-300 font-mono whitespace-pre-wrap leading-relaxed">
                {analysis.ocrText || 'No OCR text extracted from this file.'}
              </pre>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

