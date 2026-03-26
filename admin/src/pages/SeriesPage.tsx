import { useQuery } from '@tanstack/react-query';
import { Film } from 'lucide-react';
import api from '../lib/api';
import type { Movie } from '../types';

export default function SeriesPage() {
  const { data, isLoading } = useQuery({
    queryKey: ['series'],
    queryFn: async () => {
      const { data } = await api.get('/movies?contentType=series');
      return data;
    },
  });

  const series: Movie[] = data?.movies ?? data?.data ?? data ?? [];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Series</h1>

      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="bg-surface border border-border rounded-xl p-4 animate-pulse h-48" />
          ))}
        </div>
      ) : series.length === 0 ? (
        <div className="flex flex-col items-center justify-center py-20 text-text-secondary">
          <Film size={48} className="mb-4 opacity-50" />
          <p>No series found</p>
          <p className="text-sm text-text-muted mt-1">Create series content from the Movies page</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {series.map((s) => (
            <div key={s._id} className="bg-surface border border-border rounded-xl overflow-hidden hover:border-border/80 transition-colors">
              <div className="aspect-video bg-surface-light relative">
                <img
                  src={s.backdropUrl || s.posterUrl}
                  alt={s.title}
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-gradient-to-t from-surface to-transparent" />
                <div className="absolute bottom-3 left-3">
                  <h3 className="font-semibold text-lg">{s.title}</h3>
                  <p className="text-sm text-text-secondary">{s.releaseYear} &middot; {s.genres.join(', ')}</p>
                </div>
              </div>
              <div className="p-4 flex items-center justify-between text-sm">
                <span className="text-text-secondary">Rating: <span className="text-gold">{(s.averageRating ?? s.rating ?? 0).toFixed(1)}</span></span>
                <span className="text-text-secondary">{(s.viewCount ?? 0).toLocaleString()} views</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
