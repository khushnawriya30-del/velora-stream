import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { CheckCircle, XCircle, Clock } from 'lucide-react';
import api from '../lib/api';
import type { Review } from '../types';
import toast from 'react-hot-toast';
import clsx from 'clsx';
import { format } from 'date-fns';

export default function ReviewsPage() {
  const queryClient = useQueryClient();
  const [filter, setFilter] = useState<string>('pending');

  const { data, isLoading } = useQuery({
    queryKey: ['reviews', filter],
    queryFn: async () => {
      const params = filter ? `?status=${filter}` : '';
      const { data } = await api.get(`/reviews/admin${params}`);
      return data;
    },
  });

  const moderateMutation = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      api.patch(`/reviews/${id}/moderate`, { status }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      toast.success('Review moderated');
    },
    onError: () => toast.error('Failed to moderate review'),
  });

  const reviews: Review[] = data?.data ?? data ?? [];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Review Moderation</h1>

      {/* Filter tabs */}
      <div className="flex gap-2">
        {['pending', 'approved', 'rejected', ''].map((status) => (
          <button
            key={status}
            onClick={() => setFilter(status)}
            className={clsx(
              'px-4 py-2 rounded-lg text-sm font-medium transition-colors',
              filter === status
                ? 'bg-gold/10 text-gold'
                : 'bg-surface-light text-text-secondary hover:text-text-primary',
            )}
          >
            {status === '' ? 'All' : status.charAt(0).toUpperCase() + status.slice(1)}
          </button>
        ))}
      </div>

      {isLoading ? (
        <div className="space-y-3">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="bg-surface border border-border rounded-xl h-24 animate-pulse" />
          ))}
        </div>
      ) : reviews.length === 0 ? (
        <div className="text-center py-20 text-text-secondary">
          <p>No reviews found</p>
        </div>
      ) : (
        <div className="space-y-3">
          {reviews.map((review) => (
            <div key={review._id} className="bg-surface border border-border rounded-xl p-5">
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-2">
                    <span className="font-medium">{review.userName ?? 'Anonymous'}</span>
                    <span className="text-gold text-sm">{'★'.repeat(Math.round(review.rating))}</span>
                    <span className="text-sm text-text-muted">{review.rating.toFixed(1)}</span>
                    <span
                      className={clsx(
                        'flex items-center gap-1 px-2 py-0.5 rounded-full text-xs',
                        review.moderationStatus === 'pending' && 'bg-warning/10 text-warning',
                        review.moderationStatus === 'approved' && 'bg-success/10 text-success',
                        review.moderationStatus === 'rejected' && 'bg-error/10 text-error',
                      )}
                    >
                      {review.moderationStatus === 'pending' && <Clock size={10} />}
                      {review.moderationStatus === 'approved' && <CheckCircle size={10} />}
                      {review.moderationStatus === 'rejected' && <XCircle size={10} />}
                      {review.moderationStatus}
                    </span>
                  </div>
                  {review.contentTitle && (
                    <p className="text-sm text-text-muted mb-1">on: {review.contentTitle}</p>
                  )}
                  <p className="text-text-secondary text-sm">{review.text}</p>
                  <p className="text-xs text-text-muted mt-2">
                    {format(new Date(review.createdAt), 'MMM d, yyyy h:mm a')}
                  </p>
                </div>
                {review.moderationStatus === 'pending' && (
                  <div className="flex gap-2 shrink-0">
                    <button
                      onClick={() => moderateMutation.mutate({ id: review._id, status: 'approved' })}
                      className="flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm bg-success/10 text-success hover:bg-success/20 transition-colors"
                    >
                      <CheckCircle size={14} /> Approve
                    </button>
                    <button
                      onClick={() => moderateMutation.mutate({ id: review._id, status: 'rejected' })}
                      className="flex items-center gap-1 px-3 py-1.5 rounded-lg text-sm bg-error/10 text-error hover:bg-error/20 transition-colors"
                    >
                      <XCircle size={14} /> Reject
                    </button>
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
