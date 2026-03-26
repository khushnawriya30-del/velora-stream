import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Plus, Trash2, GripVertical, X } from 'lucide-react';
import api from '../lib/api';
import type { Banner } from '../types';
import toast from 'react-hot-toast';
import clsx from 'clsx';

export default function BannersPage() {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    title: '',
    subtitle: '',
    imageUrl: '',
    actionType: 'movie',
    contentId: '',
    isActive: true,
  });

  const { data: banners = [], isLoading } = useQuery<Banner[]>({
    queryKey: ['banners'],
    queryFn: async () => {
      const { data } = await api.get('/banners/all');
      console.log('Banners from API:', data); // Debug
      // Transform backend data to match frontend Banner interface
      return data.map((banner: any) => {
        const transformed = {
          ...banner,
          order: banner.displayOrder || 0,
          // Ensure subtitle is mapped from tagline if needed
          subtitle: banner.subtitle || banner.tagline || '',
        };
        console.log('Transformed banner:', transformed); // Debug
        return transformed;
      });
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: typeof form) => api.post('/banners', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['banners'] });
      toast.success('Banner created');
      setShowForm(false);
      setForm({ title: '', subtitle: '', imageUrl: '', actionType: 'movie', contentId: '', isActive: true });
    },
    onError: () => toast.error('Failed to create banner'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/banners/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['banners'] });
      toast.success('Banner deleted');
    },
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
      api.patch(`/banners/${id}`, { isActive }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['banners'] });
    },
  });

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Banners</h1>
        <button
          onClick={() => setShowForm(true)}
          className="flex items-center gap-2 bg-gold hover:bg-gold-light text-background px-4 py-2.5 rounded-xl text-sm font-semibold transition-colors"
        >
          <Plus size={18} />
          Add Banner
        </button>
      </div>

      {/* Create form modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border rounded-2xl p-6 w-full max-w-lg space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">New Banner</h2>
              <button onClick={() => setShowForm(false)} className="text-text-secondary hover:text-text-primary">
                <X size={20} />
              </button>
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Title *</label>
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                required
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Subtitle</label>
              <input
                value={form.subtitle}
                onChange={(e) => setForm({ ...form, subtitle: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div>
              <label className="block text-sm text-text-secondary mb-1">Image URL *</label>
              <input
                type="url"
                value={form.imageUrl}
                onChange={(e) => setForm({ ...form, imageUrl: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold mb-2"
                placeholder="https://example.com/image.jpg"
                required
              />
              {form.imageUrl && (
                <div className="text-xs space-y-1">
                  <p className="text-gold">URL: {form.imageUrl}</p>
                  {form.imageUrl.startsWith('http') ? (
                    <p className="text-success">✓ Valid URL format</p>
                  ) : (
                    <p className="text-error">✗ URL must start with http:// or https://</p>
                  )}
                </div>
              )}
            </div>
            {form.imageUrl && form.imageUrl.startsWith('http') && (
              <div>
                <label className="block text-sm text-text-secondary mb-2">Image Preview</label>
                <div className="w-full h-40 bg-border rounded-xl overflow-hidden flex items-center justify-center">
                  <img
                    src={form.imageUrl}
                    alt="Preview"
                    className="w-full h-full object-cover"
                    onError={() => console.log('Preview image failed to load')}
                    onLoad={() => console.log('Preview image loaded successfully')}
                  />
                </div>
              </div>
            )}
            <div>
              <label className="block text-sm text-text-secondary mb-1">Content ID</label>
              <input
                value={form.contentId}
                onChange={(e) => setForm({ ...form, contentId: e.target.value })}
                placeholder="Link to movie/series ID"
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              />
            </div>
            <div className="flex gap-3 justify-end">
              <button
                onClick={() => setShowForm(false)}
                className="px-4 py-2 rounded-xl border border-border text-text-secondary hover:text-text-primary text-sm"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  const dataToSubmit = { ...form } as any;
                  if (!dataToSubmit.contentId || dataToSubmit.contentId.trim() === '') {
                    delete dataToSubmit.contentId;
                  }
                  createMutation.mutate(dataToSubmit);
                }}
                disabled={createMutation.isPending || !form.title || !form.imageUrl}
                className="px-4 py-2 rounded-xl bg-gold hover:bg-gold-light text-background text-sm font-semibold disabled:opacity-50"
              >
                {createMutation.isPending ? 'Creating...' : 'Create Banner'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Banner list */}
      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="bg-surface border border-border rounded-xl h-24 animate-pulse" />
          ))}
        </div>
      ) : banners.length === 0 ? (
        <div className="text-center py-20 text-text-secondary">
          <p>No banners yet</p>
        </div>
      ) : (
        <div className="space-y-3">
          {banners.map((banner) => (
            <div
              key={banner._id}
              className="bg-surface border border-border rounded-xl p-4 flex items-center gap-4"
            >
              <GripVertical size={18} className="text-text-muted cursor-grab" />
              {banner.imageUrl && banner.imageUrl.trim() !== '' ? (
                <div className="relative w-32 h-20 rounded-lg overflow-hidden bg-border">
                  <img
                    src={banner.imageUrl}
                    alt={banner.title}
                    className="w-full h-full object-cover"
                    onError={(e) => {
                      console.error('Image failed to load:', banner.imageUrl);
                      (e.target as HTMLImageElement).style.display = 'none';
                      const errorDiv = document.createElement('div');
                      errorDiv.className = 'absolute inset-0 flex items-center justify-center bg-border text-xs text-text-muted';
                      errorDiv.textContent = 'Image Failed';
                      e.currentTarget.parentElement?.appendChild(errorDiv);
                    }}
                    onLoad={() => console.log('Image loaded:', banner.imageUrl)}
                  />
                </div>
              ) : (
                <div className="w-32 h-20 bg-border rounded-lg flex items-center justify-center text-xs text-text-muted">
                  No Image
                </div>
              )}
              <div className="flex-1 min-w-0">
                <h3 className="font-medium truncate">{banner.title}</h3>
                {banner.subtitle && (
                  <p className="text-sm text-text-secondary truncate">{banner.subtitle}</p>
                )}
                <p className="text-xs text-text-muted mt-1">Order: {banner.order}</p>
                {/* Debug: Show image URL */}
                {banner.imageUrl && (
                  <p className="text-xs text-gold mt-1 truncate" title={banner.imageUrl}>
                    {banner.imageUrl.substring(0, 40)}...
                  </p>
                )}
              </div>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={banner.isActive}
                  onChange={() => toggleMutation.mutate({ id: banner._id, isActive: !banner.isActive })}
                  className="sr-only peer"
                />
                <div className={clsx(
                  'w-10 h-5 rounded-full transition-colors relative',
                  banner.isActive ? 'bg-success' : 'bg-border',
                )}>
                  <div className={clsx(
                    'absolute top-0.5 w-4 h-4 bg-white rounded-full transition-transform',
                    banner.isActive ? 'translate-x-5' : 'translate-x-0.5',
                  )} />
                </div>
                <span className="text-xs text-text-secondary">{banner.isActive ? 'Active' : 'Inactive'}</span>
              </label>
              <button
                onClick={() => {
                  if (confirm('Delete this banner?')) {
                    deleteMutation.mutate(banner._id);
                  }
                }}
                className="p-2 rounded-lg hover:bg-error/10 text-text-secondary hover:text-error transition-colors"
              >
                <Trash2 size={16} />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
