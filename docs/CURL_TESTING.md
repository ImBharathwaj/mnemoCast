# System Capabilities Testing Guide - cURL Commands

Complete guide to test all Mnemocast Engine capabilities using cURL commands.

**Base URL**: `http://localhost:8080`

---

## Prerequisites

1. **Start the server**:
   ```bash
   cd backend
   sbt run
   ```

2. **Start Redis** (if not running):
   ```bash
   redis-server
   ```

3. **Start Postgres** (if using hybrid/postgres strategy):
   ```bash
   sudo systemctl start postgresql  # Linux
   # or
   brew services start postgresql  # macOS
   ```

---

## 1. Ad Delivery API

### 1.1 Request an Ad (Basic)

```bash
# Basic request - no targeting
curl -X GET "http://localhost:8080/ads/deliver"

# With targeting parameters
curl -X GET "http://localhost:8080/ads/deliver?country=IN&platform=android"

# With device and user
curl -X GET "http://localhost:8080/ads/deliver?deviceId=device-123&userId=user-456&country=IN&platform=android"
```

**Expected Response (200 OK)**:
```json
{
  "requestId": "550e8400-e29b-41d4-a716-446655440000",
  "adId": "ad-123",
  "creativeUrl": "https://example.com/ad.jpg",
  "targetUrl": "https://example.com/landing",
  "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-123&requestId=550e8400...",
  "clickTrackingUrl": "http://localhost:8080/api/v1/events/click?adId=ad-123&requestId=550e8400..."
}
```

**Expected Response (204 No Content)**: Empty response if no eligible ads

---

## 2. Admin API - Ad Management

### 2.1 Create a Basic Ad

```bash
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-1",
    "creativeUrl": "https://example.com/ad1.jpg",
    "targetUrl": "https://example.com/landing",
    "targetingRules": [],
    "isActive": true
  }'
```

**Expected Response**: Returns the created ad with generated `id`, `createdAt`, `updatedAt`

---

### 2.2 Create Ad with Targeting Rules

```bash
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-2",
    "creativeUrl": "https://example.com/ad2.jpg",
    "targetUrl": "https://example.com/offer",
    "targetingRules": [
      {"key": "country", "operator": "eq", "value": "IN"},
      {"key": "platform", "operator": "in", "value": "android,ios"}
    ],
    "isActive": true
  }'
```

---

### 2.3 Create Ad with Budget Management

```bash
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-3",
    "creativeUrl": "https://example.com/ad3.jpg",
    "targetUrl": "https://example.com/promo",
    "targetingRules": [],
    "isActive": true,
    "maxPlays": 100,
    "dailyLimit": 10,
    "hourlyLimit": 2
  }'
```

**Test Budget Exhaustion**:
```bash
# Request ad 100+ times to exhaust maxPlays
for i in {1..105}; do
  curl -X GET "http://localhost:8080/ads/deliver" > /dev/null 2>&1
done

# Next request should return 204 (no ad available)
curl -X GET "http://localhost:8080/ads/deliver"
```

---

### 2.4 Create Ad with Frequency Capping

```bash
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-4",
    "creativeUrl": "https://example.com/ad4.jpg",
    "targetUrl": "https://example.com/special",
    "targetingRules": [],
    "isActive": true,
    "maxImpressionsPerDevice": 3,
    "maxImpressionsPerUser": 2,
    "frequencyCapWindowHours": 24
  }'
```

**Test Frequency Capping**:
```bash
# Request ad 5 times from same device
for i in {1..5}; do
  curl -X GET "http://localhost:8080/ads/deliver?deviceId=test-device-123" > /dev/null 2>&1
done

# 6th request should return 204 (frequency cap reached)
curl -X GET "http://localhost:8080/ads/deliver?deviceId=test-device-123"
```

---

### 2.5 Create Ad with All Features

```bash
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-5",
    "creativeUrl": "https://example.com/ad5.jpg",
    "targetUrl": "https://example.com/campaign",
    "targetingRules": [
      {"key": "country", "operator": "eq", "value": "US"},
      {"key": "platform", "operator": "in", "value": "android,ios"}
    ],
    "isActive": true,
    "maxPlays": 1000,
    "dailyLimit": 100,
    "hourlyLimit": 10,
    "maxImpressionsPerDevice": 5,
    "maxImpressionsPerUser": 3,
    "frequencyCapWindowHours": 12
  }'
```

