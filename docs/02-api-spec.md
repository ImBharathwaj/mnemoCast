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
    "impressionTrackingUrl": null
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
  "isActive": true
}
```

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
    "isActive": true
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

