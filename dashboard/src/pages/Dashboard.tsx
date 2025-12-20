import React, { useEffect, useState } from 'react';
import { campaignApi, screenApi, analyticsApi } from '../services/api';
import { AnalyticsDashboard, Campaign, Screen } from '../types';
import { CampaignStatIcon, CheckIcon, ScreenStatIcon, OnlineIcon } from '../components/Icons';
import { API_BASE_URL } from '../config/api';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState({
    totalCampaigns: 0,
    activeCampaigns: 0,
    totalScreens: 0,
    onlineScreens: 0,
  });
  const [analytics, setAnalytics] = useState<AnalyticsDashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const [error, setError] = useState<string | null>(null);

  const loadData = React.useCallback(async () => {
    try {
      setError(null);
      const [campaigns, screens, analyticsData] = await Promise.all([
        campaignApi.list(false).catch((err) => {
          console.error('Failed to load campaigns:', err);
          setError(`Failed to load campaigns: ${err.message || 'Unknown error'}`);
          return [];
        }),
        screenApi.list().catch((err) => {
          console.error('Failed to load screens:', err);
          setError(`Failed to load screens: ${err.message || 'Unknown error'}`);
          return [];
        }),
        analyticsApi.dashboard().catch((err) => {
          console.error('Failed to load analytics:', err);
          // Don't set error for analytics as it's optional
          return null;
        }),
      ]);

      setStats({
        totalCampaigns: Array.isArray(campaigns) ? campaigns.length : 0,
        activeCampaigns: Array.isArray(campaigns) ? campaigns.filter((c: Campaign) => c.status === 'active').length : 0,
        totalScreens: Array.isArray(screens) ? screens.length : 0,
        onlineScreens: Array.isArray(screens) ? screens.filter((s: Screen) => s.isOnline).length : 0,
      });

      if (analyticsData) {
        setAnalytics(analyticsData);
      }
      
      setLastUpdated(new Date());
    } catch (error: any) {
      console.error('Failed to load dashboard data:', error);
      setError(`Failed to load dashboard data: ${error.message || 'Unknown error'}`);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(() => {
        loadData();
      }, 10000); // Refresh every 10 seconds

      return () => clearInterval(interval);
    }
  }, [autoRefresh, loadData]);

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Dashboard</h1>
        <div className="flex items-center gap-4">
          {lastUpdated && (
            <span className="text-sm text-gray-500">
              Last updated: {lastUpdated.toLocaleTimeString()}
            </span>
          )}
          <label className="flex items-center gap-2 cursor-pointer">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="w-4 h-4 text-blue-600 border-gray-300 rounded focus:ring-blue-500"
            />
            <span className="text-sm text-gray-600">Auto-refresh (10s)</span>
          </label>
          <button
            onClick={loadData}
            disabled={loading}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 text-sm"
          >
            {loading ? 'Loading...' : 'Refresh'}
          </button>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded mb-4">
          <p className="font-semibold">Error loading data</p>
          <p className="text-sm">{error}</p>
          <p className="text-xs mt-2">
            Check browser console (F12) for details. Make sure the backend is running at{' '}
            <code className="bg-red-100 px-1 rounded">{API_BASE_URL}</code>
          </p>
          <button
            onClick={() => window.open(`${API_BASE_URL}/api/v1/health`, '_blank')}
            className="mt-2 text-xs underline"
          >
            Test backend connection →
          </button>
        </div>
      )}

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        <StatCard
          title="Total Campaigns"
          value={stats.totalCampaigns}
          icon={CampaignStatIcon}
          color="blue"
        />
        <StatCard
          title="Active Campaigns"
          value={stats.activeCampaigns}
          icon={CheckIcon}
          color="green"
        />
        <StatCard
          title="Total Screens"
          value={stats.totalScreens}
          icon={ScreenStatIcon}
          color="purple"
        />
        <StatCard
          title="Online Screens"
          value={stats.onlineScreens}
          icon={OnlineIcon}
          color="green"
        />
      </div>

      {/* Analytics */}
      {analytics && (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Total Impressions</h2>
            <div className="text-4xl font-bold text-blue-600">
              {analytics.totalImpressions || 0}
            </div>
          </div>

          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Active Ads</h2>
            <div className="text-4xl font-bold text-green-600">
              {analytics.activeAds || 0}
            </div>
          </div>

          {analytics.topAds && analytics.topAds.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6 lg:col-span-2">
              <h2 className="text-xl font-semibold mb-4">Top Performing Ads</h2>
              <div className="space-y-2">
                {analytics.topAds.slice(0, 5).map((ad, index) => (
                  <div key={ad.adId} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                    <span className="font-medium">#{index + 1} {ad.adId}</span>
                    <span className="text-blue-600 font-semibold">{ad.impressions} impressions</span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}

      {!analytics && (
        <div className="bg-white rounded-lg shadow p-6">
          <p className="text-gray-500">No analytics data available. Start creating campaigns and serving ads to see statistics.</p>
        </div>
      )}
    </div>
  );
};

interface StatCardProps {
  title: string;
  value: number;
  icon: React.ComponentType<{ className?: string }>;
  color: 'blue' | 'green' | 'purple' | 'red';
}

const StatCard: React.FC<StatCardProps> = ({ title, value, icon: Icon, color }) => {
  const colorClasses = {
    blue: 'bg-blue-100 text-blue-600',
    green: 'bg-green-100 text-green-600',
    purple: 'bg-purple-100 text-purple-600',
    red: 'bg-red-100 text-red-600',
  };

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-gray-600 mb-1">{title}</p>
          <p className="text-3xl font-bold text-gray-900">{value}</p>
        </div>
        <div className={`w-12 h-12 rounded-full flex items-center justify-center ${colorClasses[color]}`}>
          <Icon className="w-6 h-6" />
        </div>
      </div>
    </div>
  );
};

export default Dashboard;

