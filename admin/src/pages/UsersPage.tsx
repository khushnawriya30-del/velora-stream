import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Shield, ShieldOff, Search, History, X, Clock } from 'lucide-react';
import api from '../lib/api';
import type { User } from '../types';
import toast from 'react-hot-toast';
import clsx from 'clsx';
import { format, formatDistanceToNow } from 'date-fns';

interface WatchHistoryItem {
  _id: string;
  contentId: string;
  contentTitle?: string;
  contentType: string;
  currentTime: number;
  totalDuration: number;
  isCompleted: boolean;
  lastWatchedAt: string;
  thumbnailUrl?: string;
}

function WatchHistoryModal({ user, onClose }: { user: User; onClose: () => void }) {
  const [page, setPage] = useState(1);
  const { data, isLoading } = useQuery({
    queryKey: ['user-watch-history', user._id, page],
    queryFn: async () => {
      const { data } = await api.get(`/admin/users/${user._id}/watch-history?page=${page}&limit=10`);
      return data;
    },
  });

  const items: WatchHistoryItem[] = data?.items ?? [];
  const total: number = data?.total ?? 0;

  function formatTime(ms: number) {
    const seconds = Math.floor(ms / 1000);
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return h > 0 ? `${h}h ${m}m` : `${m}m ${s}s`;
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60">
      <div className="bg-surface border border-border rounded-2xl w-full max-w-2xl max-h-[80vh] flex flex-col shadow-2xl">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-border">
          <div>
            <h2 className="text-lg font-semibold">{user.name}'s Watch History</h2>
            <p className="text-sm text-text-secondary">{total} total entries</p>
          </div>
          <button onClick={onClose} className="p-2 rounded-lg hover:bg-surface-light transition-colors">
            <X size={18} />
          </button>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto divide-y divide-border">
          {isLoading ? (
            <div className="flex items-center justify-center py-16 text-text-secondary">Loading...</div>
          ) : items.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-16 text-text-secondary gap-2">
              <Clock size={32} className="opacity-30" />
              <p>No watch history found</p>
            </div>
          ) : (
            items.map((item) => {
              const pct = item.totalDuration > 0
                ? Math.round((item.currentTime / item.totalDuration) * 100)
                : 0;
              return (
                <div key={item._id} className="flex items-center gap-4 px-6 py-3 hover:bg-surface-light/50">
                  {item.thumbnailUrl ? (
                    <img src={item.thumbnailUrl} alt="" className="w-16 h-10 object-cover rounded-lg bg-surface-light flex-shrink-0" />
                  ) : (
                    <div className="w-16 h-10 bg-surface-light rounded-lg flex-shrink-0" />
                  )}
                  <div className="flex-1 min-w-0">
                    <p className="font-medium text-sm truncate">
                      {item.contentTitle || item.contentId}
                    </p>
                    <div className="flex items-center gap-3 mt-1">
                      <span className={clsx('text-xs px-1.5 py-0.5 rounded', item.isCompleted ? 'bg-success/10 text-success' : 'bg-gold/10 text-gold')}>
                        {item.isCompleted ? 'Completed' : `${pct}%`}
                      </span>
                      <span className="text-xs text-text-secondary">
                        {formatTime(item.currentTime)} / {formatTime(item.totalDuration)}
                      </span>
                    </div>
                    {/* Progress bar */}
                    <div className="mt-1.5 h-1 bg-surface-light rounded-full w-full">
                      <div
                        className={clsx('h-1 rounded-full transition-all', item.isCompleted ? 'bg-success' : 'bg-gold')}
                        style={{ width: `${pct}%` }}
                      />
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0">
                    <p className="text-xs text-text-secondary">
                      {formatDistanceToNow(new Date(item.lastWatchedAt), { addSuffix: true })}
                    </p>
                    <p className="text-xs text-text-muted capitalize mt-0.5">{item.contentType}</p>
                  </div>
                </div>
              );
            })
          )}
        </div>

        {/* Pagination */}
        {total > 10 && (
          <div className="flex items-center justify-center gap-2 px-6 py-3 border-t border-border">
            <button onClick={() => setPage((p) => Math.max(1, p - 1))} disabled={page === 1}
              className="px-3 py-1.5 rounded-lg bg-surface-light text-sm disabled:opacity-30">Previous</button>
            <span className="text-sm text-text-secondary">Page {page} of {Math.ceil(total / 10)}</span>
            <button onClick={() => setPage((p) => p + 1)} disabled={page >= Math.ceil(total / 10)}
              className="px-3 py-1.5 rounded-lg bg-surface-light text-sm disabled:opacity-30">Next</button>
          </div>
        )}
      </div>
    </div>
  );
}

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [historyUser, setHistoryUser] = useState<User | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['users', page, search],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), limit: '20' });
      if (search) params.set('search', search);
      const { data } = await api.get(`/admin/users?${params}`);
      return data;
    },
  });

  const suspendMutation = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'suspend' | 'unsuspend' }) =>
      api.patch(`/admin/users/${id}/${action}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['users'] });
      toast.success('User updated');
    },
    onError: () => toast.error('Operation failed'),
  });

  // Backend returns { users: [...], total, page, pages }
  const users: User[] = data?.users ?? data?.data ?? [];

  return (
    <div className="space-y-6">
      {historyUser && (
        <WatchHistoryModal user={historyUser} onClose={() => setHistoryUser(null)} />
      )}

      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Users</h1>
        <div className="relative">
          <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setPage(1); }}
            placeholder="Search users..."
            className="pl-9 pr-4 py-2 bg-surface-light border border-border rounded-xl text-sm text-text-primary focus:outline-none focus:border-gold w-64"
          />
        </div>
      </div>

      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-surface-light border-b border-border">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Name</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Email</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Role</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Provider</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Joined</th>
                <th className="text-left px-4 py-3 font-medium text-text-secondary">Status</th>
                <th className="text-right px-4 py-3 font-medium text-text-secondary">Actions</th>
              </tr>
            </thead>
            <tbody>
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <tr key={i} className="border-b border-border">
                    <td colSpan={7} className="px-4 py-4">
                      <div className="h-4 bg-surface-light rounded animate-pulse" />
                    </td>
                  </tr>
                ))
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={7} className="text-center py-12 text-text-secondary">No users found</td>
                </tr>
              ) : (
                users.map((user) => (
                  <tr key={user._id} className="border-b border-border hover:bg-surface-light/50 transition-colors">
                    <td className="px-4 py-3 font-medium">{user.name}</td>
                    <td className="px-4 py-3 text-text-secondary">{user.email}</td>
                    <td className="px-4 py-3">
                      <span
                        className={clsx(
                          'px-2 py-0.5 rounded-full text-xs font-medium',
                          user.role === 'admin' && 'bg-gold/10 text-gold',
                          user.role === 'moderator' && 'bg-info/10 text-info',
                          user.role === 'user' && 'bg-surface-light text-text-secondary',
                        )}
                      >
                        {user.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 capitalize text-text-secondary">{user.authProvider}</td>
                    <td className="px-4 py-3 text-text-secondary">{format(new Date(user.createdAt), 'MMM d, yyyy')}</td>
                    <td className="px-4 py-3">
                      <span
                        className={clsx(
                          'px-2 py-0.5 rounded-full text-xs font-medium',
                          user.isSuspended ? 'bg-error/10 text-error' : 'bg-success/10 text-success',
                        )}
                      >
                        {user.isSuspended ? 'Suspended' : 'Active'}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-3">
                        <button
                          onClick={() => setHistoryUser(user)}
                          className="flex items-center gap-1 text-text-secondary hover:text-text-primary text-xs"
                          title="View Watch History"
                        >
                          <History size={14} /> History
                        </button>
                        {user.isSuspended ? (
                          <button
                            onClick={() => suspendMutation.mutate({ id: user._id, action: 'unsuspend' })}
                            className="flex items-center gap-1 text-success hover:text-success/80 text-xs"
                          >
                            <Shield size={14} /> Activate
                          </button>
                        ) : (
                          <button
                            onClick={() => {
                              if (confirm(`Suspend ${user.name}?`)) {
                                suspendMutation.mutate({ id: user._id, action: 'suspend' });
                              }
                            }}
                            className="flex items-center gap-1 text-error hover:text-error/80 text-xs"
                          >
                            <ShieldOff size={14} /> Suspend
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      <div className="flex justify-center gap-2">
        <button
          onClick={() => setPage((p) => Math.max(1, p - 1))}
          disabled={page === 1}
          className="px-3 py-1.5 rounded-lg bg-surface-light text-text-secondary disabled:opacity-30 hover:text-text-primary transition-colors text-sm"
        >
          Previous
        </button>
        <span className="px-3 py-1.5 text-sm text-text-secondary">Page {page}</span>
        <button
          onClick={() => setPage((p) => p + 1)}
          disabled={users.length < 20}
          className="px-3 py-1.5 rounded-lg bg-surface-light text-text-secondary disabled:opacity-30 hover:text-text-primary transition-colors text-sm"
        >
          Next
        </button>
      </div>
    </div>
  );
}