---

### 2.6 List All Ads

```bash
# List only active ads (default)
curl -X GET "http://localhost:8080/admin/ads"

# List all ads (including inactive)
curl -X GET "http://localhost:8080/admin/ads?activeOnly=false"
```

**Expected Response**: Array of ad objects

---

### 2.7 Get Events for an Ad

```bash
# Replace 'ad-123' with actual ad ID
curl -X GET "http://localhost:8080/admin/ads/ad-123/events?limit=10"
```

**Expected Response**: Array of delivery events (impressions, clicks)

---

## 3. Event Tracking API

### 3.1 Track Impression (Client-Side)

```bash
# When ad is displayed, call impression tracking URL
curl -X GET "http://localhost:8080/api/v1/events/impression?adId=ad-123&requestId=req-456"
```

**Expected Response**: 204 No Content

---

### 3.2 Track Click (With Redirect)

```bash
# When user clicks ad, call click tracking URL
# Use -L to follow redirect to target URL
curl -L "http://localhost:8080/api/v1/events/click?adId=ad-123&requestId=req-456"
```

**Expected Response**: 
- 302 Redirect to ad's `targetUrl`
- Or 204 No Content if no target URL
- Or 404 if ad not found

**Without Redirect** (just to see response):
```bash
curl -I "http://localhost:8080/api/v1/events/click?adId=ad-123&requestId=req-456"
```

---

## 4. Analytics API

### 4.1 Get Ad Performance Metrics

```bash
# Get all-time performance for an ad
curl -X GET "http://localhost:8080/api/v1/analytics/ads/ad-123"

# Get performance for specific time range
curl -X GET "http://localhost:8080/api/v1/analytics/ads/ad-123?startTime=2024-01-01T00:00:00Z&endTime=2024-01-31T23:59:59Z"
```

**Expected Response**:
```json
{
  "adId": "ad-123",
  "impressions": 1500,
  "clicks": 45,
  "ctr": 3.0,
  "startTime": "2024-01-01T00:00:00Z",
  "endTime": "2024-01-31T23:59:59Z"
}
```

---

### 4.2 Get Campaign Performance

```bash
# Get all campaigns performance
curl -X GET "http://localhost:8080/api/v1/analytics/campaigns"

# With time range
curl -X GET "http://localhost:8080/api/v1/analytics/campaigns?startTime=2024-01-01T00:00:00Z"
```

**Expected Response**: Array of campaign performance objects

---

### 4.3 Get Dashboard Metrics

```bash
# Get dashboard with default top 10 ads
curl -X GET "http://localhost:8080/api/v1/analytics/dashboard"

# Get dashboard with top 5 ads
curl -X GET "http://localhost:8080/api/v1/analytics/dashboard?topN=5"
```

**Expected Response**:
```json
{
  "totalAds": 25,
  "activeAds": 18,
  "totalImpressions": 50000,
  "totalClicks": 1500,
  "overallCTR": 3.0,
  "topPerformingAds": [...],
  "recentActivity": [...]
}
```

---

## 5. Complete Testing Workflow

### 5.1 End-to-End Test Scenario

```bash
# Step 1: Create an ad
AD_ID=$(curl -s -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/test-ad.jpg",
    "targetUrl": "https://example.com/test",
    "targetingRules": [{"key": "country", "operator": "eq", "value": "IN"}],
    "isActive": true,
    "maxPlays": 10,
    "dailyLimit": 5
  }' | jq -r '.id')

echo "Created ad: $AD_ID"

# Step 2: Request ad delivery (multiple times)
for i in {1..5}; do
  RESPONSE=$(curl -s -X GET "http://localhost:8080/ads/deliver?country=IN")
  REQUEST_ID=$(echo $RESPONSE | jq -r '.requestId')
  AD_ID_RESPONSE=$(echo $RESPONSE | jq -r '.adId')
  IMPRESSION_URL=$(echo $RESPONSE | jq -r '.impressionTrackingUrl')
  
  echo "Request $i: Ad $AD_ID_RESPONSE"
  
  # Step 3: Track impression
  curl -s -X GET "$IMPRESSION_URL" > /dev/null
  
  # Step 4: Simulate click (every 3rd request)
  if [ $((i % 3)) -eq 0 ]; then
    CLICK_URL=$(echo $RESPONSE | jq -r '.clickTrackingUrl')
    curl -s -L "$CLICK_URL" > /dev/null
    echo "  Clicked!"
  fi
done

# Step 5: Check analytics
echo ""
echo "=== Analytics ==="
curl -s -X GET "http://localhost:8080/api/v1/analytics/ads/$AD_ID" | jq

# Step 6: Check dashboard
echo ""
echo "=== Dashboard ==="
curl -s -X GET "http://localhost:8080/api/v1/analytics/dashboard?topN=3" | jq '.topPerformingAds'
```

