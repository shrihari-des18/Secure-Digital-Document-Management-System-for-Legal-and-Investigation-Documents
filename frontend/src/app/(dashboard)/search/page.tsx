'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { Search, Briefcase, FileSearch, FileText, ArrowRight, ArrowLeft, Loader2 } from 'lucide-react';
import { clsx } from 'clsx';
import { searchApi } from '@/lib/api/search';
import type { SearchResponse, SearchResultItem } from '@/types';
import { SearchEntityType } from '@/types';

const FILTER_CHIPS: { label: string; value: SearchEntityType }[] = [
  { label: 'All', value: SearchEntityType.ALL },
  { label: 'Cases', value: SearchEntityType.CASE },
  { label: 'Evidence', value: SearchEntityType.EVIDENCE },
  { label: 'Documents', value: SearchEntityType.DOCUMENT },
];

const ENTITY_ICONS: Record<string, React.ComponentType<{ className?: string }>> = {
  CASE: Briefcase,
  EVIDENCE: FileSearch,
  DOCUMENT: FileText,
};

const ENTITY_COLORS: Record<string, string> = {
  CASE: 'text-blue-400',
  EVIDENCE: 'text-emerald-400',
  DOCUMENT: 'text-amber-400',
};

const ENTITY_BADGE_CLASSES: Record<string, string> = {
  CASE: 'bg-blue-500/10 text-blue-400 border-blue-500/20',
  EVIDENCE: 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
  DOCUMENT: 'bg-amber-500/10 text-amber-400 border-amber-500/20',
};

function getEntityHref(item: SearchResultItem): string {
  switch (item.type) {
    case 'CASE':
      return `/cases/${item.id}`;
    case 'EVIDENCE':
      return `/evidence/${item.id}`;
    case 'DOCUMENT':
      return `/documents/${item.id}`;
    default:
      return '#';
  }
}

const PAGE_SIZE = 10;

