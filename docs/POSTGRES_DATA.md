# Postgres Database - What Records Are Stored

Complete guide to what data gets stored in Postgres and when.

---

## Database Tables Overview

After running the schema migration, you'll have **4 main tables**:

1. **`ads`** - Ad information
2. **`targeting_rules`** - Targeting rules for ads
3. **`delivery_events`** - Impression and click events
4. **`event_metadata`** - Metadata for events (deviceId, userId, etc.)

---

## 1. ADS Table

### What Gets Stored

**When:** Every time you create or update an ad via `POST /admin/ads`

**Records Stored:**
- Ad ID (primary key)
- Advertiser ID
- Creative URL (image/video URL)
- Target URL (landing page)
- Active status
- Budget settings (maxPlays, dailyLimit, hourlyLimit)
- Frequency capping settings
- Timestamps (created_at, updated_at)

### Example Record

```sql
SELECT * FROM ads;
```

**Result:**
```
id          | advertiser_id | creative_url              | target_url           | is_active | max_plays | daily_limit | hourly_limit | ...
------------+---------------+---------------------------+----------------------+----------+-----------+-------------+--------------+----
ad-123      | advertiser-1  | https://example.com/ad.jpg| https://example.com  | true     | 1000      | 100         | 10           | ...
ad-456      | advertiser-2  | https://example.com/ad2.mp4| https://example.com/offer | true | 5000   | 500         | 50           | ...
```

### When Records Are Created

```bash
# This command creates a record in ads table
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "isActive": true,
    "maxPlays": 1000
  }'
```

**Postgres Record Created:**
- 1 row in `ads` table
- 0 or more rows in `targeting_rules` table (if targeting rules provided)

---

## 2. TARGETING_RULES Table

### What Gets Stored

**When:** When you create/update an ad with targeting rules

**Records Stored:**
- Rule ID (UUID, auto-generated)
- Ad ID (foreign key to ads table)
- Rule key (e.g., "country", "platform")
- Operator (e.g., "eq", "in")
- Rule value (e.g., "IN", "android,ios")
- Created timestamp

### Example Record

```sql
SELECT * FROM targeting_rules;
```

**Result:**
```
id                  | ad_id   | rule_key | operator | rule_value    | created_at
--------------------+---------+----------+----------+---------------+-------------------
550e8400-...        | ad-123  | country  | eq       | IN            | 2024-12-07 10:00:00
550e8401-...        | ad-123  | platform | in       | android,ios   | 2024-12-07 10:00:00
```

### When Records Are Created

```bash
# This creates 2 records in targeting_rules table
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [
      {"key": "country", "operator": "eq", "value": "IN"},
      {"key": "platform", "operator": "in", "value": "android,ios"}
    ],
    "isActive": true
  }'
```

**Postgres Records Created:**
- 1 row in `ads` table
- 2 rows in `targeting_rules` table

---

## 3. DELIVERY_EVENTS Table

### What Gets Stored

**When:** Every time an ad is delivered or clicked

**Records Stored:**
- Event ID (UUID, auto-generated)
- Event ID (string, unique identifier)
- Request ID (from delivery request)
- Ad ID (which ad was shown/clicked)
- Event type ("impression" or "click")
- Occurred timestamp (when event happened)
- Created timestamp (when record was inserted)

### Example Records

```sql
SELECT * FROM delivery_events ORDER BY occurred_at DESC LIMIT 5;
```

**Result:**
```
id                  | event_id           | request_id        | ad_id   | event_type | occurred_at          | created_at
--------------------+--------------------+-------------------+---------+------------+----------------------+-------------------
550e8400-...        | evt-001            | req-001           | ad-123  | impression | 2024-12-07 10:30:00  | 2024-12-07 10:30:00
550e8401-...        | evt-002            | req-002           | ad-123  | click      | 2024-12-07 10:30:15  | 2024-12-07 10:30:15
550e8402-...        | evt-003            | req-003           | ad-456  | impression | 2024-12-07 10:31:00  | 2024-12-07 10:31:00
```

### When Records Are Created

**Impression Events:**
- Automatically created when `GET /ads/deliver` returns an ad
- Also created when `GET /api/v1/events/impression` is called

