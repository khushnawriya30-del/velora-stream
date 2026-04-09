import { useState, useEffect, useCallback } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, Plus, X, Cloud, Loader2, Check, Download, ChevronDown, FolderInput, Film, Layers } from 'lucide-react';
import clsx from 'clsx';
import api from '../lib/api';
import type { Movie, CastMember, StreamingSource } from '../types';
import toast from 'react-hot-toast';

function HlsTranscodeSection({ movieId }: { movieId: string }) {
  const [status, setStatus] = useState<string>('none');
  const [hlsUrl, setHlsUrl] = useState<string>('');
  const [encodeProgress, setEncodeProgress] = useState(0);
  const [availableResolutions, setAvailableResolutions] = useState('');
  const [loading, setLoading] = useState(false);
  const [fetchUrl, setFetchUrl] = useState('');

  const fetchStatus = useCallback(async () => {
    try {
      const { data } = await api.get(`/bunny/stream/movie/${movieId}/status`);
      setStatus(data.hlsStatus || 'none');
      if (data.encodeProgress) setEncodeProgress(data.encodeProgress);
      if (data.availableResolutions) setAvailableResolutions(data.availableResolutions);
      // Get movie HLS URL
      const movieRes = await api.get(`/movies/${movieId}`);
      if (movieRes.data?.hlsUrl) setHlsUrl(movieRes.data.hlsUrl);
    } catch {
      // Movie might not have a Bunny video yet — that's fine
    }
  }, [movieId]);

  useEffect(() => {
    fetchStatus();
  }, [fetchStatus]);

  // Poll while processing
  useEffect(() => {
    if (status !== 'processing') return;
    const interval = setInterval(fetchStatus, 5000);
    return () => clearInterval(interval);
  }, [status, fetchStatus]);

  // Upload from URL (fetch to Bunny Stream)
  const handleFetchToBunny = async () => {
    setLoading(true);
    try {
      const { data } = await api.post(`/bunny/stream/movie/${movieId}/fetch`, {
        url: fetchUrl || undefined,
      });
      setStatus('processing');
      setHlsUrl(data.hlsUrl);
      toast.success('Video sent to Bunny Stream for transcoding!');
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Failed to upload to Bunny Stream');
    } finally {
      setLoading(false);
    }
  };

  // Upload file directly
  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setLoading(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const { data } = await api.post(`/bunny/stream/movie/${movieId}/upload`, formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
        timeout: 0,
      });
      setStatus('processing');
      setHlsUrl(data.hlsUrl);
      toast.success('Video uploaded to Bunny Stream!');
    } catch (err: any) {
      toast.error(err?.response?.data?.message || 'Upload failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-3">
        <span className="text-sm font-medium flex items-center gap-1.5"><Cloud size={16} className="text-purple-400" /> Bunny Stream:</span>
        {status === 'none' && (
          <span className="px-2 py-1 rounded-md bg-surface-light text-text-secondary text-xs">Not Uploaded</span>
        )}
        {status === 'processing' && (
          <span className="px-2 py-1 rounded-md bg-yellow-500/20 text-yellow-400 text-xs animate-pulse">
            Transcoding... {encodeProgress > 0 && `(${encodeProgress}%)`}
          </span>
        )}
        {status === 'completed' && (
          <span className="px-2 py-1 rounded-md bg-green-500/20 text-green-400 text-xs">
            Completed {availableResolutions && `(${availableResolutions})`}
          </span>
        )}
        {status === 'failed' && (
          <span className="px-2 py-1 rounded-md bg-red-500/20 text-red-400 text-xs">Failed</span>
        )}
      </div>

      {status === 'processing' && encodeProgress > 0 && (
        <div className="w-full bg-surface-light rounded-full h-2">
          <div className="bg-purple-500 h-2 rounded-full transition-all" style={{ width: `${encodeProgress}%` }} />
        </div>
      )}

      {hlsUrl && (
        <div className="text-xs text-text-secondary bg-background rounded-lg p-3 break-all">
          <span className="font-medium text-text-primary">HLS URL: </span>{hlsUrl}
        </div>
      )}

      {(status === 'none' || status === 'failed') && (
        <div className="space-y-3">
          {/* Fetch from URL */}
          <div className="flex gap-2">
            <input
              value={fetchUrl}
              onChange={(e) => setFetchUrl(e.target.value)}
              placeholder="Video URL (or leave empty to use first streaming source)"
              className="flex-1 bg-surface-light border border-border rounded-lg px-3 py-2 text-sm"
            />
            <button
              type="button"
              onClick={handleFetchToBunny}
              disabled={loading}
              className="flex items-center gap-1.5 px-4 py-2 rounded-lg bg-purple-600 hover:bg-purple-700 text-white font-medium text-sm disabled:opacity-50 whitespace-nowrap"
            >
              {loading ? <Loader2 size={14} className="animate-spin" /> : <Cloud size={14} />}
              {status === 'failed' ? 'Retry' : 'Upload to Bunny'}
            </button>
          </div>

          {/* Direct file upload */}
          <div className="flex items-center gap-2">
            <label className="flex items-center gap-1.5 px-4 py-2 rounded-lg border border-purple-500/30 text-purple-400 hover:bg-purple-500/10 text-sm cursor-pointer transition-colors">
              <Cloud size={14} />
              Upload File Directly
              <input type="file" accept="video/*" onChange={handleFileUpload} className="hidden" disabled={loading} />
            </label>
            <span className="text-xs text-text-muted">Auto-transcodes to 1080p, 720p, 480p, 360p, 240p</span>
          </div>
        </div>
      )}
    </div>
  );
}

// ── Import from Bunny Collection (for individual movie edit page) ──

interface BunnyVideoItem {
  guid: string;
  title: string;
  status: number;
  length: number;
  storageSize: number;
  availableResolutions: string;
  thumbnailFileName: string;
}

