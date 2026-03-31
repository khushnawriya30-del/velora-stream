import { useState } from 'react';
import { useMutation } from '@tanstack/react-query';
import { Search, Download, Check, CheckSquare, Square, Loader2, AlertCircle } from 'lucide-react';
import api from '../lib/api';
import toast from 'react-hot-toast';
import clsx from 'clsx';

interface TmdbPreviewItem {
  tmdbId: number;
  title: string;
  overview: string;
  posterUrl: string | null;
  backdropUrl: string | null;
  releaseDate: string;
  rating: number;
  genreNames: string[];
  originalLanguage: string;
  alreadyImported: boolean;
}

interface ImportResult {
  imported: number;
  skipped: number;
  items: Array<{ tmdbId: number; title?: string; status: string; reason?: string }>;
}

const CONTENT_TYPES = [
  { value: 'movies', label: 'Movies' },
  { value: 'shows', label: 'TV Shows' },
  { value: 'anime', label: 'Anime' },
];

const REGIONS = [
  { value: 'bollywood', label: 'Bollywood (Hindi)' },
  { value: 'hollywood', label: 'Hollywood (English)' },
  { value: 'korean', label: 'Korean' },
  { value: 'japanese', label: 'Japanese' },
  { value: 'chinese', label: 'Chinese' },
  { value: 'tamil', label: 'Tamil' },
  { value: 'telugu', label: 'Telugu' },
  { value: 'malayalam', label: 'Malayalam' },
  { value: 'kannada', label: 'Kannada' },
  { value: 'thai', label: 'Thai' },
  { value: 'spanish', label: 'Spanish' },
  { value: 'french', label: 'French' },
  { value: 'turkish', label: 'Turkish' },
];

const COUNTS = [10, 20, 50];