**Click Events:**
- Created when `GET /api/v1/events/click` is called

**Example Flow:**
```bash
# Step 1: Request ad (creates impression event)
curl -X GET "http://localhost:8080/ads/deliver"
# → Creates 1 row in delivery_events (event_type = 'impression')

# Step 2: Track click (creates click event)
curl -X GET "http://localhost:8080/api/v1/events/click?adId=ad-123&requestId=req-001"
# → Creates 1 row in delivery_events (event_type = 'click')
```

---

## 4. EVENT_METADATA Table

### What Gets Stored

**When:** Every time an event is created (impression or click)

**Records Stored:**
- Metadata ID (UUID, auto-generated)
- Event ID (foreign key to delivery_events)
- Metadata key (e.g., "deviceId", "userId", "ip", "country", "platform")
- Metadata value (the actual value)
- Created timestamp

### Example Records

```sql
SELECT * FROM event_metadata WHERE event_id = (SELECT id FROM delivery_events WHERE event_id = 'evt-001');
```

**Result:**
```
id                  | event_id           | metadata_key | metadata_value | created_at
--------------------+--------------------+--------------+----------------+-------------------
550e8400-...        | 550e8400-...       | deviceId     | device-123     | 2024-12-07 10:30:00
550e8401-...        | 550e8400-...       | userId       | user-456       | 2024-12-07 10:30:00
550e8402-...        | 550e8400-...       | ip           | 192.168.1.1    | 2024-12-07 10:30:00
550e8403-...        | 550e8400-...       | country      | IN             | 2024-12-07 10:30:00
550e8404-...        | 550e8400-...       | platform     | android        | 2024-12-07 10:30:00
```

### When Records Are Created

Automatically created with each event. Metadata includes:
- `deviceId` - If provided in delivery request
- `userId` - If provided in delivery request
- `appId` - If provided in delivery request
- `ip` - Client IP address
- `country` - If provided in delivery request
- `platform` - If provided in delivery request

**Example:**
```bash
# Request with device and user info
curl -X GET "http://localhost:8080/ads/deliver?deviceId=device-123&userId=user-456&country=IN&platform=android"

# Creates:
# - 1 row in delivery_events
# - 5 rows in event_metadata (deviceId, userId, ip, country, platform)
```

---

## Complete Data Flow Example

### Scenario: Create Ad → Deliver → Click → View Analytics

#### Step 1: Create Ad

```bash
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "advertiser-1",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com/landing",
    "targetingRules": [{"key": "country", "operator": "eq", "value": "IN"}],
    "isActive": true,
    "maxPlays": 1000
  }'
```

**Postgres Records:**
-  1 row in `ads` table
-  1 row in `targeting_rules` table

#### Step 2: Request Ad Delivery

```bash
curl -X GET "http://localhost:8080/ads/deliver?country=IN&deviceId=device-123&userId=user-456"
```

**Postgres Records:**
-  1 row in `delivery_events` (event_type = 'impression')
-  4-6 rows in `event_metadata` (deviceId, userId, ip, country, platform, etc.)

#### Step 3: Track Click

```bash
curl -L "http://localhost:8080/api/v1/events/click?adId=ad-123&requestId=req-001"
```

**Postgres Records:**
-  1 row in `delivery_events` (event_type = 'click')
-  1-2 rows in `event_metadata` (ip, possibly others)

#### Step 4: View Analytics

```bash
curl -X GET "http://localhost:8080/api/v1/analytics/ads/ad-123"
```

**Postgres Queries:**
- Queries `delivery_events` table to count impressions and clicks
- Calculates CTR from the event counts

---

## Querying the Data

### View All Ads

```sql
SELECT id, advertiser_id, creative_url, is_active, max_plays, created_at 
FROM ads 
ORDER BY created_at DESC;
```

### View Ad with Targeting Rules

```sql
SELECT a.id, a.advertiser_id, tr.rule_key, tr.operator, tr.rule_value
FROM ads a
LEFT JOIN targeting_rules tr ON a.id = tr.ad_id
WHERE a.id = 'ad-123';
```

