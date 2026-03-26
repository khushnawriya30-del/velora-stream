import { useQuery } from '@tanstack/react-query';
import { AreaChart, Area, XAxis, YAxis, Tooltip, ResponsiveContainer, BarChart, Bar } from 'recharts';
import { Users, Film, Eye, TrendingUp } from 'lucide-react';
import api from '../lib/api';
import type { DashboardStats } from '../types';

export default function DashboardPage() {
  const { data: stats, isLoading } = useQuery<DashboardStats>({
    queryKey: ['dashboard'],
    queryFn: async () => {
      const { data } = await api.get('/analytics/dashboard');
      return data;
    },
  });

  if (isLoading) {
    return (
      <div className="space-y-6">
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div key={i} className="bg-surface border border-border rounded-xl p-5 animate-pulse h-24" />
          ))}
        </div>
      </div>
    );
  }

  const statCards = [
    { label: 'Total Users', value: stats?.totalUsers ?? 0, icon: Users, color: 'text-blue-400' },
    { label: 'Total Content', value: stats?.totalContent ?? 0, icon: Film, color: 'text-gold' },
    { label: 'Total Views', value: stats?.totalViews ?? 0, icon: Eye, color: 'text-green-400' },
    { label: 'Active Today', value: stats?.activeToday ?? 0, icon: TrendingUp, color: 'text-purple-400' },
  ];

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold">Dashboard</h1>

      {/* Stat cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {statCards.map((stat) => (
          <div
            key={stat.label}
            className="bg-surface border border-border rounded-xl p-5 flex items-center gap-4"
          >
            <div className={`p-2.5 rounded-lg bg-surface-light ${stat.color}`}>
              <stat.icon size={20} />
            </div>
            <div>
              <p className="text-2xl font-semibold">{stat.value.toLocaleString()}</p>
              <p className="text-sm text-text-secondary">{stat.label}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Signups chart */}
        <div className="bg-surface border border-border rounded-xl p-5">
          <h2 className="text-lg font-medium mb-4">User Signups</h2>
          <ResponsiveContainer width="100%" height={280}>
            <AreaChart data={stats?.signupChart ?? []}>
              <defs>
                <linearGradient id="goldGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#F5A623" stopOpacity={0.3} />
                  <stop offset="95%" stopColor="#F5A623" stopOpacity={0} />
                </linearGradient>
              </defs>
              <XAxis dataKey="date" tick={{ fill: '#6B6B6B', fontSize: 12 }} />
              <YAxis tick={{ fill: '#6B6B6B', fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  background: '#1E1E1E',
                  border: '1px solid #2A2A2A',
                  borderRadius: '8px',
                  color: '#FFFFFF',
                }}
              />
              <Area type="monotone" dataKey="count" stroke="#F5A623" fill="url(#goldGradient)" strokeWidth={2} />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        {/* Top watched */}
        <div className="bg-surface border border-border rounded-xl p-5">
          <h2 className="text-lg font-medium mb-4">Most Watched</h2>
          <ResponsiveContainer width="100%" height={280}>
            <BarChart data={stats?.topWatched ?? []} layout="vertical">
              <XAxis type="number" tick={{ fill: '#6B6B6B', fontSize: 12 }} />
              <YAxis type="category" dataKey="title" width={120} tick={{ fill: '#A0A0A0', fontSize: 12 }} />
              <Tooltip
                contentStyle={{
                  background: '#1E1E1E',
                  border: '1px solid #2A2A2A',
                  borderRadius: '8px',
                  color: '#FFFFFF',
                }}
              />
              <Bar dataKey="views" fill="#F5A623" radius={[0, 4, 4, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>
    </div>
  );
}
