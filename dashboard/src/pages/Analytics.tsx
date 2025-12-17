import React, { useEffect, useState } from 'react';
import { analyticsApi } from '../services/api';
import { Line, Bar } from 'react-chartjs-2';
import {
  Chart as ChartJS,
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend,
} from 'chart.js';

ChartJS.register(
  CategoryScale,
  LinearScale,
  PointElement,
  LineElement,
  BarElement,
  Title,
  Tooltip,
  Legend
);

const Analytics: React.FC = () => {
  const [analytics, setAnalytics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');

  useEffect(() => {
    loadAnalytics();
  }, [startDate, endDate]);

  const loadAnalytics = async () => {
    try {
      const data = await analyticsApi.dashboard(
        startDate || undefined,
        endDate || undefined
      );
      setAnalytics(data);
    } catch (error) {
      console.error('Failed to load analytics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="text-center py-8">Loading analytics...</div>;
  }

  const chartData = analytics?.topAds?.slice(0, 10) || [];

  const barChartData = {
    labels: chartData.map((ad: any) => ad.adId.substring(0, 20) + '...'),
    datasets: [
      {
        label: 'Impressions',
        data: chartData.map((ad: any) => ad.impressions),
        backgroundColor: 'rgba(59, 130, 246, 0.5)',
        borderColor: 'rgba(59, 130, 246, 1)',
        borderWidth: 1,
      },
    ],
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Analytics</h1>
        <div className="flex gap-4">
          <input
            type="date"
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg"
            placeholder="Start Date"
          />
          <input
            type="date"
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
            className="px-3 py-2 border border-gray-300 rounded-lg"
            placeholder="End Date"
          />
        </div>
      </div>

      {analytics ? (
        <div className="space-y-6">
          {/* Summary Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Total Ads</h3>
              <p className="text-3xl font-bold text-gray-900">{analytics.totalAds || 0}</p>
            </div>
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Active Ads</h3>
              <p className="text-3xl font-bold text-green-600">{analytics.activeAds || 0}</p>
            </div>
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Total Impressions</h3>
              <p className="text-3xl font-bold text-blue-600">{analytics.totalImpressions || 0}</p>
            </div>
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Top Performers</h3>
              <p className="text-3xl font-bold text-purple-600">{chartData.length}</p>
            </div>
          </div>

          {/* Chart */}
          {chartData.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Top Performing Ads</h2>
              <div className="h-96">
                <Bar
                  data={barChartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                      legend: {
                        display: false,
                      },
                      title: {
                        display: true,
                        text: 'Impressions by Ad',
                      },
                    },
                    scales: {
                      y: {
                        beginAtZero: true,
                      },
                    },
                  }}
                />
              </div>
            </div>
          )}

          {/* Recent Activity */}
          {analytics.recentActivity && analytics.recentActivity.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Recent Activity</h2>
              <div className="space-y-2">
                {analytics.recentActivity.slice(0, 10).map((activity: any, index: number) => (
                  <div key={index} className="flex justify-between items-center p-3 bg-gray-50 rounded">
                    <div>
                      <span className="font-medium">{activity.adId}</span>
                      <span className="text-gray-500 ml-2">- {activity.eventType}</span>
                    </div>
                    <span className="text-sm text-gray-500">
                      {new Date(activity.timestamp).toLocaleString()}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      ) : (
        <div className="bg-white rounded-lg shadow p-8 text-center">
          <p className="text-gray-500">No analytics data available. Start creating campaigns and serving ads to see statistics.</p>
        </div>
      )}
    </div>
  );
};

export default Analytics;

