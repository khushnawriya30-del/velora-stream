import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Send, Bell, Users, User } from 'lucide-react';
import api from '../lib/api';
import toast from 'react-hot-toast';
import { format } from 'date-fns';

interface Notification {
  _id: string;
  title: string;
  body: string;
  target: 'all' | 'subscribers' | 'specific';
  sentAt: string;
  recipientCount?: number;
}

export default function NotificationsPage() {
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [body, setBody] = useState('');
  const [target, setTarget] = useState<'all' | 'subscribers'>('all');

  const { data, isLoading } = useQuery({
    queryKey: ['notifications'],
    queryFn: async () => {
      const { data } = await api.get('/notifications/admin');
      return data;
    },
  });

  const sendMutation = useMutation({
    mutationFn: (payload: { title: string; body: string; target: string }) =>
      api.post('/notifications/send', payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['notifications'] });
      toast.success('Notification sent');
      setTitle('');
      setBody('');
    },
    onError: () => toast.error('Failed to send notification'),
  });

  const handleSend = () => {
    if (!title.trim() || !body.trim()) {
      toast.error('Title and body are required');
      return;
    }
    sendMutation.mutate({ title: title.trim(), body: body.trim(), target });
  };

  const notifications: Notification[] = data?.data ?? data ?? [];

  return (
    <div className="space-y-8">
      <h1 className="text-2xl font-semibold">Notifications</h1>

      {/* Compose notification */}
      <div className="bg-surface border border-border rounded-xl p-6 space-y-4">
        <h2 className="text-lg font-medium flex items-center gap-2">
          <Send size={18} className="text-gold" /> Compose Notification
        </h2>
        <div>
          <label className="block text-sm text-text-secondary mb-1">Title</label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="Notification title"
            className="w-full bg-surface-light border border-border rounded-lg px-4 py-2.5 text-text-primary placeholder:text-text-muted focus:border-gold focus:outline-none transition-colors"
          />
        </div>
        <div>
          <label className="block text-sm text-text-secondary mb-1">Body</label>
          <textarea
            value={body}
            onChange={(e) => setBody(e.target.value)}
            placeholder="Notification message..."
            rows={3}
            className="w-full bg-surface-light border border-border rounded-lg px-4 py-2.5 text-text-primary placeholder:text-text-muted focus:border-gold focus:outline-none transition-colors resize-none"
          />
        </div>
        <div>
          <label className="block text-sm text-text-secondary mb-1">Target Audience</label>
          <div className="flex gap-3">
            <button
              onClick={() => setTarget('all')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm transition-colors ${
                target === 'all'
                  ? 'bg-gold/10 text-gold border border-gold/30'
                  : 'bg-surface-light text-text-secondary border border-border hover:text-text-primary'
              }`}
            >
              <Users size={14} /> All Users
            </button>
            <button
              onClick={() => setTarget('subscribers')}
              className={`flex items-center gap-2 px-4 py-2 rounded-lg text-sm transition-colors ${
                target === 'subscribers'
                  ? 'bg-gold/10 text-gold border border-gold/30'
                  : 'bg-surface-light text-text-secondary border border-border hover:text-text-primary'
              }`}
            >
              <User size={14} /> Subscribers Only
            </button>
          </div>
        </div>
        <button
          onClick={handleSend}
          disabled={sendMutation.isPending}
          className="flex items-center gap-2 bg-gold hover:bg-gold-dark text-background font-medium px-6 py-2.5 rounded-lg transition-colors disabled:opacity-50"
        >
          <Send size={16} />
          {sendMutation.isPending ? 'Sending...' : 'Send Notification'}
        </button>
      </div>

      {/* Sent notifications */}
      <div className="space-y-4">
        <h2 className="text-lg font-medium flex items-center gap-2">
          <Bell size={18} className="text-gold" /> Sent Notifications
        </h2>
        {isLoading ? (
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="bg-surface border border-border rounded-xl h-20 animate-pulse" />
            ))}
          </div>
        ) : notifications.length === 0 ? (
          <div className="text-center py-16 text-text-secondary">
            <Bell size={40} className="mx-auto mb-3 opacity-30" />
            <p>No notifications sent yet</p>
          </div>
        ) : (
          <div className="space-y-3">
            {notifications.map((n) => (
              <div key={n._id} className="bg-surface border border-border rounded-xl p-4">
                <div className="flex items-start justify-between gap-4">
                  <div>
                    <h3 className="font-medium">{n.title}</h3>
                    <p className="text-sm text-text-secondary mt-1">{n.body}</p>
                    <div className="flex items-center gap-3 mt-2 text-xs text-text-muted">
                      <span className="capitalize">{n.target}</span>
                      {n.recipientCount != null && <span>{n.recipientCount} recipients</span>}
                    </div>
                  </div>
                  <span className="text-xs text-text-muted whitespace-nowrap">
                    {format(new Date(n.sentAt), 'MMM d, yyyy h:mm a')}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
