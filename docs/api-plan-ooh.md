## 0. Core Concepts (OOH-specific)

- Screen / Player – a physical display with a media player.
- Placement – a logical ad slot/group of screens (e.g., “Phoenix Mall – Food Court Screens”).
- Loop – the playlist cycle on a screen (e.g., 180 seconds).
- Play-out – one ad being shown once on a screen.
- Campaign – advertiser objective with budget + dates.
- Ad – single creative with duration.

We do not track per-user behavior. Targeting is per screen / location / time-of-day.

## 1. Inventory – Screens & Placements
### 1.1 Placements (logical ad zones)

| Endpoint                                  | Description                     |
| ----------------------------------------- | ------------------------------- |
| `POST /api/v1/placements`                 | Create a new OOH placement/zone |
| `GET /api/v1/placements`                  | List placements                 |
| `GET /api/v1/placements/{placement_id}`   | Get placement details           |
| `PATCH /api/v1/placements/{placement_id}` | Update placement info           |

### Example
```json
{
  "name": "Phoenix Mall – Food Court",
  "code": "phoenix_food_court",
  "venue_type": "mall",
  "city": "Chennai",
  "loop_duration_seconds": 180,
  "screen_count": 4
}
```

### 1.2 Screens / Players (physical devices)

| Endpoint                            | Description                       |
| ----------------------------------- | --------------------------------- |
| `POST /api/v1/screens`              | Register a physical screen/player |
| `GET /api/v1/screens`               | List screens                      |
| `GET /api/v1/screens/{screen_id}`   | Screen details                    |
| `PATCH /api/v1/screens/{screen_id}` | Update screen metadata            |

### Example
```json
{
  "placement_id": "phoenix_food_court",
  "name": "Screen 1 – Near KFC",
  "player_id": "player-001",
  "resolution": "1920x1080",
  "orientation": "landscape"
}
```

## 2. Campaigns & Ads (OOH-style)
### We keep it simple for MVP:

- Campaign = budget + schedule.
- Ads = creatives attached to campaign.
- Targeting = which placements + dayparts.

### 2.1 Campaigns

| Endpoint                                      | Description     |
| --------------------------------------------- | --------------- |
| `POST /api/v1/campaigns`                      | Create campaign |
| `GET /api/v1/campaigns`                       | List            |
| `GET /api/v1/campaigns/{campaign_id}`         | Details         |
| `PATCH /api/v1/campaigns/{campaign_id}`       | Update          |
| `POST /api/v1/campaigns/{campaign_id}/pause`  | Pause           |
| `POST /api/v1/campaigns/{campaign_id}/resume` | Resume          |


### Example
```json
{
  "name": "New Year Mall Blast",
  "advertiser": "BrandX",
  "status": "active",
  "start_date": "2025-01-01",
  "end_date": "2025-01-10",
  "total_budget": 500000,
  "target_impressions": 100000
}
```

## 2.2 Ads (OOH creatives)
| Endpoint                    | Description |
| --------------------------- | ----------- |
| `POST /api/v1/ads`          | Create ad   |
| `GET /api/v1/ads`           | List ads    |
| `GET /api/v1/ads/{ad_id}`   | Get ad      |
| `PATCH /api/v1/ads/{ad_id}` | Update ad   |

### Example
```json
{
  "campaign_id": "cmp_1",
  "name": "BrandX 15s Video",
  "creative_type": "video",
  "creative_url": "https://cdn.mnemocast.com/brandx_15s.mp4",
  "duration_seconds": 15,
  "targeting": {
    "placements": ["phoenix_food_court"],
    "venue_types": ["mall"],
    "cities": ["Chennai"],
    "dayparts": ["10:00-14:00", "17:00-21:00"]
  },
  "share_of_voice": 0.3, 
  "frequency_cap_per_screen": {
    "max_plays": 30,
    "per": "day"
  }
}
```

#### Note:
- share_of_voice = desired share of loop (e.g., 0.3 = 30% of plays in target loop).
- Frequency cap is per screen, not per user.

## 3. Delivery – Player Asking “What Do I Play Next?”
The player calls the engine for every ad slot in its loop, or every time it needs a new item.

### 3.1 Get Next Ad for a Screen
| Method | Endpoint                |
| ------ | ----------------------- |
| `POST` | `/api/v1/delivery/next` |

### Request
```json
{
  "screen_id": "screen-1",
  "player_id": "player-001",
  "placement_id": "phoenix_food_court",
  "timestamp": "2025-01-01T12:00:00Z",
  "slot_index_in_loop": 3,
  "loop_duration_seconds": 180
}
```

