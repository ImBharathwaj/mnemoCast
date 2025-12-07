# Epic 2: Basic Targeting & Ad Management — Progress Tracking

> **Goal:** Move from "random ad server" → "smart, filter-based ad server"
> - Use targeting rules (country, platform, etc.)
> - Provide API to create and list ads
> - Add a simple endpoint to inspect recorded events

---

## 📊 Overall Progress

- [ ] Step 1: Targeting Logic (Manual)
- [ ] Step 2: Integrate Targeting into Delivery
- [ ] Step 3: Admin API — Create Ad
- [ ] Step 4: Admin API — List Ads
- [ ] Step 5: Events API — List Events per Ad
- [ ] Step 6: Docs & Demo Script

**Status:** 🔴 Not Started

---

## 1️⃣ Targeting Logic (Manual)

**Status:** ⏳ Pending  
**Module:** `engine-domain`  
**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/services/TargetingService.scala`

### Responsibility

Create a pure function that evaluates whether an ad matches a delivery request based on targeting rules.

### Implementation Details

**Function signature:**
```scala
def matches(ad: Ad, request: DeliveryRequest): Boolean
```

**Supported operators:**
- `eq` → equals (exact match)
- `in` → value list matching (comma-separated)

**Examples:**
- `country eq IN` → matches when request.country == "IN"
- `platform in android,ios` → matches when request.platform is "android" or "ios"

### Logic Requirements

- If an ad has **no targeting rules**, it should match all requests (default: show everywhere)
- All targeting rules in `ad.targetingRules` must pass for the ad to be eligible
- Rule evaluation:
  - Extract the request field value by `rule.key` (e.g., "country", "platform")
  - Compare using `rule.operator`:
    - `eq`: exact string match
    - `in`: check if request value exists in comma-separated list

### Acceptance Criteria

- [ ] `TargetingService.scala` created with `matches` function
- [ ] Handles empty targeting rules (matches all)
- [ ] Supports `eq` operator for exact matching
- [ ] Supports `in` operator for list matching
- [ ] Unit tests written (optional but recommended)

### Notes

- Cursor assist: Ask Cursor to generate unit tests, not logic
- This is a pure function with no side effects

---

## 2️⃣ Integrate Targeting into Delivery

**Status:** ⏳ Pending  
**Module:** `engine-infra`  
**File:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/AdDeliveryService.scala`

### Changes Required

Update the `deliver()` method to filter ads using targeting before random selection.

**Current behavior:**
- Fetches all active ads
- Picks one randomly

**New behavior:**
1. Fetch all active ads
2. **Filter using TargetingService:** `ads.filter(TargetingService.matches(_, request))`
3. If no eligible ads → return `None` (will result in 204 No Content)
4. If eligible ads exist → pick one randomly

### Code Changes

```scala
val eligible = ads.filter(ad => TargetingService.matches(ad, request))

if (eligible.isEmpty) None
else Some(eligible(Random.nextInt(eligible.length)))
```

### Acceptance Criteria

- [ ] `AdDeliveryService.deliver()` filters ads using `TargetingService.matches`
- [ ] Returns `None` when no ads match targeting rules
- [ ] Returns a random ad from eligible ads when matches exist
- [ ] Test case: `/ads/deliver?country=US` returns 204 for IN-only ads
- [ ] Test case: `/ads/deliver?country=IN` returns an ad when IN-targeted ad exists

### Testing

```bash
# Should return 204 No Content (no matching ads)
curl "http://localhost:8080/ads/deliver?country=US"

# Should return an ad (if IN-targeted ad exists)
curl "http://localhost:8080/ads/deliver?country=IN"
```

### Cursor Prompt Idea

> Update deliver() to filter ads using TargetingService.matches before selecting randomly. Return 204 if no ads match.

---

## 3️⃣ Admin API — Create Ad

