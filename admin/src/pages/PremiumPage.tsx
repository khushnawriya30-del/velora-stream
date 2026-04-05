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

interface PlanConfig {
  _id: string;
  planId: string;
  name: string;
  months: number;
  price: number;
  originalPrice: number;
  discountPercent: number;
  badge: string;
  order: number;
  isActive: boolean;
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
  const [tab, setTab] = useState<'dashboard' | 'codes' | 'users' | 'plans'>('dashboard');
  const [stats, setStats] = useState<PremiumStats | null>(null);
  const [codes, setCodes] = useState<ActivationCode[]>([]);
  const [users, setUsers] = useState<PremiumUser[]>([]);
  const [plans, setPlans] = useState<PlanConfig[]>([]);
  const [loading, setLoading] = useState(false);

  // Generate modal
  const [showGenerate, setShowGenerate] = useState(false);
  const [genPlan, setGenPlan] = useState('1month');
  const [genCount, setGenCount] = useState(1);
  const [genNote, setGenNote] = useState('');
  const [genExpiry, setGenExpiry] = useState(90);
  const [generatedCodes, setGeneratedCodes] = useState<string[]>([]);

  // Filters
  const [codeFilter, setCodeFilter] = useState<'all' | 'available' | 'redeemed'>('all');

  // Plans modal
  const [showPlanModal, setShowPlanModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState<PlanConfig | null>(null);
  const [planForm, setPlanForm] = useState({
    planId: '',
    name: '',
    months: 1,
    price: 0,
    originalPrice: 0,
    discountPercent: 0,
    badge: '',
    order: 0,
    isActive: true,
  });

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

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/premium-plans/admin/all');
      setPlans(data);
    } catch (e) {
      console.error('Failed to fetch plans', e);
    }
    setLoading(false);
  }, []);

  useEffect(() => {
    fetchStats();
  }, [fetchStats]);

  useEffect(() => {
    if (tab === 'codes') fetchCodes();
    if (tab === 'users') fetchUsers();
    if (tab === 'plans') fetchPlans();
  }, [tab, fetchCodes, fetchUsers, fetchPlans]);

  const handleGenerate = async () => {
    try {
      const { data } = await api.post('/premium/admin/generate', {
        plan: genPlan,
        count: genCount,
        note: genNote,
        expiresInDays: genExpiry,
      });
      setGeneratedCodes(data.map((c: any) => c.code));
      fetchStats();
      if (tab === 'codes') fetchCodes();
    } catch (e: any) {
      const msg = e.response?.data?.message || e.message || 'Unknown error';
      alert('Failed to generate codes: ' + msg);
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

  const openNewPlan = () => {
    setEditingPlan(null);
    setPlanForm({ planId: '', name: '', months: 1, price: 0, originalPrice: 0, discountPercent: 0, badge: '', order: plans.length, isActive: true });
    setShowPlanModal(true);
  };

  const openEditPlan = (p: PlanConfig) => {
    setEditingPlan(p);
    setPlanForm({ planId: p.planId, name: p.name, months: p.months, price: p.price, originalPrice: p.originalPrice, discountPercent: p.discountPercent, badge: p.badge || '', order: p.order, isActive: p.isActive });
    setShowPlanModal(true);
  };

  const handleSavePlan = async () => {
    try {
      if (editingPlan) {
        const { planId, ...updateData } = planForm;
        await api.put(`/premium-plans/admin/${editingPlan._id}`, updateData);
      } else {
        await api.post('/premium-plans/admin', planForm);
      }
      setShowPlanModal(false);
      fetchPlans();
    } catch (e: any) {
      alert(e.response?.data?.message || 'Failed to save plan');
    }
  };

  const handleDeletePlan = async (id: string) => {
    if (!confirm('Delete this plan? This cannot be undone.')) return;
    try {
      await api.delete(`/premium-plans/admin/${id}`);
      fetchPlans();
    } catch (e) {
      alert('Failed to delete plan');
    }
  };

  const handleTogglePlan = async (p: PlanConfig) => {
    try {
      await api.put(`/premium-plans/admin/${p._id}`, { isActive: !p.isActive });
      fetchPlans();
    } catch (e) {
      alert('Failed to toggle plan');
    }
  };

  const handleSeedPlans = async () => {
    try {
      await api.post('/premium-plans/admin/seed');
      fetchPlans();
    } catch (e) {
      alert('Failed to seed plans');
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-white">Premium Management</h1>
        <button
          onClick={() => { setShowGenerate(true); setGeneratedCodes([]); setGenExpiry(90); }}
          className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg font-medium"
        >
          + Generate Codes
        </button>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-zinc-800 rounded-lg p-1 w-fit">
        {(['dashboard', 'codes', 'users', 'plans'] as const).map((t) => (
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

      {/* Plans */}
      {tab === 'plans' && (
        <div className="space-y-4">
          <div className="flex gap-2">
            <button onClick={openNewPlan} className="px-4 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg font-medium text-sm">+ Add Plan</button>
            <button onClick={handleSeedPlans} className="px-4 py-2 bg-zinc-700 hover:bg-zinc-600 text-white rounded-lg font-medium text-sm">Seed Defaults</button>
          </div>
          {loading ? (
            <div className="text-zinc-400">Loading...</div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
              {plans.map((p) => (
                <div key={p._id} className={`bg-zinc-800 rounded-xl p-5 border ${p.isActive ? 'border-zinc-700' : 'border-red-900/40 opacity-60'}`}>
                  <div className="flex items-center justify-between mb-3">
                    <span className="text-xs font-mono text-zinc-500">{p.planId}</span>
                    <div className="flex items-center gap-2">
                      <button onClick={() => handleTogglePlan(p)} className={`text-xs px-2 py-0.5 rounded ${p.isActive ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                        {p.isActive ? 'Active' : 'Disabled'}
                      </button>
                    </div>
                  </div>
                  <div className="text-white font-semibold text-lg mb-1">{p.name}</div>
                  {p.badge && <span className="inline-block px-2 py-0.5 text-xs rounded bg-amber-500/20 text-amber-400 mb-2">{p.badge}</span>}
                  <div className="flex items-baseline gap-2 mb-1">
                    <span className="text-2xl font-bold text-white">₹{p.price}</span>
                    <span className="text-sm text-zinc-500 line-through">₹{p.originalPrice}</span>
                    <span className="text-xs text-amber-400">{p.discountPercent}% off</span>
                  </div>
                  <div className="text-xs text-zinc-400 mb-3">{p.months} month{p.months > 1 ? 's' : ''} · Order: {p.order}</div>
                  <div className="flex gap-2">
                    <button onClick={() => openEditPlan(p)} className="flex-1 py-1.5 bg-zinc-700 hover:bg-zinc-600 text-white rounded text-xs">Edit</button>
                    <button onClick={() => handleDeletePlan(p._id)} className="py-1.5 px-3 bg-red-900/30 hover:bg-red-900/50 text-red-400 rounded text-xs">Delete</button>
                  </div>
                </div>
              ))}
              {plans.length === 0 && (
                <div className="col-span-full text-center text-zinc-500 py-8">No plans configured. Click "Seed Defaults" to create default plans.</div>
              )}
            </div>
          )}
        </div>
      )}

      {/* Plan Edit Modal */}
      {showPlanModal && (
        <div className="fixed inset-0 z-50 bg-black/60 flex items-center justify-center p-4">
          <div className="bg-zinc-800 rounded-xl p-6 w-full max-w-md space-y-4">
            <h2 className="text-xl font-bold text-white">{editingPlan ? 'Edit Plan' : 'New Plan'}</h2>
            {!editingPlan && (
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Plan ID (unique, e.g. "1m")</label>
                <input type="text" value={planForm.planId} onChange={(e) => setPlanForm({ ...planForm, planId: e.target.value })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" />
              </div>
            )}
            <div>
              <label className="block text-sm text-zinc-400 mb-1">Name</label>
              <input type="text" value={planForm.name} onChange={(e) => setPlanForm({ ...planForm, name: e.target.value })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" placeholder="e.g. 1 Month" />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Months</label>
                <input type="number" min={1} value={planForm.months} onChange={(e) => setPlanForm({ ...planForm, months: parseInt(e.target.value) || 1 })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Order</label>
                <input type="number" min={0} value={planForm.order} onChange={(e) => setPlanForm({ ...planForm, order: parseInt(e.target.value) || 0 })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Price (₹)</label>
                <input type="number" min={0} value={planForm.price} onChange={(e) => setPlanForm({ ...planForm, price: parseInt(e.target.value) || 0 })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Original Price (₹)</label>
                <input type="number" min={0} value={planForm.originalPrice} onChange={(e) => setPlanForm({ ...planForm, originalPrice: parseInt(e.target.value) || 0 })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Discount %</label>
                <input type="number" min={0} max={100} value={planForm.discountPercent} onChange={(e) => setPlanForm({ ...planForm, discountPercent: parseInt(e.target.value) || 0 })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" />
              </div>
              <div>
                <label className="block text-sm text-zinc-400 mb-1">Badge</label>
                <input type="text" value={planForm.badge} onChange={(e) => setPlanForm({ ...planForm, badge: e.target.value })} className="w-full bg-zinc-700 text-white rounded-lg px-3 py-2" placeholder="e.g. Most popular" />
              </div>
            </div>
            <label className="flex items-center gap-2 text-sm text-zinc-300 cursor-pointer">
              <input type="checkbox" checked={planForm.isActive} onChange={(e) => setPlanForm({ ...planForm, isActive: e.target.checked })} className="rounded" />
              Active (visible in app)
            </label>
            <div className="flex gap-3">
              <button onClick={() => setShowPlanModal(false)} className="flex-1 py-2 bg-zinc-700 hover:bg-zinc-600 text-zinc-300 rounded-lg">Cancel</button>
              <button onClick={handleSavePlan} className="flex-1 py-2 bg-amber-600 hover:bg-amber-700 text-white rounded-lg font-medium">Save</button>
            </div>
          </div>
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
                <div>
                  <label className="block text-sm text-zinc-400 mb-1">Code Expiry (days until unused code expires)</label>
                  <input
                    type="number"
                    min={7}
                    max={365}
                    value={genExpiry}
                    onChange={(e) => setGenExpiry(parseInt(e.target.value) || 90)}
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
