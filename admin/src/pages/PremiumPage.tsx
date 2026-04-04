import { useState, useEffect, useCallback } from 'react';
import api from '../lib/api';

interface PremiumStats {
  totalCodes: number;
  redeemedCodes: number;
  availableCodes: number;
  activePremiumUsers: number;
  expiringSoon: number;
  planBreakdown: Record<string, number>;
}

interface ActivationCode {
  _id: string;
  code: string;
  plan: string;
  durationDays: number;
  isRedeemed: boolean;
  isRevoked: boolean;
  redeemedBy?: { name: string; email: string };
  redeemedAt?: string;
  expiresAt: string;
  batchId: string;
  note: string;
  createdAt: string;
}

interface PremiumUser {
  _id: string;
  name: string;
  email: string;
  isPremium: boolean;
  premiumPlan: string;
  premiumExpiresAt: string;
  premiumActivatedAt: string;
  activationCode: string;
}

const PLAN_LABELS: Record<string, string> = {
  '1month': '1 Month',
  '3months': '3 Months',
  '6months': '6 Months',
  '1year': '1 Year',
};

const PLAN_COLORS: Record<string, string> = {
  '1month': 'bg-blue-500/20 text-blue-400',
  '3months': 'bg-purple-500/20 text-purple-400',
  '6months': 'bg-amber-500/20 text-amber-400',
  '1year': 'bg-emerald-500/20 text-emerald-400',
};

