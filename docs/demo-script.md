# Mnemocast Engine — Demo Script

> Step-by-step demonstration of Epic 2 features: Targeting & Ad Management

**Prerequisites:**
- Engine running at `http://localhost:8080`
- Redis running (for data persistence)
- `curl` or similar HTTP client installed

---

## Demo Flow

### 1. Create a Targeted Ad

Create an ad that targets India (IN) only.

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "demo-advertiser",
    "creativeUrl": "https://example.com/coffee-ad.jpg",
    "targetUrl": "https://example.com/coffee-shop",
    "targetingRules": [
      {
        "key": "country",
        "operator": "eq",
        "value": "IN"
      }
    ],
    "isActive": true
  }'
```

**Expected Response:** 200 OK with created ad JSON (note the generated `id`, `createdAt`, `updatedAt`)

**Save the `id` from the response for later steps!**

---

### 2. Create Another Ad with Platform Targeting

Create an ad that targets Android and iOS platforms.

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "tech-brand",
    "creativeUrl": "https://example.com/app-promo.mp4",
    "targetUrl": "https://example.com/download-app",
    "targetingRules": [
      {
        "key": "platform",
        "operator": "in",
        "value": "android,ios"
      }
    ],
    "isActive": true
  }'
```

---

### 3. List All Ads

Verify the ads were created.

```bash
curl "http://localhost:8080/admin/ads"
```

**Expected Response:** Array with both ads (or more if you created additional ones)

---

### 4. Test Ad Delivery with Matching Targeting

Request an ad delivery for a request that **matches** the targeting rules (IN country).

```bash
curl "http://localhost:8080/ads/deliver?country=IN"
```

**Expected Response:** 200 OK with an ad JSON (should return the IN-targeted ad)

**Note:** The ad selection is random among eligible ads, so you might get different ads on subsequent requests.

---

### 5. Test Ad Delivery with Non-Matching Targeting

Request an ad delivery for a request that **does NOT match** the IN-only targeting.

```bash
curl "http://localhost:8080/ads/deliver?country=US"
```

**Expected Response:** 204 No Content (no matching ads found)

**Explanation:** The first ad targets only IN, so a US request won't match it. If no other ads match, you get 204.

---

### 6. Test Platform Targeting

Request an ad with platform targeting.

```bash
# Should match (Android is in the list)
curl "http://localhost:8080/ads/deliver?platform=android"

# Should also match (iOS is in the list)
curl "http://localhost:8080/ads/deliver?platform=ios"

# May not match if no other ads target web
curl "http://localhost:8080/ads/deliver?platform=web"
```

---

### 7. View Events for an Ad

After making several delivery requests, check the impressions logged for a specific ad.

**Replace `ad-1` with the actual ad ID from step 1:**

```bash
curl "http://localhost:8080/admin/ads/ad-1/events?limit=10"
```

**Expected Response:** Array of delivery events (impressions) showing when and how the ad was served

**Example Response:**
```json
[
  {
    "eventId": "event-1",
    "requestId": "req-123",
    "adId": "ad-1",
    "eventType": "impression",
    "occurredAt": "2025-12-03T12:00:00Z",
    "metadata": {
      "country": "IN",
      "platform": "android"
    }
  }
]
```

---

### 8. Create Ad Without Targeting (Matches All)

Create an ad with no targeting rules (should match all requests).

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "universal-advertiser",
    "creativeUrl": "https://example.com/universal-ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [],
    "isActive": true
  }'
```

Now try the US request again:

```bash
curl "http://localhost:8080/ads/deliver?country=US"
```

**Expected Response:** 200 OK with the universal ad (no targeting = matches all)

---

### 9. List All Ads (Including Inactive)

Create an inactive ad and verify it doesn't appear in active listings.

```bash
# Create inactive ad
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "paused-advertiser",
    "creativeUrl": "https://example.com/paused-ad.jpg",
    "targetingRules": [],
    "isActive": false
  }'

# List only active ads (default)
curl "http://localhost:8080/admin/ads"

# List all ads including inactive
curl "http://localhost:8080/admin/ads?activeOnly=false"
```

---

## Key Points to Highlight

1. **Smart Targeting**: Ads are filtered based on targeting rules before random selection
2. **Context-Aware**: Different countries/platforms receive different ads
3. **Event Tracking**: All ad deliveries are logged as events
4. **Admin API**: Create and manage ads programmatically
5. **No Match Handling**: Returns 204 when no ads match (graceful degradation)

---

## Troubleshooting

- **Connection refused**: Make sure the engine is running (`sbt run` in backend/)
- **Empty responses**: Make sure ads are created and active
- **Redis errors**: Ensure Redis is running on localhost:6379
- **No events**: Events are created when ads are successfully delivered (not on 204 responses)

---

## Full Demo Sequence (Copy-Paste Ready)

```bash
# 1. Create IN-targeted ad
AD_ID=$(curl -s -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "demo",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [{"key": "country", "operator": "eq", "value": "IN"}],
    "isActive": true
  }' | grep -o '"id":"[^"]*"' | cut -d'"' -f4)

echo "Created ad: $AD_ID"

# 2. List ads
curl "http://localhost:8080/admin/ads"

# 3. Test matching request
curl "http://localhost:8080/ads/deliver?country=IN"

# 4. Test non-matching request
curl "http://localhost:8080/ads/deliver?country=US"

# 5. View events
curl "http://localhost:8080/admin/ads/$AD_ID/events?limit=5"
```

---

**Demo Duration:** ~5-10 minutes  
**Audience:** Investors, stakeholders, technical team

