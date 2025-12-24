#  Digital Screen Ad Serving API - Implementation Plan

## Executive Summary

This document outlines the plan for developing specialized API endpoints for serving ads to digital screens (OOH - Out-of-Home displays). The plan identifies what we already have, what needs enhancement, and what new endpoints are required.

---

##  What We Already Have (Good Enough)

### 1. **Core Ad Delivery Infrastructure** 
- **`GET /ads/deliver`** - Generic ad delivery endpoint
  - Supports screen-specific parameters (`screenId`, `city`, `area`, `venueType`)
  - Includes targeting, budget management, and frequency capping
  - Returns `DeliveryResponse` with ad creative URL and tracking URLs
  - **Status:**  Production-ready

### 2. **Playlist Generation** 
- **`GET /api/v1/screens/{screenId}/playlist`** - Generate playlists for screens
  - Campaign-based playlist generation
  - Ad-based playlist generation (fallback)
  - Duration-based playlist generation
  - Screen context-aware (uses screen location, tags, classification)
  - **Status:**  Production-ready

### 3. **Screen Management** 
- **`POST /api/v1/screens/register`** - Register screens
- **`GET /api/v1/screens/{screenId}`** - Get screen info
- **`PUT /api/v1/screens/{screenId}/heartbeat`** - Screen heartbeat
- **`GET /api/v1/screens`** - List all screens
- Screen model includes:
  - Location (country, city, area, venueType, timezone)
  - Tags for targeting
  - Classification for priority
  - Size (width, height)
  - Audio capability (isAudible)
  - **Status:**  Production-ready

### 4. **Campaign & Creative Management** 
- Campaign management with targeting rules
- Creative management with metadata
- Campaign-to-creative relationships
- **Status:**  Production-ready

### 5. **Event Tracking** 
- **`GET /api/v1/events/impression`** - Impression tracking
- Event storage and analytics
- **Status:**  Production-ready

### 6. **Ad Delivery Service** 
- `AdDeliveryService` with:
  - Targeting rules filtering
  - Budget constraints
  - Frequency capping
  - Weighted ad selection
  - Screen classification support
  - **Status:**  Production-ready

---

##  What Needs Enhancement

### 1. **Screen-Specific Ad Delivery Endpoint** 
**Current:** Generic `/ads/deliver` endpoint works but isn't optimized for screens

**Enhancement Needed:**
- Create dedicated `/api/v1/screens/{screenId}/ads/deliver` endpoint
- Auto-populate screen context (location, tags, classification) from screen registry
- Better error handling for unregistered screens
- Screen-specific response format

**Priority:**  High

### 2. **Screen Context Auto-Discovery** 
**Current:** Screen context must be passed manually in query params

**Enhancement Needed:**
- Auto-fetch screen details when `screenId` is provided
- Use screen's stored location, tags, and metadata
- Reduce API call complexity for screen clients

**Priority:**  High

### 3. **Batch Ad Delivery** 
**Current:** Single ad delivery per request

**Enhancement Needed:**
- Endpoint to fetch multiple ads for playlist generation
- Support for requesting N ads at once
- Optimized for playlist building

**Priority:**  Medium

### 4. **Screen Health & Status** 
**Current:** Basic heartbeat endpoint exists

**Enhancement Needed:**
- Screen status endpoint (`GET /api/v1/screens/{screenId}/status`)
- Last seen timestamp
- Connection health metrics
- Playback statistics

**Priority:**  Medium

### 5. **Screen-Specific Analytics** 
**Current:** Generic analytics endpoints

**Enhancement Needed:**
- Screen performance metrics endpoint
- Ad performance per screen
- Screen-level reporting

**Priority:**  Low

---

## 🆕 New Endpoints to Create

### 1. **Screen Ad Delivery Endpoint**  HIGH PRIORITY

**Endpoint:** `GET /api/v1/screens/{screenId}/ads/deliver`

**Purpose:** Optimized ad delivery specifically for registered screens

**Features:**
- Auto-populate screen context from registry
- Screen-specific targeting
- Better error messages for unregistered screens
- Response includes screen metadata

