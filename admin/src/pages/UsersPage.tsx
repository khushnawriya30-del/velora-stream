import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Shield, ShieldOff, Search } from 'lucide-react';
import api from '../lib/api';
import type { User } from '../types';
import toast from 'react-hot-toast';
import clsx from 'clsx';
import { format } from 'date-fns';

export default function UsersPage() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');

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

  const users: User[] = data?.data ?? data ?? [];

  return (
    <div className="space-y-6">
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
                      <div className="flex items-center justify-end">
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