---

## 6. Testing Specific Features

### 6.1 Test Targeting Rules

```bash
# Create ad targeting India
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [{"key": "country", "operator": "eq", "value": "IN"}],
    "isActive": true
  }'

# Request from India - should get ad
curl -X GET "http://localhost:8080/ads/deliver?country=IN"

# Request from US - should get 204 (no ad)
curl -X GET "http://localhost:8080/ads/deliver?country=US"
```

---

### 6.2 Test Budget Limits

```bash
# Create ad with small budget
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "isActive": true,
    "maxPlays": 3
  }'

# Request 5 times
for i in {1..5}; do
  echo "Request $i:"
  curl -X GET "http://localhost:8080/ads/deliver"
  echo ""
done
# First 3 should return ad, last 2 should return 204
```

---

### 6.3 Test Frequency Capping

```bash
# Create ad with frequency cap
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "isActive": true,
    "maxImpressionsPerDevice": 2,
    "frequencyCapWindowHours": 24
  }'

# Request from same device 4 times
for i in {1..4}; do
  echo "Request $i:"
  curl -X GET "http://localhost:8080/ads/deliver?deviceId=test-device"
  echo ""
done
# First 2 should return ad, last 2 should return 204
```

---

### 6.4 Test Platform Targeting

```bash
# Create ad for Android only
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [{"key": "platform", "operator": "eq", "value": "android"}],
    "isActive": true
  }'

# Request from Android - should get ad
curl -X GET "http://localhost:8080/ads/deliver?platform=android"

# Request from iOS - should get 204
curl -X GET "http://localhost:8080/ads/deliver?platform=ios"
```

---

## 7. Performance Testing

### 7.1 Load Test (Simple)

```bash
# Request ads 100 times
for i in {1..100}; do
  curl -s -X GET "http://localhost:8080/ads/deliver" > /dev/null
done

# Check analytics to see delivery count
curl -X GET "http://localhost:8080/api/v1/analytics/dashboard" | jq '.totalImpressions'
```

---

### 7.2 Concurrent Requests

```bash
# Run 10 concurrent requests
for i in {1..10}; do
  curl -s -X GET "http://localhost:8080/ads/deliver" > /dev/null &
done
wait
echo "All requests completed"
```

---

## 8. Error Scenarios

### 8.1 Invalid Ad ID

```bash
# Try to get events for non-existent ad
curl -X GET "http://localhost:8080/admin/ads/invalid-id/events"
# Should return 200 with empty array or 404
```

---

### 8.2 Invalid Analytics Query

```bash
# Invalid time format
curl -X GET "http://localhost:8080/api/v1/analytics/ads/ad-123?startTime=invalid"
# Should handle gracefully
```

---

## 9. Pretty Print JSON Responses

### 9.1 Using jq (Recommended)

```bash
# Install jq if not available
# Ubuntu/Debian: sudo apt-get install jq
# macOS: brew install jq

# Pretty print any response
curl -s -X GET "http://localhost:8080/admin/ads" | jq

# Extract specific fields
curl -s -X GET "http://localhost:8080/api/v1/analytics/dashboard" | jq '.totalImpressions'
```

---

### 9.2 Using Python (Alternative)

```bash
# Pretty print JSON
curl -s -X GET "http://localhost:8080/admin/ads" | python3 -m json.tool
```

---

## 10. Quick Test Script

Save this as `test-system.sh`:

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== Testing Mnemocast Engine ==="
echo ""

# Test 1: Create ad
echo "1. Creating ad..."
AD_RESPONSE=$(curl -s -X POST "$BASE_URL/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/test.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [{"key": "country", "operator": "eq", "value": "IN"}],
    "isActive": true,
    "maxPlays": 5
  }')