function BunnyCollectionImportSection({ movieId, movieTitle, onImported }: { movieId: string; movieTitle: string; onImported?: (sources: any[]) => void }) {
  const queryClient = useQueryClient();
  const [selectedCollection, setSelectedCollection] = useState('');
  const [importingVideoId, setImportingVideoId] = useState<string | null>(null);
  const [linkedVideo, setLinkedVideo] = useState<{ guid: string; title: string; hlsUrl: string } | null>(null);

  const { data: collections, isLoading: loadingCollections } = useQuery({
    queryKey: ['bunny-collections'],
    queryFn: async () => {
      const { data } = await api.get('/bunny/stream/collections');
      return data as { totalItems: number; items: { guid: string; name: string; videoCount: number }[] };
    },
  });

  const { data: videos, isLoading: loadingVideos } = useQuery({
    queryKey: ['bunny-collection-videos', selectedCollection],
    queryFn: async () => {
      const { data } = await api.get(`/bunny/stream/collections/${selectedCollection}/videos`);
      return data as { totalItems: number; items: BunnyVideoItem[] };
    },
    enabled: !!selectedCollection,
  });

  const handleImportVideo = async (video: BunnyVideoItem) => {
    setImportingVideoId(video.guid);
    try {
      const { data } = await api.post('/bunny/stream/movie/import-bunny', {
        videoId: video.guid,
        collectionId: selectedCollection,
        title: movieTitle || undefined,
        existingMovieId: movieId,
      });
      toast.success(`"${video.title}" linked! Streaming sources updated.`);
      setLinkedVideo({ guid: video.guid, title: video.title, hlsUrl: data.hlsUrl });
      queryClient.invalidateQueries({ queryKey: ['movie', movieId] });
      if (onImported && data.streamingSources) {
        onImported(data.streamingSources);
      }
    } catch (err: any) {
      toast.error(err.response?.data?.message || 'Import failed');
    } finally {
      setImportingVideoId(null);
    }
  };

  const formatDuration = (seconds: number) => {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    return m > 0 ? `${m}m ${s}s` : `${s}s`;
  };

  const formatSize = (bytes: number) => {
    if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
    if (bytes >= 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
    return `${(bytes / 1024).toFixed(0)} KB`;
  };

  const statusLabel = (s: number) => {
    const map: Record<number, string> = { 0: 'Created', 1: 'Uploaded', 2: 'Processing', 3: 'Transcoding', 4: 'Finished', 5: 'Error', 6: 'Upload Failed' };
    return map[s] || 'Unknown';
  };

  const statusColor = (s: number) => {
    if (s === 4) return 'text-green-400 bg-green-400/10';
    if (s === 5 || s === 6) return 'text-red-400 bg-red-400/10';
    return 'text-yellow-400 bg-yellow-400/10';
  };

  return (
    <div className="space-y-4">
      {/* Success Banner */}
      {linkedVideo && (
        <div className="bg-green-500/10 border border-green-500/30 rounded-xl p-4 space-y-2">
          <div className="flex items-center gap-2 text-green-400 font-semibold text-sm">
            <Check size={18} />
            <span>Video Linked Successfully!</span>
          </div>
          <p className="text-xs text-text-secondary">
            &quot;{linkedVideo.title}&quot; has been linked to this movie. Streaming sources are now active.
          </p>
          <div className="text-xs text-text-muted bg-background/50 rounded-lg px-3 py-2 break-all">
            <span className="font-medium text-green-400">HLS: </span>{linkedVideo.hlsUrl}
          </div>
          <p className="text-[11px] text-text-muted italic mt-1">
            No need to click &quot;Update Content&quot; — streaming sources are already saved.
          </p>
        </div>
      )}

      {/* Select Collection */}
      <div className="space-y-2">
        <label className="text-sm font-medium text-text-secondary">1. Select Collection</label>
        {loadingCollections ? (
          <div className="flex items-center gap-2 text-sm text-text-muted py-2"><Loader2 size={14} className="animate-spin" /> Loading collections...</div>
        ) : (
          <select
            value={selectedCollection}
            onChange={(e) => { setSelectedCollection(e.target.value); setLinkedVideo(null); }}
            className="w-full bg-background border border-border rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-purple-400"
          >
            <option value="">-- Select a collection --</option>
            {collections?.items?.map((c) => (
              <option key={c.guid} value={c.guid}>{c.name} ({c.videoCount} videos)</option>
            ))}
          </select>
        )}
      </div>

      {/* Video list with per-video import buttons */}
      {selectedCollection && (
        <div className="space-y-2">
          <label className="text-sm font-medium text-text-secondary">2. Click &quot;Import&quot; on a video to link it</label>
          {loadingVideos ? (
            <div className="flex items-center gap-2 text-sm text-text-muted py-2"><Loader2 size={14} className="animate-spin" /> Loading videos...</div>
          ) : videos?.items && videos.items.length > 0 ? (
            <div className="space-y-1.5 max-h-72 overflow-y-auto pr-1">
              {videos.items.map((video) => {
                const isLinked = linkedVideo?.guid === video.guid;
                const isImporting = importingVideoId === video.guid;
                return (
                  <div
                    key={video.guid}
                    className={clsx(
                      'flex items-center gap-3 p-3 rounded-xl transition-all border',
                      isLinked
                        ? 'border-green-400/50 bg-green-500/10'
                        : 'border-border hover:border-purple-400/30 hover:bg-surface-light/50'
                    )}
                  >
                    <div className="w-20 h-12 rounded-lg overflow-hidden bg-surface-light flex-shrink-0">
                      <img
                        src={`https://vz-f3b830f6-306.b-cdn.net/${video.guid}/${video.thumbnailFileName || 'thumbnail.jpg'}`}
                        alt={video.title}
                        className="w-full h-full object-cover"
                        onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
                      />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-sm font-medium truncate">{video.title}</p>
                      <p className="text-xs text-text-muted">
                        {video.length > 0 && formatDuration(video.length)} · {formatSize(video.storageSize)}
                        {video.availableResolutions && ` · ${video.availableResolutions}`}
                      </p>
                    </div>
                    <span className={clsx('text-[10px] px-2 py-0.5 rounded font-medium flex-shrink-0', statusColor(video.status))}>
                      {statusLabel(video.status)}
                    </span>
                    {/* Per-video import/linked button */}
                    {isLinked ? (
                      <span className="flex items-center gap-1 text-xs text-green-400 font-semibold flex-shrink-0 px-3 py-1.5 bg-green-500/10 rounded-lg">
                        <Check size={14} /> Linked
                      </span>
                    ) : (
                      <button
                        type="button"
                        onClick={() => handleImportVideo(video)}
                        disabled={isImporting || !!importingVideoId || video.status !== 4}
                        className="flex items-center gap-1 text-xs font-semibold flex-shrink-0 px-3 py-1.5 bg-purple-600 hover:bg-purple-700 text-white rounded-lg disabled:opacity-40 transition-colors"
                      >
                        {isImporting ? (
                          <><Loader2 size={12} className="animate-spin" /> Importing...</>
                        ) : (
                          <><Download size={12} /> Import</>
                        )}
                      </button>
                    )}
                  </div>
                );
              })}
            </div>
          ) : (
            <p className="text-sm text-text-muted py-4 text-center">No videos in this collection</p>
          )}
        </div>
      )}
    </div>
  );
}

// ── R2 Movie Import Section (Browse R2 → Import movie video files) ──
function R2MovieImportSection({ movieId, onImported }: { movieId: string; onImported?: (sources: any[]) => void }) {
  const queryClient = useQueryClient();
  const [currentPath, setCurrentPath] = useState('');
  const [selectedFolder, setSelectedFolder] = useState('');
  const [previewData, setPreviewData] = useState<{
    movieFolder: string;
    files: { name: string; key: string; size: number; url: string }[];
    totalFiles: number;
  } | null>(null);

  const { data: browseData, isLoading: browsing } = useQuery({
    queryKey: ['r2-browse-movie', currentPath],
    queryFn: async () => {
      const { data } = await api.get(`/r2/browse?path=${encodeURIComponent(currentPath)}`);
      return data as {
        currentPath: string;
        folders: { name: string; path: string }[];
        files: { name: string; path: string; size: number; url: string }[];
      };
    },
  });

  const previewMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.get(`/r2/preview-movie?path=${encodeURIComponent(selectedFolder)}`);
      return data;
    },
    onSuccess: (data) => {
      setPreviewData(data);
      toast.success(`Found ${data.totalFiles} video file(s)`);
    },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Failed to preview folder'),
  });

  const importMutation = useMutation({
    mutationFn: async () => {
      const { data } = await api.post(`/r2/import-movie/${movieId}`, { path: selectedFolder });
      return data as { movieTitle: string; sources: any[]; uploadSource: string };
    },
    onSuccess: (data) => {
      toast.success(`Imported ${data.sources.length} source(s) for "${data.movieTitle}"`);
      queryClient.invalidateQueries({ queryKey: ['movie', movieId] });
      if (onImported) onImported(data.sources);
    },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Import failed'),
  });

  const formatSize = (bytes: number) => {
    if (!bytes) return '—';
    if (bytes >= 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024 * 1024)).toFixed(1)} GB`;
    return `${(bytes / (1024 * 1024)).toFixed(0)} MB`;
  };

  const pathParts = currentPath.split('/').filter(Boolean);

  return (
    <div className="space-y-4">
      <div className="p-3 rounded-lg bg-orange-500/10 border border-orange-500/20 text-xs text-orange-300 space-y-1">
        <p className="font-semibold">☁️ Import Movie from Cloudflare R2</p>
        <p>Browse your R2 bucket, select the movie folder, preview files, and import as streaming sources.</p>
      </div>

      {/* Breadcrumb */}
      <div className="flex items-center gap-1 text-xs flex-wrap">
        <button
          type="button"
          onClick={() => { setCurrentPath(''); setSelectedFolder(''); setPreviewData(null); }}
          className="text-orange-400 hover:text-orange-300 font-medium"
        >
          🪣 Root
        </button>
        {pathParts.map((part, i) => (
          <span key={i} className="flex items-center gap-1">
            <span className="text-text-muted">/</span>
            <button
              type="button"
              onClick={() => { setCurrentPath(pathParts.slice(0, i + 1).join('/') + '/'); setPreviewData(null); }}
              className="text-orange-400 hover:text-orange-300"
            >
              {part}
            </button>
          </span>
        ))}
      </div>

      {/* Folder/File browser */}
      {browsing ? (
        <div className="flex items-center gap-2 text-sm text-text-muted py-4 justify-center"><Loader2 size={14} className="animate-spin" /> Browsing R2...</div>
      ) : (
        <div className="bg-surface-light border border-border rounded-lg max-h-48 overflow-y-auto">
          {browseData?.folders.map((folder) => (
            <div key={folder.path} className="flex items-center gap-2 px-3 py-2 hover:bg-surface transition-colors border-b border-border/50 last:border-0">
              <button
                type="button"
                onClick={() => { setCurrentPath(folder.path); setPreviewData(null); }}
                className="flex items-center gap-2 flex-1 text-left text-sm"
              >
                <FolderInput size={14} className="text-orange-400" />
                <span className="text-text-primary">{folder.name}/</span>
              </button>
              <button
                type="button"
                onClick={() => { setSelectedFolder(folder.path); setPreviewData(null); }}
                className={`text-xs px-2 py-1 rounded font-medium transition-colors ${
                  selectedFolder === folder.path
                    ? 'bg-orange-500 text-white'
                    : 'bg-orange-500/10 text-orange-400 hover:bg-orange-500/20'
                }`}
              >
                {selectedFolder === folder.path ? '✓ Selected' : 'Select'}
              </button>
            </div>
          ))}
          {browseData?.files.map((file) => (
            <div key={file.path} className="flex items-center gap-2 px-3 py-2 text-sm text-text-muted border-b border-border/50 last:border-0">
              <Film size={14} />
              <span className="flex-1 truncate">{file.name}</span>
              <span className="text-xs">{formatSize(file.size)}</span>
            </div>
          ))}
          {(!browseData?.folders.length && !browseData?.files.length) && (
            <p className="text-sm text-text-muted text-center py-4">Empty folder</p>
          )}
        </div>
      )}

      {selectedFolder && (
        <div className="flex items-center gap-2 text-xs text-orange-400">
          <Check size={12} />
          Selected: <code className="bg-surface px-1 rounded">{selectedFolder}</code>
        </div>
      )}

      {/* Preview */}
      {selectedFolder && (
        <button
          type="button"
          onClick={() => previewMutation.mutate()}
          disabled={previewMutation.isPending}
          className="flex items-center gap-2 bg-orange-600 hover:bg-orange-700 text-white px-4 py-2 rounded-lg text-sm font-medium disabled:opacity-50 w-full justify-center"
        >
          {previewMutation.isPending ? <Loader2 size={14} className="animate-spin" /> : <Layers size={14} />}
          Preview Video Files
        </button>
      )}

      {/* Preview Results */}
      {previewData && previewData.files.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center gap-2 text-sm font-medium text-green-400">
            <Check size={16} />
            Found {previewData.totalFiles} video file(s)
          </div>
          <div className="space-y-1 bg-surface-light border border-border rounded-lg p-3 max-h-40 overflow-y-auto">
            {previewData.files.map((file) => (
              <div key={file.key} className="flex items-center gap-3 text-xs text-text-secondary">
                <Film size={12} className="text-orange-400" />
                <span className="flex-1 truncate">{file.name}</span>
                <span className="text-text-muted">{formatSize(file.size)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Import */}
      {previewData && previewData.files.length > 0 && (
        <button
          type="button"
          onClick={() => importMutation.mutate()}
          disabled={importMutation.isPending}
          className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white px-4 py-2.5 rounded-xl text-sm font-semibold disabled:opacity-50 w-full justify-center"
        >
          {importMutation.isPending ? <Loader2 size={16} className="animate-spin" /> : <Download size={16} />}
          {importMutation.isPending ? 'Importing...' : `Import ${previewData.totalFiles} File(s) as Streaming Sources`}
        </button>
      )}

      {importMutation.isSuccess && importMutation.data && (
        <div className="bg-surface-light border border-green-500/30 rounded-xl p-4">
          <div className="flex items-center gap-2 text-green-400 font-medium text-sm">
            <Check size={16} />
            Successfully imported {importMutation.data.sources.length} source(s) for &quot;{importMutation.data.movieTitle}&quot;
          </div>
        </div>
      )}
    </div>
  );
}

export default function MovieFormPage() {
  const { id } = useParams();
  const isEdit = Boolean(id);
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const queryClient = useQueryClient();

  // Section determines which admin area this form is used from
  const section = searchParams.get('section') ?? 'movie'; // 'movie' | 'series' | 'anime'
  const isSeries = section === 'series';
  const isAnimeSection = section === 'anime';
  const cancelPath = isSeries ? '/series' : isAnimeSection ? '/anime' : '/movies';

  // For anime, track whether it's a movie or series (UI-only, both stored as 'anime')
  const [animeFormat, setAnimeFormat] = useState<'anime_movie' | 'anime_series'>('anime_movie');
  const showDuration = !isSeries && (!isAnimeSection || animeFormat === 'anime_movie');

  const [form, setForm] = useState({
    title: '',
    alternateTitle: '',
    synopsis: '',
    posterUrl: '',
    bannerUrl: '',
    logoUrl: '',
    trailerUrl: '',
    cbfcCertificateUrl: '',
    genres: [] as string[],
    contentType: (isSeries ? 'web_series' : isAnimeSection ? 'anime' : 'movie') as 'movie' | 'documentary' | 'anime' | 'web_series' | 'tv_show' | 'short_film',
    contentRating: 'UA',
    releaseYear: new Date().getFullYear(),
    duration: 0,
    status: 'draft' as 'draft' | 'published' | 'upcoming' | 'archived',
    cast: [] as CastMember[],
    streamingSources: [] as StreamingSource[],
    tags: [] as string[],
    starRating: 0,
    country: '',
    director: '',
    studio: '',
    videoQuality: '',
    languages: [] as string[],
    rankingLabel: '',
    isFeatured: false,
    isPremium: false,
    freeEpisodeCount: 0,
    platformOrigin: '',
    ottPlatforms: [] as string[],
    uploadSource: 'bunny',
    imdbId: '',
    tmdbId: '',
  });

  const [ottDropdownOpen, setOttDropdownOpen] = useState(false);

  const [durationHours, setDurationHours] = useState(0);
  const [durationMinutes, setDurationMinutes] = useState(0);

  const [genreInput, setGenreInput] = useState('');
  const [tagInput, setTagInput] = useState('');

  const { data: movie } = useQuery<Movie>({
    queryKey: ['movie', id],
    queryFn: async () => {
      const { data } = await api.get(`/movies/${id}`);
      return data;
    },
    enabled: isEdit,
  });

  useEffect(() => {
    if (movie) {
      const totalMinutes = movie.duration ?? 0;
      const hours = Math.floor(totalMinutes / 60);
      const minutes = totalMinutes % 60;
      setDurationHours(hours);
      setDurationMinutes(minutes);
      // If editing an anime from the series manager, mark it as anime_series format
      if (movie.contentType === 'anime' && section === 'series') {
        setAnimeFormat('anime_series');
      }
      setForm({
        title: movie.title,
        alternateTitle: movie.alternateTitle ?? '',
        synopsis: movie.synopsis ?? '',
        posterUrl: movie.posterUrl,
        bannerUrl: movie.bannerUrl ?? '',
        logoUrl: movie.logoUrl ?? '',
        trailerUrl: movie.trailerUrl ?? '',
        cbfcCertificateUrl: movie.cbfcCertificateUrl ?? '',
        genres: movie.genres,
        contentType: movie.contentType,
        contentRating: movie.contentRating ?? 'UA',
        releaseYear: movie.releaseYear,
        duration: movie.duration ?? 0,
        status: movie.status,
        cast: movie.cast,
        streamingSources: (movie.streamingSources || []).map((src: any) => ({
          quality: src.quality,
          url: src.url,
          label: src.label || '',
        })),
        tags: movie.tags,
        starRating: movie.starRating ?? 0,
        country: movie.country ?? '',
        director: movie.director ?? '',
        studio: movie.studio ?? '',
        videoQuality: movie.videoQuality ?? '',
        languages: movie.languages ?? [],
        rankingLabel: movie.rankingLabel ?? '',
        isFeatured: movie.isFeatured ?? false,
        isPremium: movie.isPremium ?? false,
        freeEpisodeCount: movie.freeEpisodeCount ?? 0,
        platformOrigin: movie.platformOrigin ?? '',
        ottPlatforms: (movie as any).ottPlatforms ?? [],
        uploadSource: (movie as any).uploadSource ?? 'bunny',
        imdbId: movie.imdbId ?? '',
        tmdbId: movie.tmdbId ?? '',
      });
    }
  }, [movie]);

  const mutation = useMutation({
    mutationFn: (data: typeof form) =>
      isEdit ? api.patch(`/movies/${id}`, data) : api.post('/movies', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['movies'] });
      queryClient.invalidateQueries({ queryKey: ['series-list'] });
      queryClient.invalidateQueries({ queryKey: ['anime'] });
      toast.success(isEdit ? 'Content updated' : 'Content created');
      // Navigate to the correct section page (not always /movies)
      const successPath =
        isSeries ? '/series'
        : isAnimeSection
          ? (animeFormat === 'anime_series' ? '/series' : '/anime')
          : '/movies';
      navigate(successPath);
    },
    onError: (error: any) => {
      const message = error?.response?.data?.message || 'Failed to save';
      toast.error(message);
    },
  });

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const totalMinutes = (durationHours * 60) + durationMinutes;
    const submitForm = {
      ...form,
      duration: totalMinutes,
      contentRating: form.contentRating || 'U',
      streamingSources: form.streamingSources
        .filter((src) => src.url && src.label && typeof src.label === 'string')
        .map(({ quality, url, label }) => ({ quality, url, label })),
    };
    mutation.mutate(submitForm, {
      onError: (error: any) => {
        const message = error?.response?.data?.message || 'Failed to save';
        console.error('Save error:', error?.response?.data || error);
        toast.error(message);
      },
    });
  };

  const addGenre = () => {
    if (genreInput.trim() && !form.genres.includes(genreInput.trim())) {
      setForm({ ...form, genres: [...form.genres, genreInput.trim()] });
      setGenreInput('');
    }
  };

  const addTag = () => {
    if (tagInput.trim() && !form.tags.includes(tagInput.trim())) {
      setForm({ ...form, tags: [...form.tags, tagInput.trim()] });
      setTagInput('');
    }
  };

  const addCastMember = () => {
    setForm({ ...form, cast: [...form.cast, { name: '', role: '', character: '', photoUrl: '' }] });
  };

  const updateCast = (idx: number, field: string, value: string) => {
    const updated = [...form.cast];
    updated[idx] = { ...updated[idx], [field]: value };
    setForm({ ...form, cast: updated });
  };

  const addStreamingSource = () => {
    setForm({ ...form, streamingSources: [...form.streamingSources, { quality: '1080p', url: '', label: '' }] });
  };

  const updateSource = (idx: number, field: string, value: string) => {
    const updated = [...form.streamingSources];
    updated[idx] = { ...updated[idx], [field]: value };
    setForm({ ...form, streamingSources: updated });
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div className="flex items-center gap-4">
        <button onClick={() => navigate(cancelPath)} className="text-text-secondary hover:text-text-primary">
          <ArrowLeft size={20} />
        </button>
        <h1 className="text-2xl font-semibold">
          {isEdit
            ? 'Edit Content'
            : isSeries
              ? 'Add New Series'
              : isAnimeSection
                ? 'Add New Anime'
                : 'Add New Movie'}
        </h1>
      </div>

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Basic info */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-medium">Basic Information</h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Title *</label>
              <input
                type="text"
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                required
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Alternate Title</label>
              <input
                type="text"
                value={form.alternateTitle}
                onChange={(e) => setForm({ ...form, alternateTitle: e.target.value })}
                placeholder="Original or alternate language title"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>

          {/* Anime format picker — visible in Anime section (create + edit) */}
          {isAnimeSection && (
            <div>
              <label className="block text-sm text-text-secondary mb-2">Content Format *</label>
              <div className="flex gap-2">
                {(['anime_movie', 'anime_series'] as const).map((fmt) => (
                  <button
                    key={fmt}
                    type="button"
                    onClick={() => setAnimeFormat(fmt)}
                    className={`px-4 py-2 rounded-xl text-sm font-medium border transition-colors ${
                      animeFormat === fmt
                        ? 'bg-gold text-background border-gold'
                        : 'bg-surface-light border-border text-text-secondary hover:border-gold/50'
                    }`}
                  >
                    {fmt === 'anime_movie' ? 'Anime Movie' : 'Anime Web Series'}
                  </button>
                ))}
              </div>
              {animeFormat === 'anime_series' && (
                <p className="text-xs text-text-muted mt-1">Episodes &amp; seasons are managed in the Series Manager after saving</p>
              )}
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              {/* Content type — fixed per section, no dropdown */}
              {isSeries ? (
                <div>
                  <label className="block text-sm text-text-secondary mb-1">Series Type *</label>
                  <div className="flex gap-2">
                    {(['web_series', 'tv_show'] as const).map((t) => (
                      <button
                        key={t}
                        type="button"
                        onClick={() => setForm({ ...form, contentType: t })}
                        className={`px-4 py-2 rounded-xl text-sm font-medium border transition-colors ${
                          form.contentType === t
                            ? 'bg-gold text-background border-gold'
                            : 'bg-surface-light border-border text-text-secondary hover:border-gold/50'
                        }`}
                      >
                        {t === 'web_series' ? 'Web Series' : 'TV Show'}
                      </button>
                    ))}
                  </div>
                </div>
              ) : isAnimeSection ? (
                <div>
                  <label className="block text-sm text-text-secondary mb-1">Content Type</label>
                  <div className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-muted text-sm">
                    Anime
                  </div>
                </div>
              ) : (
                <div>
                  <label className="block text-sm text-text-secondary mb-1">Content Type</label>
                  <div className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-muted text-sm">
                    Movie
                  </div>
                </div>
              )}
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Director</label>
              <input
                type="text"
                value={form.director}
                onChange={(e) => setForm({ ...form, director: e.target.value })}
                placeholder="Director name"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>

          <div>
            <label className="block text-sm text-text-secondary mb-1">Description *</label>
            <textarea
              value={form.synopsis}
              onChange={(e) => setForm({ ...form, synopsis: e.target.value })}
              rows={4}
              className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold resize-none"
              required
            />
          </div>

          <div className={`grid gap-4 ${showDuration ? 'grid-cols-1 md:grid-cols-4' : 'grid-cols-1 md:grid-cols-1'}`}>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Release Year</label>
              <input
                type="number"
                value={form.releaseYear}
                onChange={(e) => setForm({ ...form, releaseYear: parseInt(e.target.value) || 0 })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            {showDuration && (
              <>
                <div>
                  <label className="block text-sm text-text-secondary mb-1">Duration - Hours</label>
                  <input
                    type="number"
                    min="0"
                    value={durationHours}
                    onChange={(e) => setDurationHours(parseInt(e.target.value) || 0)}
                    className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                  />
                </div>
                <div>
                  <label className="block text-sm text-text-secondary mb-1">Duration - Minutes</label>
                  <input
                    type="number"
                    min="0"
                    max="59"
                    value={durationMinutes}
                    onChange={(e) => setDurationMinutes(Math.min(59, parseInt(e.target.value) || 0))}
                    className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                  />
                </div>
                <div>
                  <label className="block text-sm text-text-secondary mb-1">Total Duration</label>
                  <div className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-secondary flex items-center">
                    {durationHours}h {durationMinutes}m ({durationHours * 60 + durationMinutes} min)
                  </div>
                </div>
              </>
            )}
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Status</label>
              <select
                value={form.status}
                onChange={(e) => setForm({ ...form, status: e.target.value as typeof form.status })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              >
                <option value="draft">Draft</option>
                <option value="published">Published</option>
                <option value="archived">Archived</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Star Rating (0-10)</label>
              <input
                type="number"
                min="0"
                max="10"
                step="0.1"
                value={form.starRating}
                onChange={(e) => setForm({ ...form, starRating: Math.min(10, Math.max(0, parseFloat(e.target.value) || 0)) })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Country</label>
              <input
                type="text"
                value={form.country}
                onChange={(e) => setForm({ ...form, country: e.target.value })}
                placeholder="e.g. India, USA"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Studio / Production</label>
              <input
                type="text"
                value={form.studio}
                onChange={(e) => setForm({ ...form, studio: e.target.value })}
                placeholder="e.g. Dharma Productions"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Video Quality</label>
              <select
                value={form.videoQuality}
                onChange={(e) => setForm({ ...form, videoQuality: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              >
                <option value="">None</option>
                <option value="CAM">CAM</option>
                <option value="HDTS">HDTS</option>
                <option value="HD">HD</option>
                <option value="FHD">FHD</option>
                <option value="4K">4K</option>
                <option value="UHD">UHD</option>
              </select>
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Platform Origin</label>
              <input
                type="text"
                value={form.platformOrigin}
                onChange={(e) => setForm({ ...form, platformOrigin: e.target.value })}
                placeholder="e.g. Netflix, Amazon Prime, Original"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>

          {/* OTT Platforms Multi-Select */}
          <div>
            <label className="block text-sm text-text-secondary mb-1">OTT Platforms</label>
            <div className="relative">
              <button
                type="button"
                onClick={() => setOttDropdownOpen(!ottDropdownOpen)}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-left text-text-primary focus:outline-none focus:border-gold flex items-center justify-between"
              >
                <span className={form.ottPlatforms.length === 0 ? 'text-text-muted' : ''}>
                  {form.ottPlatforms.length === 0 ? 'Select OTT platforms...' : form.ottPlatforms.join(', ')}
                </span>
                <ChevronDown size={16} className={`text-text-muted transition-transform ${ottDropdownOpen ? 'rotate-180' : ''}`} />
              </button>
              {ottDropdownOpen && (
                <div className="absolute z-20 mt-1 w-full bg-surface border border-border rounded-xl shadow-xl max-h-60 overflow-y-auto">
                  {['Netflix', 'Amazon Prime Video', 'Disney+ Hotstar', 'JioCinema', 'SonyLIV', 'Zee5', 'Apple TV+', 'Hulu', 'HBO Max', 'Paramount+', 'Peacock', 'MX Player', 'Voot', 'ALTBalaji', 'Aha', 'Hoichoi', 'Lionsgate Play', 'Original'].map((platform) => {
                    const isSelected = form.ottPlatforms.includes(platform);
                    return (
                      <button
                        key={platform}
                        type="button"
                        onClick={() => {
                          setForm((prev) => ({
                            ...prev,
                            ottPlatforms: isSelected
                              ? prev.ottPlatforms.filter((p) => p !== platform)
                              : [...prev.ottPlatforms, platform],
                          }));
                        }}
                        className={`w-full text-left px-4 py-2.5 text-sm hover:bg-surface-light transition-colors flex items-center gap-2 ${isSelected ? 'text-gold' : 'text-text-primary'}`}
                      >
                        <div className={`w-4 h-4 rounded border flex items-center justify-center flex-shrink-0 ${isSelected ? 'bg-gold border-gold' : 'border-border'}`}>
                          {isSelected && <Check size={12} className="text-background" />}
                        </div>
                        {platform}
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
            {form.ottPlatforms.length > 0 && (
              <div className="flex flex-wrap gap-1.5 mt-2">
                {form.ottPlatforms.map((platform) => (
                  <span key={platform} className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg bg-gold/10 text-gold text-xs font-medium">
                    {platform}
                    <button type="button" onClick={() => setForm((prev) => ({ ...prev, ottPlatforms: prev.ottPlatforms.filter((p) => p !== platform) }))} className="hover:text-gold-light">
                      <X size={12} />
                    </button>
                  </span>
                ))}
              </div>
            )}
          </div>
        </section>

        {/* Media URLs */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-medium">Media</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Poster URL * (2:3 portrait)</label>
              <input
                type="url"
                value={form.posterUrl}
                onChange={(e) => setForm({ ...form, posterUrl: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                required
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Banner URL (16:9 landscape)</label>
              <input
                type="url"
                value={form.bannerUrl}
                onChange={(e) => setForm({ ...form, bannerUrl: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Logo URL (transparent PNG)</label>
              <input
                type="url"
                value={form.logoUrl}
                onChange={(e) => setForm({ ...form, logoUrl: e.target.value })}
                placeholder="Title logo with transparent background"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">CBFC Certificate URL</label>
              <input
                type="url"
                value={form.cbfcCertificateUrl}
                onChange={(e) => setForm({ ...form, cbfcCertificateUrl: e.target.value })}
                placeholder="Link to CBFC certificate document"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>
          <div>
            <label className="block text-sm text-text-secondary mb-1">Trailer URL (Google Drive share link)</label>
            <input
              type="url"
              value={form.trailerUrl}
              onChange={(e) => setForm({ ...form, trailerUrl: e.target.value })}
              placeholder="https://drive.google.com/file/d/.../view?usp=sharing"
              className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
            />
            <p className="text-xs text-text-secondary mt-1">Paste the Google Drive share link. The app will auto-convert it for playback.</p>
          </div>
        </section>

        {/* Discovery & Ranking */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-medium">Discovery & Ranking</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">Ranking Label</label>
              <input
                type="text"
                value={form.rankingLabel}
                onChange={(e) => setForm({ ...form, rankingLabel: e.target.value })}
                placeholder="e.g. Thriller #3 in India Today"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div className="flex items-center gap-3 pt-6">
              <input
                type="checkbox"
                id="isFeatured"
                checked={form.isFeatured}
                onChange={(e) => setForm({ ...form, isFeatured: e.target.checked })}
                className="w-4 h-4 accent-gold"
              />
              <label htmlFor="isFeatured" className="text-sm text-text-primary cursor-pointer">
                Feature this content (show in hero banners)
              </label>
            </div>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 pt-2">
            <div className="flex items-center gap-3 pt-2">
              <input
                type="checkbox"
                id="isPremium"
                checked={form.isPremium}
                onChange={(e) => setForm({ ...form, isPremium: e.target.checked })}
                className="w-4 h-4 accent-gold"
              />
              <label htmlFor="isPremium" className="text-sm text-text-primary cursor-pointer">
                Premium Only (requires subscription)
              </label>
            </div>
            {form.isPremium && (
              <div>
                <label className="block text-sm text-text-secondary mb-1">Free Episodes Count</label>
                <input
                  type="number"
                  min="0"
                  value={form.freeEpisodeCount}
                  onChange={(e) => setForm({ ...form, freeEpisodeCount: Math.max(0, parseInt(e.target.value) || 0) })}
                  placeholder="0"
                  className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                />
                <p className="text-xs text-text-muted mt-1">Number of episodes free users can watch</p>
              </div>
            )}
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-sm text-text-secondary mb-1">IMDb ID</label>
              <input
                type="text"
                value={form.imdbId}
                onChange={(e) => setForm({ ...form, imdbId: e.target.value })}
                placeholder="e.g. tt1234567"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">TMDB ID</label>
              <input
                type="text"
                value={form.tmdbId}
                onChange={(e) => setForm({ ...form, tmdbId: e.target.value })}
                placeholder="e.g. 123456"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
          </div>
        </section>

        {/* Genres & Tags */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-medium">Genres & Tags</h2>
          <div>
            <label className="block text-sm text-text-secondary mb-1">Genres</label>
            <div className="flex gap-2 mb-2 flex-wrap">
              {form.genres.map((g) => (
                <span key={g} className="flex items-center gap-1 bg-gold/10 text-gold px-3 py-1 rounded-full text-sm">
                  {g}
                  <button type="button" onClick={() => setForm({ ...form, genres: form.genres.filter((x) => x !== g) })}>
                    <X size={12} />
                  </button>
                </span>
              ))}
            </div>
            <div className="flex gap-2">
              <input
                value={genreInput}
                onChange={(e) => setGenreInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addGenre())}
                placeholder="Add genre"
                className="flex-1 bg-surface-light border border-border rounded-xl px-4 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <button type="button" onClick={addGenre} className="px-3 py-2 bg-surface-light border border-border rounded-xl text-sm hover:border-gold transition-colors">
                Add
              </button>
            </div>
          </div>
          <div>
            <label className="block text-sm text-text-secondary mb-1">Tags</label>
            <div className="flex gap-2 mb-2 flex-wrap">
              {form.tags.map((t) => (
                <span key={t} className="flex items-center gap-1 bg-surface-light text-text-secondary px-3 py-1 rounded-full text-sm">
                  {t}
                  <button type="button" onClick={() => setForm({ ...form, tags: form.tags.filter((x) => x !== t) })}>
                    <X size={12} />
                  </button>
                </span>
              ))}
            </div>
            <div className="flex gap-2">
              <input
                value={tagInput}
                onChange={(e) => setTagInput(e.target.value)}
                onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), addTag())}
                placeholder="Add tag"
                className="flex-1 bg-surface-light border border-border rounded-xl px-4 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <button type="button" onClick={addTag} className="px-3 py-2 bg-surface-light border border-border rounded-xl text-sm hover:border-gold transition-colors">
                Add
              </button>
            </div>
          </div>
        </section>

        {/* Languages */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-medium">Languages</h2>
          <div className="flex gap-2 mb-2 flex-wrap">
            {form.languages.map((lang) => (
              <span key={lang} className="flex items-center gap-1 bg-gold/10 text-gold px-3 py-1 rounded-full text-sm">
                {lang}
                <button type="button" onClick={() => setForm({ ...form, languages: form.languages.filter((l) => l !== lang) })}>
                  <X size={12} />
                </button>
              </span>
            ))}
          </div>
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-2">
            {[
              'Multi Language',
              'Afrikaans', 'Albanian', 'Amharic', 'Arabic', 'Armenian', 'Assamese',
              'Azerbaijani', 'Bangla', 'Basque', 'Belarusian', 'Bosnian', 'Bulgarian',
              'Burmese', 'Cantonese', 'Catalan', 'Chinese', 'Croatian', 'Czech',
              'Danish', 'Dutch', 'English', 'Estonian', 'Filipino', 'Finnish',
              'French', 'Galician', 'Georgian', 'German', 'Greek', 'Gujarati',
              'Hausa', 'Hebrew', 'Hindi', 'Hungarian', 'Icelandic', 'Indonesian',
              'Irish', 'Italian', 'Japanese', 'Javanese', 'Kannada', 'Kazakh',
              'Khmer', 'Korean', 'Kurdish', 'Kyrgyz', 'Lao', 'Latvian',
              'Lithuanian', 'Macedonian', 'Malay', 'Malayalam', 'Maltese', 'Mandarin',
              'Marathi', 'Mongolian', 'Nepali', 'Norwegian', 'Odia', 'Pashto',
              'Persian', 'Polish', 'Portuguese', 'Punjabi', 'Romanian', 'Russian',
              'Serbian', 'Sindhi', 'Sinhala', 'Slovak', 'Slovenian', 'Somali',
              'Spanish', 'Swahili', 'Swedish', 'Tagalog', 'Tajik', 'Tamil',
              'Tatar', 'Telugu', 'Thai', 'Tibetan', 'Turkish', 'Turkmen',
              'Ukrainian', 'Urdu', 'Uzbek', 'Vietnamese', 'Welsh', 'Yoruba', 'Zulu',
            ].map((lang) => {
              const isSelected = form.languages.includes(lang);
              return (
                <button
                  key={lang}
                  type="button"
                  onClick={() => {
                    if (isSelected) {
                      setForm({ ...form, languages: form.languages.filter((l) => l !== lang) });
                    } else {
                      setForm({ ...form, languages: [...form.languages, lang] });
                    }
                  }}
                  className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors border ${
                    isSelected
                      ? 'bg-gold/20 border-gold text-gold'
                      : 'bg-surface-light border-border text-text-secondary hover:border-gold/50'
                  }`}
                >
                  {lang}
                </button>
              );
            })}
          </div>
        </section>

        {/* Cast */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-medium">Cast</h2>
            <button type="button" onClick={addCastMember} className="flex items-center gap-1 text-sm text-gold hover:text-gold-light">
              <Plus size={16} /> Add Member
            </button>
          </div>
          {form.cast.map((member, idx) => (
            <div key={idx} className="grid grid-cols-1 md:grid-cols-4 gap-3 p-3 bg-surface-light rounded-lg">
              <input
                placeholder="Name"
                value={member.name}
                onChange={(e) => updateCast(idx, 'name', e.target.value)}
                className="bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <input
                placeholder="Role (Actor, Director...)"
                value={member.role}
                onChange={(e) => updateCast(idx, 'role', e.target.value)}
                className="bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <input
                placeholder="Character name"
                value={member.character ?? ''}
                onChange={(e) => updateCast(idx, 'character', e.target.value)}
                className="bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <div className="flex gap-2">
                <input
                  placeholder="Photo URL"
                  value={member.photoUrl ?? ''}
                  onChange={(e) => updateCast(idx, 'photoUrl', e.target.value)}
                  className="flex-1 bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
                />
                <button
                  type="button"
                  onClick={() => setForm({ ...form, cast: form.cast.filter((_, i) => i !== idx) })}
                  className="text-error hover:text-error/80"
                >
                  <X size={16} />
                </button>
              </div>
            </div>
          ))}
        </section>

        {/* Streaming Sources */}
        <section className="bg-surface border border-border rounded-xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-medium">Streaming Sources</h2>
            <button type="button" onClick={addStreamingSource} className="flex items-center gap-1 text-sm text-gold hover:text-gold-light">
              <Plus size={16} /> Add Source
            </button>
          </div>
          {form.streamingSources.map((src, idx) => (
            <div key={idx} className="grid grid-cols-1 md:grid-cols-4 gap-3 p-3 bg-surface-light rounded-lg">
              <select
                value={src.quality}
                onChange={(e) => updateSource(idx, 'quality', e.target.value)}
                className="bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              >
                <option value="4k">4K</option>
                <option value="1080p">1080p</option>
                <option value="720p">720p</option>
                <option value="480p">480p</option>
              </select>
              <input
                placeholder="Label"
                value={src.label || ''}
                onChange={(e) => updateSource(idx, 'label', e.target.value)}
                className="bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <input
                placeholder="Stream URL"
                value={src.url}
                onChange={(e) => updateSource(idx, 'url', e.target.value)}
                className="bg-background border border-border rounded-lg px-3 py-2 text-sm text-text-primary focus:outline-none focus:border-gold"
              />
              <button
                type="button"
                onClick={() => setForm({ ...form, streamingSources: form.streamingSources.filter((_, i) => i !== idx) })}
                className="text-error hover:text-error/80 flex items-center justify-center"
              >
                <X size={16} />
              </button>
            </div>
          ))}
        </section>

        {/* Bunny Stream — Adaptive Streaming */}
        {isEdit && id && (
          <section className="bg-surface border border-purple-500/20 rounded-xl p-6 space-y-4">
            <h2 className="text-lg font-medium flex items-center gap-2"><Cloud size={20} className="text-purple-400" /> Bunny Stream — Adaptive Streaming</h2>
            <p className="text-sm text-text-secondary">
              Upload to Bunny Stream for automatic multi-resolution transcoding (1080p, 720p, 480p, 360p, 240p).
              HLS adaptive streaming adjusts quality in real-time based on viewer's internet speed.
            </p>
            <HlsTranscodeSection movieId={id} />
          </section>
        )}

        {/* Import from Bunny Collection */}
        {isEdit && id && (
          <section className="bg-surface border border-purple-500/20 rounded-xl p-6 space-y-4">
            <h2 className="text-lg font-medium flex items-center gap-2"><Download size={20} className="text-purple-400" /> Import from Bunny Collection</h2>
            <p className="text-sm text-text-secondary">
              Link an already-uploaded video from your Bunny.net collections. This sets the HLS streaming sources automatically.
            </p>
            <BunnyCollectionImportSection
              movieId={id}
              movieTitle={form.title}
              onImported={(sources) => {
                setForm((prev) => ({
                  ...prev,
                  streamingSources: sources.map((s: any) => ({ quality: s.quality || 'auto', url: s.url, label: s.label || '' })),
                  uploadSource: 'bunny',
                }));
              }}
            />
          </section>
        )}

        {/* Import from Cloudflare R2 */}
        {isEdit && id && (
          <section className="bg-surface border border-orange-500/20 rounded-xl p-6 space-y-4">
            <h2 className="text-lg font-medium flex items-center gap-2"><Cloud size={20} className="text-orange-400" /> Import from Cloudflare R2</h2>
            <p className="text-sm text-text-secondary">
              Browse your R2 bucket and link video files for this movie. Works alongside existing Bunny.net content.
            </p>
            <R2MovieImportSection
              movieId={id}
              onImported={(sources) => {
                setForm((prev) => ({
                  ...prev,
                  streamingSources: sources.map((s: any) => ({ quality: s.quality || 'original', url: s.url, label: s.label || 'Direct' })),
                  uploadSource: 'r2',
                }));
              }}
            />
          </section>
        )}

        {/* Upload Source indicator */}
        {isEdit && (
          <div className="flex items-center gap-3 px-4 py-3 bg-surface border border-border rounded-xl">
            <span className="text-sm text-text-secondary">Upload Source:</span>
            <span className={`px-3 py-1 rounded-lg text-xs font-semibold ${form.uploadSource === 'r2' ? 'bg-orange-500/20 text-orange-400' : 'bg-purple-500/20 text-purple-400'}`}>
              {form.uploadSource === 'r2' ? '☁️ Cloudflare R2' : '🐰 Bunny.net'}
            </span>
          </div>
        )}

        {/* Submit */}
        <div className="flex gap-3 justify-end">
          <button
            type="button"
            onClick={() => navigate(cancelPath)}
            className="px-6 py-2.5 rounded-xl border border-border text-text-secondary hover:text-text-primary transition-colors"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={mutation.isPending}
            className="px-6 py-2.5 rounded-xl bg-gold hover:bg-gold-light text-background font-semibold transition-colors disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving...' : isEdit ? 'Update Content' : 'Create Content'}
          </button>
        </div>
      </form>
    </div>
  );
}
