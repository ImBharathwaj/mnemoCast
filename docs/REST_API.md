 Mnemocast – MVP REST API Plan (Pitch-Ready)

 Goal: A functioning OOH/Digital Ad Serving engine that serves ads + tracks performance + shows analytics
 No login / auth — all APIs are public for now

 API Module Overview
Module	Purpose
1. Inventory & Targeting	Where ads can appear + context metadata
2. Campaigns & Ads	Creatives, schedules, budgets, targeting
3. Delivery & Tracking	Core ad decisioning & events
4. Analytics & Reporting	Metrics for pitch demo
5. System / Debug	Health & service visibility
 1. Inventory & Targeting
1.1 Placement / Screen API
Endpoint	Description
POST /api/v1/placements	Register a new screen/placement
GET /api/v1/placements	List all placements
GET /api/v1/placements/{placement_id}	Get placement details
PATCH /api/v1/placements/{placement_id}	Update placement
Example Request
{
  "name": "Phoenix Mall Screen 1",
  "type": "display",
  "location": "Chennai - Food Court"
}

 2. Campaigns & Ads (MVP: No AdSets Layer)

AdSets = merged into ads themselves (targeting per ad).

2.1 Campaigns
Endpoint	Description
POST /api/v1/campaigns	Create a campaign
GET /api/v1/campaigns	List campaigns
GET /api/v1/campaigns/{campaign_id}	View details
PATCH /api/v1/campaigns/{campaign_id}	Update metadata
POST /api/v1/campaigns/{campaign_id}/pause	Pause campaign
POST /api/v1/campaigns/{campaign_id}/resume	Resume campaign
Example Body
{
  "name": "New Year Mega Sale",
  "start_date": "2025-01-01",
  "end_date": "2025-01-31",
  "status": "active",
  "total_budget": 100000,
  "daily_budget": 5000
}

2.2 Ads
Endpoint	Description
POST /api/v1/ads	Create ad with targeting & budget rules
GET /api/v1/ads	List ads
GET /api/v1/ads/{ad_id}	View an ad
PATCH /api/v1/ads/{ad_id}	Update status or creatives
Example Body
{
  "campaign_id": "c1",
  "name": "50% Off Banner",
  "creative_url": "https://cdn/ads/50percent.png",
  "creative_type": "image",
  "duration_seconds": 10,
  "click_url": "https://brand.com/offer",
  "placements": ["p1", "p3"],
  "frequency_cap": { "impressions": 3, "per": "day", "per_entity": "device" },
  "targeting": {
    "tags": ["mall", "shopping"],
    "time_of_day": ["10:00-18:00"]
  }
}

 3. Delivery & Tracking (Heart of Engine)
3.1 Serve Ad Endpoint
Method	Endpoint
POST	/api/v1/delivery
Request Body
{
  "placement_id": "p1",
  "device_id": "dev-001",
  "timestamp": "2025-01-01T12:00:00Z",
  "context": { "tags": ["mall"] }
}

Response — Success
{
  "request_id": "req-123",
  "decision_id": "dec-456",
  "ad": {
    "ad_id": "a1",
    "campaign_id": "c1",
    "creative_url": "https://cdn/ad.png",
    "duration_seconds": 10,
    "click_url": "/api/v1/track/click?decision_id=dec-456"
  }
}

Response — No Fill
{
  "request_id": "req-123",
  "ad": null,
  "meta": { "reason": "no_fill" }
}

3.2 Impression Tracking
Method	Endpoint
POST	/api/v1/track/impression

Body

{
  "decision_id": "dec-456",
  "timestamp": "2025-01-01T12:00:05Z"
}

3.3 Click Tracking + Redirect
Method	Endpoint
GET	/api/v1/track/click?decision_id=dec-456

Behavior:

Log click event

Redirect to original landing URL

 4. Analytics & Reporting
4.1 Summary API
Endpoint
GET /api/v1/analytics/summary?from&to&campaign_id&placement_id

Example Response

{
  "totals": { "impressions": 10234, "clicks": 340, "ctr": 0.033 },
  "by_campaign": [
    {
      "campaign_id": "c1",
      "impressions": 8000,
      "clicks": 300,
      "ctr": 0.0375
    }
  ]
}

4.2 Time Series Metrics
Endpoint
GET /api/v1/analytics/timeseries?granularity=hour&group_by=placement
 5. System / Debug
Endpoint	Description
GET /health	Service alive
GET /ready	Redis/DB connected
GET /api/v1/system/info	Version, build SHA
 Test Coverage Plan
Test Type	Purpose
Unit Tests	logic: selection, caps, budgets
Integration Tests	Redis/DB + full delivery flow
Negative Tests	No ad → correct “no_fill”
Load Tests	p95 latency within threshold
Demo Tests	dashboards reflect live traffic
 MVP Pitch Demo Checklist
Status	Task
	Ads serving consistently (/delivery)
	Budgets + caps enforced
	Impressions + clicks logged
	Summary analytics computed
	docker-compose: one command demo
	Simple dashboard / Grafana for visuals
 What This Enables in a Pitch

Run a traffic generator → ads serve in real-time

Analytics dashboard updates → metrics visibly change

Investor immediately “gets” the product value