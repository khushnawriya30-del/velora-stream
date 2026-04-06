import { useEffect, useRef, useState } from 'react';
import api from '../lib/api';
import {
  Bot,
  CreditCard,
  CheckCircle,
  XCircle,
  Clock,
  DollarSign,
  RefreshCw,
  Copy,
  Settings,
  Save,
  Power,
  Upload,
} from 'lucide-react';

interface TelegramPayment {
  _id: string;
  telegramUserId: string;
  telegramUsername: string;
  telegramFirstName: string;
  plan: string;
  amount: number;
  utrId: string;
  status: string;
  activationCode: string | null;
  isCodeRedeemed: boolean;
  verifiedAt: string | null;
  rejectedAt: string | null;
  rejectionReason: string | null;
  createdAt: string;
}

interface PaymentStats {
  total: number;
  verified: number;
  pending: number;
  rejected: number;
  totalRevenue: number;
}

interface BotStatus {
  isRunning: boolean;
  botUsername: string | null;
}

interface TelegramSettings {
  telegramBotToken: string;
  telegramBotUsername: string;
  paymentQrCodeUrl: string;
  paymentUpiId: string;
  paymentInstructions: string;
}

type Tab = 'dashboard' | 'payments' | 'settings';

export default function TelegramPaymentsPage() {
  const [tab, setTab] = useState<Tab>('dashboard');
  const [payments, setPayments] = useState<TelegramPayment[]>([]);
  const [stats, setStats] = useState<PaymentStats | null>(null);
  const [botStatus, setBotStatus] = useState<BotStatus | null>(null);
  const [loading, setLoading] = useState(false);
  const [filter, setFilter] = useState('all');
  const [settings, setSettings] = useState<TelegramSettings>({
    telegramBotToken: '',
    telegramBotUsername: '',
    paymentQrCodeUrl: '',
    paymentUpiId: '',
    paymentInstructions: '',
  });
  const [savingSettings, setSavingSettings] = useState(false);
  const [restartingBot, setRestartingBot] = useState(false);
  const [uploadingQr, setUploadingQr] = useState(false);
  const qrFileRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    fetchStats();
    fetchBotStatus();
  }, []);

  useEffect(() => {
    if (tab === 'payments') fetchPayments();
    if (tab === 'settings') fetchSettings();
  }, [tab, filter]);

  // ── Fetch Functions ──

  async function fetchStats() {
    try {
      const { data } = await api.get('/telegram-bot/admin/stats');
      setStats(data);
    } catch {}
  }

  async function fetchBotStatus() {
    try {
      const { data } = await api.get('/telegram-bot/admin/status');
      setBotStatus(data);
    } catch {}
  }

  async function fetchPayments() {
    setLoading(true);
    try {
      const params: any = { limit: 100 };
      if (filter !== 'all') params.status = filter;
      const { data } = await api.get('/telegram-bot/admin/payments', { params });
      setPayments(data.payments);
    } catch {} finally {
      setLoading(false);
    }
  }

  async function fetchSettings() {
    try {
      const { data } = await api.get('/settings');
      setSettings({
        telegramBotToken: data.telegramBotToken || '',
        telegramBotUsername: data.telegramBotUsername || '',
        paymentQrCodeUrl: data.paymentQrCodeUrl || '',
        paymentUpiId: data.paymentUpiId || '',
        paymentInstructions: data.paymentInstructions || '',
      });
    } catch {}
  }

  // ── Actions ──

  async function handleReject(id: string) {
    const reason = prompt('Rejection reason (optional):');
    if (reason === null) return;
    try {
      await api.post(`/telegram-bot/admin/reject/${id}`, { reason });
      fetchPayments();
      fetchStats();
    } catch {}
  }

  async function handleManualVerify(id: string) {
    if (!confirm('Manually verify this payment and generate activation code?')) return;
    try {
      const { data } = await api.post(`/telegram-bot/admin/verify/${id}`);
      alert(`Code generated: ${data.activationCode}`);
      fetchPayments();
      fetchStats();
    } catch {}
  }

  async function handleSaveSettings() {
    setSavingSettings(true);
    try {
      // Exclude QR from save payload (it's uploaded separately via upload-qr endpoint)
      const { paymentQrCodeUrl, ...saveData } = settings;
      await api.put('/settings', saveData);
      alert('Settings saved! Restart the bot to apply token changes.');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Unknown error';
      alert('Failed to save settings: ' + msg);
    } finally {
      setSavingSettings(false);
    }
  }

  async function handleRestartBot() {
    setRestartingBot(true);
    try {
      await api.post('/telegram-bot/admin/restart');
      await fetchBotStatus();
      alert('Bot restarted successfully');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Unknown error';
      alert('Failed to restart bot: ' + msg);
    } finally {
      setRestartingBot(false);
    }
  }

  function copyText(text: string) {
    navigator.clipboard.writeText(text);
  }

  async function handleQrUpload(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingQr(true);
    try {
      const formData = new FormData();
      formData.append('file', file);
      const { data } = await api.post('/settings/upload-qr', formData, {
        headers: { 'Content-Type': undefined },
      });
      setSettings((s) => ({ ...s, paymentQrCodeUrl: data.url }));
      alert('QR code uploaded successfully!');
    } catch (err: any) {
      const msg = err?.response?.data?.message || err?.message || 'Unknown error';
      alert('Failed to upload QR code: ' + msg);
    } finally {
      setUploadingQr(false);
      if (qrFileRef.current) qrFileRef.current.value = '';
    }
  }

  // ── Render ──

  const tabs: { key: Tab; label: string; icon: any }[] = [
    { key: 'dashboard', label: 'Dashboard', icon: Bot },
    { key: 'payments', label: 'Payments', icon: CreditCard },
    { key: 'settings', label: 'Bot Settings', icon: Settings },
  ];

  const statusColors: Record<string, string> = {
    verified: 'text-green-400 bg-green-400/10',
    pending: 'text-yellow-400 bg-yellow-400/10',
    rejected: 'text-red-400 bg-red-400/10',
    expired: 'text-gray-400 bg-gray-400/10',
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold flex items-center gap-2">
            <Bot className="text-blue-400" size={26} /> Telegram Premium Bot
          </h1>
          <p className="text-text-secondary text-sm mt-1">
            Manage Telegram bot payments, UTR verifications, and settings
          </p>
        </div>
        <div className="flex items-center gap-2">
          <span
            className={`px-3 py-1 rounded-full text-xs font-medium ${
              botStatus?.isRunning
                ? 'bg-green-400/10 text-green-400'
                : 'bg-red-400/10 text-red-400'
            }`}
          >
            {botStatus?.isRunning ? '● Online' : '● Offline'}
          </span>
          {botStatus?.botUsername && (
            <a
              href={`https://t.me/${botStatus.botUsername}`}
              target="_blank"
              rel="noreferrer"
              className="text-xs text-blue-400 hover:underline"
            >
              @{botStatus.botUsername}
            </a>
          )}
        </div>
      </div>

      {/* Tabs */}
      <div className="flex gap-1 bg-surface rounded-lg p-1 w-fit">
        {tabs.map((t) => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            className={`flex items-center gap-2 px-4 py-2 rounded-md text-sm font-medium transition-colors ${
              tab === t.key
                ? 'bg-gold/10 text-gold'
                : 'text-text-secondary hover:text-text-primary'
            }`}
          >
            <t.icon size={16} />
            {t.label}
          </button>
        ))}
      </div>

      {/* ═══ Dashboard Tab ═══ */}
      {tab === 'dashboard' && (
        <div className="space-y-6">
          {/* Stat Cards */}
          <div className="grid grid-cols-2 lg:grid-cols-5 gap-4">
            {[
              {
                label: 'Total Payments',
                value: stats?.total ?? 0,
                icon: CreditCard,
                color: 'text-blue-400',
              },
              {
                label: 'Verified',
                value: stats?.verified ?? 0,
                icon: CheckCircle,
                color: 'text-green-400',
              },
              {
                label: 'Pending',
                value: stats?.pending ?? 0,
                icon: Clock,
                color: 'text-yellow-400',
              },
              {
                label: 'Rejected',
                value: stats?.rejected ?? 0,
                icon: XCircle,
                color: 'text-red-400',
              },
              {
                label: 'Revenue',
                value: `₹${stats?.totalRevenue ?? 0}`,
                icon: DollarSign,
                color: 'text-emerald-400',
              },
            ].map((s) => (
              <div
                key={s.label}
                className="bg-surface rounded-xl p-4 border border-border"
              >
                <div className="flex items-center gap-2 mb-2">
                  <s.icon size={18} className={s.color} />
                  <span className="text-xs text-text-muted">{s.label}</span>
                </div>
                <p className="text-2xl font-bold">{s.value}</p>
              </div>
            ))}
          </div>

          {/* Bot Info */}
          <div className="bg-surface rounded-xl p-6 border border-border">
            <h3 className="font-semibold mb-3">Bot Information</h3>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
              <div>
                <span className="text-text-muted">Status:</span>
                <span
                  className={`ml-2 font-medium ${
                    botStatus?.isRunning ? 'text-green-400' : 'text-red-400'
                  }`}
                >
                  {botStatus?.isRunning ? 'Running' : 'Stopped'}
                </span>
              </div>
              <div>
                <span className="text-text-muted">Username:</span>
                <span className="ml-2 font-medium">
                  {botStatus?.botUsername ? `@${botStatus.botUsername}` : 'N/A'}
                </span>
              </div>
              <div>
                <span className="text-text-muted">Bot Link:</span>
                {botStatus?.botUsername ? (
                  <a
                    href={`https://t.me/${botStatus.botUsername}`}
                    target="_blank"
                    rel="noreferrer"
                    className="ml-2 text-blue-400 hover:underline"
                  >
                    t.me/{botStatus.botUsername}
                  </a>
                ) : (
                  <span className="ml-2 text-text-muted">N/A</span>
                )}
              </div>
            </div>
            <button
              onClick={handleRestartBot}
              disabled={restartingBot}
              className="mt-4 flex items-center gap-2 px-4 py-2 bg-blue-500/20 text-blue-400 rounded-lg text-sm font-medium hover:bg-blue-500/30 disabled:opacity-50"
            >
              <RefreshCw size={14} className={restartingBot ? 'animate-spin' : ''} />
              {restartingBot ? 'Restarting...' : 'Restart Bot'}
            </button>
          </div>
        </div>
      )}

      {/* ═══ Payments Tab ═══ */}
      {tab === 'payments' && (
        <div className="space-y-4">
          {/* Filters */}
          <div className="flex gap-2">
            {['all', 'verified', 'pending', 'rejected'].map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium capitalize transition-colors ${
                  filter === f
                    ? 'bg-gold/10 text-gold'
                    : 'bg-surface text-text-secondary hover:text-text-primary'
                }`}
              >
                {f}
              </button>
            ))}
            <button
              onClick={fetchPayments}
              className="ml-auto flex items-center gap-1 px-3 py-1.5 bg-surface rounded-lg text-xs text-text-secondary hover:text-text-primary"
            >
              <RefreshCw size={12} /> Refresh
            </button>
          </div>

          {/* Table */}
          <div className="bg-surface rounded-xl border border-border overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="border-b border-border text-text-muted text-left">
                  <th className="p-3">User</th>
                  <th className="p-3">Plan</th>
                  <th className="p-3">Amount</th>
                  <th className="p-3">UTR ID</th>
                  <th className="p-3">Code</th>
                  <th className="p-3">Status</th>
                  <th className="p-3">Date</th>
                  <th className="p-3">Actions</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-text-muted">
                      Loading...
                    </td>
                  </tr>
                ) : payments.length === 0 ? (
                  <tr>
                    <td colSpan={8} className="p-8 text-center text-text-muted">
                      No payments found
                    </td>
                  </tr>
                ) : (
                  payments.map((p) => (
                    <tr key={p._id} className="border-b border-border/50 hover:bg-surface-light/50">
                      <td className="p-3">
                        <div className="font-medium">{p.telegramFirstName || 'Unknown'}</div>
                        <div className="text-xs text-text-muted">
                          {p.telegramUsername ? `@${p.telegramUsername}` : p.telegramUserId}
                        </div>
                      </td>
                      <td className="p-3 capitalize">{p.plan}</td>
                      <td className="p-3 font-medium">₹{p.amount}</td>
                      <td className="p-3">
                        <div className="flex items-center gap-1">
                          <code className="text-xs bg-surface-light px-1.5 py-0.5 rounded">
                            {p.utrId}
                          </code>
                          <button
                            onClick={() => copyText(p.utrId)}
                            className="text-text-muted hover:text-text-primary"
                          >
                            <Copy size={12} />
                          </button>
                        </div>
                      </td>
                      <td className="p-3">
                        {p.activationCode ? (
                          <div className="flex items-center gap-1">
                            <code className="text-xs bg-surface-light px-1.5 py-0.5 rounded text-gold">
                              {p.activationCode}
                            </code>
                            <button
                              onClick={() => copyText(p.activationCode!)}
                              className="text-text-muted hover:text-text-primary"
                            >
                              <Copy size={12} />
                            </button>
                          </div>
                        ) : (
                          <span className="text-text-muted text-xs">—</span>
                        )}
                      </td>
                      <td className="p-3">
                        <span
                          className={`px-2 py-0.5 rounded-full text-xs font-medium capitalize ${
                            statusColors[p.status] || ''
                          }`}
                        >
                          {p.status}
                        </span>
                      </td>
                      <td className="p-3 text-xs text-text-muted">
                        {new Date(p.createdAt).toLocaleDateString('en-IN')}
                      </td>
                      <td className="p-3">
                        <div className="flex items-center gap-1">
                          {p.status === 'pending' && (
                            <>
                              <button
                                onClick={() => handleManualVerify(p._id)}
                                className="p-1.5 rounded bg-green-500/10 text-green-400 hover:bg-green-500/20"
                                title="Verify"
                              >
                                <CheckCircle size={14} />
                              </button>
                              <button
                                onClick={() => handleReject(p._id)}
                                className="p-1.5 rounded bg-red-500/10 text-red-400 hover:bg-red-500/20"
                                title="Reject"
                              >
                                <XCircle size={14} />
                              </button>
                            </>
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
      )}

      {/* ═══ Settings Tab ═══ */}
      {tab === 'settings' && (
        <div className="space-y-6 max-w-2xl">
          <div className="bg-surface rounded-xl p-6 border border-border space-y-5">
            <h3 className="font-semibold text-lg">Telegram Bot Configuration</h3>

            {/* Bot Token */}
            <div>
              <label className="block text-sm text-text-muted mb-1.5">
                Bot Token <span className="text-red-400">*</span>
              </label>
              <input
                type="password"
                value={settings.telegramBotToken}
                onChange={(e) =>
                  setSettings((s) => ({ ...s, telegramBotToken: e.target.value }))
                }
                placeholder="123456789:ABCdef..."
                className="w-full px-3 py-2 bg-surface-light rounded-lg border border-border text-sm focus:outline-none focus:border-gold"
              />
              <p className="text-xs text-text-muted mt-1">
                Get from{' '}
                <a
                  href="https://t.me/BotFather"
                  target="_blank"
                  rel="noreferrer"
                  className="text-blue-400 hover:underline"
                >
                  @BotFather
                </a>
              </p>
            </div>

            {/* Bot Username */}
            <div>
              <label className="block text-sm text-text-muted mb-1.5">
                Bot Username
              </label>
              <input
                type="text"
                value={settings.telegramBotUsername}
                onChange={(e) =>
                  setSettings((s) => ({ ...s, telegramBotUsername: e.target.value }))
                }
                placeholder="VeloraPremiumBot"
                className="w-full px-3 py-2 bg-surface-light rounded-lg border border-border text-sm focus:outline-none focus:border-gold"
              />
            </div>

            {/* UPI ID */}
            <div>
              <label className="block text-sm text-text-muted mb-1.5">
                UPI ID
              </label>
              <input
                type="text"
                value={settings.paymentUpiId}
                onChange={(e) =>
                  setSettings((s) => ({ ...s, paymentUpiId: e.target.value }))
                }
                placeholder="yourname@upi"
                className="w-full px-3 py-2 bg-surface-light rounded-lg border border-border text-sm focus:outline-none focus:border-gold"
              />
            </div>

            {/* QR Code Upload */}
            <div>
              <label className="block text-sm text-text-muted mb-1.5">
                Payment QR Code
              </label>
              <input
                ref={qrFileRef}
                type="file"
                accept="image/*"
                onChange={handleQrUpload}
                className="hidden"
              />
              <button
                onClick={() => qrFileRef.current?.click()}
                disabled={uploadingQr}
                className="flex items-center gap-2 px-4 py-2.5 bg-surface-light rounded-lg border border-border text-sm hover:border-gold transition-colors disabled:opacity-50"
              >
                <Upload size={16} />
                {uploadingQr ? 'Uploading...' : settings.paymentQrCodeUrl ? 'Change QR Code' : 'Upload QR Code'}
              </button>
              {settings.paymentQrCodeUrl && (
                <div className="mt-3 p-3 bg-white rounded-lg inline-block">
                  <img
                    src={settings.paymentQrCodeUrl}
                    alt="QR Preview"
                    className="w-40 h-40 object-contain"
                    onError={(e) => {
                      (e.target as HTMLImageElement).style.display = 'none';
                    }}
                  />
                </div>
              )}
            </div>

            {/* Payment Instructions */}
            <div>
              <label className="block text-sm text-text-muted mb-1.5">
                Payment Instructions
              </label>
              <textarea
                value={settings.paymentInstructions}
                onChange={(e) =>
                  setSettings((s) => ({ ...s, paymentInstructions: e.target.value }))
                }
                placeholder="Please complete the payment using the QR code above or the UPI ID. After payment, enter your UTR ID below."
                rows={3}
                className="w-full px-3 py-2 bg-surface-light rounded-lg border border-border text-sm focus:outline-none focus:border-gold resize-none"
              />
            </div>

            {/* Actions */}
            <div className="flex items-center gap-3 pt-2">
              <button
                onClick={handleSaveSettings}
                disabled={savingSettings}
                className="flex items-center gap-2 px-5 py-2.5 bg-gold text-black rounded-lg text-sm font-semibold hover:bg-gold/90 disabled:opacity-50"
              >
                <Save size={16} />
                {savingSettings ? 'Saving...' : 'Save Settings'}
              </button>
              <button
                onClick={handleRestartBot}
                disabled={restartingBot}
                className="flex items-center gap-2 px-4 py-2.5 bg-blue-500/20 text-blue-400 rounded-lg text-sm font-medium hover:bg-blue-500/30 disabled:opacity-50"
              >
                <Power size={14} />
                {restartingBot ? 'Restarting...' : 'Restart Bot'}
              </button>
            </div>
          </div>

          {/* Quick Setup Guide */}
          <div className="bg-surface rounded-xl p-6 border border-border">
            <h3 className="font-semibold mb-3">Quick Setup Guide</h3>
            <ol className="text-sm text-text-secondary space-y-2 list-decimal list-inside">
              <li>
                Open Telegram and search for{' '}
                <a
                  href="https://t.me/BotFather"
                  target="_blank"
                  rel="noreferrer"
                  className="text-blue-400"
                >
                  @BotFather
                </a>
              </li>
              <li>
                Send <code className="bg-surface-light px-1.5 py-0.5 rounded text-xs">/newbot</code> and
                follow the prompts
              </li>
              <li>Copy the bot token and paste it above</li>
              <li>Set your bot username (without @)</li>
              <li>Upload your UPI QR code image using the upload button above</li>
              <li>Enter your UPI ID</li>
              <li>Save settings and restart the bot</li>
              <li>
                Share the bot link{' '}
                <code className="bg-surface-light px-1.5 py-0.5 rounded text-xs">
                  https://t.me/YourBotUsername
                </code>{' '}
                with users
              </li>
            </ol>
          </div>
        </div>
      )}
    </div>
  );
}
