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
- `screenId` - Screen identifier (OOH, matches `DeliveryRequest.screenId`)
- `city` - City name (OOH, matches `DeliveryRequest.city`)
- `area` - Area/neighborhood (OOH, matches `DeliveryRequest.area`)
- `venueType` / `venue_type` - Venue type (OOH, matches `DeliveryRequest.venueType`)
- `screenTag` / `tag` - Screen tag (OOH, matches `DeliveryRequest.screenTags`)
- `timezone` - IANA timezone identifier (OOH, matches `DeliveryRequest.timezone`)

### Supported Operators

- **`eq`** - Exact match (case-insensitive)
  - Example: `{"key": "country", "operator": "eq", "value": "IN"}`

- **`in`** - List membership (comma-separated values, case-insensitive)
  - Example: `{"key": "platform", "operator": "in", "value": "android,ios"}`
  - For screenTag/tag: matches if any request tag intersects with rule value tags

- **`daypart`** / **`timeband`** - Time-based targeting (OOH)
  - Example: `{"key": "timezone", "operator": "daypart", "value": "09:00-17:00,monday,friday"}`
  - Format: `"HH:mm-HH:mm"` or `"HH:mm-HH:mm,day1,day2"` (days optional)
  - Days: monday, tuesday, wednesday, thursday, friday, saturday, sunday (or mon, tue, etc.)

### Rule Evaluation Logic

- If an ad has **no targeting rules**, it matches all requests (default: show everywhere)
- All targeting rules must pass for an ad to be eligible (AND logic)
- If any rule fails, the ad is excluded from delivery

---

## Campaign Management API

### POST /api/v1/campaigns

Create a new campaign.

**Request Body:**

```json
{
  "id": "optional-uuid",
  "name": "Morning Coffee Campaign",
  "advertiserId": "advertiser-123",
  "status": "active",
  "startDate": "2024-01-01T00:00:00Z",
  "endDate": "2024-12-31T23:59:59Z",
  "totalBudget": 10000,
  "targetPlayouts": 5000,
  "targetingRules": [
    {
      "key": "city",
      "operator": "in",
      "value": "Chennai,Mumbai"
    },
    {
      "key": "screenTag",
      "operator": "in",
      "value": "mall,food_court"
    },
    {
      "key": "timezone",
      "operator": "daypart",
      "value": "09:00-17:00,monday,friday"
    }
  ],
  "priority": 5
}
```

**Fields:**
- `id` - Optional campaign ID (auto-generated if not provided)
- `name` - Campaign name (required)
- `advertiserId` - Advertiser identifier (required)
- `status` - Campaign status: "active", "paused", "completed" (default: "active")
- `startDate` - Campaign start date/time (ISO8601, required)
- `endDate` - Campaign end date/time (ISO8601, required)
- `totalBudget` - Total budget in plays (optional, null = unlimited)
- `targetPlayouts` - Target number of playouts (optional)
- `targetingRules` - List of targeting rules (campaign-level, inherited by creatives)
- `priority` - Priority weight for selection (1-10, higher = more frequent, default: 1)

**Response:**

- **200 OK**: Campaign created successfully
  ```json
  {
    "id": "camp-123",
    "name": "Morning Coffee Campaign",
    "advertiserId": "advertiser-123",
    "status": "active",
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-12-31T23:59:59Z",
    "totalBudget": 10000,
    "targetPlayouts": 5000,
    "targetingRules": [...],
    "priority": 5,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
  ```

**Example:**

```bash
curl -X POST http://localhost:8080/api/v1/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Morning Coffee Campaign",
    "advertiserId": "adv-123",
    "startDate": "2024-01-01T00:00:00Z",
    "endDate": "2024-12-31T23:59:59Z",
    "priority": 5
  }'
```

---

### GET /api/v1/campaigns

List all campaigns.

**Query Parameters:**

| Parameter  | Type    | Required | Description                                    |
|------------|---------|----------|------------------------------------------------|
| activeOnly | boolean | No       | If true, only return active campaigns (default: false) |

**Response:**

- **200 OK**: List of campaigns
  ```json
  [
    {
      "id": "camp-123",
      "name": "Morning Coffee Campaign",
      ...
    }
  ]
  ```

