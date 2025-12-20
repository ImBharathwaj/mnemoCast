# 🔍 Why No Ads Are Being Delivered - Diagnostic Guide

## Overview

Ads go through **3 filtering stages** before being delivered to a screen:

1. **Targeting Filter** - Ads must match the screen's location/context
2. **Budget Filter** - Ads must have budget remaining
3. **Frequency Cap Filter** - Ads must not exceed impression limits

If **any** filter removes all ads, the screen receives an empty response.

---

## 📊 Enhanced Logging

The system now logs detailed information at each filtering stage. When you request ads, check the server logs for:

```
[AdDelivery] ========== AD DELIVERY REQUEST ==========
[AdDelivery] Request ID: ...
[AdDelivery] Screen ID: ...
[AdDelivery] Screen Classification: ...
[AdDelivery] Request Context: country=..., city=..., area=..., venueType=..., tags=[...]
[AdDelivery] Total active ads in store: X
[AdDelivery] After targeting filter: X/Y ads eligible
[AdDelivery] After budget filter: X/Y ads eligible
[AdDelivery] After frequency cap filter: X/Y ads eligible
```

---

## 🔍 Common Reasons for Empty Ads

### 1. **No Active Ads in Database**

**Symptom:** Log shows `Total active ads in store: 0`

**Check:**
```sql
-- Check if ads exist
SELECT COUNT(*) FROM ads WHERE is_active = true;

-- List all active ads
SELECT id, advertiser_id, is_active, max_plays, daily_limit, hourly_limit 
FROM ads 
WHERE is_active = true;
```

**Solution:**
- Ensure ads are created and marked as `is_active = true`
- Run the seed script: `psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/seed_demo_data.sql`

---

### 2. **Ads Don't Match Targeting Rules** ⚠️ **MOST COMMON**

**Symptom:** Log shows `After targeting filter: 0/X ads eligible`

**Why This Happens:**
- Ads have targeting rules that don't match the screen's context
- Screen location (city, area, venueType) doesn't match ad targeting
- Screen tags don't match ad targeting rules
- Screen classification doesn't meet ad requirements

**Check Logs:**
The system now logs why each ad was rejected:
```
[AdDelivery] ❌ Ad 'ad-123' rejected by targeting:
[AdDelivery]   → Rule: city eq Chennai
[AdDelivery]   → Request value: Mumbai
[AdDelivery]   → Rule result: ✗ FAIL
```

**Check Database:**
```sql
-- Check ad targeting rules
SELECT 
    a.id as ad_id,
    a.advertiser_id,
    tr.key,
    tr.operator,
    tr.value
FROM ads a
LEFT JOIN targeting_rules tr ON tr.ad_id = a.id
WHERE a.is_active = true
ORDER BY a.id;

-- Check screen details
SELECT id, name, country, city, area, venue_type, classification
FROM screens
WHERE id = 'your-screen-id';
```

**Solution:**
- **Option 1:** Create ads with no targeting rules (fallback ads) - these match ALL screens
- **Option 2:** Update ad targeting rules to match your screen's location
- **Option 3:** Update screen location to match ad targeting rules

**Example - Create Fallback Ad:**
```sql
INSERT INTO ads (id, advertiser_id, creative_url, target_url, is_active, duration_seconds, weight, created_at, updated_at)
VALUES (
    'ad-fallback-001',
    'advertiser-default',
    'http://localhost:9000/api/v1/media/creatives/default.jpg',
    'https://example.com',
    true,
    30,
    1,
    NOW(),
    NOW()
);
-- No targeting rules = matches all screens
```

---

### 3. **Ads Are Out of Budget**

**Symptom:** Log shows `After budget filter: 0/X ads eligible`

**Why This Happens:**
- `max_plays` limit reached
- `daily_limit` limit reached
- `hourly_limit` limit reached

**Check Database:**
```sql
-- Check ad budgets
SELECT 
    id,
    max_plays,
    daily_limit,
    hourly_limit
FROM ads
WHERE is_active = true;

-- Check impression counts (if tracking is implemented)
SELECT 
    ad_id,
    COUNT(*) as total_impressions
FROM delivery_events
WHERE event_type = 'impression'
GROUP BY ad_id;
```

**Solution:**
- Increase budget limits
- Reset budget counters (if applicable)
- Create new ads with higher budgets

**Example - Increase Budget:**
```sql
UPDATE ads
SET max_plays = 100000,
    daily_limit = 10000,
    hourly_limit = 1000
WHERE id = 'ad-123';
```

---

### 4. **Ads Blocked by Frequency Capping**

**Symptom:** Log shows `After frequency cap filter: 0/X ads eligible`

**Why This Happens:**
- Ad has `max_impressions_per_device` limit
- Screen has already seen this ad too many times
- Frequency cap window hasn't expired

**Check Database:**
```sql
-- Check frequency cap settings
SELECT 
    id,
    max_impressions_per_device,
    max_impressions_per_user,
    frequency_cap_window_hours
FROM ads
WHERE is_active = true;

-- Check recent impressions for this screen
SELECT 
    ad_id,
    COUNT(*) as impressions,
    MAX(occurred_at) as last_impression
FROM delivery_events
WHERE event_type = 'impression'
    AND metadata->>'deviceId' = 'your-screen-id'
GROUP BY ad_id;
```

