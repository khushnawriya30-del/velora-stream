import { useParams, useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import { ArrowLeft, Play, Star, Calendar, Clock, Volume2, Users } from 'lucide-react';
import api from '../lib/api';
import type { Movie } from '../types';
import { useState } from 'react';

export default function MovieDetailsPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [showTrailer, setShowTrailer] = useState(false);

  const { data: movie, isLoading, isError } = useQuery({
    queryKey: ['movie', id],
    queryFn: async () => {
      const { data } = await api.get(`/movies/${id}`);
      return data;
    },
    enabled: !!id,
  });

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background text-text-primary p-6">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-text-secondary hover:text-text-primary mb-6 transition-colors"
        >
          <ArrowLeft size={20} />
          Back
        </button>
        <div className="space-y-4 max-w-5xl mx-auto">
          <div className="aspect-video rounded-lg bg-surface-light animate-pulse" />
          <div className="h-8 bg-surface-light rounded animate-pulse w-1/3" />
          <div className="h-4 bg-surface-light rounded animate-pulse w-1/2" />
        </div>
      </div>
    );
  }

  if (isError || !movie) {
    return (
      <div className="min-h-screen bg-background text-text-primary p-6 flex flex-col items-center justify-center">
        <button
          onClick={() => navigate(-1)}
          className="absolute top-6 left-6 flex items-center gap-2 text-text-secondary hover:text-text-primary transition-colors"
        >
          <ArrowLeft size={20} />
          Back
        </button>
        <h1 className="text-2xl font-semibold mb-2">Movie not found</h1>
        <p className="text-text-secondary">The movie you're looking for doesn't exist.</p>
      </div>
    );
  }

  const m: Movie = movie;
  const videoUrl = m.trailerUrl || m.streamingSources?.[0]?.url || '';
  const durationText = m.duration ? `${Math.floor(m.duration / 60)}h ${m.duration % 60}m` : 'N/A';

  return (
    <div className="min-h-screen bg-background text-text-primary">
      {/* Back button */}
      <button
        onClick={() => navigate(-1)}
        className="fixed top-6 left-6 z-50 flex items-center gap-2 text-text-secondary hover:text-text-primary p-2 rounded-lg hover:bg-surface/50 transition-colors"
      >
        <ArrowLeft size={20} />
        <span className="hidden sm:inline">Back</span>
      </button>

      {/* Backdrop/Trailer Section */}
      <div className="relative w-full bg-surface overflow-hidden">
        {!showTrailer && videoUrl ? (
          <div
            className="relative aspect-video bg-black cursor-pointer group"
            onClick={() => setShowTrailer(true)}
          >
            {m.backdropUrl && (
              <img
                src={m.backdropUrl}
                alt={m.title}
                className="w-full h-full object-cover absolute inset-0"
              />
            )}
            {!m.backdropUrl && m.posterUrl && (
              <img
                src={m.posterUrl}
                alt={m.title}
                className="w-full h-full object-cover absolute inset-0"
              />
            )}
            <div className="absolute inset-0 bg-black/30 group-hover:bg-black/20 transition-colors flex items-center justify-center">
              <div className="w-20 h-20 rounded-full bg-gold flex items-center justify-center group-hover:scale-110 transition-transform">
                <Play size={32} className="text-background fill-background ml-1" />
              </div>
            </div>
          </div>
        ) : videoUrl ? (
          <div className="aspect-video bg-black">
            <iframe
              width="100%"
              height="100%"
              src={videoUrl.includes('youtube') ? videoUrl : undefined}
              title={m.title}
              allowFullScreen
              className="w-full h-full"
            />
            {!videoUrl.includes('youtube') && (
              <video
                width="100%"
                height="100%"
                controls
                autoPlay
                src={videoUrl}
                className="w-full h-full object-cover"
              />
            )}
          </div>
        ) : (
          <div className="aspect-video bg-surface-light flex items-center justify-center">
            {m.posterUrl && <img src={m.posterUrl} alt={m.title} className="w-full h-full object-cover" />}
          </div>
        )}
      </div>

      {/* Content */}
      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        {/* Title & Genre Tags */}
        <div className="space-y-3">
          <h1 className="text-4xl font-bold">{m.title}</h1>
          <div className="flex flex-wrap gap-2">
            {m.genres.map((genre) => (
              <span
                key={genre}
                className="px-3 py-1 py-1 rounded-full bg-gold/10 text-gold text-sm font-medium"
              >
                {genre}
              </span>
            ))}
          </div>
        </div>

        {/* Rating */}
        <div className="flex items-center gap-4 pb-4 border-b border-border">
          <div className="flex items-center gap-1">
            <Star size={20} className="text-gold fill-gold" />
            <span className="text-2xl font-bold">{(m.averageRating || m.rating || 0).toFixed(1)}</span>
            <span className="text-text-secondary text-sm">/5</span>
          </div>
          <div className="text-sm text-text-secondary">
            ({m.totalReviews?.toLocaleString() || 0} reviews)
          </div>
        </div>

        {/* Details Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 p-4 rounded-xl bg-surface">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-text-secondary text-sm">
              <Calendar size={16} />
              <span>Release Year</span>
            </div>
            <p className="text-lg font-semibold">{m.releaseYear}</p>
          </div>

          <div className="space-y-1">
            <div className="flex items-center gap-2 text-text-secondary text-sm">
              <Clock size={16} />
              <span>Duration</span>
            </div>
            <p className="text-lg font-semibold">{durationText}</p>
          </div>

          <div className="space-y-1">
            <div className="flex items-center gap-2 text-text-secondary text-sm">
              <Volume2 size={16} />
              <span>Language</span>
            </div>
            <p className="text-lg font-semibold capitalize">{m.language}</p>
          </div>

          <div className="space-y-1">
            <div className="flex items-center gap-2 text-text-secondary text-sm">
              <Users size={16} />
              <span>Content Type</span>
            </div>
            <p className="text-lg font-semibold capitalize">{m.contentType.replace(/_/g, ' ')}</p>
          </div>
        </div>

        {/* Description */}
        {m.description && (
          <div className="space-y-2">
            <h2 className="text-xl font-semibold">Plot</h2>
            <p className="text-text-secondary leading-relaxed">{m.description}</p>
          </div>
        )}

        {/* Cast */}
        {m.cast && m.cast.length > 0 && (
          <div className="space-y-4">
            <h2 className="text-xl font-semibold">Cast & Crew</h2>
            <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 gap-4">
              {m.cast.slice(0, 8).map((member, idx) => (
                <div key={idx} className="text-center">
                  {member.photoUrl && (
                    <img
                      src={member.photoUrl}
                      alt={member.name}
                      className="w-full aspect-square object-cover rounded-lg mb-2"
                    />
                  )}
                  <p className="font-medium text-sm">{member.name}</p>
                  <p className="text-xs text-text-secondary">{member.role}</p>
                  {member.character && <p className="text-xs text-gold">{member.character}</p>}
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Watch Now Button */}
        <div className="pt-4">
          <button className="w-full sm:w-auto px-8 py-3 bg-gold hover:bg-gold-light text-background font-semibold rounded-xl transition-colors flex items-center justify-center gap-2 text-lg">
            <Play size={20} className="fill-current" />
            Watch Now
          </button>
        </div>

        {/* Additional Info */}
        <div className="pt-4 border-t border-border space-y-2 text-sm text-text-secondary">
          <p>Views: {m.viewCount?.toLocaleString() || 0}</p>
          {m.contentRating && <p>Content Rating: {m.contentRating}</p>}
          {m.tags && m.tags.length > 0 && (
            <p>
              Tags: <span className="text-text-primary">{m.tags.join(', ')}</span>
            </p>
          )}
        </div>
      </div>
    </div>
  );
}