export default function PremiumPage() {
  const [tab, setTab] = useState<'dashboard' | 'codes' | 'users'>('dashboard');
  const [stats, setStats] = useState<PremiumStats | null>(null);
  const [codes, setCodes] = useState<ActivationCode[]>([]);
  const [users, setUsers] = useState<PremiumUser[]>([]);
  const [loading, setLoading] = useState(false);

  // Generate modal
  const [showGenerate, setShowGenerate] = useState(false);
  const [genPlan, setGenPlan] = useState('1month');
  const [genCount, setGenCount] = useState(1);
  const [genNote, setGenNote] = useState('');
  const [generatedCodes, setGeneratedCodes] = useState<string[]>([]);

  // Filters
  const [codeFilter, setCodeFilter] = useState<'all' | 'available' | 'redeemed'>('all');

  const fetchStats = useCallback(async () => {
    try {
      const { data } = await api.get('/premium/admin/stats');
      setStats(data);
    } catch (e) {
      console.error('Failed to fetch stats', e);
    }
  }, []);

  const fetchCodes = useCallback(async () => {
    setLoading(true);
    try {
      const params: any = { limit: 100 };
      if (codeFilter === 'available') params.isRedeemed = 'false';
      if (codeFilter === 'redeemed') params.isRedeemed = 'true';
      const { data } = await api.get('/premium/admin/codes', { params });
      setCodes(data.codes);
    } catch (e) {
      console.error('Failed to fetch codes', e);
    }
    setLoading(false);
  }, [codeFilter]);

  const fetchUsers = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/premium/admin/users', { params: { limit: 100 } });
      setUsers(data.users);
    } catch (e) {
      console.error('Failed to fetch users', e);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    if (tab === 'codes') fetchCodes();
    if (tab === 'users') fetchUsers();
  }, [tab, fetchCodes, fetchUsers]);

  const handleGenerate = async () => {
    try {
      const { data } = await api.post('/premium/admin/generate', {
        plan: genPlan,
        count: genCount,
        note: genNote,
      });
      setGeneratedCodes(data.map((c: any) => c.code));
      fetchStats();
      if (tab === 'codes') fetchCodes();
    } catch (e) {
      alert('Failed to generate codes');
    }
  };

  const handleRevoke = async (id: string) => {
    if (!confirm('Revoke this code? If redeemed, user will lose premium.')) return;
    try {
      await api.post(`/premium/admin/revoke/${id}`, { reason: 'Revoked from admin panel' });
      fetchCodes();
      fetchStats();
    } catch (e) {
      alert('Failed to revoke');
    }
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Premium Management</h1>
        <button
          onClick={() => { setShowGenerate(true); setGeneratedCodes([]); }}
          className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg font-medium"
        >
          + Generate Codes
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-zinc-800 rounded-lg p-1 w-fit">
        {(['dashboard', 'codes', 'users'] as const).map((t) => (
          <button
            key={t}
            onClick={() => setTab(t)}
            className={`px-4 py-2 rounded-md text-sm font-medium capitalize ${
              tab === t ? 'bg-zinc-700 text-white' : 'text-zinc-400 hover:text-white'
            }`}
          >
            {t}
          </button>
        ))}
      </div>

      {/* Dashboard */}
      {tab === 'dashboard' && stats && (
        <div className="space-y-6">
          <div className="grid grid-cols-2 md:grid-cols-5 gap-4">
            <StatCard label="Active Premium" value={stats.activePremiumUsers} color="text-emerald-400" />
            <StatCard label="Total Codes" value={stats.totalCodes} color="text-blue-400" />
            <StatCard label="Redeemed" value={stats.redeemedCodes} color="text-purple-400" />
            <StatCard label="Available" value={stats.availableCodes} color="text-amber-400" />
            <StatCard label="Expiring Soon" value={stats.expiringSoon} color="text-red-400" />
          </div>

          {Object.keys(stats.planBreakdown).length > 0 && (
            <div className="bg-zinc-800 rounded-lg p-4">
              <h3 className="text-lg font-semibold text-white mb-3">Plan Breakdown</h3>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-3">
                {Object.entries(stats.planBreakdown).map(([plan, count]) => (
                  <div key={plan} className="bg-zinc-700 rounded-lg p-3 text-center">
                    <div className="text-2xl font-bold text-white">{count}</div>
                    <div className="text-sm text-zinc-400">{PLAN_LABELS[plan] || plan}</div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {/* Codes */}
      {tab === 'codes' && (
        <div className="space-y-4">
          <div className="flex gap-2">
            {(['all', 'available', 'redeemed'] as const).map((f) => (
              <button
                key={f}
                onClick={() => setCodeFilter(f)}
                className={`px-3 py-1 rounded text-sm capitalize ${
                  codeFilter === f ? 'bg-zinc-600 text-white' : 'bg-zinc-800 text-zinc-400'
                }`}
              >
                {f}
              </button>
            ))}
          </div>

          {loading ? (
            <div className="text-zinc-400">Loading...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-zinc-400 border-b border-zinc-700">
                    <th className="text-left py-2 px-3">Code</th>
                    <th className="text-left py-2 px-3">Plan</th>
                    <th className="text-left py-2 px-3">Status</th>
                    <th className="text-left py-2 px-3">Redeemed By</th>
                    <th className="text-left py-2 px-3">Note</th>
                    <th className="text-left py-2 px-3">Created</th>
                    <th className="text-left py-2 px-3">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {codes.map((code) => (
                    <tr key={code._id} className="border-b border-zinc-800 hover:bg-zinc-800/50">
                      <td className="py-2 px-3">
                        <button
                          onClick={() => copyToClipboard(code.code)}
                          className="font-mono text-amber-400 hover:text-amber-300"
                          title="Click to copy"
                        >
                          {code.code}
                        </button>
                      </td>
                      <td className="py-2 px-3">
                        <span className={`px-2 py-0.5 rounded text-xs ${PLAN_COLORS[code.plan] || 'bg-zinc-700 text-zinc-300'}`}>
                          {PLAN_LABELS[code.plan] || code.plan}
                        </span>
                      </td>
                      <td className="py-2 px-3">
                        {code.isRevoked ? (
                          <span className="text-red-400">Revoked</span>
                        ) : code.isRedeemed ? (
                          <span className="text-green-400">Redeemed</span>
                        ) : (
                          <span className="text-amber-400">Available</span>
                        )}
                      </td>
                      <td className="py-2 px-3 text-zinc-300">
                        {code.redeemedBy ? `${code.redeemedBy.name} (${code.redeemedBy.email})` : '—'}
                      </td>
                      <td className="py-2 px-3 text-zinc-400">{code.note || '—'}</td>
                      <td className="py-2 px-3 text-zinc-400">
                        {new Date(code.createdAt).toLocaleDateString()}
                      </td>
                      <td className="py-2 px-3">
                        {!code.isRevoked && (
                          <button
                            onClick={() => handleRevoke(code._id)}
                            className="text-red-400 hover:text-red-300 text-xs"
                          >
                            Revoke
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {codes.length === 0 && (
                <div className="text-center text-zinc-500 py-8">No codes found</div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Users */}
      {tab === 'users' && (
        <div>
          {loading ? (
            <div className="text-zinc-400">Loading...</div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-zinc-400 border-b border-zinc-700">
                    <th className="text-left py-2 px-3">User</th>
                    <th className="text-left py-2 px-3">Plan</th>
                    <th className="text-left py-2 px-3">Activated</th>
                    <th className="text-left py-2 px-3">Expires</th>
                    <th className="text-left py-2 px-3">Code Used</th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => {
                    const expired = new Date(u.premiumExpiresAt) < new Date();
                    return (
                      <tr key={u._id} className="border-b border-zinc-800 hover:bg-zinc-800/50">
                        <td className="py-2 px-3">
                          <div className="text-white">{u.name}</div>
                          <div className="text-zinc-500 text-xs">{u.email}</div>
                        </td>
                        <td className="py-2 px-3">
                          <span className={`px-2 py-0.5 rounded text-xs ${PLAN_COLORS[u.premiumPlan] || 'bg-zinc-700 text-zinc-300'}`}>
                            {PLAN_LABELS[u.premiumPlan] || u.premiumPlan || '—'}
                          </span>
                        </td>
                        <td className="py-2 px-3 text-zinc-300">
                          {u.premiumActivatedAt ? new Date(u.premiumActivatedAt).toLocaleDateString() : '—'}
                        </td>
                        <td className="py-2 px-3">
                          <span className={expired ? 'text-red-400' : 'text-green-400'}>
                            {u.premiumExpiresAt ? new Date(u.premiumExpiresAt).toLocaleDateString() : '—'}
                          </span>
                        </td>
                        <td className="py-2 px-3 font-mono text-amber-400 text-xs">{u.activationCode || '—'}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
              {users.length === 0 && (
                <div className="text-center text-zinc-500 py-8">No premium users found</div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Generate Codes Modal */}
      {showGenerate && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-zinc-800 rounded-xl p-6 w-full max-w-md space-y-4">
            <h2 className="text-xl font-bold text-white">Generate Activation Codes</h2>

            {generatedCodes.length > 0 ? (
              <div className="space-y-3">
                <div className="text-green-400 font-medium">Generated {generatedCodes.length} codes:</div>
                <div className="bg-zinc-900 rounded-lg p-3 max-h-60 overflow-y-auto space-y-1">
                  {generatedCodes.map((c) => (
                    <div key={c} className="flex items-center justify-between">
                      <span className="font-mono text-amber-400">{c}</span>
                      <button
                        onClick={() => copyToClipboard(c)}
                        className="text-xs text-zinc-400 hover:text-white"
                      >
                        Copy
                      </button>
                    </div>
                  ))}
                </div>
                <button
                  onClick={() => {
                    copyToClipboard(generatedCodes.join('\n'));
                  }}
                  className="w-full py-2 bg-zinc-700 hover:bg-zinc-600 text-white rounded-lg text-sm"
                >
                  Copy All
                </button>
                <button
                  onClick={() => setShowGenerate(false)}
                  className="w-full py-2 bg-zinc-700 hover:bg-zinc-600 text-zinc-300 rounded-lg text-sm"
                >
                  Close
                </button>
              </div>
            ) : (
              <>
                <div>
                  <label className="block text-sm text-zinc-400 mb-1">Plan</label>
                  <select
                    value={genPlan}
                    onChange={(e) => setGenPlan(e.target.value)}
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2"
                  >
                    <option value="1month">1 Month (30 days)</option>
                    <option value="3months">3 Months (90 days)</option>
                    <option value="6months">6 Months (180 days)</option>
                    <option value="1year">1 Year (365 days)</option>
                  </select>
                </div>
                <div>
                  <label className="block text-sm text-zinc-400 mb-1">Number of Codes</label>
                  <input
                    type="number"
                    min={1}
                    max={100}
                    value={genCount}
                    onChange={(e) => setGenCount(parseInt(e.target.value) || 1)}
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2"
                  />
                </div>
                <div>
                  <label className="block text-sm text-zinc-400 mb-1">Note (optional)</label>
                  <input
                    type="text"
                    value={genNote}
                    onChange={(e) => setGenNote(e.target.value)}
                    placeholder="e.g., Promo giveaway, Influencer..."
                    className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2"
                  />
                </div>
                <div className="flex gap-3">
                  <button
                    onClick={() => setShowGenerate(false)}
                    className="flex-1 py-2 bg-zinc-700 hover:bg-zinc-600 text-zinc-300 rounded-lg"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleGenerate}
                    className="flex-1 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg font-medium"
                  >
                    Generate
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

function StatCard({ label, value, color }: { label: string; value: number; color: string }) {
  return (
    <div className="bg-zinc-800 rounded-lg p-4">
      <div className={`text-3xl font-bold ${color}`}>{value}</div>
      <div className="text-sm text-zinc-400 mt-1">{label}</div>
    </div>
  );
}
