✅ Development Steps (Ordered)
1️⃣ Targeting Logic (Manual)

Module: engine-domain
Path: modules/engine-domain/src/main/scala/mnemocast/engine/domain/services/TargetingService.scala

Responsibility:

Pure function:

def matches(ad: Ad, request: DeliveryRequest): Boolean

Support operators:

eq → equals

in → value list matching

Example:

country eq IN

platform in android,ios

Cursor assist: Ask Cursor to generate unit tests, not logic.

2️⃣ Integrate Targeting into Delivery (Cursor-assist)

Module: engine-infra
File: AdDeliveryService.scala

Changes:

Filter eligible ads:

val eligible = ads.filter(TargetingService.matches(_, request))


If none eligible → None

Else random pick

Cursor prompt idea:

Update deliver() to filter ads using TargetingService.matches before selecting randomly. Return 204 if no ads match.

Done when:

/ads/deliver?country=US returns 204 for IN-only ads

/ads/deliver?country=IN returns an ad

3️⃣ Admin API — Create Ad (Cursor-heavy)

Module: engine-api
Add to existing or new file:
routes/AdminAdRoutes.scala

Add endpoint:

POST /admin/ads


Input:

{
  "advertiserId": "...",
  "creativeUrl": "...",
  "targetUrl": "...",
  "targetingRules": [...],
  "isActive": true
}


Behavior:

Build Ad object

Generate UUID if id not provided

Set createdAt + updatedAt = now

AdStore.upsert(...)

Return 201 Created + JSON

Cursor prompt idea:

Scaffold a POST /admin/ads route that takes CreateAdRequest JSON, stores an Ad via AdStore, and returns 201 with the created Ad.

4️⃣ Admin API — List Ads (Cursor)

Endpoint:

GET /admin/ads?activeOnly=true


Response: list of Ads
Default: only active ads

Cursor prompt idea:

Add GET /admin/ads route returning JSON list of ads using AdStore.listActive().

Done when:

curl http://localhost:8080/admin/ads


returns array of ads.

5️⃣ Events API — List Events per Ad (Cursor)

Endpoint:

GET /admin/ads/{adId}/events?limit=50


Uses:

EventStore.findByAdId(adId, limit)


Done when:
Showing impressions logged for specific ad:

curl "http://localhost:8080/admin/ads/ad-1/events?limit=10"

6️⃣ Docs & Demo Script (Manual)

Files to update:

docs/02-api-spec.md

docs/demo-script.md

Demo flow to document:

POST /admin/ads to create a targeted ad

GET /ads/deliver with matching & non-matching params

GET /admin/ads/{adId}/events to show impressions logged
