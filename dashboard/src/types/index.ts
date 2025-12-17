export interface Campaign {
  id: string;
  name: string;
  advertiserId: string;
  status: string;
  startDate: string;
  endDate: string;
  totalBudget?: number;
  targetPlayouts?: number;
  targetingRules: TargetingRule[];
  priority: number;
  createdAt: string;
  updatedAt: string;
}

export interface Creative {
  id: string;
  campaignId: string;
  name: string;
  creativeType: string;
  creativeUrl: string;
  targetUrl?: string;
  durationSeconds: number;
  status: string;
  shareOfVoice?: number;
  frequencyCapPerScreen?: number;
  metadata: Record<string, string>;
  createdAt: string;
  updatedAt: string;
}

export interface Screen {
  id: string;
  name: string;
  location: ScreenLocation;
  tags: string[];
  metadata: Record<string, string>;
  classification: number; // Screen classification (1-10, higher = premium)
  isOnline: boolean;
  lastSeen?: string;
  createdAt: string;
  updatedAt: string;
}

export interface ScreenLocation {
  country?: string;
  city?: string;
  area?: string;
  venueType?: string;
  timezone?: string;
}

export interface TargetingRule {
  key: string;
  operator: string;
  value: string;
}

export interface PlaylistResponse {
  requestId: string;
  screenId: string;
  items: PlaylistItem[];
  validForSeconds: number;
  totalDurationSeconds: number;
}

export interface PlaylistItem {
  adId: string;
  creativeUrl: string;
  targetUrl?: string;
  durationSeconds: number;
  impressionTrackingUrl?: string;
  position: number;
}

export interface AnalyticsDashboard {
  totalAds: number;
  activeAds: number;
  totalImpressions: number;
  topAds: Array<{
    adId: string;
    impressions: number;
  }>;
  recentActivity: Array<{
    adId: string;
    timestamp: string;
    eventType: string;
  }>;
}

