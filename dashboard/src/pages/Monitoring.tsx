import React, { useEffect, useState } from 'react';
import { healthApi } from '../services/api';
import { Line, Bar } from 'react-chartjs-2';

const Monitoring: React.FC = () => {
  const [health, setHealth] = useState<any>(null);
  const [metrics, setMetrics] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [autoRefresh, setAutoRefresh] = useState(true);

  useEffect(() => {
    loadData();
    if (autoRefresh) {
      const interval = setInterval(loadData, 5000); // Refresh every 5 seconds
      return () => clearInterval(interval);
    }
  }, [autoRefresh]);

  const loadData = async () => {
    try {
      const [healthData, metricsData] = await Promise.all([
        healthApi.health(),
        healthApi.metrics(),
      ]);
      setHealth(healthData);
      setMetrics(metricsData);
    } catch (error) {
      console.error('Failed to load monitoring data:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'UP':
        return 'bg-green-500';
      case 'DOWN':
        return 'bg-red-500';
      case 'DEGRADED':
        return 'bg-yellow-500';
      default:
        return 'bg-gray-500';
    }
  };

  const getComponentStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'text-green-600';
      case 'unhealthy':
        return 'text-red-600';
      case 'unknown':
        return 'text-gray-600';
      default:
        return 'text-gray-600';
    }
  };

  if (loading && !health) {
    return <div className="text-center py-8">Loading monitoring data...</div>;
  }

  const endpointChartData = metrics?.endpoints?.slice(0, 10) || [];

  const endpointChart = {
    labels: endpointChartData.map((ep: any) => `${ep.method} ${ep.endpoint.substring(0, 30)}`),
    datasets: [
      {
        label: 'Avg Response Time (ms)',
        data: endpointChartData.map((ep: any) => ep.avgResponseTimeMs?.toFixed(2) || 0),
        backgroundColor: 'rgba(59, 130, 246, 0.5)',
        borderColor: 'rgba(59, 130, 246, 1)',
        borderWidth: 1,
      },
    ],
  };

  return (
    <div>
      <div className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold text-gray-900">System Monitoring</h1>
        <div className="flex items-center gap-4">
          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="rounded"
            />
            <span className="text-sm text-gray-600">Auto-refresh (5s)</span>
          </label>
          <button
            onClick={loadData}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
          >
            Refresh
          </button>
        </div>
      </div>

      {/* System Health Status */}
      {health && (
        <div className="mb-6">
          <div className="bg-white rounded-lg shadow p-6">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-semibold">System Health</h2>
              <div className="flex items-center gap-3">
                <div className={`w-4 h-4 rounded-full ${getStatusColor(health.status)}`}></div>
                <span className="text-lg font-bold">{health.status}</span>
              </div>
            </div>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
              <div>
                <span className="text-gray-600">Uptime:</span>
                <p className="font-semibold">{Math.floor(health.uptimeSeconds / 3600)}h {Math.floor((health.uptimeSeconds % 3600) / 60)}m</p>
              </div>
              <div>
                <span className="text-gray-600">Version:</span>
                <p className="font-semibold">{health.version}</p>
              </div>
              <div>
                <span className="text-gray-600">Timestamp:</span>
                <p className="font-semibold text-xs">{health.timestamp}</p>
              </div>
              <div>
                <span className="text-gray-600">Components:</span>
                <p className="font-semibold">{health.components?.length || 0}</p>
              </div>
            </div>

            {/* Component Health */}
            {health.components && health.components.length > 0 && (
              <div className="mt-4">
                <h3 className="text-sm font-semibold text-gray-700 mb-2">Component Status</h3>
                <div className="space-y-2">
                  {health.components.map((component: any, index: number) => (
                    <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                      <div className="flex items-center gap-3">
                        <span className={`font-medium ${getComponentStatusColor(component.status)}`}>
                          {component.name}
                        </span>
                        {component.responseTimeMs && (
                          <span className="text-xs text-gray-500">
                            {component.responseTimeMs}ms
                          </span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <span className={`text-xs ${getComponentStatusColor(component.status)}`}>
                          {component.status}
                        </span>
                        {component.message && (
                          <span className="text-xs text-gray-500" title={component.message}>
                            ℹ️
                          </span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>
        </div>
      )}

      {/* System Metrics */}
      {metrics && (
        <div className="space-y-6">
          {/* Key Metrics Cards */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-6">
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Total Requests</h3>
              <p className="text-3xl font-bold text-gray-900">{metrics.totalRequests || 0}</p>
            </div>
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Error Rate</h3>
              <p className="text-3xl font-bold text-red-600">
                {(metrics.errorRate * 100).toFixed(2)}%
              </p>
            </div>
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Avg Response Time</h3>
              <p className="text-3xl font-bold text-blue-600">
                {metrics.avgResponseTimeMs?.toFixed(2) || 0}ms
              </p>
            </div>
            <div className="bg-white rounded-lg shadow p-6">
              <h3 className="text-sm text-gray-600 mb-2">Memory Usage</h3>
              <p className="text-3xl font-bold text-purple-600">
                {metrics.memoryUsageMB || 0} MB
              </p>
            </div>
          </div>

          {/* Performance Metrics */}
          <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Response Time Percentiles</h2>
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">P95:</span>
                  <span className="font-semibold">{metrics.p95ResponseTimeMs || 'N/A'}ms</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">P99:</span>
                  <span className="font-semibold">{metrics.p99ResponseTimeMs || 'N/A'}ms</span>
                </div>
              </div>
            </div>

            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Active Resources</h2>
              <div className="space-y-3">
                <div className="flex justify-between">
                  <span className="text-gray-600">Campaigns:</span>
                  <span className="font-semibold">{metrics.activeCampaigns || 0}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Creatives:</span>
                  <span className="font-semibold">{metrics.activeCreatives || 0}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-gray-600">Screens:</span>
                  <span className="font-semibold">{metrics.activeScreens || 0}</span>
                </div>
              </div>
            </div>
          </div>

          {/* Endpoint Performance Chart */}
          {endpointChartData.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Top Endpoints by Response Time</h2>
              <div className="h-96">
                <Bar
                  data={endpointChart}
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
                        title: {
                          display: true,
                          text: 'Response Time (ms)',
                        },
                      },
                      x: {
                        ticks: {
                          maxRotation: 45,
                          minRotation: 45,
                        },
                      },
                    },
                  }}
                />
              </div>
            </div>
          )}

          {/* Endpoint Details Table */}
          {metrics.endpoints && metrics.endpoints.length > 0 && (
            <div className="bg-white rounded-lg shadow p-6">
              <h2 className="text-xl font-semibold mb-4">Endpoint Metrics</h2>
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                  <thead className="bg-gray-50">
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Method</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Endpoint</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Requests</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Errors</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Avg Time</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">P95</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">P99</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white divide-y divide-gray-200">
                    {metrics.endpoints.slice(0, 20).map((endpoint: any, index: number) => (
                      <tr key={index}>
                        <td className="px-4 py-3 text-sm font-medium text-gray-900">{endpoint.method}</td>
                        <td className="px-4 py-3 text-sm text-gray-500">{endpoint.endpoint}</td>
                        <td className="px-4 py-3 text-sm text-gray-900">{endpoint.requestCount}</td>
                        <td className="px-4 py-3 text-sm text-red-600">{endpoint.errorCount}</td>
                        <td className="px-4 py-3 text-sm text-gray-900">{endpoint.avgResponseTimeMs?.toFixed(2)}ms</td>
                        <td className="px-4 py-3 text-sm text-gray-900">{endpoint.p95ResponseTimeMs || 'N/A'}</td>
                        <td className="px-4 py-3 text-sm text-gray-900">{endpoint.p99ResponseTimeMs || 'N/A'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};

export default Monitoring;

