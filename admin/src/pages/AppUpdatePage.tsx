import { useState, useEffect } from 'react';
import api from '../lib/api';

interface AppVersion {
  versionCode: number;
  versionName: string;
  forceUpdate: boolean;
  apkUrl: string;
  releaseNotes: string;
}

export default function AppUpdatePage() {
  const [form, setForm] = useState<AppVersion>({
    versionCode: 1,
    versionName: '1.0.0',
    forceUpdate: false,
    apkUrl: '',
    releaseNotes: '',
  });
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    api.get('/app-version').then((res) => {
      setForm(res.data);
    }).catch(() => {}).finally(() => setLoading(false));
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError('');
    setSaved(false);
    try {
      await api.put('/app-version', form);
      setSaved(true);
      setTimeout(() => setSaved(false), 3000);
    } catch {
      setError('Failed to save. Check your permissions.');
    } finally {
      setSaving(false);
    }
  };

  if (loading) return <div className="p-8 text-text-secondary">Loading...</div>;

  return (
    <div className="p-6 max-w-2xl">
      <h1 className="text-2xl font-bold text-text-primary mb-2">App Update Manager</h1>
      <p className="text-text-secondary mb-6 text-sm">
        Set the current APK version. Users on older versions will see an update popup inside the app.
      </p>

      <form onSubmit={handleSubmit} className="bg-surface border border-border rounded-xl p-6 space-y-5">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Version Code</label>
            <input
              type="number"
              value={form.versionCode}
              onChange={(e) => setForm({ ...form, versionCode: parseInt(e.target.value) || 1 })}
              className="w-full bg-background border border-border rounded-lg px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-accent"
              min={1}
              required
            />
            <p className="text-xs text-text-secondary mt-1">Increment this for each new release (e.g. 1, 2, 3...)</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-text-secondary mb-1">Version Name</label>
            <input
              type="text"
              value={form.versionName}
              onChange={(e) => setForm({ ...form, versionName: e.target.value })}
              className="w-full bg-background border border-border rounded-lg px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-accent"
              placeholder="e.g. 1.2.0"
              required
            />
          </div>
        </div>

        <div>
          <label className="block text-sm font-medium text-text-secondary mb-1">APK Download URL</label>
          <input
            type="url"
            value={form.apkUrl}
            onChange={(e) => setForm({ ...form, apkUrl: e.target.value })}
            className="w-full bg-background border border-border rounded-lg px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-accent"
            placeholder="https://your-storage/cinevault-1.2.0.apk"
            required
          />
          <p className="text-xs text-text-secondary mt-1">Direct download link to the APK file (GitHub Releases, Google Drive direct link, etc.)</p>
        </div>

        <div>
          <label className="block text-sm font-medium text-text-secondary mb-1">Release Notes</label>
          <textarea
            value={form.releaseNotes}
            onChange={(e) => setForm({ ...form, releaseNotes: e.target.value })}
            className="w-full bg-background border border-border rounded-lg px-3 py-2 text-text-primary focus:outline-none focus:ring-2 focus:ring-accent"
            rows={3}
            placeholder="What's new in this version..."
          />
        </div>

        <div className="flex items-center gap-3">
          <input
            type="checkbox"
            id="forceUpdate"
            checked={form.forceUpdate}
            onChange={(e) => setForm({ ...form, forceUpdate: e.target.checked })}
            className="w-4 h-4 accent-red-600"
          />
          <label htmlFor="forceUpdate" className="text-sm text-text-primary">
            Force Update <span className="text-text-secondary">(user cannot skip, must update)</span>
          </label>
        </div>

        {error && <p className="text-red-400 text-sm">{error}</p>}
        {saved && <p className="text-green-400 text-sm">✓ Saved successfully!</p>}

        <button
          type="submit"
          disabled={saving}
          className="w-full bg-accent hover:bg-accent/90 disabled:opacity-50 text-white font-semibold py-2.5 rounded-lg transition-colors"
        >
          {saving ? 'Saving...' : 'Save Update Info'}
        </button>
      </form>

      <div className="mt-6 bg-surface border border-border rounded-xl p-4">
        <h3 className="text-sm font-semibold text-text-primary mb-2">How it works</h3>
        <ul className="text-sm text-text-secondary space-y-1 list-disc list-inside">
          <li>App checks this on every launch</li>
          <li>If Version Code here &gt; app's built-in version, popup appears</li>
          <li>User taps "Update Now", APK downloads inside app with progress %</li>
          <li>After 100%, "Install Now" button appears — no Play Store needed</li>
          <li>Force Update = user cannot dismiss the popup</li>
        </ul>
      </div>
    </div>
  );
}
