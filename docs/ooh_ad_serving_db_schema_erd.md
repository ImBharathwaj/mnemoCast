# OOH Ad Serving — Database Schema, ERD & Implementation

This document describes a recommended **Postgres-first** schema for the OOH ad serving engine, plus Redis runtime keys and notes. It includes:

- Entity-relationship diagram (ASCII) for quick reference
- Detailed `CREATE TABLE` DDL for Postgres
- Indexes, constraints, partitioning recommendations
- Materialized views / aggregates for analytics
- Redis key designs for runtime (AdStore, freq caps, SOV counters)
- Sample seed data and queries for testing

---

## 1. High-level entities

- `placements` — logical zones (e.g., "Phoenix Mall - Food Court")
- `screens` — physical players / devices assigned to placements
- `campaigns` — advertiser campaign (budget, dates, targets)
- `ads` — creatives attached to campaigns, with targeting, duration
- `decisions` — the ad selection decision returned to a player
- `playout_events` — actual recorded playouts (impressions)
- `click_events` — optional, for QR/URL interactions
- `placement_traffic` — placement-level traffic multipliers
- `aggregates` — materialized views for dashboard

---

## 2. ASCII ERD (simplified)

```
+------------+      +----------+      +--------+
| placements |1---< | screens  | >--- | playout |
+------------+      +----------+      +--------+
      ^                   ^               ^
      |                   |               |
      |                   |               |
+------------+      +----------+      +--------+
| campaigns  |1---< |  ads     |1---< | decisions|
+------------+      +----------+      +--------+
      |                                  |
      |                                  v
      |                              +---------+
      +----------------------------->| clicks  |
                                     +---------+
```

Notes: 1-to-many shown with `1---<`.

---

## 3. Postgres DDL (recommended)

Assumptions:
- Postgres >= 13
- UUID extension enabled for stable IDs
- Timestamps use `timestamptz`

### 3.1 Common setup

```sql
-- enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS pgcrypto;
```

### 3.2 placements

```sql
CREATE TABLE placements (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  code TEXT UNIQUE NOT NULL,          -- short code, e.g. phoenix_food_court
  name TEXT NOT NULL,
  venue_type TEXT,                    -- mall, airport, metro, roadside
  city TEXT,
  loop_duration_seconds INT NOT NULL DEFAULT 180,
  screen_count INT DEFAULT 0,
  avg_impressions_per_playout NUMERIC(10,2) DEFAULT 1.0,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX idx_placements_city ON placements (city);
```

### 3.3 screens

```sql
CREATE TABLE screens (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  placement_id UUID NOT NULL REFERENCES placements(id) ON DELETE CASCADE,
  player_id TEXT UNIQUE NOT NULL,    -- device/player identifier
  name TEXT,
  resolution TEXT,
  orientation TEXT,
  last_seen timestamptz,              -- heartbeat time
  is_online BOOLEAN DEFAULT false,
  metadata JSONB,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX idx_screens_placement ON screens (placement_id);
CREATE INDEX idx_screens_playerid ON screens (player_id);
```

### 3.4 campaigns

```sql
CREATE TABLE campaigns (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name TEXT NOT NULL,
  advertiser TEXT,
  status TEXT NOT NULL DEFAULT 'active', -- active, paused, completed
  start_date date NOT NULL,
  end_date date NOT NULL,
  total_budget BIGINT DEFAULT 0,  -- cents/paise units if billing
  target_playouts BIGINT DEFAULT NULL, -- optional target
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

CREATE INDEX idx_campaigns_status ON campaigns (status);
CREATE INDEX idx_campaigns_dates ON campaigns (start_date, end_date);
```

### 3.5 ads

```sql
CREATE TABLE ads (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  campaign_id UUID NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
  name TEXT NOT NULL,
  creative_type TEXT NOT NULL,   -- video, image, html
  creative_url TEXT NOT NULL,
  duration_seconds INT NOT NULL,
  status TEXT NOT NULL DEFAULT 'active', -- active, paused, deleted
  placements UUID[] DEFAULT '{}',  -- allowed placement ids (array of UUIDs)
  cities TEXT[] DEFAULT '{}',      -- allowed cities
  venue_types TEXT[] DEFAULT '{}', -- allowed venues
  dayparts TSTZRANGE[] DEFAULT '{}', -- optional time ranges (see notes)
  share_of_voice NUMERIC(5,4) DEFAULT NULL, -- 0..1 for SOV distribution
  frequency_cap_per_screen INT DEFAULT NULL, -- plays per day per screen
  metadata JSONB,
  created_at timestamptz DEFAULT now(),
  updated_at timestamptz DEFAULT now()
);

-- GIN index for arrays
CREATE INDEX idx_ads_placements_gin ON ads USING GIN (placements);
CREATE INDEX idx_ads_cities_gin ON ads USING GIN (cities);
CREATE INDEX idx_ads_venue_types_gin ON ads USING GIN (venue_types);
```

