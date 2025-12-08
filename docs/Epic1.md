# Mnemocast — Epic 1 Development Steps (Ordered)

> Goal: Have a minimal but real ad delivery engine:
> - Store ads in Redis
> - Serve ads via HTTP API
> - Log impressions as events

---

## 1. Implement Core Data Models  
Create case classes + Circe JSON codecs.

**Module:** `engine-domain`  
**Path:** `modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/`

**Files:**
- `Ad.scala`
- `TargetingRule.scala`
- `DeliveryRequest.scala`
- `DeliveryResponse.scala`
- `DeliveryEvent.scala`

---

## 2. Define Storage Interfaces  
Create minimal CRUD-style traits for ads and events.

**Module:** `engine-infra`  
**Path:** `modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/`

**Files:**
- `AdStore.scala`
- `EventStore.scala`

Responsibilities:

- `AdStore`:
  - `upsert(ad: Ad): Future[Unit]`
  - `getById(id: String): Future[Option[Ad]]`
  - `listActive(): Future[List[Ad]]`
  - `delete(id: String): Future[Unit]`

- `EventStore`:
  - `append(event: DeliveryEvent): Future[Unit]`
  - `findByAdId(adId: String, limit: Int): Future[List[DeliveryEvent]]`

---

## 3. Implement Redis-Based Storage  
Setup Redis client + JSON-based storage.

**Module:** `engine-infra`  
**Path:** `modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/redis/`

**Files:**
- `RedisClient.scala`
- `RedisAdStore.scala`
- `RedisEventStore.scala`

Key patterns:

- Ads:
  - Key: `ads:<adId>`
  - Index set: `ads:index`
  - Value: JSON of `Ad`
- Events:
  - Key: `events:ad:<adId>`
  - Value: Redis list of `DeliveryEvent` JSON (one per event)

---

## 4. Implement Ad Delivery Service  
Implement v1 ad delivery logic with random selection.

**Module:** `engine-infra`  
**Path:** `modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/`

**File:**
- `AdDeliveryService.scala`

Behavior:

- Calls `AdStore.listActive()`
- Randomly selects one `Ad` (if any)
- Builds a `DeliveryResponse`
- (After Step 6) logs a `DeliveryEvent` via `EventStore`

---

## 5. Create HTTP API Endpoint  
Expose `GET /ads/deliver` to serve ads.

**Module:** `engine-api`  

**Files & Paths:**

- JSON support (Circe + Pekko HTTP)
  - `modules/engine-api/src/main/scala/mnemocast/engine/api/json/JsonSupport.scala`

- Routes:
  - `modules/engine-api/src/main/scala/mnemocast/engine/api/routes/AdRoutes.scala`
  - Exposes:
    - `GET /ads/deliver?deviceId=...&appId=...&country=IN&platform=android`
  - Builds `DeliveryRequest` from query + IP
  - Uses `AdDeliveryService.deliver(...)`
  - Returns:
    - `200 OK` + `DeliveryResponse` JSON when an ad is available  
    - `204 No Content` when no ad is available

- HTTP server bootstrap:
  - `modules/engine-api/src/main/scala/mnemocast/engine/api/HttpServer.scala`
  - Wires:
    - `RedisClient`
    - `RedisAdStore` (AdStore)
    - `RedisEventStore` (EventStore)
    - `AdDeliveryService`
    - `AdRoutes`
  - Binds Pekko HTTP on `0.0.0.0:8080`

---

## 6. Add Event Logging  
Ensure each successful delivery logs a `DeliveryEvent`.

**File touched:**
- `AdDeliveryService.scala`

Behavior:

- When an ad is selected:
  - Build `DeliveryEvent` with:
    - `eventType = "impression"`
    - `requestId`, `adId`, `occurredAt`
    - `metadata` containing optional fields (deviceId, userId, appId, ip, country, platform)
  - Call `EventStore.append(event)`
  - Then return the `DeliveryResponse` to the caller

Events persist in Redis: `events:ad:<adId>` list.

---

## 7. End-to-End Testing  
Validate the entire flow from seed → API → Redis.

### 7.1 Seed a demo ad

**Module:** `engine-infra`  
**File:**  
`modules/engine-infra/src/main/scala/mnemocast/engine/infra/SeedDemoAds.scala`

Responsibility:
- Create a demo `Ad`
- Use `RedisAdStore.upsert(...)` to seed `ad-1` (or similar) into Redis.

Run:

```bash
cd backend
sbt "engineInfra/runMain mnemocast.engine.infra.SeedDemoAds"
sbt "engineApi/runMain mnemocast.engine.api.HttpServer"

curl "http://localhost:8080/ads/deliver?deviceId=dev1&appId=app1&country=IN&platform=android"