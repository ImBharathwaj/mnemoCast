import axios from 'axios';
import { API_ENDPOINTS } from '../config/api';
import { authService } from './authService';

const api = axios.create({
  baseURL: '',
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000, // 10 second timeout
});

// Add request interceptor for auth headers and debugging
api.interceptors.request.use(
  (config) => {
    // Add auth token to all requests
    const authHeaders = authService.getAuthHeaders();
    if (authHeaders.Authorization) {
      config.headers.Authorization = authHeaders.Authorization;
    }
    console.log(`API Request: ${config.method?.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    console.error('API Request Error:', error);
    return Promise.reject(error);
  }
);

// Add response interceptor for debugging
api.interceptors.response.use(
  (response) => {
    console.log(`API Response: ${response.status} ${response.config.url}`);
    return response;
  },
  (error) => {
    console.error(`API Error: ${error.response?.status || 'Network Error'} ${error.config?.url}`);
    if (error.response) {
      console.error('Response data:', error.response.data);
    }
    return Promise.reject(error);
  }
);

// Campaign APIs
export const campaignApi = {
  list: (activeOnly: boolean = false) =>
    api.get(`${API_ENDPOINTS.campaigns}?activeOnly=${activeOnly}`).then(res => res.data),
  
  get: (id: string) =>
    api.get(`${API_ENDPOINTS.campaigns}/${id}`).then(res => res.data),
  
  create: (data: any) =>
    api.post(API_ENDPOINTS.campaigns, data).then(res => res.data),
  
  update: (id: string, data: any) =>
    api.put(`${API_ENDPOINTS.campaigns}/${id}`, data).then(res => res.data),
  
  delete: (id: string) =>
    api.delete(`${API_ENDPOINTS.campaigns}/${id}`).then(res => res.data),
};

// Creative APIs
export const creativeApi = {
  list: () =>
    api.get(`${API_ENDPOINTS.creatives}`).then(res => res.data),
  
  get: (id: string) =>
    api.get(`${API_ENDPOINTS.creatives}/${id}`).then(res => res.data),
  
  listByCampaign: (campaignId: string) =>
    api.get(API_ENDPOINTS.campaignCreatives(campaignId)).then(res => res.data),
  
  create: (campaignId: string, data: any) =>
    api.post(API_ENDPOINTS.campaignCreatives(campaignId), data).then(res => res.data),
  
  update: (id: string, data: any) =>
    api.put(`${API_ENDPOINTS.creatives}/${id}`, data).then(res => res.data),
  
  delete: (id: string) =>
    api.delete(`${API_ENDPOINTS.creatives}/${id}`).then(res => res.data),
};

// Screen APIs
export const screenApi = {
  list: () =>
    api.get(API_ENDPOINTS.screens).then(res => res.data),
  
  get: (id: string) =>
    api.get(`${API_ENDPOINTS.screens}/${id}`).then(res => res.data),
  
  register: (data: any) =>
    api.post(API_ENDPOINTS.screenRegister, data).then(res => res.data),
  
  update: (id: string, data: any) =>
    api.put(`${API_ENDPOINTS.screens}/${id}`, data).then(res => res.data),
  
  delete: (id: string) =>
    api.delete(`${API_ENDPOINTS.screens}/${id}`).then(res => res.data),
  
  heartbeat: (id: string) =>
    api.put(API_ENDPOINTS.screenHeartbeat(id)).then(res => res.data),
};

// Playlist APIs
export const playlistApi = {
  generate: (screenId: string, durationMinutes: number = 3) =>
    api.get(API_ENDPOINTS.playlist(screenId, durationMinutes)).then(res => res.data),
};

// Analytics APIs
export const analyticsApi = {
  dashboard: (startDate?: string, endDate?: string) => {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    const query = params.toString();
    return api.get(`${API_ENDPOINTS.analytics.dashboard}${query ? '?' + query : ''}`).then(res => res.data);
  },
  
  ad: (adId: string, startDate?: string, endDate?: string) => {
    const params = new URLSearchParams();
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    const query = params.toString();
    return api.get(`${API_ENDPOINTS.analytics.ad(adId)}${query ? '?' + query : ''}`).then(res => res.data);
  },
  
  campaigns: (startDate?: string, endDate?: string, format?: string) => {
    const params = new URLSearchParams();
    if (startDate) params.append('startTime', startDate);
    if (endDate) params.append('endTime', endDate);
    if (format) params.append('format', format);
    const query = params.toString();
    return api.get(`${API_ENDPOINTS.analytics.campaigns}${query ? '?' + query : ''}`).then(res => res.data);
  },
  
  compareCampaigns: (campaignIds: string[], startTime?: string, endTime?: string, format?: string) => {
    const params = new URLSearchParams();
    params.append('campaignIds', campaignIds.join(','));
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (format) params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.compareCampaigns}?${params.toString()}`).then(res => res.data);
  },
  
  timeSeries: (adId: string | undefined, startTime: string, endTime: string, intervalHours: number = 1, format?: string) => {
    const params = new URLSearchParams();
    if (adId) params.append('adId', adId);
    params.append('startTime', startTime);
    params.append('endTime', endTime);
    params.append('intervalHours', intervalHours.toString());
    if (format) params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.timeSeries}?${params.toString()}`).then(res => res.data);
  },
  
  roi: (startTime?: string, endTime?: string, format?: string) => {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (format) params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.roi}${params.toString() ? '?' + params.toString() : ''}`).then(res => res.data);
  },
  
  screens: (startTime?: string, endTime?: string, format?: string) => {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (format) params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.screens}${params.toString() ? '?' + params.toString() : ''}`).then(res => res.data);
  },
  
  creatives: (startTime?: string, endTime?: string, format?: string) => {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (format) params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.creatives}${params.toString() ? '?' + params.toString() : ''}`).then(res => res.data);
  },
  
  geographic: (startTime?: string, endTime?: string, format?: string) => {
    const params = new URLSearchParams();
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    if (format) params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.geographic}${params.toString() ? '?' + params.toString() : ''}`).then(res => res.data);
  },
  
  export: (type: string, startTime?: string, endTime?: string, format: string = 'csv') => {
    const params = new URLSearchParams();
    params.append('type', type);
    if (startTime) params.append('startTime', startTime);
    if (endTime) params.append('endTime', endTime);
    params.append('format', format);
    return api.get(`${API_ENDPOINTS.analytics.export}?${params.toString()}`, {
      responseType: 'blob',
    }).then(res => {
      const blob = new Blob([res.data], { type: format === 'csv' ? 'text/csv' : 'application/json' });
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `analytics-export-${Date.now()}.${format}`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
    });
  },
};

// Health and Metrics APIs
export const healthApi = {
  health: () => api.get(API_ENDPOINTS.health.health).then(res => res.data),
  ready: () => api.get(API_ENDPOINTS.health.ready).then(res => res.data),
  live: () => api.get(API_ENDPOINTS.health.live).then(res => res.data),
  metrics: () => api.get(API_ENDPOINTS.health.metrics).then(res => res.data),
};

// Media Upload API
export const mediaApi = {
  upload: (file: File, campaignId?: string, creativeId?: string, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);
    if (campaignId) {
      formData.append('campaignId', campaignId);
    }
    if (creativeId) {
      formData.append('creativeId', creativeId);
    }
    
    return api.post(API_ENDPOINTS.mediaUpload, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (onProgress && progressEvent.total) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(progress);
        }
      },
    }).then(res => res.data);
  },
};

export default api;

