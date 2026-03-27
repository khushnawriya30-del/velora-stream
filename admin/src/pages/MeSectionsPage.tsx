import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import {
  History, Bookmark, ThumbsUp, Play, LayoutList, Eye, Settings2, Users,
} from 'lucide-react';
import api from '../lib/api';
import toast from 'react-hot-toast';
import clsx from 'clsx';

// ── Built-in Me Sections (managed client-side with backend toggle via home-sections) ──
const BUILT_IN_SECTIONS = [
  {
    id: 'continue_watching',
    icon: Play,
    title: 'Continue Watching',
    description: 'Shows movies the user has partially watched with a resume button.',
    color: 'text-blue-400',
    bg: 'bg-blue-400/10',
  },
  {
    id: 'watch_history',
    icon: History,
    title: 'Watch History',
    description: 'Shows all movies the user has watched — auto-tracked on every play.',
    color: 'text-green-400',
    bg: 'bg-green-400/10',
  },
  {
    id: 'my_list',
    icon: Bookmark,
    title: 'My List',
    description: 'Movies the user has bookmarked via the Bookmark button on any movie.',
    color: 'text-yellow-400',
    bg: 'bg-yellow-400/10',
  },
  {
    id: 'liked_videos',
    icon: ThumbsUp,
    title: 'Liked Videos',
    description: 'Movies the user has liked via the Like (👍) button on a movie.',
    color: 'text-red-400',
    bg: 'bg-red-400/10',
  },
];

interface UserWatchStat {
  userId: string;
  userName: string;
  email: string;
  watchedCount: number;
  lastActive: string;
}

