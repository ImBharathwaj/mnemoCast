# Mnemocast Engine — Features Overview

This document describes the features implemented in the Mnemocast Ad Serving Engine.

---

## Core Features

### 1. Ad Delivery
- **Smart Ad Selection**: Random selection from eligible ads based on targeting rules
- **Targeting Rules**: Filter ads by country, platform, device, user, and app
- **Real-time Delivery**: Fast ad serving via HTTP API

### 2. Budget Management
Control ad delivery with flexible budget constraints:

- **Total Budget (`maxPlays`)**: Set a global limit on total plays across all time
- **Daily Limit (`dailyLimit`)**: Limit plays per day (resets at start of each day)
- **Hourly Limit (`hourlyLimit`)**: Limit plays per hour (resets at start of each hour)

**Behavior:**
- Budgets are checked before ad delivery
- Once exhausted, ads are automatically excluded from delivery
- Budgets reset automatically based on their time period
- All budget fields are optional (null = unlimited)

**Example:**
```json
{
  "maxPlays": 1000,      // Total: 1000 plays
  "dailyLimit": 100,     // Max 100 per day
  "hourlyLimit": 10      // Max 10 per hour
}
```

### 3. Frequency Capping
Limit how often an ad is shown to the same device or user:

- **Per-Device Capping (`maxImpressionsPerDevice`)**: Maximum impressions per device
- **Per-User Capping (`maxImpressionsPerUser`)**: Maximum impressions per user
- **Time Window (`frequencyCapWindowHours`)**: Time window for frequency cap (default: 24 hours)

**Behavior:**
- Frequency caps are checked per device/user before ad delivery
- Both device and user caps must be satisfied (AND logic)
- Time window determines how far back to look for previous impressions
- All frequency cap fields are optional (null = no capping)

**Example:**
```json
{
  "maxImpressionsPerDevice": 5,   // Max 5 per device
  "maxImpressionsPerUser": 3,     // Max 3 per user
  "frequencyCapWindowHours": 24   // Within 24 hours
}
```

### 4. Click Tracking
Track user clicks on ads:

- **Click Event Logging**: All clicks are logged with metadata (IP, timestamp, etc.)
- **Automatic Redirect**: Click tracking URLs redirect to the ad's target URL
- **Click-Through Rate (CTR)**: Calculate CTR for analytics

**Usage:**
1. Ad delivery response includes `clickTrackingUrl`
2. Client calls the click tracking URL when user clicks
3. Server logs the click event and redirects to target URL

### 5. Analytics & Reporting
Comprehensive analytics for ad performance:

- **Ad Performance Metrics**: Impressions, clicks, CTR for individual ads
- **Campaign Performance**: Aggregated metrics across all ads
- **Dashboard Summary**: Key metrics, top performers, recent activity
- **Time Range Filtering**: Filter analytics by date range

**Metrics Calculated:**
- Total impressions
- Total clicks
- Click-through rate (CTR) = (clicks / impressions) × 100
- Top performing ads (sorted by CTR and impressions)

---

## API Endpoints Summary

### Ad Delivery
- `GET /ads/deliver` - Request an ad for delivery

### Admin
- `POST /admin/ads` - Create a new ad
- `GET /admin/ads` - List all ads
- `GET /admin/ads/{adId}/events` - Get events for an ad

### Event Tracking
- `GET /api/v1/events/click` - Track click events
- `GET /api/v1/events/impression` - Track impression events

### Analytics
- `GET /api/v1/analytics/ads/{adId}` - Get ad performance metrics
- `GET /api/v1/analytics/campaigns` - Get campaign performance
- `GET /api/v1/analytics/dashboard` - Get dashboard summary

---

## Feature Workflow

### Ad Delivery Flow

1. **Request Received**: Client requests ad via `GET /ads/deliver`
2. **Targeting Filter**: Ads filtered by targeting rules
3. **Budget Check**: Ads filtered by budget constraints (maxPlays, dailyLimit, hourlyLimit)
4. **Frequency Cap Check**: Ads filtered by frequency capping rules
5. **Ad Selected**: Random selection from eligible ads
6. **Response Sent**: Ad details with tracking URLs returned
7. **Event Logged**: Impression event logged automatically

### Click Tracking Flow

1. **User Clicks**: Client calls `clickTrackingUrl` from ad response
2. **Click Logged**: Server logs click event with metadata
3. **Redirect**: Server redirects to ad's `targetUrl`
4. **Analytics Updated**: Click counted in analytics metrics

### Analytics Flow

1. **Query Requested**: Client requests analytics via analytics endpoints
2. **Events Retrieved**: Server queries event store for relevant events
3. **Metrics Calculated**: Impressions, clicks, CTR calculated
4. **Response Sent**: Metrics returned as JSON

---

## Configuration Examples

### Creating an Ad with Budget Management

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-1",
    "creativeUrl": "https://example.com/ad.mp4",
    "targetUrl": "https://example.com/landing",
    "targetingRules": [
      {"key": "country", "operator": "eq", "value": "IN"}
    ],
    "isActive": true,
    "maxPlays": 5000,
    "dailyLimit": 500,
    "hourlyLimit": 50
  }'
```

### Creating an Ad with Frequency Capping

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-2",
    "creativeUrl": "https://example.com/ad2.jpg",
    "targetUrl": "https://example.com/offer",
    "targetingRules": [],
    "isActive": true,
    "maxImpressionsPerDevice": 3,
    "maxImpressionsPerUser": 2,
    "frequencyCapWindowHours": 12
  }'
```

### Querying Analytics

```bash
# Get ad performance
curl "http://localhost:8080/api/v1/analytics/ads/ad-123"

# Get performance for time range
curl "http://localhost:8080/api/v1/analytics/ads/ad-123?startTime=2024-01-01T00:00:00Z&endTime=2024-01-31T23:59:59Z"

# Get dashboard summary
curl "http://localhost:8080/api/v1/analytics/dashboard?topN=5"
```

---

## Technical Details

### Budget Enforcement
- Budgets are checked **before** ad delivery
- Budgets use UTC timezone for daily/hourly calculations
- Budget exhaustion is automatic (no manual intervention needed)
- Budgets are evaluated in order: maxPlays → dailyLimit → hourlyLimit

### Frequency Capping
- Frequency caps are checked **per device/user** before ad delivery
- Device and user caps are evaluated independently (both must pass)
- Time window is calculated from current time backwards
- Frequency cap data is stored in event metadata

### Analytics Calculation
- Impressions: Count of events with `eventType == "impression"`
- Clicks: Count of events with `eventType == "click"`
- CTR: `(clicks / impressions) × 100` (0.0 if no impressions)
- Time filtering: Events filtered by `occurredAt` timestamp

### Event Storage
- All events stored in Redis
- Events indexed by `adId` for fast queries
- Events include metadata (deviceId, userId, IP, country, platform)
- Events are append-only (immutable)

---

## Future Enhancements

See `docs/Epic3.md` for planned features:
- Advanced targeting (time-based, geographic, device type)
- Campaign grouping and management
- Revenue/ROI tracking
- Export reports (CSV/JSON)
- Performance-based ad prioritization
- A/B testing support

---

For detailed API documentation, see `docs/02-api-spec.md`.

