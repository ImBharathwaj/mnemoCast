-- Insert an ad that will be fetched for Forum Mall Food Court Screen 1
-- Screen Details:
--   Screen ID: d31f2fe7-16f3-4842-8db7-4b67868ecdc6
--   Country: India
--   City: Chennai
--   Area: Vadapalani
--   Venue Type: Mall
--   Classification: 6

-- Insert the ad
INSERT INTO ads (
    id,
    advertiser_id,
    creative_url,
    target_url,
    is_active,
    max_plays,
    daily_limit,
    hourly_limit,
    max_impressions_per_device,
    max_impressions_per_user,
    frequency_cap_window_hours,
    duration_seconds,
    weight,
    created_at,
    updated_at
) VALUES (
    'ad-forum-mall-food-court-001',  -- Ad ID
    'advertiser-food-beverage-001',  -- Advertiser ID
    'https://example.com/creatives/forum-mall-food-ad.mp4',  -- Creative URL (placeholder)
    'https://example.com/landing/forum-mall-promo',  -- Target URL (optional)
    true,  -- is_active
    10000,  -- max_plays: Total 10,000 impressions
    500,    -- daily_limit: 500 impressions per day
    50,     -- hourly_limit: 50 impressions per hour
    10,     -- max_impressions_per_device: 10 per device in window
    20,     -- max_impressions_per_user: 20 per user in window
    24,     -- frequency_cap_window_hours: 24 hour window
    30,     -- duration_seconds: 30 second ad
    5,      -- weight: Higher weight for better selection probability
    NOW(),  -- created_at
    NOW()   -- updated_at
)
ON CONFLICT (id) DO UPDATE SET
    advertiser_id = EXCLUDED.advertiser_id,
    creative_url = EXCLUDED.creative_url,
    target_url = EXCLUDED.target_url,
    is_active = EXCLUDED.is_active,
    max_plays = EXCLUDED.max_plays,
    daily_limit = EXCLUDED.daily_limit,
    hourly_limit = EXCLUDED.hourly_limit,
    max_impressions_per_device = EXCLUDED.max_impressions_per_device,
    max_impressions_per_user = EXCLUDED.max_impressions_per_user,
    frequency_cap_window_hours = EXCLUDED.frequency_cap_window_hours,
    duration_seconds = EXCLUDED.duration_seconds,
    weight = EXCLUDED.weight,
    updated_at = EXCLUDED.updated_at;

-- Delete existing targeting rules for this ad (if re-running)
DELETE FROM targeting_rules WHERE ad_id = 'ad-forum-mall-food-court-001';

-- Insert targeting rules to match the screen
-- Rule 1: Country must be India
INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
VALUES ('ad-forum-mall-food-court-001', 'country', 'eq', 'India');

-- Rule 2: City must be Chennai
INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
VALUES ('ad-forum-mall-food-court-001', 'city', 'eq', 'Chennai');

-- Rule 3: Area must be Vadapalani (optional, but specific targeting)
INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
VALUES ('ad-forum-mall-food-court-001', 'area', 'eq', 'Vadapalani');

-- Rule 4: Venue type must be Mall
INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
VALUES ('ad-forum-mall-food-court-001', 'venueType', 'eq', 'Mall');

-- Optional: Add screen classification targeting (targeting premium screens)
-- This ad targets screens with classification >= 5 (premium screens)
INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
VALUES ('ad-forum-mall-food-court-001', 'classification', 'gte', '5');

-- Verify the ad was inserted correctly
SELECT 
    a.id,
    a.advertiser_id,
    a.creative_url,
    a.is_active,
    a.weight,
    COUNT(tr.rule_key) as targeting_rule_count
FROM ads a
LEFT JOIN targeting_rules tr ON a.id = tr.ad_id
WHERE a.id = 'ad-forum-mall-food-court-001'
GROUP BY a.id, a.advertiser_id, a.creative_url, a.is_active, a.weight;

-- Show all targeting rules for this ad
SELECT 
    rule_key,
    operator,
    rule_value
FROM targeting_rules
WHERE ad_id = 'ad-forum-mall-food-court-001'
ORDER BY rule_key;

