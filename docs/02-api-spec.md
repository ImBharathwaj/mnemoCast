# Mnemocast Engine — API Specification

> HTTP API endpoints for the Mnemocast Ad Serving Engine

**Base URL:** `http://localhost:8080`

---

## Ad Delivery API

### GET /ads/deliver

Request an ad for delivery to a screen/device.

**Query Parameters:**

| Parameter | Type   | Required | Description                          |
|-----------|--------|----------|--------------------------------------|
| deviceId  | string | No       | Unique device identifier              |
| userId    | string | No       | User identifier (if available)        |
| appId     | string | No       | Application identifier                |
| country   | string | No       | Country code (e.g., "IN", "US")       |
| platform  | string | No       | Platform ("android", "ios", "web")    |

**Response:**

- **200 OK**: Ad successfully delivered
  ```json
  {
    "requestId": "uuid",
    "adId": "ad-123",
    "creativeUrl": "https://cdn.example.com/ad.mp4",
    "targetUrl": "https://example.com/landing",
    "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-123&requestId=uuid"
  }
  ```

- **204 No Content**: No matching ad found (all targeting rules failed)

**Example:**

```bash
curl "http://localhost:8080/ads/deliver?country=IN&platform=android"
```

---

## Admin API

### POST /admin/ads

Create a new ad with targeting rules.

**Request Body:**

```json
{
  "id": "optional-uuid",
  "advertiserId": "advertiser-123",
  "creativeUrl": "https://cdn.example.com/ad.mp4",
  "targetUrl": "https://example.com/landing",
  "targetingRules": [
    {
      "key": "country",
      "operator": "eq",
      "value": "IN"
    },
    {
      "key": "platform",
      "operator": "in",
      "value": "android,ios"
    }
  ],
  "isActive": true,
  "maxPlays": 1000,
  "dailyLimit": 100,
  "hourlyLimit": 10,
  "maxImpressionsPerDevice": 5,
  "maxImpressionsPerUser": 3,
  "frequencyCapWindowHours": 24
}
```

**Budget Fields (all optional):**
- `maxPlays` - Total maximum plays across all time (null = unlimited)
- `dailyLimit` - Maximum plays per day (null = unlimited)
- `hourlyLimit` - Maximum plays per hour (null = unlimited)

**Frequency Capping Fields (all optional):**
- `maxImpressionsPerDevice` - Maximum impressions per device in time window (null = no limit)
- `maxImpressionsPerUser` - Maximum impressions per user in time window (null = no limit)
- `frequencyCapWindowHours` - Time window in hours for frequency cap (defaults to 24 if not set)

**Response:**

- **200 OK**: Ad created successfully (returns the created ad with generated fields)
  ```json
  {
    "id": "generated-uuid",
    "advertiserId": "advertiser-123",
    "creativeUrl": "https://cdn.example.com/ad.mp4",
    "targetUrl": "https://example.com/landing",
    "targetingRules": [...],
    "isActive": true,
    "createdAt": "2025-12-03T12:00:00Z",
    "updatedAt": "2025-12-03T12:00:00Z"
  }
  ```

**Example:**

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [
      {"key": "country", "operator": "eq", "value": "IN"}
    ],
    "isActive": true,
    "maxPlays": 1000,
    "dailyLimit": 100,
    "hourlyLimit": 10
  }'
```

---

### GET /admin/ads

List all ads.

**Query Parameters:**

| Parameter  | Type    | Required | Default | Description                    |
|------------|---------|----------|---------|--------------------------------|
| activeOnly | boolean | No       | true    | Filter to show only active ads |

**Response:**

- **200 OK**: Array of ads
  ```json
  [
    {
      "id": "ad-1",
      "advertiserId": "advertiser-123",
      "creativeUrl": "https://cdn.example.com/ad.mp4",
      "targetUrl": "https://example.com/landing",
      "targetingRules": [...],
      "isActive": true,
      "createdAt": "2025-12-03T12:00:00Z",
      "updatedAt": "2025-12-03T12:00:00Z"
    }
  ]
  ```

**Examples:**

```bash
# List only active ads (default)
curl "http://localhost:8080/admin/ads"