### View Event History

```sql
SELECT 
    de.event_type,
    de.occurred_at,
    a.advertiser_id,
    COUNT(*) as count
FROM delivery_events de
JOIN ads a ON de.ad_id = a.id
GROUP BY de.event_type, de.occurred_at::date, a.advertiser_id
ORDER BY de.occurred_at DESC;
```

### View Impressions by Device

```sql
SELECT 
    em.metadata_value as device_id,
    COUNT(*) as impressions
FROM delivery_events de
JOIN event_metadata em ON de.id = em.event_id
WHERE de.event_type = 'impression'
  AND em.metadata_key = 'deviceId'
GROUP BY em.metadata_value
ORDER BY impressions DESC;
```

### View Ad Performance (Using View)

```sql
SELECT * FROM ad_performance;
```

**Result:**
```
ad_id   | impressions | clicks | ctr
--------+-------------+--------+-----
ad-123  | 1500        | 45     | 3.0
ad-456  | 5000        | 200    | 4.0
```

---

## Data Growth Over Time

### Typical Data Volume

**Ads Table:**
- Small: 10-100 ads
- Medium: 100-1,000 ads
- Large: 1,000+ ads

**Targeting Rules Table:**
- ~1-5 rules per ad
- Total: ~10-5,000 rows

**Delivery Events Table:**
- Grows continuously (append-only)
- 1 impression per ad delivery
- 1 click per user click
- Can grow to millions of rows over time

**Event Metadata Table:**
- ~4-6 metadata rows per event
- Total: ~4-6x the number of events

### Example: After 1 Month of Operation

**Assumptions:**
- 50 active ads
- 1,000 ad deliveries per day
- 50 clicks per day (5% CTR)

**Expected Records:**
- `ads`: ~50 rows
- `targeting_rules`: ~100-250 rows
- `delivery_events`: ~31,500 rows (30 days × 1,050 events/day)
- `event_metadata`: ~126,000 rows (31,500 × 4)

---

## Data Retention

### Current Implementation

- **No automatic deletion** - All data is kept indefinitely
- **Manual cleanup** - You can delete old events manually

### Future Considerations

For production, you might want to:
- Archive old events (> 90 days) to separate table
- Delete very old events (> 1 year)
- Partition events table by date

---

## Backup & Recovery

### Backup Database

```bash
# Full backup
pg_dump -U postgres -d mnemocast > mnemocast_backup.sql

# Restore
psql -U postgres -d mnemocast < mnemocast_backup.sql
```

### Backup Specific Tables

```bash
# Backup only ads
pg_dump -U postgres -d mnemocast -t ads > ads_backup.sql

# Backup only events
pg_dump -U postgres -d mnemocast -t delivery_events > events_backup.sql
```

---

## Summary

### What Gets Stored When

| Action | Tables Updated | Records Created |
|--------|---------------|-----------------|
| **Create Ad** | `ads`, `targeting_rules` | 1 ad + N targeting rules |
| **Update Ad** | `ads`, `targeting_rules` | Updates existing records |
| **Deliver Ad** | `delivery_events`, `event_metadata` | 1 event + 4-6 metadata rows |
| **Click Ad** | `delivery_events`, `event_metadata` | 1 event + 1-2 metadata rows |
| **Track Impression** | `delivery_events`, `event_metadata` | 1 event + metadata |

### Data Relationships

```
ads (1) ──< (many) targeting_rules
ads (1) ──< (many) delivery_events
delivery_events (1) ──< (many) event_metadata
```

---

## Viewing Your Data

### Quick Commands

```bash
# Count all ads
psql -U postgres -d mnemocast -c "SELECT COUNT(*) FROM ads;"

# Count all events
psql -U postgres -d mnemocast -c "SELECT COUNT(*) FROM delivery_events;"

# View recent events
psql -U postgres -d mnemocast -c "SELECT * FROM delivery_events ORDER BY occurred_at DESC LIMIT 10;"

# View ad performance
psql -U postgres -d mnemocast -c "SELECT * FROM ad_performance;"
```

---

After running the schema migration and using the system, you'll see records appear in these tables as you create ads and deliver them!

