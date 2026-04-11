import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2, Pencil, X, Image as ImageIcon, Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';
import api from '../lib/api';
import toast from 'react-hot-toast';

interface EarnerProof {
  _id: string;
  imageUrl: string;
  caption: string;
  displayOrder: number;
  isActive: boolean;
  createdAt: string;
}

export default function EarnerProofsPage() {
  const queryClient = useQueryClient();
  const [showForm, setShowForm] = useState(false);
  const [editingProof, setEditingProof] = useState<EarnerProof | null>(null);
  const [imageUrl, setImageUrl] = useState('');
  const [caption, setCaption] = useState('');

  const { data: proofs = [], isLoading } = useQuery({
    queryKey: ['earner-proofs'],
    queryFn: async () => {
      const { data } = await api.get('/earner-proofs/all');
      return data as EarnerProof[];
    },
  });

  const createMutation = useMutation({
    mutationFn: (body: { imageUrl: string; caption?: string }) => api.post('/earner-proofs', body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['earner-proofs'] });
      toast.success('Screenshot added');
      resetForm();
    },
    onError: () => toast.error('Failed to add'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, body }: { id: string; body: Partial<EarnerProof> }) => api.patch(`/earner-proofs/${id}`, body),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['earner-proofs'] });
      toast.success('Updated');
      resetForm();
    },
    onError: () => toast.error('Failed to update'),
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.delete(`/earner-proofs/${id}`),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['earner-proofs'] });
      toast.success('Deleted');
    },
    onError: () => toast.error('Failed to delete'),
  });

  const toggleMutation = useMutation({
    mutationFn: ({ id, isActive }: { id: string; isActive: boolean }) =>
      api.patch(`/earner-proofs/${id}`, { isActive }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['earner-proofs'] });
      toast.success('Status updated');
    },
  });

  const resetForm = () => {
    setShowForm(false);
    setEditingProof(null);
    setImageUrl('');
    setCaption('');
  };

  const handleSubmit = () => {
    if (!imageUrl.trim()) {
      toast.error('Image URL is required');
      return;
    }
    if (editingProof) {
      updateMutation.mutate({ id: editingProof._id, body: { imageUrl, caption } });
    } else {
      createMutation.mutate({ imageUrl, caption });
    }
  };

  const openEdit = (proof: EarnerProof) => {
    setEditingProof(proof);
    setImageUrl(proof.imageUrl);
    setCaption(proof.caption);
    setShowForm(true);
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-semibold">User Earnings Screenshots</h1>
          <p className="text-sm text-text-secondary mt-1">
            Manage screenshots shown in "How Much Others Earned" section of the app
          </p>
        </div>
        <button
          onClick={() => { resetForm(); setShowForm(true); }}
          className="flex items-center gap-2 bg-gold hover:bg-gold-light text-background px-4 py-2.5 rounded-xl text-sm font-semibold transition-colors"
        >
          <Plus size={18} />
          Add Screenshot
        </button>
      </div>

      {/* Add/Edit Form */}
      {showForm && (
        <div className="bg-surface border border-border rounded-2xl p-6 space-y-4">
          <div className="flex items-center justify-between">
            <h3 className="font-semibold text-lg">
              {editingProof ? 'Edit Screenshot' : 'Add New Screenshot'}
            </h3>
            <button onClick={resetForm} className="text-text-muted hover:text-text-primary">
              <X size={18} />
            </button>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="space-y-2">
              <label className="text-xs text-text-muted font-medium">Image URL *</label>
              <input
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
                placeholder="https://example.com/screenshot.jpg"
                className="w-full bg-background border border-border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-gold"
              />
            </div>
            <div className="space-y-2">
              <label className="text-xs text-text-muted font-medium">Caption (optional)</label>
              <input
                value={caption}
                onChange={(e) => setCaption(e.target.value)}
                placeholder="e.g. User Earned ₹500"
                className="w-full bg-background border border-border rounded-lg px-3 py-2.5 text-sm focus:outline-none focus:border-gold"
              />
            </div>
          </div>

          {/* Preview */}
          {imageUrl && (
            <div className="mt-3">
              <p className="text-xs text-text-muted mb-2">Preview:</p>
              <img
                src={imageUrl}
                alt="Preview"
                className="max-h-48 rounded-lg border border-border object-contain"
                onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }}
              />
            </div>
          )}

          <div className="flex gap-2 justify-end">
            <button
              onClick={resetForm}
              className="px-4 py-2 text-sm text-text-secondary hover:text-text-primary border border-border rounded-lg"
            >
              Cancel
            </button>
            <button
              onClick={handleSubmit}
              disabled={createMutation.isPending || updateMutation.isPending}
              className="px-4 py-2 text-sm bg-gold text-background font-medium rounded-lg hover:bg-gold-light disabled:opacity-50"
            >
              {(createMutation.isPending || updateMutation.isPending) ? 'Saving...' : editingProof ? 'Update' : 'Add Screenshot'}
            </button>
          </div>
        </div>
      )}

      {/* Screenshots Grid */}
      {isLoading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="aspect-[3/4] rounded-xl bg-surface-light animate-pulse" />
          ))}
        </div>
      ) : proofs.length === 0 ? (
        <div className="text-center py-16 bg-surface border border-border rounded-2xl">
          <ImageIcon size={48} className="mx-auto text-text-muted mb-4" />
          <p className="text-text-secondary">No screenshots added yet</p>
          <p className="text-sm text-text-muted mt-1">Add screenshots to show in "How Much Others Earned" section</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
          {proofs.map((proof) => (
            <div
              key={proof._id}
              className="group bg-surface border border-border rounded-xl overflow-hidden hover:border-gold/50 transition-all"
            >
              <div className="relative aspect-[3/4] bg-surface-light">
                <img
                  src={proof.imageUrl}
                  alt={proof.caption || 'Earnings screenshot'}
                  className="w-full h-full object-cover"
                  loading="lazy"
                />
                {!proof.isActive && (
                  <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                    <span className="text-xs bg-red-500/80 text-white px-3 py-1 rounded-full font-medium">Hidden</span>
                  </div>
                )}
                {/* Action overlay */}
                <div className="absolute inset-0 bg-black/0 group-hover:bg-black/40 transition-colors flex items-center justify-center gap-2 opacity-0 group-hover:opacity-100">
                  <button
                    onClick={() => openEdit(proof)}
                    className="p-2 bg-white/20 rounded-lg hover:bg-white/30 transition-colors"
                    title="Edit"
                  >
                    <Pencil size={16} className="text-white" />
                  </button>
                  <button
                    onClick={() => toggleMutation.mutate({ id: proof._id, isActive: !proof.isActive })}
                    className="p-2 bg-white/20 rounded-lg hover:bg-white/30 transition-colors"
                    title={proof.isActive ? 'Hide' : 'Show'}
                  >
                    {proof.isActive ? <EyeOff size={16} className="text-white" /> : <Eye size={16} className="text-white" />}
                  </button>
                  <button
                    onClick={() => {
                      if (confirm('Delete this screenshot?')) deleteMutation.mutate(proof._id);
                    }}
                    className="p-2 bg-red-500/50 rounded-lg hover:bg-red-500/70 transition-colors"
                    title="Delete"
                  >
                    <Trash2 size={16} className="text-white" />
                  </button>
                </div>
              </div>
              {proof.caption && (
                <div className="p-3 border-t border-border">
                  <p className="text-sm text-text-primary truncate">{proof.caption}</p>
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
