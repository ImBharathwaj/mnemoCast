#  Demo Data Seed Script

**Purpose:** Populate database with realistic dummy data for dashboard showcase and testing.

---

##  Overview

The `seed_demo_data.sql` script creates comprehensive demo data including:
- **9 Screens** across different cities and venue types
- **8 Campaigns** with various statuses (active, paused, completed)
- **11 Creatives** with different types (image/video) and durations
- **Screen Tags** for targeting
- **Targeting Rules** for campaigns
- **Delivery Events** (impressions) for analytics
- **Ads** (legacy model)

---

##  Usage

### Run the Seed Script

```bash
# Make sure database is initialized first
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql

# Then seed demo data
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/seed_demo_data.sql
```

### Verify Data

```bash
# Check counts
psql -h localhost -U postgres -d mnemocast -c "
SELECT 
    'Screens' as table_name, COUNT(*) as count FROM screens
UNION ALL
SELECT 'Campaigns', COUNT(*) FROM campaigns
UNION ALL
SELECT 'Creatives', COUNT(*) FROM creatives
UNION ALL
SELECT 'Events', COUNT(*) FROM delivery_events;
"
```

---

##  Data Overview

### Screens (9 total)
- **Chennai Airport** (2 screens) - Premium, 4K, audible
- **Mumbai Mall** (2 screens) - Food court and entrance
- **Delhi Metro** (1 screen) - Transit, non-audible
- **Bangalore Office** (1 screen) - IT park lobby
- **Hyderabad Mall** (1 screen) - Cinema hall, 4K
- **Pune Transit** (1 screen) - Bus stand (offline)
- **Kolkata Mall** (1 screen) - Food court

**Display Specifications:**
- Mix of 1920×1080, 2560×1440, and 3840×2160 resolutions
- Some audible, some non-audible
- Classification range: 3-9 (premium to standard)

### Campaigns (8 total)
1. **Summer Sale 2024** - Active, high priority (8)
2. **Morning Coffee Push** - Active, medium priority (7)
3. **Premium Watch Collection** - Active, highest priority (10)
4. **Weekend Shopping Spree** - Active, medium priority (6)
5. **Tech Product Launch** - Paused, high priority (9)
6. **Food Delivery Service** - Active, low priority (5)
7. **Fitness Center Membership** - Active, medium priority (7)
8. **Completed Test Campaign** - Completed status

### Creatives (11 total)
- Mix of images and videos
- Different durations (10-60 seconds)
- Various share of voice allocations
- Frequency caps set

### Events (Generated)
- **100-500 events per screen-ad combination**
- Spread over last 7 days
- Creates realistic analytics data
- Includes event metadata

---

##  Use Cases Demonstrated

### 1. **Geographic Targeting**
- Campaigns target specific cities (Chennai, Mumbai, Delhi, etc.)
- Screens in different locations

### 2. **Venue Type Targeting**
- Malls, airports, metros, offices, transit
- Different campaigns target different venue types

### 3. **Time-Based Targeting**
- Morning coffee campaign targets morning hours
- Weekend shopping targets weekends

### 4. **Screen Classification**
- Premium screens (classification 7-9)
- Standard screens (classification 3-6)

### 5. **Display Specifications**
- Different resolutions (HD, 2K, 4K)
- Audio capability variations
- Size-based targeting ready

### 6. **Analytics Data**
- Historical impression data
- Performance metrics
- Time-series data for charts

---

##  Re-seeding Data

The script is **idempotent** - safe to run multiple times:
- Uses `ON CONFLICT DO UPDATE` for most inserts
- Updates existing records instead of failing
- Can be run to refresh data

### To Clear and Re-seed:

```sql
-- Clear all data (run in psql)
TRUNCATE TABLE event_metadata CASCADE;
TRUNCATE TABLE delivery_events CASCADE;
TRUNCATE TABLE screen_metadata CASCADE;
TRUNCATE TABLE screen_tags CASCADE;
TRUNCATE TABLE screens CASCADE;
TRUNCATE TABLE creative_metadata CASCADE;
TRUNCATE TABLE creatives CASCADE;
TRUNCATE TABLE targeting_rules CASCADE;
TRUNCATE TABLE campaigns CASCADE;
TRUNCATE TABLE ads CASCADE;

-- Then run seed script again
\i infra/local-dev/postgres/seed_demo_data.sql
```

---

##  Expected Dashboard Display

After seeding, the dashboard should show:

### Stats:
- **Total Campaigns:** 8
- **Active Campaigns:** 6 (excluding paused and completed)
- **Total Screens:** 9
- **Online Screens:** 8 (one screen is offline)

### Analytics:
- **Total Impressions:** ~10,000+ (varies based on random generation)
- **Active Ads:** 3
- **Top Performing Ads:** List of ads with impression counts

### Campaigns Page:
- 8 campaigns with different statuses
- Various priorities and budgets
- Targeting rules visible

### Screens Page:
- 9 screens in different locations
- Display sizes and audio capabilities shown
- Online/offline status indicators

### Creatives Page:
- 11 creatives across campaigns
- Mix of images and videos
- Duration and share of voice displayed

---

##  Testing Scenarios

### Scenario 1: Campaign Performance
- View analytics for "Summer Sale 2024"
- See impression counts and performance

### Scenario 2: Screen Targeting
- Create campaign targeting "mall" venue type
- See which screens match

### Scenario 3: Geographic Analytics
- View analytics by city
- See performance across locations

### Scenario 4: Time-Based Targeting
- Test morning coffee campaign
- Verify time-based rules

---

##  Notes

1. **Media URLs:** Use placeholder URLs (`localhost:8080`). Replace with actual media URLs after uploading files.

2. **Event Generation:** Events are randomly generated. Counts will vary each time you run the script.

3. **Timestamps:** All timestamps are relative to `NOW()`, so they stay current.

4. **Foreign Keys:** All foreign key relationships are maintained (campaigns → creatives, screens → tags, etc.).

5. **Idempotency:** Script can be run multiple times safely. Existing data will be updated.

---

##  Customization

### Add More Screens:
```sql
INSERT INTO screens (id, name, city, ...) VALUES (...);
```

### Add More Campaigns:
```sql
INSERT INTO campaigns (id, name, ...) VALUES (...);
```

### Adjust Event Counts:
Modify the loop range in the DO block:
```sql
FOR event_counter IN 1..(100 + floor(random() * 400)::INTEGER) LOOP
-- Change 100 and 400 to adjust min/max events
```

---

**Last Updated:** December 2024