AD_ID=$(echo $AD_RESPONSE | jq -r '.id')
echo "   Created ad: $AD_ID"
echo ""

# Test 2: Request ad
echo "2. Requesting ad..."
DELIVERY_RESPONSE=$(curl -s -X GET "$BASE_URL/ads/deliver?country=IN")
echo "   Response: $(echo $DELIVERY_RESPONSE | jq -r '.adId')"
echo ""

# Test 3: Track impression
echo "3. Tracking impression..."
IMPRESSION_URL=$(echo $DELIVERY_RESPONSE | jq -r '.impressionTrackingUrl')
curl -s -X GET "$IMPRESSION_URL" > /dev/null
echo "   Impression tracked"
echo ""

# Test 4: Track click
echo "4. Tracking click..."
CLICK_URL=$(echo $DELIVERY_RESPONSE | jq -r '.clickTrackingUrl')
curl -s -L "$CLICK_URL" > /dev/null
echo "   Click tracked"
echo ""

# Test 5: Check analytics
echo "5. Checking analytics..."
curl -s -X GET "$BASE_URL/api/v1/analytics/ads/$AD_ID" | jq
echo ""

# Test 6: Check dashboard
echo "6. Dashboard summary:"
curl -s -X GET "$BASE_URL/api/v1/analytics/dashboard?topN=3" | jq '{totalAds, activeAds, totalImpressions, totalClicks, overallCTR}'
echo ""

echo "=== Tests Complete ==="
```

**Run the script**:
```bash
chmod +x test-system.sh
./test-system.sh
```

---

## 11. System Capabilities Checklist

Use these commands to verify each capability:

###  Ad Delivery
- [ ] Basic ad delivery works
- [ ] Targeting rules filter correctly
- [ ] Multiple targeting rules (AND logic)

###  Budget Management
- [ ] maxPlays limit enforced
- [ ] dailyLimit enforced
- [ ] hourlyLimit enforced
- [ ] Budget exhaustion handled

###  Frequency Capping
- [ ] Per-device capping works
- [ ] Per-user capping works
- [ ] Time window respected

###  Click Tracking
- [ ] Click events logged
- [ ] Redirect to target URL works
- [ ] Click counted in analytics

###  Analytics
- [ ] Ad performance metrics accurate
- [ ] Campaign performance calculated
- [ ] Dashboard shows correct data
- [ ] Time range filtering works

###  Event Tracking
- [ ] Impressions logged
- [ ] Clicks logged
- [ ] Events queryable by ad

---

## 12. Troubleshooting

### No Ads Returned (204)

```bash
# Check if any ads exist
curl -X GET "http://localhost:8080/admin/ads"

# Check if ads are active
curl -X GET "http://localhost:8080/admin/ads" | jq '.[] | {id, isActive}'

# Create a test ad
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [],
    "isActive": true
  }'
```

### Connection Refused

```bash
# Check if server is running
curl -X GET "http://localhost:8080/ads/deliver"

# Check Redis
redis-cli ping

# Check Postgres (if using)
psql -U postgres -d mnemocast -c "SELECT 1;"
```

---

## 13. Advanced Examples

### 13.1 Create Multiple Ads for Testing

```bash
# Create 5 different ads
for i in {1..5}; do
  curl -X POST "http://localhost:8080/admin/ads" \
    -H "Content-Type: application/json" \
    -d "{
      \"advertiserId\": \"advertiser-$i\",
      \"creativeUrl\": \"https://example.com/ad$i.jpg\",
      \"targetingRules\": [],
      \"isActive\": true,
      \"maxPlays\": $((i * 100))
    }"
done
```

### 13.2 Generate Traffic and Check Analytics

```bash
# Generate 50 ad requests
for i in {1..50}; do
  curl -s -X GET "http://localhost:8080/ads/deliver" > /dev/null
done

# Wait a moment for events to process
sleep 2

# Check analytics
curl -X GET "http://localhost:8080/api/v1/analytics/dashboard" | jq
```

---

**All commands assume the server is running on `http://localhost:8080`**

For more details, see:
- `docs/02-api-spec.md` - Complete API specification
- `docs/CAPABILITIES.md` - System capabilities overview
- `docs/FEATURES.md` - Feature details