**Request:**
```http
GET /api/v1/screens/{screenId}/ads/deliver?durationSeconds=30
```

**Response:**
```json
{
  "adId": "ad-123",
  "creativeUrl": "http://localhost:9000/api/v1/media/creatives/video.mp4",
  "creativeType": "video",
  "durationSeconds": 30,
  "targetUrl": "https://example.com/promo",
  "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-123&requestId=req-456",
  "screenId": "screen-789",
  "screenClassification": 2,
  "campaignId": "campaign-456",
  "metadata": {
    "width": 1920,
    "height": 1080,
    "isAudible": true
  }
}
```

**Implementation:**
- Create `ScreenAdRoutes.scala`
- Use `ScreenStore` to fetch screen details
- Auto-populate `DeliveryRequest` from screen data
- Call `AdDeliveryService.deliver()`
- Enhance response with screen-specific metadata

---

### 2. **Batch Ad Delivery Endpoint**  MEDIUM PRIORITY

**Endpoint:** `GET /api/v1/screens/{screenId}/ads/batch`

**Purpose:** Fetch multiple ads for playlist generation in a single request

**Request:**
```http
GET /api/v1/screens/{screenId}/ads/batch?count=10&durationMinutes=5
```

**Response:**
```json
{
  "screenId": "screen-789",
  "ads": [
    {
      "adId": "ad-123",
      "creativeUrl": "...",
      "durationSeconds": 30,
      "order": 1
    },
    {
      "adId": "ad-456",
      "creativeUrl": "...",
      "durationSeconds": 15,
      "order": 2
    }
  ],
  "totalDurationSeconds": 300,
  "requestedDurationMinutes": 5
}
```

**Implementation:**
- Extend `ScreenAdRoutes.scala`
- Call `AdDeliveryService.deliver()` multiple times
- Apply deduplication logic
- Respect frequency caps across batch

---

### 3. **Screen Status Endpoint**  MEDIUM PRIORITY

**Endpoint:** `GET /api/v1/screens/{screenId}/status`

**Purpose:** Get screen health, last seen, and connection status

**Response:**
```json
{
  "screenId": "screen-789",
  "isOnline": true,
  "lastSeen": "2025-12-18T21:30:00Z",
  "lastSeenAgoSeconds": 45,
  "totalRequests": 1250,
  "lastRequestAt": "2025-12-18T21:29:15Z",
  "playbackStats": {
    "adsPlayed": 450,
    "errors": 2,
    "lastError": null
  }
}
```

**Implementation:**
- Add to `ScreenRoutes.scala`
- Query `ScreenStore` for screen data
- Calculate time since last heartbeat
- Track request counts (optional enhancement)

---

### 4. **Screen Ad Preferences Endpoint**  LOW PRIORITY

**Endpoint:** `GET /api/v1/screens/{screenId}/ads/preferences`

**Purpose:** Get ad delivery preferences and constraints for a screen

**Response:**
```json
{
  "screenId": "screen-789",
  "supportedFormats": ["video", "image"],
  "maxDurationSeconds": 60,
  "minDurationSeconds": 5,
  "isAudible": true,
  "preferredCategories": ["retail", "food"],
  "blockedCategories": ["alcohol"],
  "targetingRules": {
    "city": "Mumbai",
    "area": "Bandra",
    "venueType": "mall"
  }
}
```

**Implementation:**
- Extract from screen metadata
- Return screen capabilities and constraints

---

### 5. **Screen Ad History Endpoint**  LOW PRIORITY

**Endpoint:** `GET /api/v1/screens/{screenId}/ads/history`

**Purpose:** Get history of ads served to a specific screen

**Request:**
```http
GET /api/v1/screens/{screenId}/ads/history?limit=50&since=2025-12-18T00:00:00Z
```

**Response:**
```json
{
  "screenId": "screen-789",
  "ads": [
    {
      "adId": "ad-123",
      "servedAt": "2025-12-18T21:25:00Z",
      "impressionTracked": true
    }
  ],
  "total": 450
}
```