# List all ads (including inactive)
curl "http://localhost:8080/admin/ads?activeOnly=false"
```

---

### GET /admin/ads/{adId}/events

Get delivery events (impressions) for a specific ad.

**Path Parameters:**

| Parameter | Type   | Required | Description        |
|-----------|--------|----------|--------------------|
| adId      | string | Yes      | The ad identifier  |

**Query Parameters:**

| Parameter | Type   | Required | Default | Description                    |
|-----------|--------|----------|---------|--------------------------------|
| limit     | int    | No       | 50      | Maximum number of events       |

**Response:**

- **200 OK**: Array of delivery events
  ```json
  [
    {
      "eventId": "event-1",
      "requestId": "req-1",
      "adId": "ad-1",
      "eventType": "impression",
      "occurredAt": "2025-12-03T12:00:00Z",
      "metadata": {
        "deviceId": "device-123",
        "country": "IN",
        "platform": "android"
      }
    }
  ]
  ```

**Example:**

```bash
curl "http://localhost:8080/admin/ads/ad-1/events?limit=10"
```

---

## Event Tracking API

### GET /api/v1/events/impression

Track impression events (for client-side tracking).

**Query Parameters:**

| Parameter | Type   | Required | Description                    |
|-----------|--------|----------|--------------------------------|
| adId      | string | Yes      | The ad identifier              |
| requestId | string | No       | Original request ID (optional) |

**Response:**

- **204 No Content**: Impression event logged successfully

**Example:**

```bash
# Impression tracking URL (typically called by client when ad is displayed)
curl "http://localhost:8080/api/v1/events/impression?adId=ad-123&requestId=req-456"
```

**Note:** This endpoint is useful for client-side impression tracking when the server-side impression logging isn't sufficient.

---

## Analytics API

### GET /api/v1/analytics/ads/{adId}

Get performance metrics for a specific ad.

**Path Parameters:**

| Parameter | Type   | Required | Description       |
|-----------|--------|----------|-------------------|
| adId      | string | Yes      | The ad identifier |

**Query Parameters:**

| Parameter | Type   | Required | Description                                    |
|-----------|--------|----------|------------------------------------------------|
| startTime | string | No       | Start time in ISO 8601 format (e.g., "2024-01-01T00:00:00Z") |
| endTime   | string | No       | End time in ISO 8601 format (defaults to now)  |

**Response:**

- **200 OK**: Ad performance metrics
  ```json
  {
    "adId": "ad-123",
    "impressions": 1500,
    "startTime": "2024-01-01T00:00:00Z",
    "endTime": "2024-01-31T23:59:59Z"
  }
  ```

- **404 Not Found**: Ad not found

**Example:**

```bash
# Get all-time performance
curl "http://localhost:8080/api/v1/analytics/ads/ad-123"

# Get performance for a specific time range
curl "http://localhost:8080/api/v1/analytics/ads/ad-123?startTime=2024-01-01T00:00:00Z&endTime=2024-01-31T23:59:59Z"
```

---

### GET /api/v1/analytics/campaigns

Get performance metrics for all campaigns (ads).

**Query Parameters:**

| Parameter | Type   | Required | Description                                    |
|-----------|--------|----------|------------------------------------------------|
| startTime | string | No       | Start time in ISO 8601 format                  |
| endTime   | string | No       | End time in ISO 8601 format (defaults to now)  |

**Response:**

- **200 OK**: Array of campaign performance metrics
  ```json
  [
    {
      "campaignId": "ad-123",
      "totalImpressions": 1500,
      "ads": [
        {
          "adId": "ad-123",
          "impressions": 1500
        }
      ]
    }
  ]
  ```

**Example:**

```bash
# Get all campaign performance
curl "http://localhost:8080/api/v1/analytics/campaigns"

