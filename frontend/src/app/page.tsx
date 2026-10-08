'use client';

import Link from 'next/link';
import { Shield, Lock, Brain, FileCheck, ArrowRight, Activity, Cpu } from 'lucide-react';
import { motion } from 'framer-motion';

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-black text-white overflow-hidden font-sans selection:bg-emerald-500/30">
      {/* Dynamic Background */}
      <div className="fixed inset-0 z-0">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_80%_at_50%_-20%,rgba(16,185,129,0.15),rgba(0,0,0,1))]" />
        <div className="absolute inset-0 bg-[url('/grid.svg')] bg-center [mask-image:linear-gradient(180deg,white,rgba(255,255,255,0))]" />
      </div>

      {/* Navigation */}
      <nav className="relative z-10 border-b border-white/5 bg-black/50 backdrop-blur-xl">
        <div className="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-lg bg-emerald-500/20 border border-emerald-500/50 flex items-center justify-center">
              <Shield className="w-5 h-5 text-emerald-400" />
            </div>
            <span className="font-bold tracking-widest text-sm text-zinc-100">E-VAULT <span className="text-emerald-500">CORE</span></span>
          </div>
          <Link href="/login" className="text-xs font-semibold uppercase tracking-wider text-zinc-300 hover:text-white transition-colors">
            Agency Login &rarr;
          </Link>
        </div>
      </nav>

      <main className="relative z-10 max-w-7xl mx-auto px-6 pt-24 pb-32">
        {/* Hero Section */}
        <div className="text-center max-w-4xl mx-auto space-y-8">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5 }}
            className="inline-flex items-center gap-2 px-3 py-1 rounded-full border border-emerald-500/30 bg-emerald-500/10 text-emerald-400 text-xs font-medium tracking-wide uppercase"
          >
            <Activity className="w-3.5 h-3.5" />
            <span>Smart India Hackathon 2026 (SIH26190)</span>
          </motion.div>
          
          <motion.h1 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.1 }}
            className="text-5xl md:text-7xl font-extrabold tracking-tight text-transparent bg-clip-text bg-gradient-to-br from-white via-zinc-200 to-zinc-500"
          >
            Digital Evidence.<br />Mathematically Proven.
          </motion.h1>

          <motion.p 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.2 }}
            className="text-lg text-zinc-400 max-w-2xl mx-auto leading-relaxed"
          >
            An enterprise-grade, zero-trust platform engineered to secure the acquisition, storage, analysis, and legal certification of digital evidence for Indian law enforcement under the Bharatiya Sakshya Adhiniyam, 2023.
          </motion.p>

          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay: 0.3 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4 pt-4"
          >
            <Link 
              href="/login" 
              className="group relative inline-flex items-center gap-2 px-8 py-4 bg-white text-black font-bold rounded-xl overflow-hidden transition-transform hover:scale-105 active:scale-95"
            >
              <div className="absolute inset-0 bg-gradient-to-r from-emerald-400 to-teal-400 opacity-0 group-hover:opacity-10 transition-opacity" />
              Access Secure Portal
              <ArrowRight className="w-4 h-4 group-hover:translate-x-1 transition-transform" />
            </Link>
            <a 
              href="https://github.com/Shrihari2127/Secure-Digital-Document-Management-System-for-Legal-and-Investigation-Documents" 
              target="_blank" 
              rel="noreferrer"
              className="px-8 py-4 bg-zinc-900 border border-zinc-800 hover:border-zinc-700 text-zinc-300 font-semibold rounded-xl transition-all hover:bg-zinc-800"
            >
              View System Architecture
            </a>
          </motion.div>
        </div>

        {/* Feature Grid */}
        <motion.div 
          initial={{ opacity: 0, y: 40 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.7, delay: 0.5 }}
          className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-32"
        >
          <div className="p-8 rounded-2xl bg-zinc-900/50 border border-white/5 backdrop-blur-sm hover:bg-zinc-900/80 transition-colors group">
            <div className="w-12 h-12 rounded-xl bg-blue-500/10 border border-blue-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Lock className="w-6 h-6 text-blue-400" />
            </div>
            <h3 className="text-xl font-bold text-zinc-100 mb-3">Immutable Audit Ledger</h3>
            <p className="text-sm text-zinc-400 leading-relaxed">
              Every system interaction is hashed and chained. Powered by SHA-256 cryptographic linkages ensuring absolute non-repudiation of evidence custody.
            </p>
          </div>

          <div className="p-8 rounded-2xl bg-zinc-900/50 border border-white/5 backdrop-blur-sm hover:bg-zinc-900/80 transition-colors group">
            <div className="w-12 h-12 rounded-xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <FileCheck className="w-6 h-6 text-emerald-400" />
            </div>
            <h3 className="text-xl font-bold text-zinc-100 mb-3">BSA 2023 Compliance</h3>
            <p className="text-sm text-zinc-400 leading-relaxed">
              Automated issuance of Statutory Section 63 Court Certificates. Integrates RFC 3161 Trusted Timestamping for direct court admissibility.
            </p>
          </div>

          <div className="p-8 rounded-2xl bg-zinc-900/50 border border-white/5 backdrop-blur-sm hover:bg-zinc-900/80 transition-colors group">
            <div className="w-12 h-12 rounded-xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center mb-6 group-hover:scale-110 transition-transform">
              <Cpu className="w-6 h-6 text-purple-400" />
            </div>
            <h3 className="text-xl font-bold text-zinc-100 mb-3">AI Forensics Engine</h3>
            <p className="text-sm text-zinc-400 leading-relaxed">
              Automated PII detection, OCR extraction, and perceptual hashing (pHash) for deduplication, running securely on containerized microservices.
            </p>
          </div>
        </motion.div>
      </main>
    </div>
  );
}
