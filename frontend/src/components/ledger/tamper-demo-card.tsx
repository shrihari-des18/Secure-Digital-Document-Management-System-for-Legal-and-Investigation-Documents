'use client';

import { useState } from 'react';
import {
  ShieldAlert,
  Zap,
  CheckCircle2,
  AlertTriangle,
} from 'lucide-react';
import { clsx } from 'clsx';

export function TamperDemoCard() {
  const [activeStep, setActiveStep] = useState<number>(1);

  return (
    <div className="border border-zinc-800 bg-zinc-950 rounded-xl p-6 space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-zinc-800">
        <div className="flex items-center gap-3">
          <div className="w-9 h-9 rounded-lg bg-zinc-900 border border-zinc-800 flex items-center justify-center text-zinc-300">
            <Zap className="w-4 h-4" />
          </div>
          <div>
            <h3 className="text-sm font-bold tracking-tight text-zinc-100">
              Cryptographic Tamper-Evidence Architecture (Chain Security)
            </h3>
            <p className="text-xs text-zinc-400 mt-0.5">
              How E-Vault mathematically guarantees court admissibility and proves zero tampering
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 text-xs font-mono">
          <button
            onClick={() => setActiveStep(1)}
            className={clsx(
              'px-3 py-1.5 rounded-md border transition-colors cursor-pointer',
              activeStep === 1
                ? 'bg-zinc-100 text-zinc-950 border-zinc-100 font-bold'
                : 'bg-zinc-900 text-zinc-400 border-zinc-800 hover:text-zinc-200'
            )}
          >
            1. Normal Hash Chain
          </button>
          <button
            onClick={() => setActiveStep(2)}
            className={clsx(
              'px-3 py-1.5 rounded-md border transition-colors cursor-pointer',
              activeStep === 2
                ? 'bg-red-500 text-white border-red-500 font-bold'
                : 'bg-zinc-900 text-zinc-400 border-zinc-800 hover:text-zinc-200'
            )}
          >
            2. Tamper Detection
          </button>
        </div>
      </div>

      {activeStep === 1 ? (
        <div className="space-y-4 animate-fade-in">
          <p className="text-xs text-zinc-300 leading-relaxed">
            Every transaction in E-Vault (evidence ingest, custody transfer, certificate generation) creates an
            append-only block. Each block&apos;s <span className="font-mono text-emerald-400 font-bold">entryHash</span> is
            computed over its entire payload plus the previous block&apos;s hash:
          </p>

          {/* Diagram of Block 1 -> Block 2 -> Block 3 */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div className="p-4 rounded-xl bg-zinc-900/60 border border-emerald-500/20 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-mono font-bold text-emerald-400">Block #1 (Genesis)</span>
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              </div>
              <p className="text-[11px] text-zinc-400 font-mono">Action: USER_REGISTERED</p>
              <div className="p-2 rounded bg-zinc-950 font-mono text-[10px] text-emerald-400 break-all border border-zinc-800">
                Hash: 0000a4f...
              </div>
            </div>

            <div className="p-4 rounded-xl bg-zinc-900/60 border border-emerald-500/20 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-mono font-bold text-emerald-400">Block #2 (Ingest)</span>
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              </div>
              <p className="text-[11px] text-zinc-400 font-mono">Prev: 0000a4f... (Block 1)</p>
              <div className="p-2 rounded bg-zinc-950 font-mono text-[10px] text-emerald-400 break-all border border-zinc-800">
                Hash: 7b81c9e...
              </div>
            </div>

            <div className="p-4 rounded-xl bg-zinc-900/60 border border-emerald-500/20 space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="font-mono font-bold text-emerald-400">Block #3 (Handoff)</span>
                <CheckCircle2 className="w-4 h-4 text-emerald-400" />
              </div>
              <p className="text-[11px] text-zinc-400 font-mono">Prev: 7b81c9e... (Block 2)</p>
              <div className="p-2 rounded bg-zinc-950 font-mono text-[10px] text-emerald-400 break-all border border-zinc-800">
                Hash: ac9c925...
              </div>
            </div>
          </div>
        </div>
      ) : (
        <div className="space-y-4 animate-fade-in">
          <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/20 text-red-200 text-xs space-y-2">
            <div className="flex items-center gap-2 font-bold text-red-400">
              <ShieldAlert className="w-5 h-5" />
              <span>Simulated Attack: Rogue Actor Edits Database Directly</span>
            </div>
            <p className="leading-relaxed">
              If an attacker modifies even 1 single byte of Block #2 in PostgreSQL, its recomputed hash changes from{' '}
              <span className="font-mono text-emerald-400">7b81c9e...</span> to{' '}
              <span className="font-mono text-red-400">99f2a01...</span>.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-xs">
            <div className="p-4 rounded-xl bg-zinc-900/60 border border-emerald-500/20 space-y-2">
              <span className="font-mono font-bold text-emerald-400">Block #1</span>
              <p className="text-[11px] text-zinc-400 font-mono">Intact</p>
            </div>

            <div className="p-4 rounded-xl bg-red-500/10 border border-red-500/30 space-y-2">
              <div className="flex items-center justify-between">
                <span className="font-mono font-bold text-red-400">Block #2 (Tampered)</span>
                <AlertTriangle className="w-4 h-4 text-red-400" />
              </div>
              <p className="text-[11px] text-red-300 font-semibold font-mono">Row Data Modified</p>
              <div className="p-2 rounded bg-zinc-950 font-mono text-[10px] text-red-400 break-all border border-red-500/20">
                Hash mismatch!
              </div>
            </div>

            <div className="p-4 rounded-xl bg-amber-500/10 border border-amber-500/20 space-y-2">
              <span className="font-mono font-bold text-amber-400">Block #3 (Invalidated)</span>
              <p className="text-[11px] text-amber-300 font-mono">Previous hash pointer broken!</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