---

### GET /api/v1/campaigns/{campaignId}

Get a campaign by ID.

**Response:**

- **200 OK**: Campaign details
- **404 Not Found**: Campaign not found

---

## Creative Management API

### POST /api/v1/campaigns/{campaignId}/creatives

Create a new creative for a campaign.

**Request Body:**

```json
{
  "id": "optional-uuid",
  "campaignId": "camp-123",
  "name": "Coffee Ad Video",
  "creativeType": "video",
  "creativeUrl": "https://cdn.example.com/coffee-ad.mp4",
  "targetUrl": "https://example.com/coffee",
  "durationSeconds": 30,
  "status": "active",
  "shareOfVoice": 0.5,
  "frequencyCapPerScreen": 10,
  "metadata": {
    "version": "1.0",
    "language": "en"
  }
}
```

**Fields:**
- `id` - Optional creative ID (auto-generated if not provided)
- `campaignId` - Campaign ID (required, must exist)
- `name` - Creative name (required)
- `creativeType` - Type: "video", "image", "html" (default: "video")
- `creativeUrl` - URL to creative asset (required)
- `targetUrl` - Click-through URL (optional)
- `durationSeconds` - Duration in seconds (required)
- `status` - Creative status: "active", "paused", "deleted" (default: "active")
- `shareOfVoice` - Share of voice (0.0 to 1.0, optional, not yet used in selection)
- `frequencyCapPerScreen` - Max plays per day per screen (optional, not yet enforced)
- `metadata` - Additional metadata key-value pairs (optional)

**Response:**

- **200 OK**: Creative created successfully
- **404 Not Found**: Campaign not found

---

### GET /api/v1/campaigns/{campaignId}/creatives

List all creatives for a campaign.

**Response:**

- **200 OK**: List of creatives

---

### GET /api/v1/creatives/{creativeId}

Get a creative by ID.

**Response:**

- **200 OK**: Creative details
- **404 Not Found**: Creative not found

---

## Screen Management API (OOH)

### POST /api/v1/screens/register

Register a new screen.

**Request Body:**

```json
{
  "id": "optional-screen-id",
  "name": "Phoenix Mall - Food Court Screen 1",
  "location": {
    "country": "IN",
    "city": "Chennai",
    "area": "Velachery",
    "venueType": "mall",
    "timezone": "Asia/Kolkata"
  },
  "tags": ["mall", "food_court"],
  "metadata": {
    "building": "Phoenix Mall",
    "floor": "3"
  }
}
```

**Response:**

- **200 OK**: Screen registered successfully

---

### GET /api/v1/screens/{screenId}

Get screen details.

**Response:**

- **200 OK**: Screen details
- **404 Not Found**: Screen not found

---

### GET /api/v1/screens

List all screens.

**Response:**

- **200 OK**: List of all screens

---

### PUT /api/v1/screens/{screenId}/heartbeat

Update screen heartbeat (mark as online).

**Response:**

- **200 OK**: Heartbeat updated

---

## Playlist API (OOH)

### GET /api/v1/screens/{screenId}/playlist

Generate a playlist for a screen.

**Query Parameters:**

| Parameter      | Type | Required | Description                        |
|----------------|------|----------|------------------------------------|
| durationMinutes | int | No       | Playlist duration in minutes (default: 3) |

**Response:**

- **200 OK**: Playlist generated successfully
  ```json
  {
    "requestId": "req-uuid",
    "screenId": "screen-123",
    "items": [
      {
        "adId": "creative-123",
        "creativeUrl": "https://cdn.example.com/ad.mp4",
        "targetUrl": "https://example.com/landing",
        "durationSeconds": 30,
        "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=creative-123&campaignId=camp-123&requestId=req-uuid&position=0",
        "position": 0
      }
    ],
    "validForSeconds": 180,
    "totalDurationSeconds": 180
  }
  ```

- **204 No Content**: No playlist available (no matching campaigns/ads)

**Example:**

```bash
curl "http://localhost:8080/api/v1/screens/screen-123/playlist?durationMinutes=5"
```

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

