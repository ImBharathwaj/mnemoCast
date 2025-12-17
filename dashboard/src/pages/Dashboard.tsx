import React, { useEffect, useState } from 'react';
import { campaignApi, screenApi, analyticsApi } from '../services/api';
import { AnalyticsDashboard, Campaign, Screen } from '../types';
import { CampaignStatIcon, CheckIcon, ScreenStatIcon, OnlineIcon } from '../components/Icons';

const Dashboard: React.FC = () => {
  const [stats, setStats] = useState({
    totalCampaigns: 0,
    activeCampaigns: 0,
    totalScreens: 0,
    onlineScreens: 0,
  });
  const [analytics, setAnalytics] = useState<AnalyticsDashboard | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadData();
  }, []);

  const loadData = async () => {
    try {
      const [campaigns, screens, analyticsData] = await Promise.all([
        campaignApi.list(false).catch((err) => {
          console.error('Failed to load campaigns:', err);
          return [];
        }),
        screenApi.list().catch((err) => {
          console.error('Failed to load screens:', err);
          return [];
        }),
        analyticsApi.dashboard().catch((err) => {
          console.error('Failed to load analytics:', err);
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
    } catch (error) {
      console.error('Failed to load dashboard data:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">Loading...</div>
      </div>
    );
  }

  return (
    <div>
      <h1 className="text-3xl font-bold text-gray-900 mb-8">Dashboard</h1>

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

