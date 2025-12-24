#  Mnemocast API Documentation

**Version:** 1.0.0  
**Base URL:** `http://localhost:8080`  
**API Version:** v1

---

##  Table of Contents

1. [Authentication](#authentication)
2. [Base URL & Versioning](#base-url--versioning)
3. [Response Format](#response-format)
4. [Error Handling](#error-handling)
5. [Endpoints](#endpoints)
   - [Health & Monitoring](#health--monitoring)
   - [Campaigns](#campaigns)
   - [Creatives](#creatives)
   - [Screens](#screens)
   - [Playlists](#playlists)
   - [Analytics](#analytics)
   - [Events](#events)
   - [Media](#media)
   - [Ad Delivery](#ad-delivery)
6. [Interactive Documentation](#interactive-documentation)

---

## Authentication

Currently, the API does not require authentication. API key authentication can be enabled as an optional feature.

**Note:** For production deployments, authentication should be enabled.

---

## Base URL & Versioning

All API endpoints are prefixed with `/api/v1/`:

```
http://localhost:8080/api/v1/{endpoint}
```

**Example:**
```
GET http://localhost:8080/api/v1/campaigns
```

---

## Response Format

All responses are returned in JSON format with appropriate HTTP status codes.

### Success Response
```json
{
  "id": "campaign-1",
  "name": "Summer Sale Campaign",
  "status": "active",
  ...
}
```

### Error Response
```json
{
  "error": {
    "code": "NOT_FOUND",
    "httpStatus": 404
  },
  "message": "Campaign not found: campaign-123",
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2024-12-18T15:00:00Z"
}
```

---

## Error Handling

### HTTP Status Codes

| Code | Description |
|------|-------------|
| 200 | Success |
| 201 | Created |
| 400 | Bad Request |
| 404 | Not Found |
| 409 | Conflict |
| 429 | Too Many Requests |
| 500 | Internal Server Error |
| 503 | Service Unavailable |

### Error Codes

| Code | Description |
|------|-------------|
| `NOT_FOUND` | Resource not found |
| `BAD_REQUEST` | Invalid request parameters |
| `VALIDATION_ERROR` | Validation failed |
| `CONFLICT` | Resource conflict |
| `TOO_MANY_REQUESTS` | Rate limit exceeded |
| `INTERNAL_SERVER_ERROR` | Server error |
| `SERVICE_UNAVAILABLE` | Service unavailable |

---

## Endpoints

### Health & Monitoring

#### **GET** `/api/v1/health`
Get system health status with component-level details.

**Response:** `200 OK`
```json
{
  "status": "UP",
  "timestamp": "2024-12-18T15:00:00Z",
  "uptimeSeconds": 3600,
  "components": [
    {
      "name": "Redis",
      "status": "healthy",
      "responseTimeMs": 2
    },
    {
      "name": "PostgreSQL",
      "status": "healthy",
      "responseTimeMs": 15
    },
    {
      "name": "MediaStorage",
      "status": "healthy",
      "responseTimeMs": 5
    }
  ],
  "version": "1.0.0"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/health
```

---

#### **GET** `/api/v1/ready`
Kubernetes readiness probe endpoint.

**Response:** `200 OK` or `503 Service Unavailable`
```json
{
  "status": "ready"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/ready
```

---

#### **GET** `/api/v1/live`
Kubernetes liveness probe endpoint.

**Response:** `200 OK` or `503 Service Unavailable`
```json
{
  "status": "alive"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/live
```

---

#### **GET** `/api/v1/metrics`
Get system performance metrics.

**Response:** `200 OK`
```json
{
  "totalRequests": 10000,
  "averageResponseTimeMs": 25.5,
  "p95ResponseTimeMs": 85.0,
  "p99ResponseTimeMs": 150.0,
  "errorRate": 0.001,
  "activeCampaigns": 5,
  "activeCreatives": 17,
  "activeScreens": 9,
  "endpointMetrics": [
    {
      "endpoint": "/api/v1/screens/screen-1/playlist",
      "method": "GET",
      "requestCount": 5000,
      "averageResponseTimeMs": 35.0,
      "errorCount": 2
    }
  ],
  "uptimeSeconds": 3600
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/metrics
```

---

### Campaigns

#### **GET** `/api/v1/campaigns`
List all campaigns.

**Query Parameters:**
- `status` (optional): Filter by status (`active`, `paused`, `completed`)

**Response:** `200 OK`
```json
[
  {
    "id": "campaign-1",
    "name": "Summer Sale Campaign",
    "status": "active",
    "priority": 5,
    "startDate": "2024-06-01T00:00:00Z",
    "endDate": "2024-08-31T23:59:59Z",
    "targeting": {
      "cities": ["Chennai", "Mumbai"],
      "venueTypes": ["mall", "airport"],
      "screenClassifications": [1, 2, 3]
    },
    "budget": {
      "total": 100000.0,
      "daily": 5000.0,
      "hourly": 500.0
    }
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/campaigns
curl "http://localhost:8080/api/v1/campaigns?status=active"
```

---

#### **GET** `/api/v1/campaigns/{id}`
Get a specific campaign by ID.

**Path Parameters:**
- `id` (required): Campaign ID

**Response:** `200 OK`
```json
{
  "id": "campaign-1",
  "name": "Summer Sale Campaign",
  "status": "active",
  "priority": 5,
  "startDate": "2024-06-01T00:00:00Z",
  "endDate": "2024-08-31T23:59:59Z",
  "targeting": {
    "cities": ["Chennai", "Mumbai"],
    "venueTypes": ["mall", "airport"],
    "screenClassifications": [1, 2, 3]
  },
  "budget": {
    "total": 100000.0,
    "daily": 5000.0,
    "hourly": 500.0
  }
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/campaigns/campaign-1
```

---

#### **POST** `/api/v1/campaigns`
Create a new campaign.

**Request Body:**
```json
{
  "name": "Summer Sale Campaign",
  "status": "active",
  "priority": 5,
  "startDate": "2024-06-01T00:00:00Z",
  "endDate": "2024-08-31T23:59:59Z",
  "targeting": {
    "cities": ["Chennai", "Mumbai"],
    "venueTypes": ["mall", "airport"],
    "screenClassifications": [1, 2, 3]
  },
  "budget": {
    "total": 100000.0,
    "daily": 5000.0,
    "hourly": 500.0
  }
}
```

**Response:** `201 Created`
```json
{
  "id": "campaign-1",
  "name": "Summer Sale Campaign",
  "status": "active",
  ...
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/campaigns \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Summer Sale Campaign",
    "status": "active",
    "priority": 5,
    "startDate": "2024-06-01T00:00:00Z",
    "endDate": "2024-08-31T23:59:59Z",
    "targeting": {
      "cities": ["Chennai", "Mumbai"],
      "venueTypes": ["mall", "airport"],
      "screenClassifications": [1, 2, 3]
    },
    "budget": {
      "total": 100000.0,
      "daily": 5000.0,
      "hourly": 500.0
    }
  }'
```

---

#### **PUT** `/api/v1/campaigns/{id}`
Update an existing campaign.

**Path Parameters:**
- `id` (required): Campaign ID

**Request Body:** Same as POST

**Response:** `200 OK`
```json
{
  "id": "campaign-1",
  "name": "Updated Campaign Name",
  ...
}
```

**cURL Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/campaigns/campaign-1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Campaign Name",
    "status": "paused",
    ...
  }'
```

---

#### **DELETE** `/api/v1/campaigns/{id}`
Delete a campaign.

**Path Parameters:**
- `id` (required): Campaign ID

**Response:** `200 OK`
```json
{
  "message": "Campaign deleted successfully"
}
```

**cURL Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/campaigns/campaign-1
```

---

### Creatives

#### **GET** `/api/v1/creatives`
List all creatives.

**Query Parameters:**
- `campaignId` (optional): Filter by campaign ID
- `status` (optional): Filter by status (`active`, `paused`)

**Response:** `200 OK`
```json
[
  {
    "id": "creative-1",
    "campaignId": "campaign-1",
    "name": "Summer Sale Banner",
    "type": "image",
    "url": "https://storage.example.com/creatives/banner.jpg",
    "durationSeconds": 10,
    "weight": 50,
    "status": "active"
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/creatives
curl "http://localhost:8080/api/v1/creatives?campaignId=campaign-1"
```

---

#### **GET** `/api/v1/creatives/{id}`
Get a specific creative by ID.

**Path Parameters:**
- `id` (required): Creative ID

**Response:** `200 OK`
```json
{
  "id": "creative-1",
  "campaignId": "campaign-1",
  "name": "Summer Sale Banner",
  "type": "image",
  "url": "https://storage.example.com/creatives/banner.jpg",
  "durationSeconds": 10,
  "weight": 50,
  "status": "active"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/creatives/creative-1
```

---

#### **POST** `/api/v1/creatives`
Create a new creative.

**Request Body:**
```json
{
  "campaignId": "campaign-1",
  "name": "Summer Sale Banner",
  "type": "image",
  "url": "https://storage.example.com/creatives/banner.jpg",
  "durationSeconds": 10,
  "weight": 50,
  "status": "active"
}
```

**Response:** `201 Created`
```json
{
  "id": "creative-1",
  "campaignId": "campaign-1",
  "name": "Summer Sale Banner",
  ...
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/creatives \
  -H "Content-Type: application/json" \
  -d '{
    "campaignId": "campaign-1",
    "name": "Summer Sale Banner",
    "type": "image",
    "url": "https://storage.example.com/creatives/banner.jpg",
    "durationSeconds": 10,
    "weight": 50,
    "status": "active"
  }'
```

---

#### **PUT** `/api/v1/creatives/{id}`
Update an existing creative.

**Path Parameters:**
- `id` (required): Creative ID

**Request Body:** Same as POST

**Response:** `200 OK`

**cURL Example:**
```bash
curl -X PUT http://localhost:8080/api/v1/creatives/creative-1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Creative Name",
    "weight": 75,
    ...
  }'
```

---

#### **DELETE** `/api/v1/creatives/{id}`
Delete a creative.

**Path Parameters:**
- `id` (required): Creative ID

**Response:** `200 OK`

**cURL Example:**
```bash
curl -X DELETE http://localhost:8080/api/v1/creatives/creative-1
```

---

### Screens

#### **GET** `/api/v1/screens`
List all registered screens.

**Response:** `200 OK`
```json
[
  {
    "id": "screen-1",
    "name": "Chennai Airport Screen 1",
    "location": {
      "city": "Chennai",
      "area": "Airport",
      "venueType": "airport"
    },
    "classification": 1,
    "isOnline": true,
    "lastHeartbeat": "2024-12-18T15:00:00Z"
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/screens
```

---

#### **GET** `/api/v1/screens/{id}`
Get a specific screen by ID.

**Path Parameters:**
- `id` (required): Screen ID

**Response:** `200 OK`
```json
{
  "id": "screen-1",
  "name": "Chennai Airport Screen 1",
  "location": {
    "city": "Chennai",
    "area": "Airport",
    "venueType": "airport"
  },
  "classification": 1,
  "isOnline": true,
  "lastHeartbeat": "2024-12-18T15:00:00Z"
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/screens/screen-1
```

---

#### **POST** `/api/v1/screens/register`
Register a new screen.

**Request Body:**
```json
{
  "id": "screen-1",
  "name": "Chennai Airport Screen 1",
  "location": {
    "city": "Chennai",
    "area": "Airport",
    "venueType": "airport"
  },
  "classification": 1
}
```

**Response:** `201 Created`
```json
{
  "id": "screen-1",
  "name": "Chennai Airport Screen 1",
  "location": {
    "city": "Chennai",
    "area": "Airport",
    "venueType": "airport"
  },
  "classification": 1,
  "isOnline": true,
  "lastHeartbeat": "2024-12-18T15:00:00Z"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/screens/register \
  -H "Content-Type: application/json" \
  -d '{
    "id": "screen-1",
    "name": "Chennai Airport Screen 1",
    "location": {
      "city": "Chennai",
      "area": "Airport",
      "venueType": "airport"
    },
    "classification": 1
  }'
```

---

#### **POST** `/api/v1/screens/{id}/heartbeat`
Send a heartbeat to indicate screen is online.

**Path Parameters:**
- `id` (required): Screen ID

**Response:** `200 OK`
```json
{
  "message": "Heartbeat recorded"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/screens/screen-1/heartbeat
```

---

### Playlists

#### **GET** `/api/v1/screens/{id}/playlist`
Generate a dynamic playlist for a screen.

**Path Parameters:**
- `id` (required): Screen ID

**Query Parameters:**
- `durationMinutes` (optional): Target playlist duration in minutes (default: 3)

**Response:** `200 OK`
```json
{
  "screenId": "screen-1",
  "generatedAt": "2024-12-18T15:00:00Z",
  "durationMinutes": 3,
  "items": [
    {
      "creativeId": "creative-1",
      "campaignId": "campaign-1",
      "url": "https://storage.example.com/creatives/banner.jpg",
      "durationSeconds": 10,
      "order": 1
    },
    {
      "creativeId": "creative-2",
      "campaignId": "campaign-1",
      "url": "https://storage.example.com/creatives/video.mp4",
      "durationSeconds": 30,
      "order": 2
    }
  ]
}
```

**cURL Example:**
```bash
curl "http://localhost:8080/api/v1/screens/screen-1/playlist?durationMinutes=3"
```

---

### Analytics

#### **GET** `/api/v1/analytics/dashboard`
Get dashboard summary metrics.

**Response:** `200 OK`
```json
{
  "totalImpressions": 100000,
  "totalPlays": 95000,
  "totalCampaigns": 5,
  "totalCreatives": 17,
  "totalScreens": 9,
  "averagePlayRate": 0.95,
  "period": {
    "start": "2024-12-01T00:00:00Z",
    "end": "2024-12-18T23:59:59Z"
  }
}
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/analytics/dashboard
```

---

#### **GET** `/api/v1/analytics/campaigns`
Get performance metrics for all campaigns.

**Query Parameters:**
- `startTime` (optional): Start time (ISO 8601 format)
- `endTime` (optional): End time (ISO 8601 format)

**Response:** `200 OK`
```json
[
  {
    "campaignId": "campaign-1",
    "campaignName": "Summer Sale Campaign",
    "impressions": 50000,
    "plays": 47500,
    "playRate": 0.95,
    "budgetSpent": 45000.0,
    "budgetRemaining": 55000.0
  }
]
```

**cURL Example:**
```bash
curl "http://localhost:8080/api/v1/analytics/campaigns?startTime=2024-12-01T00:00:00Z&endTime=2024-12-18T23:59:59Z"
```

---

#### **GET** `/api/v1/analytics/campaigns/compare`
Compare multiple campaigns.

**Query Parameters:**
- `campaignIds` (required): Comma-separated list of campaign IDs
- `startTime` (optional): Start time (ISO 8601 format)
- `endTime` (optional): End time (ISO 8601 format)

**Response:** `200 OK`
```json
[
  {
    "campaignId": "campaign-1",
    "campaignName": "Summer Sale Campaign",
    "impressions": 50000,
    "plays": 47500,
    "playRate": 0.95
  },
  {
    "campaignId": "campaign-2",
    "campaignName": "Winter Campaign",
    "impressions": 30000,
    "plays": 28500,
    "playRate": 0.95
  }
]
```

**cURL Example:**
```bash
curl "http://localhost:8080/api/v1/analytics/campaigns/compare?campaignIds=campaign-1,campaign-2"
```

---

#### **GET** `/api/v1/analytics/campaigns/roi`
Get ROI metrics for campaigns.

**Response:** `200 OK`
```json
[
  {
    "campaignId": "campaign-1",
    "campaignName": "Summer Sale Campaign",
    "budgetAllocated": 100000.0,
    "budgetSpent": 45000.0,
    "budgetUtilization": 0.45,
    "impressions": 50000,
    "impressionsPerDollar": 1.11,
    "costPerImpression": 0.90
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/analytics/campaigns/roi
```

---

#### **GET** `/api/v1/analytics/screens`
Get screen-level performance analytics.

**Response:** `200 OK`
```json
[
  {
    "screenId": "screen-1",
    "screenName": "Chennai Airport Screen 1",
    "impressions": 10000,
    "plays": 9500,
    "playRate": 0.95,
    "classification": 1
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/analytics/screens
```

---

#### **GET** `/api/v1/analytics/creatives`
Get creative performance analytics.

**Response:** `200 OK`
```json
[
  {
    "creativeId": "creative-1",
    "creativeName": "Summer Sale Banner",
    "campaignId": "campaign-1",
    "impressions": 25000,
    "plays": 23750,
    "playRate": 0.95
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/analytics/creatives
```

---

#### **GET** `/api/v1/analytics/geographic`
Get geographic performance analytics.

**Response:** `200 OK`
```json
[
  {
    "city": "Chennai",
    "impressions": 50000,
    "plays": 47500,
    "playRate": 0.95,
    "screens": 5
  },
  {
    "city": "Mumbai",
    "impressions": 30000,
    "plays": 28500,
    "playRate": 0.95,
    "screens": 3
  }
]
```

**cURL Example:**
```bash
curl http://localhost:8080/api/v1/analytics/geographic
```

---

#### **GET** `/api/v1/analytics/timeseries`
Get time-series analytics data.

**Query Parameters:**
- `startTime` (required): Start time (ISO 8601 format)
- `endTime` (required): End time (ISO 8601 format)
- `granularity` (optional): `hour`, `day`, `week` (default: `day`)

**Response:** `200 OK`
```json
{
  "startTime": "2024-12-01T00:00:00Z",
  "endTime": "2024-12-18T23:59:59Z",
  "granularity": "day",
  "data": [
    {
      "timestamp": "2024-12-01T00:00:00Z",
      "impressions": 5000,
      "plays": 4750
    },
    {
      "timestamp": "2024-12-02T00:00:00Z",
      "impressions": 5500,
      "plays": 5225
    }
  ]
}
```

**cURL Example:**
```bash
curl "http://localhost:8080/api/v1/analytics/timeseries?startTime=2024-12-01T00:00:00Z&endTime=2024-12-18T23:59:59Z&granularity=day"
```

---

#### **GET** `/api/v1/analytics/export`
Export analytics data.

**Query Parameters:**
- `format` (required): `csv` or `json`
- `type` (required): `campaigns`
- `startTime` (optional): Start time (ISO 8601 format)
- `endTime` (optional): End time (ISO 8601 format)

**Response:** `200 OK`
- CSV: `Content-Type: text/csv`
- JSON: `Content-Type: application/json`

**cURL Example:**
```bash
curl "http://localhost:8080/api/v1/analytics/export?format=csv&type=campaigns" -o analytics.csv
curl "http://localhost:8080/api/v1/analytics/export?format=json&type=campaigns" -o analytics.json
```

---

#### **GET** `/api/v1/analytics/ads/{adId}`
Get performance metrics for a specific ad.

**Path Parameters:**
- `adId` (required): Ad/Creative ID

**Query Parameters:**
- `startTime` (optional): Start time (ISO 8601 format)
- `endTime` (optional): End time (ISO 8601 format)

**Response:** `200 OK`
```json
{
  "adId": "creative-1",
  "impressions": 25000,
  "plays": 23750,
  "playRate": 0.95,
  "period": {
    "start": "2024-12-01T00:00:00Z",
    "end": "2024-12-18T23:59:59Z"
  }
}
```

**cURL Example:**
```bash
curl "http://localhost:8080/api/v1/analytics/ads/creative-1?startTime=2024-12-01T00:00:00Z&endTime=2024-12-18T23:59:59Z"
```

---

### Events

#### **POST** `/api/v1/events/play`
Record a play event.

**Request Body:**
```json
{
  "screenId": "screen-1",
  "creativeId": "creative-1",
  "campaignId": "campaign-1",
  "timestamp": "2024-12-18T15:00:00Z"
}
```

**Response:** `201 Created`
```json
{
  "message": "Event recorded"
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/events/play \
  -H "Content-Type: application/json" \
  -d '{
    "screenId": "screen-1",
    "creativeId": "creative-1",
    "campaignId": "campaign-1",
    "timestamp": "2024-12-18T15:00:00Z"
  }'
```

---

#### **POST** `/api/v1/events/impression`
Record an impression event.

**Request Body:**
```json
{
  "screenId": "screen-1",
  "creativeId": "creative-1",
  "campaignId": "campaign-1",
  "timestamp": "2024-12-18T15:00:00Z"
}
```

**Response:** `201 Created`

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/events/impression \
  -H "Content-Type: application/json" \
  -d '{
    "screenId": "screen-1",
    "creativeId": "creative-1",
    "campaignId": "campaign-1",
    "timestamp": "2024-12-18T15:00:00Z"
  }'
```

---

### Media

#### **POST** `/api/v1/media/upload`
Upload a media file (image or video).

**Request:** `multipart/form-data`
- `file` (required): Media file
- `campaignId` (optional): Campaign ID
- `name` (optional): Media name

**Response:** `201 Created`
```json
{
  "id": "media-1",
  "url": "https://storage.example.com/creatives/banner.jpg",
  "type": "image",
  "size": 1024000
}
```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/api/v1/media/upload \
  -F "file=@banner.jpg" \
  -F "campaignId=campaign-1" \
  -F "name=Summer Sale Banner"
```

---

### Ad Delivery

#### **GET** `/ads/deliver`
Request an ad for delivery (legacy endpoint).

**Query Parameters:**
- `deviceId` (optional): Device identifier
- `userId` (optional): User identifier
- `appId` (optional): Application identifier
- `country` (optional): Country code (e.g., `IN`)
- `platform` (optional): Platform (`android`, `ios`)
- `screenId` (optional): Screen ID
- `city` (optional): City name
- `area` (optional): Area name
- `venueType` (optional): Venue type
- `timezone` (optional): Timezone

**Response:** `200 OK`
```json
{
  "adId": "creative-1",
  "url": "https://storage.example.com/creatives/banner.jpg",
  "durationSeconds": 10,
  "campaignId": "campaign-1"
}
```

**cURL Example:**
```bash
curl "http://localhost:8080/ads/deliver?screenId=screen-1&city=Chennai&venueType=airport"
```

---

## Interactive Documentation

### Swagger UI

Access interactive API documentation at:
```
http://localhost:8080/api/docs
```

The Swagger UI provides:
- Interactive API explorer
- Try-it-out functionality
- Request/response examples
- Schema definitions

### OpenAPI Specification

Download the OpenAPI 3.0 specification:
```
http://localhost:8080/api/docs/openapi.yaml
```

---

## Rate Limiting

Rate limiting is implemented per IP address:
- Default limit: 100 requests per 60 seconds
- Per-endpoint limits:
  - `/api/v1/analytics`: 50 requests/minute
  - `/api/v1/metrics`: 30 requests/minute
  - `/api/v1/health`: 100 requests/minute

Rate limit headers are included in responses:
- `X-RateLimit-Limit`: Maximum requests allowed
- `X-RateLimit-Remaining`: Remaining requests in window
- `X-RateLimit-Reset`: Reset time (Unix timestamp)

---

## Request/Response Headers

### Request Headers
- `Content-Type: application/json` (for POST/PUT requests)
- `X-Request-ID` (optional): Custom request ID for tracking

### Response Headers
- `Content-Type: application/json`
- `X-Request-ID`: Request ID for tracking
- `X-RateLimit-Limit`: Rate limit information
- `X-RateLimit-Remaining`: Remaining requests
- `X-RateLimit-Reset`: Reset timestamp

---

## Best Practices

1. **Use HTTPS in production** - Always use HTTPS for API calls in production
2. **Handle errors gracefully** - Check HTTP status codes and error responses
3. **Use request IDs** - Include `X-Request-ID` header for request tracking
4. **Respect rate limits** - Implement exponential backoff when rate limited
5. **Cache responses** - Cache GET requests when appropriate
6. **Use pagination** - For large datasets, implement pagination
7. **Validate input** - Validate all input data before sending requests

---

## Support

For API support:
- **Documentation:** See `docs/` directory
- **Interactive Docs:** http://localhost:8080/api/docs
- **Health Check:** http://localhost:8080/api/v1/health

---

**Last Updated:** December 2024