### Response – Ad Selected
```json
{
  "request_id": "req-123",
  "decision_id": "dec-456",
  "ad": {
    "ad_id": "ad_1",
    "campaign_id": "cmp_1",
    "creative_type": "video",
    "creative_url": "https://cdn.mnemocast.com/brandx_15s.mp4",
    "duration_seconds": 15
  },
  "playout": {
    "screen_id": "screen-1",
    "placement_id": "phoenix_food_court",
    "slot_index_in_loop": 3,
    "scheduled_start": "2025-01-01T12:00:10Z"
  }
}
```

### Response – No Fill
```json
{
  "request_id": "req-123",
  "ad": null,
  "meta": {
    "reason": "no_fill"
  }
}
```

#### Internally, you enforce:

- Campaign active window (start/end date)
- Placement + city + daypart matching
- Campaign/ad budgets + impression/play caps
- Share-of-voice distribution

## 3.2 Impression / Play-out Tracking
In OOH, you mostly track play-outs (how many times an ad actually played).

| Method | Endpoint                |
| ------ | ----------------------- |
| `POST` | `/api/v1/track/playout` |

### Request Body
```json
{
  "decision_id": "dec-456",
  "screen_id": "screen-1",
  "timestamp": "2025-01-01T12:00:15Z",
  "loop_index": 12
}
```
#### This is what drives:

- Impression counts
- Frequency caps per screen
- Billing / budget consumption

### 3.3 Clicks (Optional for OOH – QR / Short URL)
If you do want to support QR / URL tracking:
| Method | Endpoint                                  |
| ------ | ----------------------------------------- |
| `GET`  | `/api/v1/track/click?decision_id=dec-456` |

### Behavior:
1. Log “click” event tied to that playout.
2. Redirect to configured landing page.

#### For pure OOH MVP, you can:

- Keep this optional.
- Or enable only for special campaigns.

## 4. Analytics for OOH Networks
### 4.1 Network Summary

| Endpoint                                                                   |
| -------------------------------------------------------------------------- |
| `GET /api/v1/analytics/summary?from=...&to=...&placement_id=&campaign_id=` |

Example Response
```json
{
  "from": "2025-01-01",
  "to": "2025-01-01",
  "totals": {
    "playouts": 12000,
    "unique_screens": 25,
    "active_campaigns": 5
  },
  "by_campaign": [
    {
      "campaign_id": "cmp_1",
      "campaign_name": "New Year Mall Blast",
      "playouts": 5000,
      "estimated_impressions": 200000 
    }
  ],
  "by_placement": [
    {
      "placement_id": "phoenix_food_court",
      "playouts": 3000
    }
  ]
}
```

`estimated_impressions` can be derived from playouts × screen traffic multiplier per placement
(you can store `avg_impressions_per_playout` at placement level).

### 4.2 Time-Series Metrics
| Endpoint                                                   |                       |            |
| ---------------------------------------------------------- | --------------------- | ---------- |
| `GET /api/v1/analytics/timeseries?from&to&granularity=hour | day&group_by=campaign | placement` |

#### Used to build charts:

- Playouts per hour per placement
- Activity per campaign across dayparts

### 4.3 Fill-Rate Diagnostics
| Endpoint                                                |
| ------------------------------------------------------- |
| `GET /api/v1/analytics/fill-rate?from&to&placement_id=` |

### Example
```json
{
  "placement_id": "phoenix_food_court",
  "from": "2025-01-01",
  "to": "2025-01-01",
  "requested_slots": 10000,
  "filled_slots": 8000,
  "fill_rate": 0.8
}
```
Very strong in pitch:
👉 “Our engine keeps your screens monetized; here’s your fill-rate over time.”

## 5. System / Debug (Same, Just OOH Context)
| Endpoint                  | Description                  |
| ------------------------- | ---------------------------- |
| `GET /health`             | Service up                   |
| `GET /ready`              | Can talk to Redis/DB         |
| `GET /api/v1/system/info` | Version, git SHA, build time |

## 6. Testing Focus – OOH Specific

### 6.1 Unit Tests

#### Ad selection:

- Correct matching by placement, city, venue_type, daypart.
- Respect share-of-voice proportions over many requests.
- Respect frequency caps per screen.

#### Budget logic:

- Campaign stops serving when target play-outs or budget is hit.

### 6.2 Integration Tests

- Spin up Redis/DB + service.

- Scenario:

1. Create 2 placements, 3 screens.
2. Create 2 campaigns with different targeting & budgets.
3. Create ads for each campaign.
4. Simulate 10k `/delivery/next` calls.
5. Log play-outs.
6. Assert:
    - No campaign exceeds max play-outs / budget.
    - No screen exceeds frequency cap for any ad.
    - Fill-rate metrics are correct.

### 6.3 Demo / Pitch Scenario

- Script:
    - Seed demo placements & screens.
    - Seed 2–3 campaigns (e.g., FMCG, fashion, food).
    - Run traffic generator simulating 20 screens.
    - Open dashboard:
        - Show playout counts per campaign/placement.
        - Show fill-rate improving when you enable another campaign.