# Get performance for a specific time range
curl "http://localhost:8080/api/v1/analytics/campaigns?startTime=2024-01-01T00:00:00Z"
```

---

### GET /api/v1/analytics/dashboard

Get dashboard summary with key metrics.

**Query Parameters:**

| Parameter | Type   | Required | Default | Description                          |
|-----------|--------|----------|---------|--------------------------------------|
| topN      | int    | No       | 10      | Number of top performing ads to show |

**Response:**

- **200 OK**: Dashboard metrics
  ```json
  {
    "totalAds": 25,
    "activeAds": 18,
    "totalImpressions": 50000,
    "topPerformingAds": [
      {
        "adId": "ad-123",
        "impressions": 5000,
        "ctr": 4.0
      }
    ],
    "recentActivity": [
      {
        "eventId": "event-1",
        "requestId": "req-1",
        "adId": "ad-123",
        "eventType": "impression",
        "occurredAt": "2024-01-15T10:30:00Z",
        "metadata": {}
      }
    ]
  }
  ```

**Example:**

```bash
# Get dashboard with default top 10 ads
curl "http://localhost:8080/api/v1/analytics/dashboard"

# Get dashboard with top 5 ads
curl "http://localhost:8080/api/v1/analytics/dashboard?topN=5"
```

---

## Budget Management

Ads can have budget constraints to control delivery:

### Budget Fields

| Field         | Type    | Description                                    |
|---------------|---------|------------------------------------------------|
| maxPlays      | int?    | Total maximum plays across all time (global)   |
| dailyLimit    | int?    | Maximum plays per day                          |
| hourlyLimit   | int?    | Maximum plays per hour                         |

**Behavior:**
- If a budget field is `null` or not provided, that limit is not enforced
- Budgets are checked before ad delivery
- Once a budget is exhausted, the ad is automatically excluded from delivery
- Budgets reset automatically (daily/hourly limits reset at the start of each period)

**Example:**

```json
{
  "maxPlays": 1000,      // Total limit: 1000 plays
  "dailyLimit": 100,     // Max 100 plays per day
  "hourlyLimit": 10      // Max 10 plays per hour
}
```

---

## Frequency Capping

Frequency capping limits how often an ad is shown to the same device or user:

### Frequency Cap Fields

| Field                    | Type    | Description                                    |
|--------------------------|---------|------------------------------------------------|
| maxImpressionsPerDevice  | int?    | Maximum impressions per device in time window  |
| maxImpressionsPerUser    | int?    | Maximum impressions per user in time window    |
| frequencyCapWindowHours  | int?    | Time window in hours (defaults to 24 if not set) |

**Behavior:**
- If frequency cap fields are `null` or not provided, frequency capping is not enforced
- Frequency caps are checked per device/user before ad delivery
- The time window determines how far back to look for previous impressions
- Once the cap is reached, the ad won't be shown to that device/user until the window expires

**Example:**

```json
{
  "maxImpressionsPerDevice": 5,   // Max 5 impressions per device
  "maxImpressionsPerUser": 3,     // Max 3 impressions per user
  "frequencyCapWindowHours": 24   // Within a 24-hour window
}
```

This means:
- A device can see the ad at most 5 times in any 24-hour period
- A user can see the ad at most 3 times in any 24-hour period
- Both conditions must be satisfied for the ad to be delivered

---

## Targeting Rules

Targeting rules determine which ads are eligible for a given delivery request.

### Rule Structure

```json
{
  "key": "country",
  "operator": "eq",
  "value": "IN"
}
```

### Supported Keys

- `country` - Country code (matches `DeliveryRequest.country`)
- `platform` - Platform type (matches `DeliveryRequest.platform`)
- `deviceId` - Device identifier (matches `DeliveryRequest.deviceId`)
- `userId` - User identifier (matches `DeliveryRequest.userId`)
- `appId` - Application identifier (matches `DeliveryRequest.appId`)

### Supported Operators

- **`eq`** - Exact match (case-insensitive)
  - Example: `{"key": "country", "operator": "eq", "value": "IN"}`

- **`in`** - List membership (comma-separated values, case-insensitive)
  - Example: `{"key": "platform", "operator": "in", "value": "android,ios"}`

### Rule Evaluation Logic

- If an ad has **no targeting rules**, it matches all requests (default: show everywhere)
- All targeting rules must pass for an ad to be eligible (AND logic)
- If any rule fails, the ad is excluded from delivery

---

## Error Responses

All endpoints may return standard HTTP error codes:

- **400 Bad Request** - Invalid request format or parameters
- **404 Not Found** - Resource not found
- **500 Internal Server Error** - Server error

Error response format (when applicable):

```json
{
  "error": "Error message description"
}
```