**Solution:**
- Increase frequency cap limits
- Remove frequency cap restrictions
- Wait for frequency cap window to expire

**Example - Remove Frequency Cap:**
```sql
UPDATE ads
SET max_impressions_per_device = NULL,
    frequency_cap_window_hours = NULL
WHERE id = 'ad-123';
```

---

## 🛠️ Diagnostic Steps

### Step 1: Check Server Logs

When a screen requests ads, look for the detailed log output:

```
[AdDelivery] ========== AD DELIVERY REQUEST ==========
[AdDelivery] Total active ads in store: X
[AdDelivery] After targeting filter: X/Y ads eligible
[AdDelivery] After budget filter: X/Y ads eligible
[AdDelivery] After frequency cap filter: X/Y ads eligible
```

### Step 2: Identify the Filtering Stage

- **If targeting filter = 0:** Ads don't match screen context
- **If budget filter = 0:** Ads are out of budget
- **If frequency cap filter = 0:** Ads are blocked by frequency caps

### Step 3: Check Specific Ad Rejection Reasons

For targeting rejections, the logs now show:
```
[AdDelivery] ❌ Ad 'ad-123' rejected by targeting:
[AdDelivery]   → Rule: city eq Chennai
[AdDelivery]   → Request value: Mumbai
[AdDelivery]   → Rule result: ✗ FAIL
```

### Step 4: Verify Screen Context

Check what context the screen is sending:
```
[AdDelivery] Request Context: country=IN, city=Chennai, area=Airport, venueType=airport, tags=[...]
```

### Step 5: Verify Ad Targeting Rules

Check what targeting rules ads have:
```sql
SELECT 
    a.id,
    tr.key,
    tr.operator,
    tr.value
FROM ads a
LEFT JOIN targeting_rules tr ON tr.ad_id = a.id
WHERE a.is_active = true;
```

---

## ✅ Quick Fixes

### Fix 1: Create Universal Fallback Ads

Create ads with **no targeting rules** - these match ALL screens:

```sql
INSERT INTO ads (id, advertiser_id, creative_url, target_url, is_active, duration_seconds, weight, created_at, updated_at)
VALUES
    ('ad-universal-001', 'advertiser-default', 'http://localhost:9000/api/v1/media/creatives/default-1.jpg', 'https://example.com', true, 30, 1, NOW(), NOW()),
    ('ad-universal-002', 'advertiser-default', 'http://localhost:9000/api/v1/media/creatives/default-2.jpg', 'https://example.com', true, 30, 1, NOW(), NOW()),
    ('ad-universal-003', 'advertiser-default', 'http://localhost:9000/api/v1/media/creatives/default-3.jpg', 'https://example.com', true, 30, 1, NOW(), NOW());
-- No targeting rules = matches all screens automatically
```

### Fix 2: Remove All Targeting Rules

Make existing ads universal by removing their targeting rules:

```sql
DELETE FROM targeting_rules WHERE ad_id IN (
    SELECT id FROM ads WHERE is_active = true
);
```

### Fix 3: Update Screen Location

Update screen location to match ad targeting:

```sql
UPDATE screens
SET city = 'Chennai',
    area = 'Airport',
    venue_type = 'airport'
WHERE id = 'your-screen-id';
```

### Fix 4: Update Ad Targeting

Update ad targeting to match screen location:

```sql
UPDATE targeting_rules
SET value = 'Mumbai'
WHERE ad_id = 'ad-123' AND key = 'city';
```

---

## 📋 Checklist

When ads aren't being delivered, check:

- [ ] Are there active ads in the database? (`SELECT COUNT(*) FROM ads WHERE is_active = true`)
- [ ] Do ads have targeting rules? (`SELECT * FROM targeting_rules`)
- [ ] Do targeting rules match screen context? (Check logs for rejection reasons)
- [ ] Do ads have budget remaining? (Check `max_plays`, `daily_limit`, `hourly_limit`)
- [ ] Are ads blocked by frequency caps? (Check `max_impressions_per_device`)
- [ ] Are there fallback ads with no targeting rules? (These should always match)

---

## 🎯 Best Practices

1. **Always Create Fallback Ads**
   - Create at least 3-5 ads with no targeting rules
   - These ensure screens always receive content

2. **Use Targeting Rules Wisely**
   - Start with broad targeting (e.g., country-level)
   - Gradually narrow down (city → area → venue type)
   - Test targeting rules before deploying

3. **Set Realistic Budgets**
   - Set high `max_plays` limits for testing
   - Monitor budget usage
   - Adjust limits based on actual usage

4. **Monitor Frequency Caps**
   - Set reasonable frequency cap limits
   - Consider screen-specific caps vs. global caps
   - Test frequency cap behavior

---

## 📝 Summary

**Most Common Issue:** Ads don't match targeting rules

**Quick Fix:** Create fallback ads with no targeting rules

**Best Practice:** Always have fallback ads to ensure screens receive content

**Debugging:** Check server logs for detailed filtering information

---

**Status:** ✅ Enhanced logging added  
**Date:** 2025-12-19


