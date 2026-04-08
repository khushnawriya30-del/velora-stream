import { useState, useEffect, useCallback } from 'react';
import api from '../lib/api';

interface PremiumOffer {
  _id: string;
  title: string;
  subtitle: string;
  description: string;
  bannerText: string;
  originalPrice: number;
  discountPrice: number;
  discountPercent: number;
  badgeText: string;
  planId: string;
  durationMonths: number;
  targetUserType: string;
  offerType: string;
  isVisible: boolean;
  showAsPopup: boolean;
  order: number;
  startDate?: string;
  endDate?: string;
  createdAt: string;
}

interface InviteSettings {
  _id: string;
  targetAmount: number;
  defaultBalance: number;
  rewardPerInvite: number;
  earnWindowDays: number;
  isActive: boolean;
}

const emptyOffer = {
  title: '',
  subtitle: '',
  description: '',
  bannerText: '',
  originalPrice: 159,
  discountPrice: 99,
  badgeText: '',
  planId: '1m',
  durationMonths: 1,
  targetUserType: 'non_premium',
  offerType: 'subscription',
  isVisible: true,
  showAsPopup: false,
  order: 0,
  startDate: '',
  endDate: '',
};

export default function PremiumOffersPage() {
  const [tab, setTab] = useState<'offers' | 'invite'>('offers');
  const [userTypeFilter, setUserTypeFilter] = useState<'non_premium' | 'premium'>('non_premium');
  const [offers, setOffers] = useState<PremiumOffer[]>([]);
  const [inviteSettings, setInviteSettings] = useState<InviteSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [editId, setEditId] = useState<string | null>(null);
  const [form, setForm] = useState(emptyOffer);
  const [saving, setSaving] = useState(false);

  const fetchOffers = useCallback(async () => {
    try {
      const { data } = await api.get('/premium-offers/admin/all');
      setOffers(data);
    } catch {}
  }, []);

  const fetchInvite = useCallback(async () => {
    try {
      const { data } = await api.get('/premium-offers/invite-settings');
      setInviteSettings(data);
    } catch {}
  }, []);

  useEffect(() => {
    setLoading(true);
    Promise.all([fetchOffers(), fetchInvite()]).finally(() => setLoading(false));
  }, [fetchOffers, fetchInvite]);

  const handleSave = async () => {
    setSaving(true);
    try {
      if (editId) {
        await api.put(`/premium-offers/admin/${editId}`, form);
      } else {
        await api.post('/premium-offers/admin', form);
      }
      await fetchOffers();
      setShowForm(false);
      setEditId(null);
      setForm(emptyOffer);
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Error saving offer');
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this offer?')) return;
    try {
      await api.delete(`/premium-offers/admin/${id}`);
      await fetchOffers();
    } catch {}
  };

  const handleEdit = (o: PremiumOffer) => {
    setUserTypeFilter(o.targetUserType === 'premium' ? 'premium' : 'non_premium');
    setForm({
      title: o.title,
      subtitle: o.subtitle,
      description: o.description,
      bannerText: o.bannerText || '',
      originalPrice: o.originalPrice,
      discountPrice: o.discountPrice,
      badgeText: o.badgeText,
      planId: o.planId,
      durationMonths: o.durationMonths,
      targetUserType: o.targetUserType,
      offerType: o.offerType,
      isVisible: o.isVisible,
      showAsPopup: o.showAsPopup,
      order: o.order,
      startDate: o.startDate?.slice(0, 10) || '',
      endDate: o.endDate?.slice(0, 10) || '',
    });
    setEditId(o._id);
    setShowForm(true);
  };

  const handleSaveInvite = async () => {
    setSaving(true);
    try {
      const { data } = await api.put('/premium-offers/admin/invite-settings', inviteSettings);
      setInviteSettings(data);
    } catch (e: any) {
      alert(e?.response?.data?.message || 'Error saving');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-gold" />
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-display text-gold tracking-wide">Premium Offers & Invite</h1>
      </div>

      {/* Tabs */}
      <div className="flex gap-2">
        <button
          onClick={() => setTab('offers')}
          className={`px-4 py-2 rounded-lg font-medium text-sm transition-colors ${tab === 'offers' ? 'bg-gold/20 text-gold' : 'bg-surface text-text-secondary hover:bg-surface-light'}`}
        >
          Premium Offers
        </button>
        <button
          onClick={() => setTab('invite')}
          className={`px-4 py-2 rounded-lg font-medium text-sm transition-colors ${tab === 'invite' ? 'bg-gold/20 text-gold' : 'bg-surface text-text-secondary hover:bg-surface-light'}`}
        >
          Invite Settings
        </button>
      </div>

      {/* ═══ OFFERS TAB ═══ */}
      {tab === 'offers' && (
        <div className="space-y-4">
          {/* Sub-tabs: Non-Subscriber / Subscriber */}
          <div className="flex gap-2">
            <button
              onClick={() => setUserTypeFilter('non_premium')}
              className={`px-4 py-2 rounded-lg font-medium text-sm transition-colors ${userTypeFilter === 'non_premium' ? 'bg-gold/20 text-gold border border-gold/40' : 'bg-surface text-text-secondary hover:bg-surface-light'}`}
            >
              🔓 Non-Subscriber Offers
            </button>
            <button
              onClick={() => setUserTypeFilter('premium')}
              className={`px-4 py-2 rounded-lg font-medium text-sm transition-colors ${userTypeFilter === 'premium' ? 'bg-gold/20 text-gold border border-gold/40' : 'bg-surface text-text-secondary hover:bg-surface-light'}`}
            >
              👑 Subscriber Offers
            </button>
          </div>

          <div className="text-xs text-text-muted bg-surface/50 rounded-lg px-3 py-2">
            {userTypeFilter === 'non_premium'
              ? '📋 These offers appear on the PNG banner in Me section for non-subscribers. Set Banner Text + price to display dynamically.'
              : '📋 These offers appear for existing subscribers (renewal / upgrade offers).'}
          </div>

          <button
            onClick={() => { setForm({ ...emptyOffer, targetUserType: userTypeFilter }); setEditId(null); setShowForm(true); }}
            className="px-4 py-2 bg-gold text-black font-semibold rounded-lg hover:bg-gold/90 transition"
          >
            + Create {userTypeFilter === 'non_premium' ? 'Non-Subscriber' : 'Subscriber'} Offer
          </button>

          {/* Form Modal */}
          {showForm && (
            <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4" onClick={() => setShowForm(false)}>
              <div className="bg-surface rounded-xl p-6 w-full max-w-2xl max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
                <h2 className="text-lg font-bold text-gold mb-4">{editId ? 'Edit Offer' : 'Create Offer'}</h2>
                <div className="space-y-4">
                  {/* Banner preview */}
                  <div className="bg-[#1e1e2e] rounded-xl p-4 border border-gold/20">
                    <p className="text-xs text-text-muted mb-2">📱 Banner Preview</p>
                    <p className="text-white text-sm font-semibold">{form.bannerText || 'Your message here'}</p>
                    <div className="flex items-baseline gap-2 mt-1">
                      <span className="text-[#F3E5AB] text-3xl font-black">₹{form.discountPrice}</span>
                      <span className="text-[#F3E5AB] text-base font-bold">
                        {form.durationMonths === 1 ? '/ month' : form.durationMonths === 3 ? '/ 3 months' : form.durationMonths === 6 ? '/ 6 months' : '/ year'}
                      </span>
                    </div>
                  </div>

                  {/* Field 1: Message */}
                  <div>
                    <label className="text-sm font-semibold text-gold">📝 Banner Message</label>
                    <p className="text-xs text-text-muted mb-1">Text shown above the price on the banner</p>
                    <input
                      className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                      value={form.bannerText}
                      onChange={(e) => setForm({ ...form, bannerText: e.target.value, title: e.target.value || 'Offer' })}
                      placeholder="e.g. Subscribe to Velora Premium Only"
                    />
                  </div>

                  {/* Field 2: Amount */}
                  <div>
                    <label className="text-sm font-semibold text-gold">💰 Price (₹)</label>
                    <p className="text-xs text-text-muted mb-1">Amount shown on banner in gold</p>
                    <input
                      type="number"
                      className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                      value={form.discountPrice}
                      onChange={(e) => setForm({ ...form, discountPrice: +e.target.value, originalPrice: +e.target.value })}
                    />
                  </div>

                  {/* Field 3: Duration */}
                  <div>
                    <label className="text-sm font-semibold text-gold">📅 Duration</label>
                    <p className="text-xs text-text-muted mb-1">Shown after the price</p>
                    <select
                      className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                      value={form.durationMonths}
                      onChange={(e) => {
                        const m = +e.target.value;
                        const planId = m === 1 ? '1m' : m === 3 ? '3m' : m === 6 ? '6m' : '12m';
                        setForm({ ...form, durationMonths: m, planId });
                      }}
                    >
                      <option value={1}>/ month</option>
                      <option value={3}>/ 3 months</option>
                      <option value={6}>/ 6 months</option>
                      <option value={12}>/ year</option>
                    </select>
                  </div>
                </div>
                <div className="flex gap-3 mt-6">
                  <button onClick={handleSave} disabled={saving} className="px-6 py-2 bg-gold text-black font-semibold rounded-lg hover:bg-gold/90 transition disabled:opacity-50">
                    {saving ? 'Saving...' : editId ? 'Update' : 'Create'}
                  </button>
                  <button onClick={() => setShowForm(false)} className="px-6 py-2 bg-surface-light text-text-secondary rounded-lg hover:bg-border transition">
                    Cancel
                  </button>
                </div>
              </div>
            </div>
          )}

          {/* Offers List */}
          {offers.filter(o => o.targetUserType === userTypeFilter || o.targetUserType === 'all').length === 0 ? (
            <div className="text-center py-12 text-text-muted">No {userTypeFilter === 'non_premium' ? 'non-subscriber' : 'subscriber'} offers created yet</div>
          ) : (
            <div className="grid gap-4">
              {offers.filter(o => o.targetUserType === userTypeFilter || o.targetUserType === 'all').map((o) => (
                <div key={o._id} className="bg-surface rounded-xl p-5 border border-border">
                  <div className="flex items-start justify-between">
                    <div className="flex-1">
                      <div className="flex items-center gap-2 mb-1">
                        <h3 className="font-bold text-text-primary">{o.title}</h3>
                        {o.badgeText && (
                          <span className="px-2 py-0.5 bg-gold/20 text-gold text-xs font-semibold rounded">{o.badgeText}</span>
                        )}
                        <span className={`px-2 py-0.5 text-xs font-medium rounded ${o.isVisible ? 'bg-green-500/20 text-green-400' : 'bg-red-500/20 text-red-400'}`}>
                          {o.isVisible ? 'Visible' : 'Hidden'}
                        </span>
                        {o.showAsPopup && (
                          <span className="px-2 py-0.5 bg-blue-500/20 text-blue-400 text-xs font-medium rounded">Popup</span>
                        )}
                      </div>
                      {o.subtitle && <p className="text-sm text-text-secondary">{o.subtitle}</p>}
                      <div className="flex items-center gap-4 mt-2">
                        <span className="text-text-muted line-through text-sm">₹{o.originalPrice}</span>
                        <span className="text-gold font-bold text-lg">₹{o.discountPrice}</span>
                        <span className="bg-gold/10 text-gold px-2 py-0.5 rounded text-sm font-semibold">{o.discountPercent}% OFF</span>
                      </div>
                      <div className="flex gap-3 mt-2 text-xs text-text-muted">
                        <span>Target: {o.targetUserType === 'non_premium' ? 'Non-Premium' : o.targetUserType === 'premium' ? 'Premium' : 'All'}</span>
                        <span>Type: {o.offerType}</span>
                        <span>Plan: {o.planId} ({o.durationMonths}m)</span>
                      </div>
                    </div>
                    <div className="flex gap-2">
                      <button onClick={() => handleEdit(o)} className="px-3 py-1.5 bg-gold/10 text-gold text-sm rounded-lg hover:bg-gold/20 transition">Edit</button>
                      <button onClick={() => handleDelete(o._id)} className="px-3 py-1.5 bg-red-500/10 text-red-400 text-sm rounded-lg hover:bg-red-500/20 transition">Delete</button>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ═══ INVITE TAB ═══ */}
      {tab === 'invite' && inviteSettings && (
        <div className="bg-surface rounded-xl p-6 border border-border max-w-lg">
          <h2 className="text-lg font-bold text-gold mb-4">Invite & Referral Settings</h2>
          <div className="space-y-4">
            <div>
              <label className="text-xs text-text-muted">Target Amount (₹) — Withdraw threshold</label>
              <input
                type="number"
                className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                value={inviteSettings.targetAmount}
                onChange={(e) => setInviteSettings({ ...inviteSettings, targetAmount: +e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-text-muted">Default Balance (₹) — New user starting balance</label>
              <input
                type="number"
                className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                value={inviteSettings.defaultBalance}
                onChange={(e) => setInviteSettings({ ...inviteSettings, defaultBalance: +e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-text-muted">Reward Per Invite (₹)</label>
              <input
                type="number"
                className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                value={inviteSettings.rewardPerInvite}
                onChange={(e) => setInviteSettings({ ...inviteSettings, rewardPerInvite: +e.target.value })}
              />
            </div>
            <div>
              <label className="text-xs text-text-muted">Earn Window (days)</label>
              <input
                type="number"
                className="w-full mt-1 px-3 py-2 bg-background border border-border rounded-lg text-sm text-text-primary"
                value={inviteSettings.earnWindowDays}
                onChange={(e) => setInviteSettings({ ...inviteSettings, earnWindowDays: +e.target.value })}
              />
            </div>
            <label className="flex items-center gap-2 cursor-pointer">
              <input type="checkbox" checked={inviteSettings.isActive} onChange={(e) => setInviteSettings({ ...inviteSettings, isActive: e.target.checked })} className="accent-gold" />
              <span className="text-sm text-text-primary">Active</span>
            </label>

            {/* Preview */}
            <div className="mt-4 p-4 bg-gold/5 rounded-lg border border-gold/20">
              <p className="text-sm text-text-secondary mb-1">Preview</p>
              <p className="text-text-primary">
                New users start with <span className="text-gold font-bold">₹{inviteSettings.defaultBalance}</span>
              </p>
              <p className="text-text-primary">
                Need <span className="text-gold font-bold">₹{inviteSettings.targetAmount - inviteSettings.defaultBalance}</span> more ({Math.ceil((inviteSettings.targetAmount - inviteSettings.defaultBalance) / inviteSettings.rewardPerInvite)} invites) to withdraw <span className="text-gold font-bold">₹{inviteSettings.targetAmount}</span>
              </p>
              <p className="text-text-primary">
                Progress: <span className="text-gold font-bold">{inviteSettings.defaultBalance} / {inviteSettings.targetAmount}</span> = {Math.round((inviteSettings.defaultBalance / inviteSettings.targetAmount) * 100)}%
              </p>
            </div>

            <button onClick={handleSaveInvite} disabled={saving} className="px-6 py-2 bg-gold text-black font-semibold rounded-lg hover:bg-gold/90 transition disabled:opacity-50">
              {saving ? 'Saving...' : 'Save Settings'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