export default function TmdbImportPage() {
  const [contentType, setContentType] = useState<string>('movies');
  const [region, setRegion] = useState<string>('bollywood');
  const [count, setCount] = useState<number>(20);
  const [results, setResults] = useState<TmdbPreviewItem[]>([]);
  const [selected, setSelected] = useState<Set<number>>(new Set());
  const [importResult, setImportResult] = useState<ImportResult | null>(null);

  const discoverMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.post('/tmdb/discover', { contentType, region, count });
      return data as TmdbPreviewItem[];
    },
    onSuccess: (data) => {
      setResults(data);
      setSelected(new Set());
      setImportResult(null);
      toast.success(`Found ${data.length} results`);
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Failed to fetch from TMDB');
    },
  });

  const importMutation = useMutation({
    mutationFn: async () => {
      const tmdbIds = Array.from(selected);
      const { data } = await api.post('/tmdb/import', { tmdbIds, contentType });
      return data as ImportResult;
    },
    onSuccess: (data) => {
      setImportResult(data);
      toast.success(`Imported ${data.imported} items, skipped ${data.skipped}`);
      // Mark newly imported items in the results
      setResults((prev) =>
        prev.map((item) => {
          const result = data.items.find((r) => r.tmdbId === item.tmdbId);
          if (result?.status === 'imported') return { ...item, alreadyImported: true };
          return item;
        }),
      );
      setSelected(new Set());
    },
    onError: (err: any) => {
      toast.error(err.response?.data?.message || 'Import failed');
    },
  });

  const selectableItems = results.filter((r) => !r.alreadyImported);
  const allSelected = selectableItems.length > 0 && selectableItems.every((r) => selected.has(r.tmdbId));

  const toggleSelect = (tmdbId: number) => {
    setSelected((prev) => {
      const next = new Set(prev);
      if (next.has(tmdbId)) next.delete(tmdbId);
      else next.add(tmdbId);
      return next;
    });
  };

  const toggleAll = () => {
    if (allSelected) {
      setSelected(new Set());
    } else {
      setSelected(new Set(selectableItems.map((r) => r.tmdbId)));
    }
  };

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">TMDB Import</h1>

      {/* Controls */}
      <div className="bg-surface border border-border rounded-xl p-6 space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div>
            <label className="block text-sm text-text-secondary mb-1.5">Content Type</label>
            <select
              value={contentType}
              onChange={(e) => setContentType(e.target.value)}
              className="w-full bg-surface-light border border-border rounded-lg px-3 py-2.5 text-sm text-text-primary focus:outline-none focus:border-gold"
            >
              {CONTENT_TYPES.map((ct) => (
                <option key={ct.value} value={ct.value}>{ct.label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-text-secondary mb-1.5">Region</label>
            <select
              value={region}
              onChange={(e) => setRegion(e.target.value)}
              className="w-full bg-surface-light border border-border rounded-lg px-3 py-2.5 text-sm text-text-primary focus:outline-none focus:border-gold"
            >
              {REGIONS.map((r) => (
                <option key={r.value} value={r.value}>{r.label}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm text-text-secondary mb-1.5">Count</label>
            <select
              value={count}
              onChange={(e) => setCount(Number(e.target.value))}
              className="w-full bg-surface-light border border-border rounded-lg px-3 py-2.5 text-sm text-text-primary focus:outline-none focus:border-gold"
            >
              {COUNTS.map((c) => (
                <option key={c} value={c}>{c} items</option>
              ))}
            </select>
          </div>
        </div>

        <button
          onClick={() => discoverMutation.mutate()}
          disabled={discoverMutation.isPending}
          className="flex items-center gap-2 bg-gold hover:bg-gold-light text-background px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
        >
          {discoverMutation.isPending ? <Loader2 size={18} className="animate-spin" /> : <Search size={18} />}
          {discoverMutation.isPending ? 'Fetching...' : 'Discover'}
        </button>
      </div>

      {/* Results */}
      {results.length > 0 && (
        <div className="space-y-4">
          {/* Action bar */}
          <div className="flex items-center justify-between flex-wrap gap-3">
            <div className="flex items-center gap-3">
              <button
                onClick={toggleAll}
                className="flex items-center gap-2 text-sm text-text-secondary hover:text-text-primary transition-colors"
              >
                {allSelected ? <CheckSquare size={18} className="text-gold" /> : <Square size={18} />}
                {allSelected ? 'Deselect All' : 'Select All'}
              </button>
              <span className="text-sm text-text-secondary">
                {selected.size} of {selectableItems.length} selected
              </span>
            </div>
            <button
              onClick={() => importMutation.mutate()}
              disabled={selected.size === 0 || importMutation.isPending}
              className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-5 py-2.5 rounded-xl text-sm font-semibold transition-colors disabled:opacity-50"
            >
              {importMutation.isPending ? <Loader2 size={18} className="animate-spin" /> : <Download size={18} />}
              {importMutation.isPending ? 'Importing...' : `Import ${selected.size} Items`}
            </button>
          </div>

          {/* Import results banner */}
          {importResult && (
            <div className="bg-surface border border-green-600/30 rounded-xl p-4 flex items-start gap-3">
              <Check size={20} className="text-green-500 mt-0.5 shrink-0" />
              <div className="text-sm">
                <p className="font-medium text-green-400">
                  Import complete: {importResult.imported} imported, {importResult.skipped} skipped
                </p>
                {importResult.items.filter((i) => i.status === 'error').length > 0 && (
                  <div className="mt-2 space-y-1">
                    {importResult.items
                      .filter((i) => i.status === 'error')
                      .map((i) => (
                        <p key={i.tmdbId} className="text-red-400 flex items-center gap-1">
                          <AlertCircle size={14} />
                          TMDB #{i.tmdbId}: {i.reason}
                        </p>
                      ))}
                  </div>
                )}
              </div>
            </div>
          )}

          {/* Grid */}
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5 gap-4">
            {results.map((item) => {
              const isSelected = selected.has(item.tmdbId);
              const disabled = item.alreadyImported;
              return (
                <div
                  key={item.tmdbId}
                  onClick={() => !disabled && toggleSelect(item.tmdbId)}
                  className={clsx(
                    'relative bg-surface border rounded-xl overflow-hidden cursor-pointer transition-all group',
                    disabled
                      ? 'opacity-50 cursor-not-allowed border-border'
                      : isSelected
                        ? 'border-gold ring-1 ring-gold'
                        : 'border-border hover:border-gold/50',
                  )}
                >
                  {/* Checkbox */}
                  {!disabled && (
                    <div className="absolute top-2 left-2 z-10">
                      <div
                        className={clsx(
                          'w-6 h-6 rounded flex items-center justify-center transition-colors',
                          isSelected ? 'bg-gold text-background' : 'bg-black/60 text-white border border-white/30',
                        )}
                      >
                        {isSelected && <Check size={14} />}
                      </div>
                    </div>
                  )}

                  {/* Already imported badge */}
                  {disabled && (
                    <div className="absolute top-2 right-2 z-10 bg-green-600 text-white text-[10px] font-bold px-2 py-0.5 rounded-full">
                      IMPORTED
                    </div>
                  )}

                  {/* Poster */}
                  <div className="aspect-[2/3] bg-surface-light">
                    {item.posterUrl ? (
                      <img
                        src={item.posterUrl}
                        alt={item.title}
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-text-secondary text-xs">
                        No Poster
                      </div>
                    )}
                  </div>

                  {/* Info */}
                  <div className="p-3 space-y-1">
                    <h3 className="text-sm font-medium text-text-primary line-clamp-2 leading-tight">
                      {item.title}
                    </h3>
                    <div className="flex items-center gap-2 text-xs text-text-secondary">
                      {item.releaseDate && <span>{item.releaseDate.split('-')[0]}</span>}
                      {item.rating > 0 && (
                        <span className="flex items-center gap-0.5">
                          <span className="text-yellow-400">★</span>
                          {item.rating.toFixed(1)}
                        </span>
                      )}
                    </div>
                    {item.genreNames.length > 0 && (
                      <div className="flex flex-wrap gap-1 pt-1">
                        {item.genreNames.slice(0, 2).map((g) => (
                          <span
                            key={g}
                            className="text-[10px] bg-surface-light text-text-secondary px-1.5 py-0.5 rounded"
                          >
                            {g}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Empty state */}
      {results.length === 0 && !discoverMutation.isPending && (
        <div className="bg-surface border border-border rounded-xl p-12 text-center">
          <Search size={48} className="mx-auto text-text-secondary/30 mb-4" />
          <p className="text-text-secondary">
            Select content type, region and count, then click Discover to browse TMDB
          </p>
        </div>
      )}

      {/* Loading state */}
      {discoverMutation.isPending && (
        <div className="bg-surface border border-border rounded-xl p-12 text-center">
          <Loader2 size={48} className="mx-auto text-gold animate-spin mb-4" />
          <p className="text-text-secondary">Fetching from TMDB...</p>
        </div>
      )}
    </div>
  );
}
