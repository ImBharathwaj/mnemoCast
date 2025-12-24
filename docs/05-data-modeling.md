#  Mnemocast Engine — Data Modeling Plan (v1)

> Goal: Design how data for the Mnemocast Engine is structured and stored,  
> with an MVP that starts simple (in-memory / single DB) but can scale later.

---

## 1. Design Principles

1. **Engine-first, UI-agnostic**
   - Model is driven by engine decision needs: targeting, playlisting, budgeting.
2. **MVP: simple & fast**
   - Start with **in-memory stores** or a **single relational DB** (Postgres).
3. **Evolvable**
   - Design tables and IDs so later we can:
     - shard by screen or region
     - archive events
     - add more budget types.

---

## 2. Storage Strategy (MVP → Future)

### MVP Phase (Engine v0)

- **Config/metadata** (screens, campaigns, creatives):
  - Can be in-memory or in a small relational DB (e.g., Postgres).
- **Events** (play events):
  - Small volume → can start in DB too.
- You can literally start:
  - In-memory for everything (Phase 1)
  - Then move **screens/campaigns/creatives/events** into Postgres when needed.

### Future Phase (Scaling)

- **Relational DB** (Postgres) for:
  - Screens, campaigns, creatives, targeting config, budgets.
- **Event store**:
  - Append-only events in:
    - Postgres table (initial)
    - OR time-partitioned storage for scale.
- **Cache layer**:
  - Redis for hot data (active campaigns, screen metadata).

---

## 3. Core Entities → Tables

### 3.1 `screens`

Represents registered displays.

**Table:** `screens`

| Column      | Type           | Constraints                      |
|------------|----------------|----------------------------------|
| id         | UUID / TEXT    | PK, unique                       |
| name       | TEXT           | NOT NULL                         |
| city       | TEXT           | NULL                             |
| area       | TEXT           | NULL                             |
| latitude   | DOUBLE PRECISION | NULL                           |
| longitude  | DOUBLE PRECISION | NULL                           |
| active     | BOOLEAN        | NOT NULL                         |
| created_at | TIMESTAMPTZ    | NOT NULL, default now()          |
| updated_at | TIMESTAMPTZ    | NOT NULL, default now()          |

**Tags modeling options:**

- MVP: separate table `screen_tags`
  - `screen_id`, `tag`
- Or JSONB field `tags` in `screens`.

**Recommended (flexible & queryable):**

**Table:** `screen_tags`

| Column    | Type        | Constraints         |
|-----------|-------------|---------------------|
| screen_id | UUID/TEXT   | FK → screens(id)    |
| tag       | TEXT        |                     |

**Indexes:**

- `idx_screens_city_area` on `(city, area)`
- `idx_screen_tags_tag` on `(tag)`

---

### 3.2 `campaigns`

Represents campaigns.

**Table:** `campaigns`

| Column       | Type           | Constraints                         |
|-------------|----------------|-------------------------------------|
| id          | UUID / TEXT    | PK                                  |
| name        | TEXT           | NOT NULL                            |
| priority    | INT            | NOT NULL                            |
| active      | BOOLEAN        | NOT NULL                            |
| start_time  | TIMESTAMPTZ    | NOT NULL                            |
| end_time    | TIMESTAMPTZ    | NOT NULL                            |
| max_plays   | BIGINT         | NULL (no limit if NULL)             |
| created_at  | TIMESTAMPTZ    | NOT NULL, default now()             |
| updated_at  | TIMESTAMPTZ    | NOT NULL, default now()             |

> `max_plays` is our MVP budget field.

---

### 3.3 Targeting tables

We break targeting into separate link tables for flexibility and fast lookup.

#### `campaign_target_cities`

| Column      | Type        | Constraints                   |
|-------------|-------------|-------------------------------|
| campaign_id | UUID/TEXT   | FK → campaigns(id)            |
| city        | TEXT        |                               |

#### `campaign_target_areas`

| Column      | Type        | Constraints                   |
|-------------|-------------|-------------------------------|
| campaign_id | UUID/TEXT   | FK → campaigns(id)            |
| area        | TEXT        |                               |

#### `campaign_target_tags`

| Column      | Type        | Constraints                   |
|-------------|-------------|-------------------------------|
| campaign_id | UUID/TEXT   | FK → campaigns(id)            |
| tag         | TEXT        |                               |

#### `campaign_target_dow`

(allowed days of week)

| Column      | Type        | Constraints                   |
|-------------|-------------|-------------------------------|
| campaign_id | UUID/TEXT   | FK → campaigns(id)            |
| day_of_week | SMALLINT    | 1–7 (1=Monday .. 7=Sunday)    |

#### `campaign_target_timebands`

| Column      | Type        | Constraints                          |
|-------------|-------------|--------------------------------------|
| id          | SERIAL / BIGINT | PK                              |
| campaign_id | UUID/TEXT   | FK → campaigns(id)                   |
| start_time  | TIME        | NOT NULL (local-time logical)       |
| end_time    | TIME        | NOT NULL                            |

**Indexes:**

- `idx_campaign_target_tags_tag` on `(tag)`
- `idx_campaign_target_cities_city` on `(city)`
- `idx_campaign_target_areas_area` on `(area)`

> For MVP, you might implement targeting in-memory using objects, and only later project into SQL tables. This schema just ensures future readiness.

---

### 3.4 `creatives`

Represents individual playable assets.

**Table:** `creatives`

| Column           | Type           | Constraints                       |
|------------------|----------------|-----------------------------------|
| id               | UUID / TEXT    | PK                                |
| campaign_id      | UUID / TEXT    | FK → campaigns(id)                |
| type             | TEXT           | `"video"` or `"image"`            |
| duration_seconds | INT            | NOT NULL                          |
| asset_url        | TEXT           | NOT NULL                          |
| created_at       | TIMESTAMPTZ    | NOT NULL, default now()           |
| updated_at       | TIMESTAMPTZ    | NOT NULL, default now()           |

**Index:**

- `idx_creatives_campaign_id` on `(campaign_id)`

---

### 3.5 `play_events`

Represents actual playbacks.

**Table:** `play_events`

| Column       | Type           | Constraints                          |
|-------------|----------------|--------------------------------------|
| id          | BIGSERIAL      | PK                                   |
| screen_id   | UUID / TEXT    | FK → screens(id)                     |
| campaign_id | UUID / TEXT    | FK → campaigns(id)                   |
| creative_id | UUID / TEXT    | FK → creatives(id)                   |
| started_at  | TIMESTAMPTZ    | NOT NULL                             |
| ended_at    | TIMESTAMPTZ    | NOT NULL                             |
| created_at  | TIMESTAMPTZ    | NOT NULL, default now()              |

**Indexes:**

- `idx_play_events_campaign_id` on `(campaign_id)`
- `idx_play_events_screen_id` on `(screen_id)`
- `idx_play_events_created_at` on `(created_at)`

**MVP Budget Query Pattern:**

To get how many times a campaign was played:

```sql
SELECT COUNT(*) 
FROM play_events 
WHERE campaign_id = :campaignId;
