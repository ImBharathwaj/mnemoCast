export const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

console.log('API Base URL:', API_BASE_URL);

export const API_ENDPOINTS = {
  campaigns: `${API_BASE_URL}/api/v1/campaigns`,
  creatives: `${API_BASE_URL}/api/v1/creatives`,
  campaignCreatives: (campaignId: string) => `${API_BASE_URL}/api/v1/campaigns/${campaignId}/creatives`,
  screens: `${API_BASE_URL}/api/v1/screens`,
  screenRegister: `${API_BASE_URL}/api/v1/screens/register`,
  screenHeartbeat: (screenId: string) => `${API_BASE_URL}/api/v1/screens/${screenId}/heartbeat`,
  playlist: (screenId: string, durationMinutes: number = 3) => 
    `${API_BASE_URL}/api/v1/screens/${screenId}/playlist?durationMinutes=${durationMinutes}`,
  analytics: {
    dashboard: `${API_BASE_URL}/api/v1/analytics/dashboard`,
    ad: (adId: string) => `${API_BASE_URL}/api/v1/analytics/ads/${adId}`,
    campaigns: `${API_BASE_URL}/api/v1/analytics/campaigns`,
    compareCampaigns: `${API_BASE_URL}/api/v1/analytics/campaigns/compare`,
    roi: `${API_BASE_URL}/api/v1/analytics/campaigns/roi`,
    screens: `${API_BASE_URL}/api/v1/analytics/screens`,
    creatives: `${API_BASE_URL}/api/v1/analytics/creatives`,
    geographic: `${API_BASE_URL}/api/v1/analytics/geographic`,
    timeSeries: `${API_BASE_URL}/api/v1/analytics/timeseries`,
    export: `${API_BASE_URL}/api/v1/analytics/export`,
  },
  health: {
    health: `${API_BASE_URL}/api/v1/health`,
    ready: `${API_BASE_URL}/api/v1/ready`,
    live: `${API_BASE_URL}/api/v1/live`,
    metrics: `${API_BASE_URL}/api/v1/metrics`,
  },
  mediaUpload: `${API_BASE_URL}/api/v1/creatives/upload`,
  mediaServe: (filename: string) => `${API_BASE_URL}/api/v1/media/creatives/${filename}`,
};

