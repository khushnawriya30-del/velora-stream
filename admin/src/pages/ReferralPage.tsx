import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Users, Search, ChevronLeft, ChevronRight, Eye, CheckCircle, XCircle, Clock, IndianRupee, ArrowDownToLine, UserPlus, X, Loader2, AlertTriangle } from 'lucide-react';
import api from '../lib/api';
import toast from 'react-hot-toast';

// ── Types ──

interface ReferralUser {
  _id: string;
  name: string;
  email: string;
  referralCode: string;
  totalReferrals: number;
  totalEarned: number;
  joinedAt: string;
}

interface ReferralDetail {
  id: string;
  referredUser: { name: string; email: string; device?: string } | null;
  amount: number;
  status: string;
  date: string;
}

interface WithdrawalItem {
  _id: string;
  userName: string;
  userEmail: string;
  amount: number;
  upiId: string;
  bankName?: string;
  accountNumber?: string;
  ifscCode?: string;
  accountHolderName?: string;
  phoneNumber?: string;
  email?: string;
  status: string;
  rejectionReason?: string;
  createdAt: string;
}

type Tab = 'referrals' | 'withdrawals';

// ── Main Page ──

export default function ReferralPage() {
  const [tab, setTab] = useState<Tab>('referrals');

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold">Referral Management</h1>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface border border-border rounded-xl p-1">
        <button
          onClick={() => setTab('referrals')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${tab === 'referrals' ? 'bg-orange-500 text-white' : 'text-text-secondary hover:bg-surface-light'}`}
        >
          <UserPlus size={16} /> Referrals
        </button>
        <button
          onClick={() => setTab('withdrawals')}
          className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm font-medium transition-colors ${tab === 'withdrawals' ? 'bg-orange-500 text-white' : 'text-text-secondary hover:bg-surface-light'}`}
        >
          <ArrowDownToLine size={16} /> Withdrawals
        </button>
      </div>

      {tab === 'referrals' ? <ReferralsTab /> : <WithdrawalsTab />}
    </div>
  );
}

// ═══════════════════════════════════════════════════════
//  REFERRALS TAB
// ═══════════════════════════════════════════════════════

