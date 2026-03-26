import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Plus, Trash2, GripVertical, X, Edit2 } from 'lucide-react';
import api from '../lib/api';
import toast from 'react-hot-toast';
import clsx from 'clsx';

interface HomeSection {
  _id: string;
  title: string;
  type: 'standard' | 'large_card' | 'mid_banner' | 'trending';
  displayOrder: number;
  isVisible: boolean;
  cardSize: 'small' | 'medium' | 'large';
  showViewMore: boolean;
  viewMoreText: string;
  showTrendingNumbers: boolean;
  bannerImageUrl?: string;
  contentIds: string[];
  maxItems: number;
}

export default function HomeSectionsPage() {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<string | null>(null);
  const [form, setForm] = useState({
    title: '',
    type: 'standard' as const,
    cardSize: 'small' as const,
    maxItems: 20,
    showViewMore: true,
    viewMoreText: 'View More',
    showTrendingNumbers: false,
    bannerImageUrl: '',
  });

  const { data: sections = [], isLoading } = useQuery<HomeSection[]>({
    queryKey: ['homeSections'],
    queryFn: async () => {
      const { data } = await api.get('/home/sections');
      return data;
    },
  });

  const createMutation = useMutation({
    mutationFn: (data: typeof form) => 
      editingId 
        ? api.patch(`/home/sections/${editingId}`, data)
        : api.post('/home/sections', data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['homeSections'] });
      toast.success(editingId ? 'Section updated' : 'Section created');
      setShowForm(false);
      setEditingId(null);
      resetForm();
    },
    onError: () => toast.error('Failed to save section'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/home/sections/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['homeSections'] });
      toast.success('Section deleted');
    },
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, isVisible }: { id: string; isVisible: boolean }) =>
      api.patch(`/home/sections/${id}`, { isVisible }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['homeSections'] });
    },
  });

  const reorderMutation = useMutation({
    mutationFn: (orderedIds: string[]) =>
      api.post('/home/sections/reorder', { orderedIds }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['homeSections'] });
      toast.success('Sections reordered');
    },
  });

  const resetForm = () => {
    setForm({
      title: '',
      type: 'standard',
      cardSize: 'small',
      maxItems: 20,
      showViewMore: true,
      viewMoreText: 'View More',
      showTrendingNumbers: false,
      bannerImageUrl: '',
    });
  };

  const handleEdit = (section: HomeSection) => {
    setForm({
      title: section.title,
      type: section.type,
      cardSize: section.cardSize,
      maxItems: section.maxItems,
      showViewMore: section.showViewMore,
      viewMoreText: section.viewMoreText,
      showTrendingNumbers: section.showTrendingNumbers,
      bannerImageUrl: section.bannerImageUrl || '',
    });
    setEditingId(section._id);
    setShowForm(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">Home Sections</h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage dynamic sections that appear on the mobile app home screen
          </p>
        </div>
        <button
          onClick={() => {
            setEditingId(null);
            resetForm();
            setShowForm(true);
          }}
          className="flex items-center gap-2 bg-gold hover:bg-gold-light text-background px-4 py-2.5 rounded-xl text-sm font-semibold transition-colors"
        >
          <Plus size={18} />
          Add Section
        </button>
      </div>

      {/* Create/Edit Form Modal */}
      {showForm && (
        <div className="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4">
          <div className="bg-surface border border-border rounded-2xl p-6 w-full max-w-lg space-y-4 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between sticky top-0 bg-surface pb-4">
              <h2 className="text-lg font-semibold">
                {editingId ? 'Edit Section' : 'New Section'}
              </h2>
              <button
                onClick={() => {
                  setShowForm(false);
                  setEditingId(null);
                  resetForm();
                }}
                className="text-text-secondary hover:text-text-primary"
              >
                <X size={20} />
              </button>
            </div>

            <div>
              <label className="block text-sm text-text-secondary mb-1">
                Section Title *
              </label>
              <input
                value={form.title}
                onChange={(e) => setForm({ ...form, title: e.target.value })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                placeholder="e.g., Popular Movies, Trending, Marvel Universe"
                required
              />
            </div>

            <div>
              <label className="block text-sm text-text-secondary mb-1">
                Section Type *
              </label>
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value as any })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              >
                <option value="standard">Standard (Square Cards)</option>
                <option value="large_card">Large Cards (Bigger Posters)</option>
                <option value="trending">Trending (Top 10 with Numbers)</option>
                <option value="mid_banner">Mid Banner (Featured Section)</option>
              </select>
            </div>

            <div>
              <label className="block text-sm text-text-secondary mb-1">
                Card Size
              </label>
              <select
                value={form.cardSize}
                onChange={(e) => setForm({ ...form, cardSize: e.target.value as any })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
              >
                <option value="small">Small (130px)</option>
                <option value="medium">Medium (160px)</option>
                <option value="large">Large (180px+)</option>
              </select>
            </div>

            <div>
              <label className="block text-sm text-text-secondary mb-1">
                Max Items to show
              </label>
              <input
                type="number"
                value={form.maxItems}
                onChange={(e) => setForm({ ...form, maxItems: parseInt(e.target.value) })}
                className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                min="1"
                max="100"
              />
            </div>

            {form.type === 'mid_banner' && (
              <div>
                <label className="block text-sm text-text-secondary mb-1">
                  Banner Image URL
                </label>
                <input
                  type="url"
                  value={form.bannerImageUrl}
                  onChange={(e) => setForm({ ...form, bannerImageUrl: e.target.value })}
                  className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                  placeholder="https://example.com/banner.jpg"
                />
              </div>
            )}

            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="showViewMore"
                checked={form.showViewMore}
                onChange={(e) => setForm({ ...form, showViewMore: e.target.checked })}
                className="rounded border-border"
              />
              <label htmlFor="showViewMore" className="text-sm text-text-secondary">
                Show "View More" button
              </label>
            </div>

            {form.showViewMore && (
              <div>
                <label className="block text-sm text-text-secondary mb-1">
                  "View More" Button Text
                </label>
                <input
                  value={form.viewMoreText}
                  onChange={(e) => setForm({ ...form, viewMoreText: e.target.value })}
                  className="w-full bg-surface-light border border-border rounded-xl px-4 py-2.5 text-text-primary focus:outline-none focus:border-gold"
                  placeholder="View More"
                />
              </div>
            )}

            {form.type === 'trending' && (
              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="showNumbers"
                  checked={form.showTrendingNumbers}
                  onChange={(e) => setForm({ ...form, showTrendingNumbers: e.target.checked })}
                  className="rounded border-border"
                />
                <label htmlFor="showNumbers" className="text-sm text-text-secondary">
                  Show ranking numbers (1, 2, 3...)
                </label>
              </div>
            )}

            <div className="text-xs text-text-muted bg-surface-light p-3 rounded-lg">
              <p className="font-semibold mb-2">📌 Quick Setup Tips:</p>
              <ul className="space-y-1 list-disc list-inside">
                <li>
                  <strong>Standard:</strong> Great for Popular, Recent, Recommended
                </li>
                <li>
                  <strong>Large Cards:</strong> Perfect for Featured, Marvel, Collections
                </li>
                <li>
                  <strong>Trending:</strong> Shows Top 10 with numbers (1, 2, 3...)
                </li>
                <li>
                  <strong>Mid Banner:</strong> Feature a movie between sections
                </li>
              </ul>
            </div>

            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setShowForm(false);
                  setEditingId(null);
                  resetForm();
                }}
                className="px-4 py-2 rounded-xl border border-border text-text-secondary hover:text-text-primary text-sm"
              >
                Cancel
              </button>
              <button
                onClick={() => createMutation.mutate(form)}
                disabled={createMutation.isPending || !form.title}
                className="px-4 py-2 rounded-xl bg-gold hover:bg-gold-light text-background text-sm font-semibold disabled:opacity-50"
              >
                {createMutation.isPending ? 'Saving...' : editingId ? 'Update' : 'Create'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Sections List */}
      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <div key={i} className="bg-surface border border-border rounded-xl h-24 animate-pulse" />
          ))}
        </div>
      ) : sections.length === 0 ? (
        <div className="text-center py-20 text-text-secondary">
          <p>No sections yet. Create one to get started!</p>
        </div>
      ) : (
        <div className="space-y-3">
          <div className="text-xs text-text-muted mb-4 p-3 bg-surface-light rounded-lg">
            💡 <strong>Mobile App Reflection:</strong> Changes appear instantly on the mobile app. The
            order shown here matches the app's home screen layout.
          </div>
          {sections
            .sort((a, b) => a.displayOrder - b.displayOrder)
            .map((section) => (
              <div
                key={section._id}
                className="bg-surface border border-border rounded-xl p-4 flex items-center gap-4"
              >
                <GripVertical size={18} className="text-text-muted cursor-grab" />

                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2">
                    <h3 className="font-medium">{section.title}</h3>
                    <span className="text-xs bg-gold/20 text-gold px-2 py-1 rounded">
                      {section.type === 'standard' && 'Standard'}
                      {section.type === 'large_card' && 'Large Cards'}
                      {section.type === 'trending' && 'Trending'}
                      {section.type === 'mid_banner' && 'Featured Banner'}
                    </span>
                  </div>
                  <p className="text-xs text-text-secondary">
                    {section.contentIds?.length || 0} items • Order: {section.displayOrder}
                  </p>
                </div>

                <div className="flex items-center gap-2">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="checkbox"
                      checked={section.isVisible}
                      onChange={() =>
                        toggleMutation.mutate({
                          id: section._id,
                          isVisible: !section.isVisible,
                        })
                      }
                      className="sr-only peer"
                    />
                    <div className={clsx(
                      'w-10 h-5 rounded-full transition-colors relative',
                      section.isVisible ? 'bg-success' : 'bg-border'
                    )}>
                      <div className={clsx(
                        'absolute top-0.5 w-4 h-4 bg-white rounded-full transition-transform',
                        section.isVisible ? 'translate-x-5' : 'translate-x-0.5'
                      )} />
                    </div>
                  </label>

                  <button
                    onClick={() => handleEdit(section)}
                    className="p-2 rounded-lg hover:bg-gold/10 text-text-secondary hover:text-gold transition-colors"
                  >
                    <Edit2 size={16} />
                  </button>

                  <button
                    onClick={() => {
                      if (confirm('Delete this section?')) {
                        deleteMutation.mutate(section._id);
                      }
                    }}
                    className="p-2 rounded-lg hover:bg-error/10 text-text-secondary hover:text-error transition-colors"
                  >
                    <Trash2 size={16} />
                  </button>
                </div>
              </div>
            ))}
        </div>
      )}

      {sections.length > 0 && (
        <div className="text-xs text-text-muted bg-surface-light p-3 rounded-lg">
          <p className="font-semibold mb-2">🔄 What Happens When Changes?</p>
          <ul className="space-y-1 list-disc list-inside">
            <li>Add Section → Appears at bottom of mobile home</li>
            <li>Reorder → Changes mobile app layout instantly</li>
            <li>Edit → Updates visible immediately</li>
            <li>Delete → Removes from app automatically</li>
            <li>Enable/Disable → Shows/hides section without deletion</li>
          </ul>
        </div>
      )}
    </div>
  );
}