export default function SearchPage() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [activeType, setActiveType] = useState<SearchEntityType>(SearchEntityType.ALL);
  const [page, setPage] = useState(0);
  const [results, setResults] = useState<SearchResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);

  const executeSearch = async (searchQuery: string, type: SearchEntityType, searchPage: number) => {
    if (!searchQuery.trim()) return;
    setLoading(true);
    setSearched(true);
    try {
      const data = await searchApi.search({
        query: searchQuery.trim(),
        type: type === SearchEntityType.ALL ? 'ALL' : type,
        page: searchPage,
        size: PAGE_SIZE,
      });
      setResults({
        ...data,
        query: searchQuery.trim(),
        totalPages: Math.ceil((data.totalHits || 0) / PAGE_SIZE) || 1,
      });
    } catch {
      setResults(null);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    executeSearch(query, activeType, 0);
  };

  const handleFilterChange = (type: SearchEntityType) => {
    setActiveType(type);
    setPage(0);
    if (searched && query.trim()) {
      executeSearch(query, type, 0);
    }
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage);
    executeSearch(query, activeType, newPage);
  };

  return (
    <div className="space-y-8 max-w-4xl">
      {/* Page Header */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-zinc-100 flex items-center gap-3">
          <div className="icon-box-primary">
            <Search className="w-5 h-5" />
          </div>
          Search
        </h1>
        <p className="text-xs text-zinc-400 mt-2 leading-relaxed max-w-lg">
          Full-text and semantic search across case dockets, sealed evidence metadata, AI OCR text, and legal documents.
        </p>
      </div>

      {/* Search Input */}
      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="relative">
          <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-4 h-4 text-zinc-500" />
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search cases, evidence, documents..."
            className="input-field !pl-12 pr-4"
            autoFocus
          />
        </div>

        {/* Filter Chips */}
        <div className="flex items-center gap-2">
          {FILTER_CHIPS.map((chip) => (
            <button
              key={chip.value}
              type="button"
              onClick={() => handleFilterChange(chip.value)}
              className={clsx(
                'px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors cursor-pointer',
                activeType === chip.value
                  ? 'bg-zinc-100 text-zinc-950 border-zinc-100'
                  : 'bg-zinc-900 text-zinc-400 border-zinc-800 hover:bg-zinc-800 hover:text-zinc-200 hover:border-zinc-700'
              )}
            >
              {chip.label}
            </button>
          ))}

          <div className="flex-1" />

          <button
            type="submit"
            disabled={!query.trim() || loading}
            className="btn-primary text-xs disabled:opacity-40"
          >
            {loading ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Search className="w-3.5 h-3.5" />}
            <span>Search</span>
          </button>
        </div>
      </form>

      {/* Results */}
      {loading && (
        <div className="py-16 text-center">
          <Loader2 className="w-6 h-6 text-zinc-400 animate-spin mx-auto" />
          <p className="text-xs text-zinc-500 mt-3">Searching vault...</p>
        </div>
      )}

      {!loading && searched && results && results.hits.length === 0 && (
        <div className="py-16 text-center border border-zinc-800 bg-zinc-950 rounded-xl">
          <Search className="w-8 h-8 text-zinc-700 mx-auto mb-3" />
          <p className="text-sm text-zinc-400">No results found for &ldquo;{results.query}&rdquo;</p>
          <p className="text-xs text-zinc-500 mt-1">Try a different search term or filter.</p>
        </div>
      )}

      {!loading && results && results.hits.length > 0 && (
        <div className="space-y-3">
          {/* Results count */}
          <div className="flex items-center justify-between">
            <p className="text-xs text-zinc-400 font-mono">
              {results.totalHits} result{results.totalHits !== 1 ? 's' : ''} for &ldquo;{results.query}&rdquo;
            </p>
            {results.totalPages > 1 && (
              <p className="text-xs text-zinc-500 font-mono">
                Page {results.page + 1} of {results.totalPages}
              </p>
            )}
          </div>

          {/* Results List */}
          <div className="space-y-2 stagger-children">
            {results.hits.map((item) => {
              const Icon = ENTITY_ICONS[item.type] || FileSearch;
              const color = ENTITY_COLORS[item.type] || 'text-zinc-400';
              const badgeClass = ENTITY_BADGE_CLASSES[item.type] || 'bg-zinc-800 text-zinc-300 border-zinc-700';

              return (
                <button
                  key={`${item.type}-${item.id}`}
                  onClick={() => router.push(getEntityHref(item))}
                  className="w-full text-left block p-4 rounded-xl border border-zinc-800 bg-zinc-950 hover:bg-zinc-800/20 hover:border-zinc-700 transition-colors group cursor-pointer"
                >
                  <div className="flex items-start gap-3">
                    <div className="icon-box-secondary mt-0.5">
                      <Icon className={clsx('w-4 h-4', color)} />
                    </div>

                    <div className="flex-1 min-w-0 space-y-1">
                      <div className="flex items-center gap-2">
                        <h3 className="text-sm font-semibold text-zinc-200 group-hover:text-zinc-100 transition-colors truncate">
                          {item.title}
                        </h3>
                        <span className={clsx('text-[10px] font-mono px-1.5 py-0.5 rounded border shrink-0', badgeClass)}>
                          {item.type}
                        </span>
                      </div>

                      {item.snippet && (
                        <p className="text-xs text-zinc-400 leading-relaxed line-clamp-2">
                          {item.snippet}
                        </p>
                      )}

                      {item.score !== null && item.score !== undefined && (
                        <span className="text-[10px] font-mono text-zinc-600">
                          Relevance: {(item.score * 100).toFixed(1)}%
                        </span>
                      )}
                    </div>

                    <ArrowRight className="w-4 h-4 text-zinc-600 group-hover:text-zinc-300 transition-colors shrink-0 mt-1" />
                  </div>
                </button>
              );
            })}
          </div>

          {/* Pagination */}
          {results.totalPages > 1 && (
            <div className="flex items-center justify-center gap-3 pt-4">
              <button
                onClick={() => handlePageChange(page - 1)}
                disabled={page === 0}
                className="btn-secondary text-xs disabled:opacity-30"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Previous</span>
              </button>

              <span className="text-xs font-mono text-zinc-400">
                {page + 1} / {results.totalPages}
              </span>

              <button
                onClick={() => handlePageChange(page + 1)}
                disabled={page >= results.totalPages - 1}
                className="btn-secondary text-xs disabled:opacity-30"
              >
                <span>Next</span>
                <ArrowRight className="w-3.5 h-3.5" />
              </button>
            </div>
          )}
        </div>
      )}

      {/* Empty state before first search */}
      {!searched && !loading && (
        <div className="py-16 text-center border border-zinc-800 bg-zinc-950 rounded-xl">
          <Search className="w-10 h-10 text-zinc-800 mx-auto mb-4" />
          <p className="text-sm text-zinc-400">Enter a query to search the vault</p>
          <p className="text-xs text-zinc-500 mt-1">
            Searches across case dockets, evidence metadata, OCR text, PII, and legal documents.
          </p>
        </div>
      )}
    </div>
  );
}