function ReferralsTab() {
  const [page, setPage] = useState(1);
  const [search, setSearch] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [selectedUserId, setSelectedUserId] = useState<string | null>(null);

  const { data, isLoading } = useQuery({
    queryKey: ['admin-referrals', page, search],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), limit: '20' });
      if (search) params.set('search', search);
      const { data } = await api.get(`/referral/admin/dashboard?${params}`);
      return data as {
        items: ReferralUser[];
        total: number;
        page: number;
        pages: number;
        overview: { totalReferrals: number; totalEarnings: number; totalReferrers: number };
      };
    },
  });

  const handleSearch = () => {
    setSearch(searchInput);
    setPage(1);
  };

  if (selectedUserId) {
    return <UserReferralDetail userId={selectedUserId} onBack={() => setSelectedUserId(null)} />;
  }

  return (
    <div className="space-y-5">
      {/* Overview Cards */}
      {data?.overview && (
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-surface border border-border rounded-xl p-4">
            <div className="flex items-center gap-2 text-text-muted text-xs mb-1"><UserPlus size={14} /> Total Referrers</div>
            <p className="text-2xl font-bold">{data.overview.totalReferrers}</p>
          </div>
          <div className="bg-surface border border-border rounded-xl p-4">
            <div className="flex items-center gap-2 text-text-muted text-xs mb-1"><Users size={14} /> Total Referrals</div>
            <p className="text-2xl font-bold">{data.overview.totalReferrals}</p>
          </div>
          <div className="bg-surface border border-border rounded-xl p-4">
            <div className="flex items-center gap-2 text-text-muted text-xs mb-1"><IndianRupee size={14} /> Total Earnings</div>
            <p className="text-2xl font-bold text-green-400">₹{data.overview.totalEarnings}</p>
          </div>
        </div>
      )}

      {/* Search */}
      <div className="flex gap-2">
        <div className="relative flex-1">
          <Search size={14} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
          <input
            type="text"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
            placeholder="Search by name, email, or referral code..."
            className="w-full bg-surface border border-border rounded-lg pl-9 pr-4 py-2.5 text-sm placeholder:text-text-muted focus:outline-none focus:border-orange-400"
          />
        </div>
        <button onClick={handleSearch} className="bg-orange-600 hover:bg-orange-700 text-white px-4 py-2.5 rounded-lg text-sm font-medium">Search</button>
        {search && (
          <button onClick={() => { setSearch(''); setSearchInput(''); setPage(1); }} className="text-text-muted hover:text-text-primary px-2"><X size={16} /></button>
        )}
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12 text-text-muted"><Loader2 size={20} className="animate-spin mr-2" /> Loading...</div>
      ) : (
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-light">
                <th className="text-left px-4 py-3 font-medium text-text-muted">User</th>
                <th className="text-left px-4 py-3 font-medium text-text-muted">Referral Code</th>
                <th className="text-center px-4 py-3 font-medium text-text-muted">Referrals</th>
                <th className="text-center px-4 py-3 font-medium text-text-muted">Earnings</th>
                <th className="text-left px-4 py-3 font-medium text-text-muted">Joined</th>
                <th className="text-center px-4 py-3 font-medium text-text-muted">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.items.map((user) => (
                <tr key={user._id} className="border-b border-border/50 hover:bg-surface-light/50 transition-colors">
                  <td className="px-4 py-3">
                    <div className="font-medium text-text-primary">{user.name}</div>
                    <div className="text-xs text-text-muted">{user.email}</div>
                  </td>
                  <td className="px-4 py-3">
                    <code className="text-xs bg-surface-light px-2 py-1 rounded font-mono">{user.referralCode}</code>
                  </td>
                  <td className="px-4 py-3 text-center font-semibold">{user.totalReferrals}</td>
                  <td className="px-4 py-3 text-center font-semibold text-green-400">₹{user.totalEarned}</td>
                  <td className="px-4 py-3 text-xs text-text-muted">{new Date(user.joinedAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={() => setSelectedUserId(user._id)}
                      className="flex items-center gap-1 mx-auto text-xs px-3 py-1.5 rounded-lg bg-orange-500/10 text-orange-400 hover:bg-orange-500/20 font-medium transition-colors"
                    >
                      <Eye size={12} /> View
                    </button>
                  </td>
                </tr>
              ))}
              {!data?.items.length && (
                <tr><td colSpan={6} className="text-center py-8 text-text-muted">No referral data found</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {data && data.pages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-xs text-text-muted">Page {data.page} of {data.pages} ({data.total} total)</span>
          <div className="flex gap-2">
            <button disabled={page <= 1} onClick={() => setPage(page - 1)} className="p-2 rounded-lg border border-border text-text-muted hover:bg-surface-light disabled:opacity-30"><ChevronLeft size={16} /></button>
            <button disabled={page >= data.pages} onClick={() => setPage(page + 1)} className="p-2 rounded-lg border border-border text-text-muted hover:bg-surface-light disabled:opacity-30"><ChevronRight size={16} /></button>
          </div>
        </div>
      )}
    </div>
  );
}

// ═══════════════════════════════════════════════════════
//  USER REFERRAL DETAIL VIEW
// ═══════════════════════════════════════════════════════

function UserReferralDetail({ userId, onBack }: { userId: string; onBack: () => void }) {
  const { data, isLoading } = useQuery({
    queryKey: ['admin-referral-user', userId],
    queryFn: async () => {
      const { data } = await api.get(`/referral/admin/user/${userId}`);
      return data as {
        user: { _id: string; name: string; email: string; referralCode: string; joinedAt: string };
        totalReferrals: number;
        totalEarned: number;
        referrals: ReferralDetail[];
      };
    },
  });

  if (isLoading) return <div className="flex items-center justify-center py-12 text-text-muted"><Loader2 size={20} className="animate-spin mr-2" /> Loading...</div>;
  if (!data) return null;

  return (
    <div className="space-y-5">
      <button onClick={onBack} className="flex items-center gap-2 text-sm text-text-muted hover:text-text-primary transition-colors">
        <ChevronLeft size={16} /> Back to Referral Dashboard
      </button>

      {/* User Info Card */}
      <div className="bg-surface border border-border rounded-xl p-5">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold">{data.user.name}</h2>
            <p className="text-sm text-text-muted">{data.user.email}</p>
            <p className="text-xs text-text-muted mt-1">Joined: {new Date(data.user.joinedAt).toLocaleDateString()}</p>
          </div>
          <div className="text-right">
            <div className="text-xs text-text-muted mb-1">Referral Code</div>
            <code className="text-sm bg-orange-500/10 text-orange-400 px-3 py-1.5 rounded-lg font-mono font-bold">{data.user.referralCode}</code>
          </div>
        </div>
        <div className="grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-border">
          <div>
            <div className="text-xs text-text-muted">Total Referred</div>
            <p className="text-xl font-bold">{data.totalReferrals} users</p>
          </div>
          <div>
            <div className="text-xs text-text-muted">Total Earned</div>
            <p className="text-xl font-bold text-green-400">₹{data.totalEarned}</p>
          </div>
        </div>
      </div>

      {/* Referred Users List */}
      <div className="bg-surface border border-border rounded-xl overflow-hidden">
        <div className="px-4 py-3 border-b border-border bg-surface-light">
          <h3 className="text-sm font-semibold">Referred Users ({data.referrals.length})</h3>
        </div>
        <div className="divide-y divide-border/50">
          {data.referrals.map((ref) => (
            <div key={ref.id} className="flex items-center justify-between px-4 py-3">
              <div>
                <div className="font-medium text-sm">{ref.referredUser?.name || 'Unknown User'}</div>
                <div className="text-xs text-text-muted">{ref.referredUser?.email || '—'}</div>
                {ref.referredUser?.device && (
                  <div className="text-[10px] text-text-muted mt-0.5">Device: {ref.referredUser.device}</div>
                )}
              </div>
              <div className="text-right">
                <div className="text-sm font-semibold text-green-400">+₹{ref.amount}</div>
                <div className="text-xs text-text-muted">{new Date(ref.date).toLocaleDateString()} {new Date(ref.date).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</div>
                <span className={`inline-block text-[10px] px-1.5 py-0.5 rounded-full mt-0.5 font-medium ${ref.status === 'success' ? 'bg-green-500/15 text-green-400' : 'bg-yellow-500/15 text-yellow-400'}`}>
                  {ref.status}
                </span>
              </div>
            </div>
          ))}
          {!data.referrals.length && (
            <div className="py-8 text-center text-text-muted text-sm">No referrals yet</div>
          )}
        </div>
      </div>
    </div>
  );
}

// ═══════════════════════════════════════════════════════
//  WITHDRAWALS TAB
// ═══════════════════════════════════════════════════════

function WithdrawalsTab() {
  const queryClient = useQueryClient();
  const [page, setPage] = useState(1);
  const [statusFilter, setStatusFilter] = useState('');
  const [rejectingId, setRejectingId] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState('');

  const { data, isLoading } = useQuery({
    queryKey: ['admin-withdrawals', page, statusFilter],
    queryFn: async () => {
      const params = new URLSearchParams({ page: String(page), limit: '20' });
      if (statusFilter) params.set('status', statusFilter);
      const { data } = await api.get(`/withdraw/admin/all?${params}`);
      return data as {
        items: WithdrawalItem[];
        total: number;
        page: number;
        pages: number;
        overview: { pending: number; totalApproved: number; totalRejected: number };
      };
    },
  });

  const approveMutation = useMutation({
    mutationFn: async (id: string) => {
      const { data } = await api.patch(`/withdraw/admin/${id}/approve`);
      return data;
    },
    onSuccess: () => {
      toast.success('Withdrawal approved');
      queryClient.invalidateQueries({ queryKey: ['admin-withdrawals'] });
    },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Failed to approve'),
  });

  const rejectMutation = useMutation({
    mutationFn: async ({ id, reason }: { id: string; reason: string }) => {
      const { data } = await api.patch(`/withdraw/admin/${id}/reject`, { reason });
      return data;
    },
    onSuccess: () => {
      toast.success('Withdrawal rejected, balance refunded');
      setRejectingId(null);
      setRejectReason('');
      queryClient.invalidateQueries({ queryKey: ['admin-withdrawals'] });
    },
    onError: (err: any) => toast.error(err.response?.data?.message || 'Failed to reject'),
  });

  const statusBadge = (status: string) => {
    if (status === 'pending') return <span className="flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-yellow-500/15 text-yellow-400 font-medium"><Clock size={10} /> Pending</span>;
    if (status === 'approved') return <span className="flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-green-500/15 text-green-400 font-medium"><CheckCircle size={10} /> Approved</span>;
    return <span className="flex items-center gap-1 text-[11px] px-2 py-0.5 rounded-full bg-red-500/15 text-red-400 font-medium"><XCircle size={10} /> Rejected</span>;
  };

  return (
    <div className="space-y-5">
      {/* Rejection Dialog */}
      {rejectingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="bg-surface border border-border rounded-2xl p-6 max-w-md w-full mx-4 space-y-4 shadow-2xl">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-red-500/10 rounded-lg"><AlertTriangle size={20} className="text-red-400" /></div>
              <h3 className="font-semibold text-lg">Reject Withdrawal?</h3>
            </div>
            <p className="text-sm text-text-secondary">The amount will be refunded to the user's wallet.</p>
            <textarea
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              placeholder="Rejection reason (optional)..."
              className="w-full bg-surface-light border border-border rounded-lg px-3 py-2 text-sm resize-none h-20 placeholder:text-text-muted focus:outline-none focus:border-orange-400"
            />
            <div className="flex items-center gap-3 justify-end">
              <button onClick={() => { setRejectingId(null); setRejectReason(''); }} className="px-4 py-2 text-sm rounded-lg border border-border text-text-secondary hover:bg-surface-light transition-colors">Cancel</button>
              <button
                onClick={() => rejectMutation.mutate({ id: rejectingId, reason: rejectReason })}
                disabled={rejectMutation.isPending}
                className="flex items-center gap-2 px-4 py-2 text-sm rounded-lg bg-red-600 hover:bg-red-700 text-white font-medium disabled:opacity-50 transition-colors"
              >
                {rejectMutation.isPending ? <Loader2 size={14} className="animate-spin" /> : <XCircle size={14} />}
                Reject & Refund
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Overview Cards */}
      {data?.overview && (
        <div className="grid grid-cols-3 gap-4">
          <div className="bg-surface border border-yellow-500/20 rounded-xl p-4">
            <div className="flex items-center gap-2 text-yellow-400 text-xs mb-1"><Clock size={14} /> Pending</div>
            <p className="text-2xl font-bold text-yellow-400">{data.overview.pending}</p>
          </div>
          <div className="bg-surface border border-green-500/20 rounded-xl p-4">
            <div className="flex items-center gap-2 text-green-400 text-xs mb-1"><CheckCircle size={14} /> Total Approved</div>
            <p className="text-2xl font-bold text-green-400">₹{data.overview.totalApproved}</p>
          </div>
          <div className="bg-surface border border-red-500/20 rounded-xl p-4">
            <div className="flex items-center gap-2 text-red-400 text-xs mb-1"><XCircle size={14} /> Rejected</div>
            <p className="text-2xl font-bold text-red-400">{data.overview.totalRejected}</p>
          </div>
        </div>
      )}

      {/* Filter */}
      <div className="flex gap-2">
        {['', 'pending', 'approved', 'rejected'].map((s) => (
          <button
            key={s}
            onClick={() => { setStatusFilter(s); setPage(1); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${statusFilter === s ? 'bg-orange-500 text-white' : 'bg-surface border border-border text-text-secondary hover:bg-surface-light'}`}
          >
            {s ? s.charAt(0).toUpperCase() + s.slice(1) : 'All'}
          </button>
        ))}
      </div>

      {/* Table */}
      {isLoading ? (
        <div className="flex items-center justify-center py-12 text-text-muted"><Loader2 size={20} className="animate-spin mr-2" /> Loading...</div>
      ) : (
        <div className="bg-surface border border-border rounded-xl overflow-hidden">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-border bg-surface-light">
                <th className="text-left px-4 py-3 font-medium text-text-muted">User</th>
                <th className="text-center px-4 py-3 font-medium text-text-muted">Amount</th>
                <th className="text-left px-4 py-3 font-medium text-text-muted">Bank Details</th>
                <th className="text-center px-4 py-3 font-medium text-text-muted">Status</th>
                <th className="text-left px-4 py-3 font-medium text-text-muted">Date</th>
                <th className="text-center px-4 py-3 font-medium text-text-muted">Actions</th>
              </tr>
            </thead>
            <tbody>
              {data?.items.map((w) => (
                <tr key={w._id} className="border-b border-border/50 hover:bg-surface-light/50 transition-colors">
                  <td className="px-4 py-3">
                    <div className="font-medium text-text-primary">{w.userName}</div>
                    <div className="text-xs text-text-muted">{w.userEmail}</div>
                  </td>
                  <td className="px-4 py-3 text-center font-bold">₹{w.amount}</td>
                  <td className="px-4 py-3">
                    {w.bankName ? (
                      <div className="space-y-0.5">
                        <div className="text-xs font-medium text-text-primary">{w.bankName}</div>
                        <div className="text-[11px] text-text-muted">A/C: {w.accountNumber}</div>
                        <div className="text-[11px] text-text-muted">IFSC: {w.ifscCode}</div>
                        <div className="text-[11px] text-text-muted">{w.accountHolderName}</div>
                        {w.phoneNumber && <div className="text-[11px] text-text-muted">📞 {w.phoneNumber}</div>}
                        {w.email && <div className="text-[11px] text-text-muted">✉ {w.email}</div>}
                      </div>
                    ) : (
                      <code className="text-xs bg-surface-light px-2 py-1 rounded">{w.upiId || '—'}</code>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">{statusBadge(w.status)}</td>
                  <td className="px-4 py-3 text-xs text-text-muted">{new Date(w.createdAt).toLocaleDateString()}<br />{new Date(w.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</td>
                  <td className="px-4 py-3 text-center">
                    {w.status === 'pending' ? (
                      <div className="flex items-center gap-1.5 justify-center">
                        <button
                          onClick={() => approveMutation.mutate(w._id)}
                          disabled={approveMutation.isPending}
                          className="text-xs px-2.5 py-1 rounded-lg bg-green-500/10 text-green-400 hover:bg-green-500/20 font-medium transition-colors"
                        >
                          Approve
                        </button>
                        <button
                          onClick={() => setRejectingId(w._id)}
                          className="text-xs px-2.5 py-1 rounded-lg bg-red-500/10 text-red-400 hover:bg-red-500/20 font-medium transition-colors"
                        >
                          Reject
                        </button>
                      </div>
                    ) : (
                      <span className="text-xs text-text-muted">
                        {w.status === 'rejected' && w.rejectionReason ? w.rejectionReason : '—'}
                      </span>
                    )}
                  </td>
                </tr>
              ))}
              {!data?.items.length && (
                <tr><td colSpan={6} className="text-center py-8 text-text-muted">No withdrawal requests</td></tr>
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {data && data.pages > 1 && (
        <div className="flex items-center justify-between">
          <span className="text-xs text-text-muted">Page {data.page} of {data.pages} ({data.total} total)</span>
          <div className="flex gap-2">
            <button disabled={page <= 1} onClick={() => setPage(page - 1)} className="p-2 rounded-lg border border-border text-text-muted hover:bg-surface-light disabled:opacity-30"><ChevronLeft size={16} /></button>
            <button disabled={page >= data.pages} onClick={() => setPage(page + 1)} className="p-2 rounded-lg border border-border text-text-muted hover:bg-surface-light disabled:opacity-30"><ChevronRight size={16} /></button>
          </div>
        </div>
      )}
    </div>
  );
}
