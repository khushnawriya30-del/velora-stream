import { useEffect, useState } from 'react';
import { Monitor, Tv, Film, Sparkles, RefreshCw, CheckCircle, Wifi, Database } from 'lucide-react';
import api from '../lib/api';

interface Stats {
  totalMovies: number;
  totalSeries: number;
  totalAnime: number;
  totalBanners: number;
  totalSections: number;
  premiumUsers: number;
}

export default function TvManagementPage() {
  const [stats, setStats] = useState<Stats | null>(null);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [syncDone, setSyncDone] = useState(false);

  useEffect(() => {
    loadStats();
  }, []);

  const loadStats = async () => {
    setLoading(true);
    try {
      // Fetch stats from existing endpoints
      const [moviesRes, seriesRes, animeRes, bannersRes, sectionsRes] = await Promise.allSettled([
        api.get('/movies', { params: { limit: 1, contentType: 'movie' } }),
        api.get('/movies', { params: { limit: 1, contentType: 'series' } }),
        api.get('/movies', { params: { limit: 1, contentType: 'anime' } }),
        api.get('/banners'),
        api.get('/home-sections'),
      ]);

      setStats({
        totalMovies: moviesRes.status === 'fulfilled' ? (moviesRes.value.data?.total ?? 0) : 0,
        totalSeries: seriesRes.status === 'fulfilled' ? (seriesRes.value.data?.total ?? 0) : 0,
        totalAnime: animeRes.status === 'fulfilled' ? (animeRes.value.data?.total ?? 0) : 0,
        totalBanners: bannersRes.status === 'fulfilled' ? (Array.isArray(bannersRes.value.data) ? bannersRes.value.data.length : 0) : 0,
        totalSections: sectionsRes.status === 'fulfilled' ? (Array.isArray(sectionsRes.value.data) ? sectionsRes.value.data.length : 0) : 0,
        premiumUsers: 0,
      });
    } catch {
      // Fallback stats
      setStats({ totalMovies: 0, totalSeries: 0, totalAnime: 0, totalBanners: 0, totalSections: 0, premiumUsers: 0 });
    }
    setLoading(false);
  };

  const handleSync = async () => {
    setSyncing(true);
    setSyncDone(false);
    // The TV app uses the same database, so "sync" just refreshes the stats
    await loadStats();
    setSyncing(false);
    setSyncDone(true);
    setTimeout(() => setSyncDone(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-purple-500/20 rounded-xl">
            <Monitor className="w-8 h-8 text-purple-400" />
          </div>
          <div>
            <h1 className="text-2xl font-bold text-white">TV App Management</h1>
            <p className="text-sm text-gray-400">
              Velora TV uses the same database as the mobile app — all content is automatically shared.
            </p>
          </div>
        </div>

        <button
          onClick={handleSync}
          disabled={syncing}
          className="flex items-center gap-2 px-4 py-2 bg-purple-600 hover:bg-purple-700 disabled:bg-purple-800 text-white rounded-lg transition-colors"
        >
          {syncing ? (
            <RefreshCw className="w-4 h-4 animate-spin" />
          ) : syncDone ? (
            <CheckCircle className="w-4 h-4" />
          ) : (
            <RefreshCw className="w-4 h-4" />
          )}
          {syncing ? 'Syncing...' : syncDone ? 'Synced!' : 'Refresh Stats'}
        </button>
      </div>

      {/* Sync Status Banner */}
      <div className="bg-green-500/10 border border-green-500/30 rounded-xl p-4 flex items-center gap-3">
        <Wifi className="w-5 h-5 text-green-400" />
        <div>
          <p className="text-green-400 font-medium">Shared Database Active</p>
          <p className="text-sm text-gray-400">
            Content uploaded through the admin panel is instantly available on both Mobile and TV apps.
          </p>
        </div>
      </div>

      {/* Stats Grid */}
      {loading ? (
        <div className="text-center py-12 text-gray-400">Loading stats...</div>
      ) : stats && (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-5 gap-4">
          <StatCard
            icon={Film}
            label="Movies"
            value={stats.totalMovies}
            color="blue"
          />
          <StatCard
            icon={Tv}
            label="Series"
            value={stats.totalSeries}
            color="green"
          />
          <StatCard
            icon={Sparkles}
            label="Anime"
            value={stats.totalAnime}
            color="purple"
          />
          <StatCard
            icon={Database}
            label="Banners"
            value={stats.totalBanners}
            color="yellow"
          />
          <StatCard
            icon={Database}
            label="Home Sections"
            value={stats.totalSections}
            color="pink"
          />
        </div>
      )}

      {/* How It Works */}
      <div className="bg-surface border border-border rounded-xl p-6">
        <h2 className="text-lg font-semibold text-white mb-4">How TV App Content Works</h2>
        <div className="grid md:grid-cols-2 gap-6">
          <div className="space-y-3">
            <h3 className="text-sm font-medium text-purple-400 uppercase tracking-wider">Automatic Sync</h3>
            <ul className="space-y-2 text-sm text-gray-300">
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-green-400 mt-0.5 flex-shrink-0" />
                <span>All movies, series, and anime uploaded via this admin panel are <strong>automatically available</strong> on TV</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-green-400 mt-0.5 flex-shrink-0" />
                <span>Banners and home sections appear on TV exactly as configured</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-green-400 mt-0.5 flex-shrink-0" />
                <span>Watch progress, watchlist, and history sync between mobile and TV</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-green-400 mt-0.5 flex-shrink-0" />
                <span>Premium status carries over — same subscription works on both</span>
              </li>
            </ul>
          </div>
          <div className="space-y-3">
            <h3 className="text-sm font-medium text-purple-400 uppercase tracking-wider">TV-Specific Features</h3>
            <ul className="space-y-2 text-sm text-gray-300">
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
                <span>QR code login — users scan from their phone</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
                <span>Landscape-only UI optimized for TVs and remote control</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
                <span>4 tabs: Home, Shows, Movies, Anime (no Live TV or Sports)</span>
              </li>
              <li className="flex items-start gap-2">
                <CheckCircle className="w-4 h-4 text-blue-400 mt-0.5 flex-shrink-0" />
                <span>Free users can browse content but need Premium to play</span>
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* TV App Info */}
      <div className="bg-surface border border-border rounded-xl p-6">
        <h2 className="text-lg font-semibold text-white mb-4">TV App Details</h2>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
          <InfoItem label="Package" value="com.cinevault.tv" />
          <InfoItem label="Min SDK" value="Android 8.0 (API 26)" />
          <InfoItem label="UI Framework" value="Jetpack Compose TV" />
          <InfoItem label="Auth Method" value="QR Code Scan" />
        </div>
      </div>
    </div>
  );
}

function StatCard({ icon: Icon, label, value, color }: {
  icon: typeof Film;
  label: string;
  value: number;
  color: string;
}) {
  const bgClasses: Record<string, string> = {
    blue: 'bg-blue-500/10 border-blue-500/30',
    green: 'bg-green-500/10 border-green-500/30',
    purple: 'bg-purple-500/10 border-purple-500/30',
    yellow: 'bg-yellow-500/10 border-yellow-500/30',
    pink: 'bg-pink-500/10 border-pink-500/30',
  };
  const iconClasses: Record<string, string> = {
    blue: 'text-blue-400',
    green: 'text-green-400',
    purple: 'text-purple-400',
    yellow: 'text-yellow-400',
    pink: 'text-pink-400',
  };

  return (
    <div className={`border rounded-xl p-4 ${bgClasses[color] ?? ''}`}>
      <Icon className={`w-5 h-5 mb-2 ${iconClasses[color] ?? ''}`} />
      <p className="text-2xl font-bold text-white">{value}</p>
      <p className="text-sm text-gray-400">{label}</p>
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-gray-500 text-xs uppercase tracking-wider">{label}</p>
      <p className="text-white font-medium">{value}</p>
    </div>
  );
}