**Status:** ⏳ Pending  
**Module:** `engine-api`  
**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/AdminAdRoutes.scala` (new file)

### Endpoint

**POST** `/admin/ads`

### Request Body

```json
{
  "id": "optional-uuid",           // Optional: auto-generated if not provided
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

### Behavior

1. Accept JSON request body (CreateAdRequest)
2. Generate UUID for `id` if not provided
3. Set `createdAt` and `updatedAt` to current timestamp
4. Build `Ad` object from request
5. Call `AdStore.upsert(ad)`
6. Return **201 Created** with the created Ad as JSON

### Response

**201 Created** with JSON body:
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

### Acceptance Criteria

- [ ] `AdminAdRoutes.scala` created
- [ ] `POST /admin/ads` endpoint implemented
- [ ] Auto-generates UUID if `id` not provided
- [ ] Sets timestamps correctly
- [ ] Stores ad via `AdStore.upsert()`
- [ ] Returns 201 with created Ad JSON
- [ ] Route wired into `HttpServer.scala`

### Testing

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

### Cursor Prompt Idea

> Scaffold a POST /admin/ads route that takes CreateAdRequest JSON, stores an Ad via AdStore, and returns 201 with the created Ad.

---

## 4️⃣ Admin API — List Ads

**Status:** ⏳ Pending  
**Module:** `engine-api`  
**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/AdminAdRoutes.scala` (add to existing)

### Endpoint

**GET** `/admin/ads?activeOnly=true`

### Query Parameters

- `activeOnly` (optional, default: `true`) — filter to show only active ads

### Behavior

1. Extract `activeOnly` query parameter (default: `true`)
2. If `activeOnly=true`: call `AdStore.listActive()`
3. If `activeOnly=false`: list all ads (may need new method or use existing)
4. Return JSON array of ads

### Response

**200 OK** with JSON array:
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
  },
  ...
]
```

### Acceptance Criteria

- [ ] `GET /admin/ads` endpoint added to `AdminAdRoutes`
- [ ] Respects `activeOnly` query parameter (default: `true`)
- [ ] Returns JSON array of ads
- [ ] Empty array if no ads found

### Testing

```bash
# List only active ads (default)
curl "http://localhost:8080/admin/ads"

# List all ads (including inactive)
curl "http://localhost:8080/admin/ads?activeOnly=false"
```

### Cursor Prompt Idea

> Add GET /admin/ads route returning JSON list of ads using AdStore.listActive().

---

## 5️⃣ Events API — List Events per Ad

**Status:** ⏳ Pending  
**Module:** `engine-api`  
**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/AdminAdRoutes.scala` (add to existing)

### Endpoint

**GET** `/admin/ads/{adId}/events?limit=50`

### Path Parameters

- `adId` — the ID of the ad to get events for

### Query Parameters

- `limit` (optional, default: `50`) — maximum number of events to return

### Behavior

1. Extract `adId` from path
2. Extract `limit` query parameter (default: `50`)
3. Call `EventStore.findByAdId(adId, limit)` 
4. Return JSON array of delivery events

### Response

**200 OK** with JSON array of events:
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
  },
  ...
]
```

### Prerequisites

- `EventStore` interface needs `findByAdId(adId: String, limit: Int): Future[List[DeliveryEvent]]` method
- Implement this in `RedisEventStore` if not already present

### Acceptance Criteria

- [ ] `EventStore.findByAdId()` method exists (or added)
- [ ] `GET /admin/ads/{adId}/events` endpoint implemented
- [ ] Respects `limit` query parameter
- [ ] Returns JSON array of delivery events for the specified ad
- [ ] Returns empty array if no events found

### Testing

```bash
# Get events for a specific ad
curl "http://localhost:8080/admin/ads/ad-1/events?limit=10"
```

### Done When

Showing impressions logged for specific ad:
```bash
curl "http://localhost:8080/admin/ads/ad-1/events?limit=10"
```
returns array of events.

---

## 6️⃣ Docs & Demo Script

**Status:** ⏳ Pending  
**Type:** Manual documentation

### Files to Update

- [ ] `docs/02-api-spec.md` — document new admin endpoints
- [ ] `docs/demo-script.md` — create/update demo flow

### Demo Flow to Document

1. **POST /admin/ads** — Create a targeted ad
   ```bash
   curl -X POST http://localhost:8080/admin/ads \
     -H "Content-Type: application/json" \
     -d '{
       "advertiserId": "demo-advertiser",
       "creativeUrl": "https://example.com/ad.jpg",
       "targetingRules": [
         {"key": "country", "operator": "eq", "value": "IN"}
       ],
       "isActive": true
     }'
   ```

2. **GET /ads/deliver** — Test with matching & non-matching params
   ```bash
   # Should return ad (matches targeting)
   curl "http://localhost:8080/ads/deliver?country=IN"
   
   # Should return 204 (no matching ads)
   curl "http://localhost:8080/ads/deliver?country=US"
   ```

3. **GET /admin/ads/{adId}/events** — Show impressions logged
   ```bash
   curl "http://localhost:8080/admin/ads/ad-1/events?limit=10"
   ```

### Acceptance Criteria

- [ ] `docs/02-api-spec.md` updated with:
  - POST /admin/ads endpoint spec
  - GET /admin/ads endpoint spec
  - GET /admin/ads/{adId}/events endpoint spec
- [ ] `docs/demo-script.md` created/updated with:
  - Complete demo flow
  - Example curl commands
  - Expected responses

---

## 🧪 Integration Testing Checklist

After completing all steps, verify end-to-end:

- [ ] Create an ad with country targeting using POST /admin/ads
- [ ] List ads using GET /admin/ads
- [ ] Request ad delivery with matching country → receives ad
- [ ] Request ad delivery with non-matching country → 204 No Content
- [ ] Verify events are logged after ad delivery
- [ ] Query events for specific ad using GET /admin/ads/{adId}/events

---

## 📝 Notes

- **Step 1** is manual implementation (pure domain logic)
- **Steps 2-5** can use Cursor assistance for scaffolding
- **Step 6** is manual documentation
- All steps should be completed in order for proper integration
- Test each step before moving to the next

---

**Last Updated:** 2025-12-03  
**Current Focus:** Step 1 — Targeting Logic


## Epic 2 Outcome

## Core ad delivery
- HTTP API for requesting ads
- Context-aware ad delivery using targeting rules
- Random selection from eligible ads
- Graceful handling when no ads match (returns 204)

## Targeting system
- Country-based targeting (e.g., show only in India)
- Platform targeting (Android, iOS, Web, or combinations)
- Multi-rule support (all rules must match — AND logic)
- Universal ads (no targeting rules = show everywhere)
- Operators: eq (equals) and in (list membership)

## Admin API
- Create ads programmatically via REST API
- List all ads (with filter for active/inactive)
- View delivery events (impressions) per ad
- Auto-generate IDs and timestamps for new ads

## Data storage
- Redis-backed persistence for ads and events
- Structured ad metadata (creative URLs, targeting rules, status)
- Event logging for all ad deliveries
- Query events by ad ID with pagination

## Event tracking
- Logs every ad impression with metadata
- Tracks device, user, country, platform info
- Query delivery history for specific ads
- Useful for analytics and debugging

## Technical capabilities
- RESTful HTTP API (Pekko HTTP)
- JSON-based communication (Circe)
- Multi-project Scala architecture
- Modular design (domain, infrastructure, API layers)
- Ready for deployment (configured build system)

## Current API endpoints
1. GET /ads/deliver — Request an ad (with targeting)
2. POST /admin/ads — Create a new ad
3. GET /admin/ads — List all ads
4. GET /admin/ads/{adId}/events — View delivery events

## Use cases supported
- Context-aware ad serving (different ads for different countries/platforms)
- Programmatic ad management (create/manage ads via API)
- Event tracking (monitor ad performance)
- Smart filtering (show relevant ads based on context)

In summary, it’s a functional ad serving engine with targeting, admin APIs, and event tracking, ready for demo and further development.