**Note about `dayparts`**: Representing dayparts can be done in multiple ways. Simpler approach: store strings like `"10:00-14:00"` in `metadata` or as `text[]`. If you need robust queries, consider separate `ad_dayparts` table with `time_from` and `time_to` (time without date) per ad.

### 3.6 decisions

Record each decision returned to a screen. This is helpful for auditing, debugging playback gaps.

```sql
CREATE TABLE decisions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  request_id UUID NOT NULL,
  screen_id UUID REFERENCES screens(id) ON DELETE SET NULL,
  placement_id UUID REFERENCES placements(id) ON DELETE SET NULL,
  ad_id UUID REFERENCES ads(id) ON DELETE SET NULL,
  campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
  created_at timestamptz DEFAULT now(),
  served BOOLEAN DEFAULT false, -- whether playout later confirmed
  metadata JSONB
);

CREATE INDEX idx_decisions_request ON decisions (request_id);
CREATE INDEX idx_decisions_screen ON decisions (screen_id);
CREATE INDEX idx_decisions_ad ON decisions (ad_id);
```

### 3.7 playout_events (impressions / play-outs)

This table will be high-volume. Use partitioning (time-based) recommended.

```sql
CREATE TABLE playout_events (
  id BIGSERIAL PRIMARY KEY,
  decision_id UUID REFERENCES decisions(id) ON DELETE SET NULL,
  screen_id UUID REFERENCES screens(id) ON DELETE SET NULL,
  placement_id UUID REFERENCES placements(id) ON DELETE SET NULL,
  ad_id UUID REFERENCES ads(id) ON DELETE SET NULL,
  campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
  timestamp timestamptz NOT NULL DEFAULT now(),
  loop_index INT,
  loop_position INT, -- optional slot index
  metadata JSONB
) PARTITION BY RANGE (timestamp);

-- Example monthly partitions (create via job / migration)
```

Partition strategy: create `playout_events_YYYY_MM` for each month. Keep raw events for e.g., 6-12 months then move to cold storage.

Indexes:

```sql
-- index for queries by ad/campaign/time
CREATE INDEX idx_playout_ad_time ON playout_events (ad_id, timestamp DESC);
CREATE INDEX idx_playout_campaign_time ON playout_events (campaign_id, timestamp DESC);
CREATE INDEX idx_playout_screen_time ON playout_events (screen_id, timestamp DESC);
```

### 3.8 click_events (optional)

```sql
CREATE TABLE click_events (
  id BIGSERIAL PRIMARY KEY,
  decision_id UUID REFERENCES decisions(id) ON DELETE SET NULL,
  ad_id UUID REFERENCES ads(id) ON DELETE SET NULL,
  campaign_id UUID REFERENCES campaigns(id) ON DELETE SET NULL,
  screen_id UUID REFERENCES screens(id) ON DELETE SET NULL,
  timestamp timestamptz NOT NULL DEFAULT now(),
  landing_url TEXT,
  metadata JSONB
);

CREATE INDEX idx_clicks_ad_time ON click_events (ad_id, timestamp DESC);
```

### 3.9 placement_traffic (traffic multipliers used to estimate impressions)

```sql
CREATE TABLE placement_traffic (
  placement_id UUID PRIMARY KEY REFERENCES placements(id) ON DELETE CASCADE,
  avg_impressions_per_playout NUMERIC(10,2) DEFAULT 1.0,
  traffic_source TEXT, -- optional description
  updated_at timestamptz DEFAULT now()
);
```

### 3.10 aggregates / materialized views

Create materialized views to power the dashboard. Examples:

```sql
CREATE MATERIALIZED VIEW mv_campaign_daily AS
SELECT
  campaign_id,
  date_trunc('day', timestamp) as day,
  count(*) as playouts,
  sum(coalesce((metadata->>'estimated_impressions')::bigint, 0)) as estimated_impressions
FROM playout_events
GROUP BY campaign_id, date_trunc('day', timestamp);

CREATE INDEX ON mv_campaign_daily (campaign_id, day DESC);
```

Refresh strategy: refresh concurrently every X minutes (or incremental approach via ETL job).

---

## 4. Redis: runtime / low-latency data

Redis is used for:

- Fast ad selection caches
- Frequency caps per screen per ad
- Share-of-voice counters (sliding windows)
- Temporary pacing counters

### 4.1 Key patterns

**Ad Store (cache of eligible ads per placement):**
- `ads:placement:{placement_id}` → SORTED SET or LIST of `ad_id` with score = weight (share-of-voice priority)

**Ad metadata cache:**
- `ad:meta:{ad_id}` → HASH or JSON string (creative_url, duration, placements, caps)

**Decision write-through:**
- When a decision is made, write to Postgres `decisions` and also push to Redis stream for async processing if necessary:
  - `stream:decisions`

**Frequency cap per screen/ad (per day):**
- `freq:screen:{screen_id}:ad:{ad_id}:YYYY-MM-DD` → INTEGER (INCR on playout)
  - TTL: 2 days

**SOV counters (short window):**
- `sov:placement:{placement_id}:window:{window_start_ts}` → HASH of `{ad_id: counter}`
  - Use sliding buckets per loop or per minute

