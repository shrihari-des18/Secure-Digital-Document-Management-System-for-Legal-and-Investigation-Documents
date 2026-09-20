'use client';

import { useState } from 'react';
import {
  ShieldCheck,
  Eye,
  EyeOff,
  Copy,
  Check,
  Lock,
} from 'lucide-react';
import { clsx } from 'clsx';
import type { AiAnalysisResponse } from '@/types';

interface PiiRedactorWorkbenchProps {
  analysis: AiAnalysisResponse;
}

export function PiiRedactorWorkbench({ analysis }: PiiRedactorWorkbenchProps) {
  const [isRedactedView, setIsRedactedView] = useState(true);
  const [copied, setCopied] = useState(false);

  // Parse PII entities
  let detectedEntities: Array<{ type: string; value: string; placeholder?: string }> = [];

  if (analysis.detectedPii) {
    try {
      const parsed = JSON.parse(analysis.detectedPii);
      if (typeof parsed === 'object' && !Array.isArray(parsed)) {
        Object.entries(parsed).forEach(([key, val]) => {
          if (Array.isArray(val)) {
            val.forEach((item) => {
              detectedEntities.push({
                type: key.toUpperCase(),
                value: String(item),
                placeholder: `[REDACTED_${key.toUpperCase()}]`,
              });
            });
          }
        });
      } else if (Array.isArray(parsed)) {
        detectedEntities = parsed.map((p) => ({
          type: p.type || 'PII',
          value: p.value || p.span || '',
          placeholder: `[REDACTED_${(p.type || 'PII').toUpperCase()}]`,
        }));
      }
    } catch {
      // Fallback
    }
  }

  // Generate redacted version of OCR text
  let rawText = analysis.ocrText || 'No text available.';
  let redactedText = rawText;

  detectedEntities.forEach((ent) => {
    if (ent.value && ent.value.length > 2) {
      redactedText = redactedText.split(ent.value).join(ent.placeholder || '[REDACTED]');
    }
  });

  const handleCopy = () => {
    navigator.clipboard.writeText(isRedactedView ? redactedText : rawText);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 space-y-5">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-zinc-800">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-amber-400">
            <Lock className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold tracking-tight text-zinc-100 flex items-center gap-2">
              <span>Forensic PII Redaction Workbench</span>
              <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 font-semibold uppercase">
                DPDP ACT 2023 COMPLIANT
              </span>
            </h3>
            <p className="text-xs text-zinc-400 mt-0.5">
              Live automated mask application for sensitive Aadhaar, PAN, phone numbers, and victim identifiers
            </p>
          </div>
        </div>

        {/* View mode toggle */}
        <div className="flex items-center gap-1.5 bg-zinc-900/60 p-1 rounded-lg border border-zinc-800">
          <button
            onClick={() => setIsRedactedView(true)}
            className={clsx(
              'flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition-colors cursor-pointer',
              isRedactedView
                ? 'bg-zinc-100 text-zinc-950'
                : 'text-zinc-400 hover:text-zinc-200'
            )}
          >
            <EyeOff className="w-3.5 h-3.5" />
            <span>Redacted Filing</span>
          </button>
          <button
            onClick={() => setIsRedactedView(false)}
            className={clsx(
              'flex items-center gap-1.5 px-3 py-1 rounded-md text-xs font-semibold transition-colors cursor-pointer',
              !isRedactedView
                ? 'bg-zinc-100 text-zinc-950'
                : 'text-zinc-400 hover:text-zinc-200'
            )}
          >
            <Eye className="w-3.5 h-3.5" />
            <span>Unredacted Master</span>
          </button>
        </div>
      </div>

      {/* Detected PII chips */}
      <div className="space-y-2">
        <span className="text-xs font-semibold font-mono uppercase text-zinc-400">
          Detected Protected Entities ({detectedEntities.length}):
        </span>
        {detectedEntities.length === 0 ? (
          <p className="text-xs text-emerald-400 flex items-center gap-1.5 font-mono">
            <ShieldCheck className="w-4 h-4" /> No sensitive PII entities identified.
          </p>
        ) : (
          <div className="flex flex-wrap gap-2">
            {detectedEntities.map((ent, i) => (
              <div
                key={i}
                className="inline-flex items-center gap-2 text-xs px-2.5 py-1 rounded-md bg-zinc-900 border border-zinc-800 font-mono text-zinc-300"
              >
                <span className="font-bold text-amber-400">{ent.type}</span>
                <span className="text-zinc-600">→</span>
                <span className="text-zinc-200">{isRedactedView ? ent.placeholder : ent.value}</span>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Text display with copy button */}
      <div className="space-y-2">
        <div className="flex items-center justify-between text-xs">
          <span className="font-semibold text-zinc-200">
            {isRedactedView ? 'Redacted Output for Court Submission:' : 'Original Ingest Text:'}
          </span>
          <button
            onClick={handleCopy}
            className="btn-secondary text-xs py-1 px-2.5"
          >
            {copied ? <Check className="w-3.5 h-3.5 text-emerald-400" /> : <Copy className="w-3.5 h-3.5 text-zinc-400" />}
            <span>{copied ? 'Copied' : 'Copy Text'}</span>
          </button>
        </div>

        <div className="p-4 rounded-lg bg-zinc-900/60 border border-zinc-800 max-h-64 overflow-y-auto">
          <pre className="font-mono text-xs text-zinc-300 whitespace-pre-wrap leading-relaxed">
            {isRedactedView ? redactedText : rawText}
          </pre>
        </div>
      </div>
    </div>
  );
}
