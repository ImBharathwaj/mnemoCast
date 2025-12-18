import React, { useEffect, useState } from 'react';
import { analyticsApi, campaignApi } from '../services/api';
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
  const [campaigns, setCampaigns] = useState<any[]>([]);
  const [selectedCampaigns, setSelectedCampaigns] = useState<string[]>([]);
  const [comparison, setComparison] = useState<any[]>([]);
  const [timeSeries, setTimeSeries] = useState<any>(null);
  const [roiMetrics, setRoiMetrics] = useState<any[]>([]);
  const [screenPerformance, setScreenPerformance] = useState<any[]>([]);
  const [creativePerformance, setCreativePerformance] = useState<any[]>([]);
  const [geographicPerformance, setGeographicPerformance] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [activeTab, setActiveTab] = useState<'overview' | 'comparison' | 'timeseries' | 'roi' | 'screens' | 'creatives' | 'geographic'>('overview');
  const [autoRefresh, setAutoRefresh] = useState(false);
  const [refreshInterval, setRefreshInterval] = useState(30); // seconds

  useEffect(() => {
    loadCampaigns();
    loadAnalytics();
  }, [startDate, endDate]);

  useEffect(() => {
    if (autoRefresh) {
      const interval = setInterval(() => {
        loadAnalytics();
        if (selectedCampaigns.length > 0 && startDate && endDate) {
          loadComparison();
        }
        if (startDate && endDate) {
          loadTimeSeries();
          loadROI();
          loadScreenPerformance();
          loadCreativePerformance();
          loadGeographicPerformance();
        }
      }, refreshInterval * 1000);
      return () => clearInterval(interval);
    }
  }, [autoRefresh, refreshInterval, startDate, endDate, selectedCampaigns]);

  useEffect(() => {
    if (selectedCampaigns.length > 0 && startDate && endDate) {
      loadComparison();
    }
  }, [selectedCampaigns, startDate, endDate]);

  useEffect(() => {
    if (startDate && endDate) {
      loadTimeSeries();
      loadROI();
      loadScreenPerformance();
      loadCreativePerformance();
      loadGeographicPerformance();
    }
  }, [startDate, endDate]);

  const loadCampaigns = async () => {
    try {
      const data = await campaignApi.list();
      setCampaigns(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load campaigns:', error);
    }
  };

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

  const loadComparison = async () => {
    try {
      const data = await analyticsApi.compareCampaigns(
        selectedCampaigns,
        startDate ? new Date(startDate).toISOString() : undefined,
        endDate ? new Date(endDate).toISOString() : undefined
      );
      setComparison(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load comparison:', error);
    }
  };

  const loadTimeSeries = async () => {
    try {
      const data = await analyticsApi.timeSeries(
        undefined,
        new Date(startDate).toISOString(),
        new Date(endDate).toISOString(),
        1
      );
      setTimeSeries(data);
    } catch (error) {
      console.error('Failed to load time series:', error);
    }
  };

  const loadROI = async () => {
    try {
      const data = await analyticsApi.roi(
        startDate ? new Date(startDate).toISOString() : undefined,
        endDate ? new Date(endDate).toISOString() : undefined
      );
      setRoiMetrics(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load ROI metrics:', error);
    }
  };

  const loadScreenPerformance = async () => {
    try {
      const data = await analyticsApi.screens(
        startDate ? new Date(startDate).toISOString() : undefined,
        endDate ? new Date(endDate).toISOString() : undefined
      );
      setScreenPerformance(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load screen performance:', error);
    }
  };

  const loadCreativePerformance = async () => {
    try {
      const data = await analyticsApi.creatives(
        startDate ? new Date(startDate).toISOString() : undefined,
        endDate ? new Date(endDate).toISOString() : undefined
      );
      setCreativePerformance(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load creative performance:', error);
    }
  };

  const loadGeographicPerformance = async () => {
    try {
      const data = await analyticsApi.geographic(
        startDate ? new Date(startDate).toISOString() : undefined,
        endDate ? new Date(endDate).toISOString() : undefined
      );
      setGeographicPerformance(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error('Failed to load geographic performance:', error);
    }
  };

  const handleExport = async (format: 'csv' | 'json') => {
    try {
      await analyticsApi.export('campaigns', startDate, endDate, format);
    } catch (error) {
      console.error('Export failed:', error);
      alert('Export failed. Please try again.');
    }
  };

  if (loading) {
    return <div className="text-center py-8">Loading analytics...</div>;
  }

  const chartData = analytics?.topPerformingAds?.slice(0, 10) || [];

  const barChartData = {
    labels: chartData.map((ad: any) => ad.adId?.substring(0, 20) + '...' || 'Unknown'),
    datasets: [
      {
        label: 'Impressions',
        data: chartData.map((ad: any) => ad.impressions || 0),
        backgroundColor: 'rgba(59, 130, 246, 0.5)',
        borderColor: 'rgba(59, 130, 246, 1)',
        borderWidth: 1,
      },
    ],
  };

  const comparisonChartData = {
    labels: comparison.map((c: any) => c.campaignName || c.campaignId),
    datasets: [
      {
        label: 'Impressions',
        data: comparison.map((c: any) => c.impressions || 0),
        backgroundColor: 'rgba(16, 185, 129, 0.5)',
        borderColor: 'rgba(16, 185, 129, 1)',
        borderWidth: 1,
      },
    ],
  };

  const timeSeriesChartData = timeSeries?.points ? {
    labels: timeSeries.points.map((p: any) => new Date(p.timestamp).toLocaleDateString()),
    datasets: [
      {
        label: timeSeries.metric || 'Impressions',
        data: timeSeries.points.map((p: any) => p.value),
        borderColor: 'rgba(139, 92, 246, 1)',
        backgroundColor: 'rgba(139, 92, 246, 0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  } : null;

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">Analytics</h1>
        <div className="flex gap-4 items-center">
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
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="rounded"
            />
            <span className="text-sm text-gray-600">Auto-refresh</span>
          </label>
          {autoRefresh && (
            <select
              value={refreshInterval}
              onChange={(e) => setRefreshInterval(Number(e.target.value))}
              className="px-2 py-1 border border-gray-300 rounded text-sm"
            >
              <option value={10}>10s</option>
              <option value={30}>30s</option>
              <option value={60}>1m</option>
              <option value={300}>5m</option>
            </select>
          )}
          <button
            onClick={() => {
              loadAnalytics();
              if (selectedCampaigns.length > 0 && startDate && endDate) loadComparison();
              if (startDate && endDate) {
                loadTimeSeries();
                loadROI();
                loadScreenPerformance();
                loadCreativePerformance();
                loadGeographicPerformance();
              }
            }}
            className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700"
          >
            Refresh
          </button>
          <button
            onClick={() => handleExport('csv')}
            className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700"
            disabled={!startDate || !endDate}
          >
            Export CSV
          </button>
          <button
            onClick={() => handleExport('json')}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            disabled={!startDate || !endDate}
          >
            Export JSON
          </button>
        </div>
      </div>

      {/* Tabs */}
      <div className="border-b border-gray-200 mb-6">
        <nav className="-mb-px flex space-x-8 overflow-x-auto">
          <button
            onClick={() => setActiveTab('overview')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'overview'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Overview
          </button>
          <button
            onClick={() => setActiveTab('comparison')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'comparison'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Campaign Comparison
          </button>
          <button
            onClick={() => setActiveTab('roi')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'roi'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            ROI Metrics
          </button>
          <button
            onClick={() => setActiveTab('screens')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'screens'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Screen Performance
          </button>
          <button
            onClick={() => setActiveTab('creatives')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'creatives'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Creative Performance
          </button>
          <button
            onClick={() => setActiveTab('geographic')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'geographic'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Geographic
          </button>
          <button
            onClick={() => setActiveTab('timeseries')}
            className={`py-4 px-1 border-b-2 font-medium text-sm whitespace-nowrap ${
              activeTab === 'timeseries'
                ? 'border-blue-500 text-blue-600'
                : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
            }`}
          >
            Time Series
          </button>
        </nav>
      </div>

      {/* Overview Tab */}
      {activeTab === 'overview' && (
        <>
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
                          {new Date(activity.occurredAt || activity.timestamp).toLocaleString()}
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
        </>
      )}

      {/* Campaign Comparison Tab */}
      {activeTab === 'comparison' && (
        <div className="space-y-6">
          <div className="bg-white rounded-lg shadow p-6">
            <h2 className="text-xl font-semibold mb-4">Select Campaigns to Compare</h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4">
              {campaigns.map((campaign) => (
                <label key={campaign.id} className="flex items-center gap-2 p-3 border rounded-lg cursor-pointer hover:bg-gray-50">
                  <input
                    type="checkbox"
                    checked={selectedCampaigns.includes(campaign.id)}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedCampaigns([...selectedCampaigns, campaign.id]);
                      } else {
                        setSelectedCampaigns(selectedCampaigns.filter(id => id !== campaign.id));
                      }
                    }}
                  />
                  <span className="text-sm">{campaign.name}</span>
                </label>
              ))}
            </div>
            {selectedCampaigns.length === 0 && (
              <p className="text-gray-500 text-sm">Select at least one campaign to compare</p>
            )}
          </div>

          {comparison.length > 0 && (
            <>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Campaign Comparison</h2>
                <div className="h-96">
                  <Bar
                    data={comparisonChartData}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: {
                          display: false,
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

              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Comparison Details</h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Campaign</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Impressions</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Start Time</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">End Time</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {comparison.map((c: any, index: number) => (
                        <tr key={index}>
                          <td className="px-4 py-3 text-sm font-medium text-gray-900">{c.campaignName || c.campaignId}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{c.impressions}</td>
                          <td className="px-4 py-3 text-sm text-gray-500">{c.startTime ? new Date(c.startTime).toLocaleString() : 'N/A'}</td>
                          <td className="px-4 py-3 text-sm text-gray-500">{c.endTime ? new Date(c.endTime).toLocaleString() : 'N/A'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          )}
        </div>
      )}

      {/* ROI Metrics Tab */}
      {activeTab === 'roi' && (
        <div className="space-y-6">
          {roiMetrics.length > 0 ? (
            <>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">ROI Metrics</h2>
                <div className="h-96">
                  <Bar
                    data={{
                      labels: roiMetrics.map((r: any) => r.campaignName || r.campaignId),
                      datasets: [
                        {
                          label: 'Budget Utilization %',
                          data: roiMetrics.map((r: any) => r.budgetUtilization || 0),
                          backgroundColor: 'rgba(34, 197, 94, 0.5)',
                          borderColor: 'rgba(34, 197, 94, 1)',
                          borderWidth: 1,
                        },
                      ],
                    }}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: {
                        legend: { display: true },
                      },
                      scales: { y: { beginAtZero: true, max: 100 } },
                    }}
                  />
                </div>
              </div>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">ROI Details</h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Campaign</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Impressions</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Budget Allocated</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Budget Spent</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Utilization %</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {roiMetrics.map((r: any, index: number) => (
                        <tr key={index}>
                          <td className="px-4 py-3 text-sm font-medium text-gray-900">{r.campaignName || r.campaignId}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{r.impressions}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{r.budgetAllocated || 'N/A'}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{r.budgetSpent}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{r.budgetUtilization.toFixed(2)}%</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-500">No ROI data available. Select date range to view metrics.</p>
            </div>
          )}
        </div>
      )}

      {/* Screen Performance Tab */}
      {activeTab === 'screens' && (
        <div className="space-y-6">
          {screenPerformance.length > 0 ? (
            <>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Top Performing Screens</h2>
                <div className="h-96">
                  <Bar
                    data={{
                      labels: screenPerformance.slice(0, 10).map((s: any) => s.screenName || s.screenId),
                      datasets: [
                        {
                          label: 'Impressions',
                          data: screenPerformance.slice(0, 10).map((s: any) => s.impressions),
                          backgroundColor: 'rgba(168, 85, 247, 0.5)',
                          borderColor: 'rgba(168, 85, 247, 1)',
                          borderWidth: 1,
                        },
                      ],
                    }}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: { legend: { display: false } },
                      scales: { y: { beginAtZero: true } },
                    }}
                  />
                </div>
              </div>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Screen Performance Details</h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Screen</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">City</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Area</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Classification</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Impressions</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {screenPerformance.map((s: any, index: number) => (
                        <tr key={index}>
                          <td className="px-4 py-3 text-sm font-medium text-gray-900">{s.screenName || s.screenId}</td>
                          <td className="px-4 py-3 text-sm text-gray-500">{s.city || 'N/A'}</td>
                          <td className="px-4 py-3 text-sm text-gray-500">{s.area || 'N/A'}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{s.classification}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{s.impressions}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-500">No screen performance data available. Select date range to view metrics.</p>
            </div>
          )}
        </div>
      )}

      {/* Creative Performance Tab */}
      {activeTab === 'creatives' && (
        <div className="space-y-6">
          {creativePerformance.length > 0 ? (
            <>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Top Performing Creatives</h2>
                <div className="h-96">
                  <Bar
                    data={{
                      labels: creativePerformance.slice(0, 10).map((c: any) => c.creativeName || c.creativeId),
                      datasets: [
                        {
                          label: 'Impressions',
                          data: creativePerformance.slice(0, 10).map((c: any) => c.impressions),
                          backgroundColor: 'rgba(236, 72, 153, 0.5)',
                          borderColor: 'rgba(236, 72, 153, 1)',
                          borderWidth: 1,
                        },
                      ],
                    }}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: { legend: { display: false } },
                      scales: { y: { beginAtZero: true } },
                    }}
                  />
                </div>
              </div>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Creative Performance Details</h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Creative</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Campaign</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Impressions</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Play Count</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {creativePerformance.map((c: any, index: number) => (
                        <tr key={index}>
                          <td className="px-4 py-3 text-sm font-medium text-gray-900">{c.creativeName || c.creativeId}</td>
                          <td className="px-4 py-3 text-sm text-gray-500">{c.campaignName || c.campaignId}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{c.impressions}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{c.playCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-500">No creative performance data available. Select date range to view metrics.</p>
            </div>
          )}
        </div>
      )}

      {/* Geographic Performance Tab */}
      {activeTab === 'geographic' && (
        <div className="space-y-6">
          {geographicPerformance.length > 0 ? (
            <>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Performance by Location</h2>
                <div className="h-96">
                  <Bar
                    data={{
                      labels: geographicPerformance.slice(0, 10).map((g: any) => 
                        `${g.city || 'Unknown'}, ${g.area || 'N/A'}`
                      ),
                      datasets: [
                        {
                          label: 'Impressions',
                          data: geographicPerformance.slice(0, 10).map((g: any) => g.impressions),
                          backgroundColor: 'rgba(59, 130, 246, 0.5)',
                          borderColor: 'rgba(59, 130, 246, 1)',
                          borderWidth: 1,
                        },
                      ],
                    }}
                    options={{
                      responsive: true,
                      maintainAspectRatio: false,
                      plugins: { legend: { display: false } },
                      scales: { y: { beginAtZero: true } },
                    }}
                  />
                </div>
              </div>
              <div className="bg-white rounded-lg shadow p-6">
                <h2 className="text-xl font-semibold mb-4">Geographic Performance Details</h2>
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                      <tr>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">City</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Area</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Impressions</th>
                        <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Screen Count</th>
                      </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                      {geographicPerformance.map((g: any, index: number) => (
                        <tr key={index}>
                          <td className="px-4 py-3 text-sm font-medium text-gray-900">{g.city || 'N/A'}</td>
                          <td className="px-4 py-3 text-sm text-gray-500">{g.area || 'N/A'}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{g.impressions}</td>
                          <td className="px-4 py-3 text-sm text-gray-900">{g.screenCount}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            </>
          ) : (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-500">No geographic performance data available. Select date range to view metrics.</p>
            </div>
          )}
        </div>
      )}

      {/* Time Series Tab */}
      {activeTab === 'timeseries' && (
        <div className="space-y-6">
          {!startDate || !endDate ? (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-500">Please select start and end dates to view time series data.</p>
            </div>
          ) : timeSeriesChartData ? (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Impressions Over Time</h2>
              <div className="h-96">
                <Line
                  data={timeSeriesChartData}
                  options={{
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                      legend: {
                        display: true,
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
          ) : (
            <div className="bg-white rounded-lg shadow p-8 text-center">
              <p className="text-gray-500">Loading time series data...</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Analytics;