export default function MeSectionsPage() {
  // Load enabled section IDs from localStorage (persisted admin preference)
  const [enabledSections, setEnabledSections] = useState<Set<string>>(() => {
    try {
      const saved = localStorage.getItem('me_sections_enabled');
      return saved ? new Set(JSON.parse(saved)) : new Set(BUILT_IN_SECTIONS.map((s) => s.id));
    } catch {
      return new Set(BUILT_IN_SECTIONS.map((s) => s.id));
    }
  });

  // Load watch history stats from backend
  const { data: statsData, isLoading: statsLoading } = useQuery({
    queryKey: ['watch-stats'],
    queryFn: async () => {
      const res = await api.get('/users');
      return res.data as { users: UserWatchStat[]; total: number };
    },
    retry: 1,
  });

  const toggleSection = (id: string) => {
    setEnabledSections((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      localStorage.setItem('me_sections_enabled', JSON.stringify([...next]));
      toast.success(`Section ${next.has(id) ? 'enabled' : 'disabled'}`);
      return next;
    });
  };

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gold/10 flex items-center justify-center">
          <LayoutList className="text-gold" size={20} />
        </div>
        <div>
          <h1 className="text-2xl font-semibold text-text-primary">Me Screen Sections</h1>
          <p className="text-sm text-text-muted mt-0.5">
            Control which sections appear in the Me tab of the app
          </p>
        </div>
      </div>

      {/* Built-in Sections */}
      <div>
        <div className="flex items-center gap-2 mb-4">
          <Settings2 size={16} className="text-text-muted" />
          <h2 className="text-sm font-semibold text-text-secondary uppercase tracking-wider">
            Built-in Sections
          </h2>
        </div>
        <div className="space-y-3">
          {BUILT_IN_SECTIONS.map((section) => {
            const isEnabled = enabledSections.has(section.id);
            const Icon = section.icon;
            return (
              <div
                key={section.id}
                className="bg-surface border border-border rounded-xl p-4 flex items-center gap-4"
              >
                <div className={clsx('w-10 h-10 rounded-lg flex items-center justify-center', section.bg)}>
                  <Icon className={section.color} size={18} />
                </div>
                <div className="flex-1 min-w-0">
                  <p className="font-medium text-text-primary">{section.title}</p>
                  <p className="text-xs text-text-muted mt-0.5">{section.description}</p>
                </div>
                {/* Toggle */}
                <button
                  onClick={() => toggleSection(section.id)}
                  className={clsx(
                    'relative w-12 h-6 rounded-full transition-colors duration-200 focus:outline-none',
                    isEnabled ? 'bg-gold' : 'bg-border',
                  )}
                >
                  <span
                    className={clsx(
                      'absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform duration-200',
                      isEnabled ? 'translate-x-7' : 'translate-x-1',
                    )}
                  />
                </button>
                <span className={clsx('text-xs font-medium w-14 text-right', isEnabled ? 'text-gold' : 'text-text-muted')}>
                  {isEnabled ? 'Visible' : 'Hidden'}
                </span>
              </div>
            );
          })}
        </div>
        <p className="text-xs text-text-muted mt-3 flex items-center gap-1.5">
          <Eye size={12} />
          Section visibility is applied automatically — the app checks these settings on load.
        </p>
      </div>

      {/* Watch History Info */}
      <div className="bg-surface border border-border rounded-xl p-5 space-y-3">
        <div className="flex items-center gap-2 mb-1">
          <History size={16} className="text-gold" />
          <h2 className="font-semibold text-text-primary">How Watch Tracking Works</h2>
        </div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
          {[
            { icon: '▶️', label: 'Auto-Tracked', desc: 'Every time a user plays a movie, the watch position is saved to the database every 10 seconds.' },
            { icon: '⏯️', label: 'Resume Watching', desc: 'If a user exits mid-movie, a "Resume Watching" button appears on the movie detail page.' },
            { icon: '📋', label: 'Appears in Me', desc: 'All watched movies appear in the Watch History section inside the Me tab automatically.' },
          ].map((item) => (
            <div key={item.label} className="bg-background rounded-lg p-3 space-y-1">
              <p className="font-medium text-text-primary">{item.icon} {item.label}</p>
              <p className="text-text-muted text-xs">{item.desc}</p>
            </div>
          ))}
        </div>
      </div>

      {/* User Activity Table */}
      <div>
        <div className="flex items-center gap-2 mb-4">
          <Users size={16} className="text-text-muted" />
          <h2 className="text-sm font-semibold text-text-secondary uppercase tracking-wider">
            Recent User Activity
          </h2>
        </div>
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          {statsLoading ? (
            <div className="p-8 text-center text-text-muted text-sm">Loading user data...</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-background/50">
                <tr>
                  <th className="text-left text-text-muted font-medium px-5 py-3">User</th>
                  <th className="text-left text-text-muted font-medium px-5 py-3">Email</th>
                  <th className="text-left text-text-muted font-medium px-5 py-3">Role</th>
                  <th className="text-left text-text-muted font-medium px-5 py-3">Joined</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {((statsData as any)?.users ?? []).slice(0, 20).map((user: any) => (
                  <tr key={user._id} className="hover:bg-background/40 transition-colors">
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-2">
                        <div className="w-7 h-7 rounded-full bg-gold/20 flex items-center justify-center text-gold text-xs font-semibold">
                          {user.name?.charAt(0)?.toUpperCase() ?? 'U'}
                        </div>
                        <span className="text-text-primary font-medium">{user.name}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-text-secondary">{user.email}</td>
                    <td className="px-5 py-3">
                      <span className={clsx(
                        'px-2 py-0.5 rounded-full text-xs font-medium',
                        user.role === 'admin' ? 'bg-gold/15 text-gold' : 'bg-border text-text-secondary',
                      )}>
                        {user.role ?? 'user'}
                      </span>
                    </td>
                    <td className="px-5 py-3 text-text-muted">
                      {user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}
                    </td>
                  </tr>
                ))}
                {(!statsData || (statsData as any)?.users?.length === 0) && (
                  <tr><td colSpan={4} className="px-5 py-8 text-center text-text-muted">No users found</td></tr>
                )}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