**Locking / atomic checks:**
- Use Redis Lua scripts to perform atomic: check cap, decrement budget counters, increment sov counters, return whether ad can be served.

### 4.2 Example Redis flow for `delivery/next`

1. Player calls `delivery/next`.
2. Server fetches candidate ad_ids from `ads:placement:{placement_id}` (ZREVRANGE or ZRANGEBYSCORE by weight).
3. For each candidate, call Lua script that atomically:
   - Checks `freq:screen:{screen_id}:ad:{ad_id}:YYYY-MM-DD` < cap
   - Checks campaign budget counter (optional in Redis)
   - Increments SOV bucket for placement & ad
   - Returns `ok` or `reject_reason`
4. First `ok` ad becomes decision. Write `decisions` row and reply with ad metadata.

This approach avoids race conditions under concurrency.

---

## 5. Schema considerations & scale

- `playout_events` will be the largest table; partition by month or week.
- Keep `decisions` smaller; store every decision but consider TTL/purge policy for >90 days if not needed long-term.
- Use `BIGSERIAL` for event tables to optimize insertion speed.
- Store large JSONB only where necessary; store frequently queried attributes in columns.
- For analytics, prefer periodic ETL to refresh materialized views rather than querying raw event tables on the fly.

Estimated scale guidance:
- 100 screens → 1k playouts/day * 100 = 100k/day (tiny)
- 1k screens → 100k playouts/day * 365 ≈ 36M/year (medium)
- Use partition retention after 6–12 months

---

## 6. Sample SQL: seed & test queries

### 6.1 Seed a placement & screen

```sql
INSERT INTO placements (code, name, city, loop_duration_seconds, screen_count)
VALUES ('phoenix_food_court', 'Phoenix Mall - Food Court', 'Chennai', 180, 4)
RETURNING id;

-- assume id = '...'
INSERT INTO screens (placement_id, player_id, name, resolution, orientation)
VALUES ('<placement-id>', 'player-001', 'Screen 1 - Near KFC', '1920x1080', 'landscape');
```

### 6.2 Create campaign + ad

```sql
INSERT INTO campaigns (name, advertiser, status, start_date, end_date, total_budget, target_playouts)
VALUES ('New Year Mall Blast', 'BrandX', 'active', '2025-01-01', '2025-01-10', 500000, 100000)
RETURNING id;

INSERT INTO ads (campaign_id, name, creative_type, creative_url, duration_seconds, placements, share_of_voice, frequency_cap_per_screen)
VALUES ('<campaign-id>', 'BrandX 15s video', 'video', 'https://cdn/brandx_15s.mp4', 15, ARRAY['<placement-id>']::uuid[], 0.3, 30)
RETURNING id;
```

### 6.3 Simulate a decision + playout

```sql
-- create decision
INSERT INTO decisions (request_id, screen_id, placement_id, ad_id, campaign_id, metadata)
VALUES (gen_random_uuid(), '<screen-id>', '<placement-id>', '<ad-id>', '<campaign-id>', jsonb_build_object('slot', 3))
RETURNING id;

-- record playout
INSERT INTO playout_events (decision_id, screen_id, placement_id, ad_id, campaign_id, timestamp, loop_index)
VALUES ('<decision-id>', '<screen-id>', '<placement-id>', '<ad-id>', '<campaign-id>', now(), 12);
```

### 6.4 Query: campaign daily playouts

```sql
SELECT campaign_id, date_trunc('day', timestamp) as day, count(*) as playouts
FROM playout_events
WHERE campaign_id = '<campaign-id>'
  AND timestamp >= '2025-01-01'::timestamptz
GROUP BY campaign_id, day
ORDER BY day DESC;
```

---

## 7. Backup, retention & GDPR-ish notes

- Keep raw `playout_events` for 6–12 months, then archive to S3 (CSV/Parquet) and delete old partitions.
- Keep `decisions` for 90 days by default (if needed for debugging extend longer).
- If storing any PII (not expected for OOH), ensure encryption and opt-out flows.

---

## 8. Migration & evolution

- Start simple: no ad_dayparts table; store `dayparts` in `ads.metadata` for MVP.
- As load grows:
  - Move to normalized `ad_dayparts` and `ad_placements` join tables for efficient filtering.
  - Add `campaign_budget_counters` in Redis backed by Postgres reconciliation jobs.
  - Adopt incremental materialized views for analytics.

---

## 9. Next actions (practical)

1. Create migrations for the tables above (use Flyway/liquibase/slick-evolution).
2. Implement Redis key patterns and Lua scripts for atomic checks.
3. Add periodic job to create partitions for `playout_events` monthly.
4. Build simple materialized views and a refresh job for dashboard.

---

If you want, I can now:

- Generate **Flyway SQL migration files** for the DDL above.
- Produce a **visual ERD** (PNG/SVG) you can embed in README.
- Create the **Redis Lua script** templates for atomic selection logic.

Which one should I do next?