**Implementation:**
- Query `EventStore` filtered by screenId
- Return recent ad delivery history

---

##  Implementation Phases

### Phase 1: Core Screen Ad Delivery (Week 1) 
**Priority:** Critical

1.  Create `ScreenAdRoutes.scala`
2.  Implement `GET /api/v1/screens/{screenId}/ads/deliver`
3.  Auto-populate screen context
4.  Enhanced error handling
5.  Update API documentation
6.  Add tests

**Deliverables:**
- Screen-specific ad delivery endpoint
- Auto-discovery of screen context
- Better error messages

---

### Phase 2: Batch Delivery & Status (Week 2) 
**Priority:** Important

1.  Implement batch ad delivery endpoint
2.  Implement screen status endpoint
3.  Add request tracking
4.  Update documentation

**Deliverables:**
- Batch ad delivery
- Screen health monitoring
- Request statistics

---

### Phase 3: Advanced Features (Week 3) 
**Priority:** Nice to have

1.  Screen ad preferences endpoint
2.  Screen ad history endpoint
3.  Screen-specific analytics
4.  Performance optimizations

**Deliverables:**
- Advanced screen features
- Historical data access
- Enhanced analytics

---

##  Architecture Changes

### New Files to Create

1. **`backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/ScreenAdRoutes.scala`**
   - Screen-specific ad delivery routes
   - Batch delivery routes
   - Screen status routes

2. **`backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/ScreenAdResponse.scala`** (optional)
   - Enhanced response model for screen ad delivery
   - Includes screen metadata

3. **`backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/ScreenAdService.scala`** (optional)
   - Service layer for screen-specific ad logic
   - Screen context enrichment
   - Batch delivery orchestration

### Files to Modify

1. **`HttpServer.scala`**
   - Wire up `ScreenAdRoutes`
   - Pass required dependencies

2. **`AdDeliveryService.scala`** (optional enhancements)
   - Add batch delivery method
   - Screen-specific optimizations

---

##  Technical Considerations

### 1. **Performance**
- Cache screen data to reduce database lookups
- Batch queries for multiple ads
- Optimize playlist generation

### 2. **Error Handling**
- Handle unregistered screens gracefully
- Provide clear error messages
- Log screen-specific errors

### 3. **Security**
- Screen endpoints remain public (no auth required)
- Rate limiting per screen
- Validate screenId format

### 4. **Monitoring**
- Track screen ad delivery success rates
- Monitor screen health
- Alert on offline screens

---

##  Success Metrics

1. **Ad Delivery Performance**
   - < 100ms response time for single ad delivery
   - < 500ms response time for batch delivery (10 ads)
   - 99.9% uptime

2. **Screen Coverage**
   - 100% of registered screens can receive ads
   - Auto-discovery works for all screens

3. **Developer Experience**
   - Simple API for screen clients
   - Clear documentation
   - Easy integration

---

##  Summary

### What's Already Good 
- Core ad delivery infrastructure
- Playlist generation
- Screen management
- Campaign & creative management
- Event tracking

### What Needs Enhancement 
- Screen-specific ad delivery endpoint
- Auto-discovery of screen context
- Batch delivery support
- Screen health monitoring

### What's New 🆕
- `GET /api/v1/screens/{screenId}/ads/deliver` - Screen-specific ad delivery
- `GET /api/v1/screens/{screenId}/ads/batch` - Batch ad delivery
- `GET /api/v1/screens/{screenId}/status` - Screen health status
- `GET /api/v1/screens/{screenId}/ads/preferences` - Screen preferences
- `GET /api/v1/screens/{screenId}/ads/history` - Ad delivery history

---

##  Next Steps

1. **Review and approve this plan**
2. **Start Phase 1 implementation**
3. **Create `ScreenAdRoutes.scala`**
4. **Implement screen-specific ad delivery**
5. **Test with real screens**
6. **Deploy and monitor**

---

**Document Version:** 1.0  
**Last Updated:** 2025-12-18  
**Status:** Ready for Implementation

