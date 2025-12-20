-- Mnemocast Engine - Demo Data Seed Script
-- This script populates all tables with realistic dummy data for dashboard showcase
-- Run after init.sql: psql -h localhost -U postgres -d mnemocast -f seed_demo_data.sql
-- This script is idempotent (safe to run multiple times)

-- Enable required extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Clear existing data (optional - comment out if you want to keep existing data)
-- TRUNCATE TABLE event_metadata CASCADE;
-- TRUNCATE TABLE delivery_events CASCADE;
-- TRUNCATE TABLE screen_metadata CASCADE;
-- TRUNCATE TABLE screen_tags CASCADE;
-- TRUNCATE TABLE screens CASCADE;
-- TRUNCATE TABLE creative_metadata CASCADE;
-- TRUNCATE TABLE creatives CASCADE;
-- TRUNCATE TABLE targeting_rules CASCADE;
-- TRUNCATE TABLE campaigns CASCADE;
-- TRUNCATE TABLE ads CASCADE;

-- ============================================
-- SCREENS (OOH Displays)
-- ============================================
-- Note: Passkeys are generated using encode(gen_random_bytes(32), 'base64')
-- For demo purposes, we use deterministic passkeys (you can change these to random ones)
INSERT INTO screens (id, name, country, city, area, venue_type, timezone, width, height, is_audible, is_online, last_seen, passkey, classification, created_at, updated_at)
VALUES 
    ('screen-chennai-airport-1', 'Chennai Airport Terminal 1 - Gate A1', 'IN', 'Chennai', 'Airport', 'airport', 'Asia/Kolkata', 3840, 2160, true, true, NOW(), encode(gen_random_bytes(32), 'base64'), 8, NOW() - INTERVAL '30 days', NOW()),
    ('screen-chennai-airport-2', 'Chennai Airport Terminal 2 - Food Court', 'IN', 'Chennai', 'Airport', 'airport', 'Asia/Kolkata', 1920, 1080, true, true, NOW() - INTERVAL '5 minutes', encode(gen_random_bytes(32), 'base64'), 7, NOW() - INTERVAL '25 days', NOW()),
    ('screen-mumbai-mall-1', 'Phoenix Mall Mumbai - Food Court', 'IN', 'Mumbai', 'Phoenix Mall', 'mall', 'Asia/Kolkata', 1920, 1080, false, true, NOW() - INTERVAL '2 minutes', encode(gen_random_bytes(32), 'base64'), 6, NOW() - INTERVAL '20 days', NOW()),
    ('screen-mumbai-mall-2', 'Phoenix Mall Mumbai - Entrance', 'IN', 'Mumbai', 'Phoenix Mall', 'mall', 'Asia/Kolkata', 2560, 1440, true, true, NOW() - INTERVAL '1 minute', encode(gen_random_bytes(32), 'base64'), 9, NOW() - INTERVAL '18 days', NOW()),
    ('screen-delhi-metro-1', 'Delhi Metro Station - Platform 1', 'IN', 'Delhi', 'Connaught Place', 'metro', 'Asia/Kolkata', 1920, 1080, false, true, NOW() - INTERVAL '3 minutes', encode(gen_random_bytes(32), 'base64'), 5, NOW() - INTERVAL '15 days', NOW()),
    ('screen-bangalore-office-1', 'IT Park Bangalore - Lobby', 'IN', 'Bangalore', 'Whitefield', 'office', 'Asia/Kolkata', 1920, 1080, false, true, NOW() - INTERVAL '10 minutes', encode(gen_random_bytes(32), 'base64'), 4, NOW() - INTERVAL '12 days', NOW()),
    ('screen-hyderabad-mall-1', 'Inorbit Mall Hyderabad - Cinema Hall', 'IN', 'Hyderabad', 'HITEC City', 'mall', 'Asia/Kolkata', 3840, 2160, true, true, NOW(), encode(gen_random_bytes(32), 'base64'), 7, NOW() - INTERVAL '10 days', NOW()),
    ('screen-pune-transit-1', 'Pune Bus Stand - Waiting Area', 'IN', 'Pune', 'Swargate', 'transit', 'Asia/Kolkata', 1920, 1080, false, false, NOW() - INTERVAL '2 hours', encode(gen_random_bytes(32), 'base64'), 3, NOW() - INTERVAL '8 days', NOW()),
    ('screen-kolkata-mall-1', 'South City Mall Kolkata - Food Court', 'IN', 'Kolkata', 'Alipore', 'mall', 'Asia/Kolkata', 1920, 1080, true, true, NOW() - INTERVAL '5 minutes', encode(gen_random_bytes(32), 'base64'), 6, NOW() - INTERVAL '5 days', NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    country = EXCLUDED.country,
    city = EXCLUDED.city,
    area = EXCLUDED.area,
    venue_type = EXCLUDED.venue_type,
    timezone = EXCLUDED.timezone,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    is_audible = EXCLUDED.is_audible,
    is_online = EXCLUDED.is_online,
    last_seen = EXCLUDED.last_seen,
    passkey = EXCLUDED.passkey,
    classification = EXCLUDED.classification,
    updated_at = NOW();

-- Screen Tags
INSERT INTO screen_tags (screen_id, tag)
VALUES 
    ('screen-chennai-airport-1', 'airport'),
    ('screen-chennai-airport-1', 'premium'),
    ('screen-chennai-airport-1', '4k'),
    ('screen-chennai-airport-2', 'airport'),
    ('screen-chennai-airport-2', 'food_court'),
    ('screen-mumbai-mall-1', 'mall'),
    ('screen-mumbai-mall-1', 'food_court'),
    ('screen-mumbai-mall-2', 'mall'),
    ('screen-mumbai-mall-2', 'entrance'),
    ('screen-mumbai-mall-2', 'premium'),
    ('screen-delhi-metro-1', 'metro'),
    ('screen-delhi-metro-1', 'transit'),
    ('screen-bangalore-office-1', 'office'),
    ('screen-bangalore-office-1', 'it_park'),
    ('screen-hyderabad-mall-1', 'mall'),
    ('screen-hyderabad-mall-1', 'cinema'),
    ('screen-hyderabad-mall-1', 'premium'),
    ('screen-pune-transit-1', 'transit'),
    ('screen-pune-transit-1', 'bus_station'),
    ('screen-kolkata-mall-1', 'mall'),
    ('screen-kolkata-mall-1', 'food_court')
ON CONFLICT (screen_id, tag) DO NOTHING;

-- Screen Metadata
INSERT INTO screen_metadata (screen_id, metadata_key, metadata_value)
VALUES 
    ('screen-chennai-airport-1', 'device_model', 'Samsung QLED 4K'),
    ('screen-chennai-airport-1', 'install_date', '2024-01-15'),
    ('screen-mumbai-mall-2', 'device_model', 'LG OLED 2K'),
    ('screen-mumbai-mall-2', 'install_date', '2024-02-01'),
    ('screen-hyderabad-mall-1', 'device_model', 'Sony 4K Cinema Display'),
    ('screen-hyderabad-mall-1', 'install_date', '2024-03-10')
ON CONFLICT (screen_id, metadata_key) DO UPDATE SET metadata_value = EXCLUDED.metadata_value;

-- ============================================
-- CAMPAIGNS
-- ============================================
INSERT INTO campaigns (id, name, advertiser_id, status, start_date, end_date, total_budget, target_playouts, priority, created_at, updated_at)
VALUES 
    ('campaign-summer-sale-2024', 'Summer Sale 2024', 'advertiser-nike', 'active', NOW() - INTERVAL '5 days', NOW() + INTERVAL '60 days', 5000000, 250000, 8, NOW() - INTERVAL '10 days', NOW()),
    ('campaign-morning-coffee', 'Morning Coffee Push', 'advertiser-starbucks', 'active', NOW() - INTERVAL '3 days', NOW() + INTERVAL '30 days', 2000000, 100000, 7, NOW() - INTERVAL '8 days', NOW()),
    ('campaign-premium-watch', 'Premium Watch Collection', 'advertiser-rolex', 'active', NOW() - INTERVAL '7 days', NOW() + INTERVAL '90 days', 10000000, 500000, 10, NOW() - INTERVAL '15 days', NOW()),
    ('campaign-weekend-shopping', 'Weekend Shopping Spree', 'advertiser-amazon', 'active', NOW() - INTERVAL '2 days', NOW() + INTERVAL '45 days', 3000000, 150000, 6, NOW() - INTERVAL '5 days', NOW()),
    ('campaign-tech-launch', 'Tech Product Launch', 'advertiser-apple', 'paused', NOW() - INTERVAL '10 days', NOW() + INTERVAL '20 days', 8000000, 400000, 9, NOW() - INTERVAL '12 days', NOW()),
    ('campaign-food-delivery', 'Food Delivery Service', 'advertiser-zomato', 'active', NOW() - INTERVAL '1 day', NOW() + INTERVAL '30 days', 1500000, 75000, 5, NOW() - INTERVAL '3 days', NOW()),
    ('campaign-fitness-center', 'Fitness Center Membership', 'advertiser-cultfit', 'active', NOW() - INTERVAL '4 days', NOW() + INTERVAL '60 days', 2500000, 125000, 7, NOW() - INTERVAL '7 days', NOW()),
    ('campaign-completed-test', 'Completed Test Campaign', 'advertiser-test', 'completed', NOW() - INTERVAL '90 days', NOW() - INTERVAL '30 days', 1000000, 50000, 5, NOW() - INTERVAL '95 days', NOW())
ON CONFLICT (id) DO UPDATE SET
    name = EXCLUDED.name,
    advertiser_id = EXCLUDED.advertiser_id,
    status = EXCLUDED.status,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    total_budget = EXCLUDED.total_budget,
    target_playouts = EXCLUDED.target_playouts,
    priority = EXCLUDED.priority,
    updated_at = NOW();

-- ============================================
-- CREATIVES
-- ============================================
INSERT INTO creatives (id, campaign_id, name, creative_type, creative_url, target_url, duration_seconds, status, share_of_voice, frequency_cap_per_screen, created_at, updated_at)
VALUES 
    -- Summer Sale Campaign Creatives
    ('creative-summer-sale-1', 'campaign-summer-sale-2024', 'Summer Sale Banner - 50% Off', 'image', 'http://localhost:9000/api/v1/media/creatives/summer-sale-banner.jpg', 'https://example.com/summer-sale', 10, 'active', 0.4, 5, NOW() - INTERVAL '10 days', NOW()),
    ('creative-summer-sale-2', 'campaign-summer-sale-2024', 'Summer Sale Video - Product Showcase', 'video', 'http://localhost:9000/api/v1/media/creatives/summer-sale-video.mp4', 'https://example.com/summer-sale', 30, 'active', 0.6, 3, NOW() - INTERVAL '10 days', NOW()),
    
    -- Morning Coffee Campaign Creatives
    ('creative-coffee-1', 'campaign-morning-coffee', 'Morning Coffee Banner', 'image', 'http://localhost:9000/api/v1/media/creatives/coffee-banner.jpg', 'https://example.com/coffee', 15, 'active', 1.0, 10, NOW() - INTERVAL '8 days', NOW()),
    
    -- Premium Watch Campaign Creatives
    ('creative-watch-1', 'campaign-premium-watch', 'Premium Watch Collection - Video', 'video', 'http://localhost:9000/api/v1/media/creatives/watch-video.mp4', 'https://example.com/watches', 45, 'active', 0.5, 2, NOW() - INTERVAL '15 days', NOW()),
    ('creative-watch-2', 'campaign-premium-watch', 'Premium Watch Collection - Image', 'image', 'http://localhost:9000/api/v1/media/creatives/watch-image.jpg', 'https://example.com/watches', 20, 'active', 0.5, 2, NOW() - INTERVAL '15 days', NOW()),
    
    -- Weekend Shopping Campaign Creatives
    ('creative-shopping-1', 'campaign-weekend-shopping', 'Weekend Shopping Banner', 'image', 'http://localhost:9000/api/v1/media/creatives/shopping-banner.jpg', 'https://example.com/shopping', 12, 'active', 0.7, 8, NOW() - INTERVAL '5 days', NOW()),
    ('creative-shopping-2', 'campaign-weekend-shopping', 'Weekend Shopping Video', 'video', 'http://localhost:9000/api/v1/media/creatives/shopping-video.mp4', 'https://example.com/shopping', 25, 'active', 0.3, 5, NOW() - INTERVAL '5 days', NOW()),
    
    -- Tech Launch Campaign Creatives (Paused)
    ('creative-tech-1', 'campaign-tech-launch', 'Tech Product Launch Video', 'video', 'http://localhost:9000/api/v1/media/creatives/tech-video.mp4', 'https://example.com/tech', 60, 'paused', 1.0, 1, NOW() - INTERVAL '12 days', NOW()),
    
    -- Food Delivery Campaign Creatives
    ('creative-food-1', 'campaign-food-delivery', 'Food Delivery App Banner', 'image', 'http://localhost:9000/api/v1/media/creatives/food-banner.jpg', 'https://example.com/food', 10, 'active', 1.0, 15, NOW() - INTERVAL '3 days', NOW()),
    
    -- Fitness Center Campaign Creatives
    ('creative-fitness-1', 'campaign-fitness-center', 'Fitness Center Video', 'video', 'http://localhost:9000/api/v1/media/creatives/fitness-video.mp4', 'https://example.com/fitness', 30, 'active', 0.6, 5, NOW() - INTERVAL '7 days', NOW()),
    ('creative-fitness-2', 'campaign-fitness-center', 'Fitness Center Banner', 'image', 'http://localhost:9000/api/v1/media/creatives/fitness-banner.jpg', 'https://example.com/fitness', 15, 'active', 0.4, 5, NOW() - INTERVAL '7 days', NOW())
ON CONFLICT (id) DO UPDATE SET
    campaign_id = EXCLUDED.campaign_id,
    name = EXCLUDED.name,
    creative_type = EXCLUDED.creative_type,
    creative_url = EXCLUDED.creative_url,
    target_url = EXCLUDED.target_url,
    duration_seconds = EXCLUDED.duration_seconds,
    status = EXCLUDED.status,
    share_of_voice = EXCLUDED.share_of_voice,
    frequency_cap_per_screen = EXCLUDED.frequency_cap_per_screen,
    updated_at = NOW();

-- Creative Metadata
INSERT INTO creative_metadata (creative_id, metadata_key, metadata_value)
VALUES 
    ('creative-summer-sale-1', 'aspect_ratio', '16:9'),
    ('creative-summer-sale-1', 'file_size_mb', '2.5'),
    ('creative-summer-sale-2', 'aspect_ratio', '16:9'),
    ('creative-summer-sale-2', 'file_size_mb', '15.8'),
    ('creative-watch-1', 'aspect_ratio', '21:9'),
    ('creative-watch-1', 'file_size_mb', '25.3'),
    ('creative-fitness-1', 'aspect_ratio', '16:9'),
    ('creative-fitness-1', 'file_size_mb', '12.1')
ON CONFLICT (creative_id, metadata_key) DO UPDATE SET metadata_value = EXCLUDED.metadata_value;

-- ============================================
-- TARGETING RULES (for Campaigns)
-- ============================================
INSERT INTO targeting_rules (campaign_id, rule_key, operator, rule_value)
VALUES 
    -- Summer Sale - Target malls in Chennai and Mumbai
    ('campaign-summer-sale-2024', 'city', 'in', 'Chennai,Mumbai'),
    ('campaign-summer-sale-2024', 'venueType', 'eq', 'mall'),
    
    -- Morning Coffee - Target airports and offices in morning hours
    ('campaign-morning-coffee', 'venueType', 'in', 'airport,office'),
    ('campaign-morning-coffee', 'timeBand', 'eq', '07:00-11:00'),
    
    -- Premium Watch - Target premium screens (high classification)
    ('campaign-premium-watch', 'classification', 'gte', '7'),
    ('campaign-premium-watch', 'city', 'in', 'Mumbai,Delhi,Bangalore'),
    
    -- Weekend Shopping - Target malls on weekends
    ('campaign-weekend-shopping', 'venueType', 'eq', 'mall'),
    ('campaign-weekend-shopping', 'dayOfWeek', 'in', '6,7'), -- Saturday, Sunday
    
    -- Food Delivery - Target all food courts
    ('campaign-food-delivery', 'tag', 'eq', 'food_court'),
    
    -- Fitness Center - Target malls and offices
    ('campaign-fitness-center', 'venueType', 'in', 'mall,office')
ON CONFLICT DO NOTHING;

-- ============================================
-- ADS (Legacy/Alternative Ad Model)
-- ============================================
-- Create ads that correspond to creatives for delivery events compatibility
INSERT INTO ads (id, advertiser_id, creative_url, target_url, is_active, max_plays, daily_limit, hourly_limit, duration_seconds, weight, created_at, updated_at)
VALUES 
    -- Summer Sale Campaign Ads
    ('ad-summer-sale-001', 'advertiser-nike', 'http://localhost:9000/api/v1/media/creatives/summer-sale-banner.jpg', 'https://example.com/summer-sale', true, 50000, 5000, 500, 10, 8, NOW() - INTERVAL '10 days', NOW()),
    ('ad-summer-sale-002', 'advertiser-nike', 'http://localhost:9000/api/v1/media/creatives/summer-sale-video.mp4', 'https://example.com/summer-sale', true, 50000, 5000, 500, 30, 8, NOW() - INTERVAL '10 days', NOW()),
    
    -- Morning Coffee Campaign Ads
    ('ad-coffee-001', 'advertiser-starbucks', 'http://localhost:9000/api/v1/media/creatives/coffee-banner.jpg', 'https://example.com/coffee', true, 30000, 3000, 300, 15, 7, NOW() - INTERVAL '8 days', NOW()),
    
    -- Premium Watch Campaign Ads
    ('ad-watch-001', 'advertiser-rolex', 'http://localhost:9000/api/v1/media/creatives/watch-video.mp4', 'https://example.com/watches', true, 20000, 2000, 200, 45, 10, NOW() - INTERVAL '15 days', NOW()),
    ('ad-watch-002', 'advertiser-rolex', 'http://localhost:9000/api/v1/media/creatives/watch-image.jpg', 'https://example.com/watches', true, 20000, 2000, 200, 20, 10, NOW() - INTERVAL '15 days', NOW()),
    
    -- Weekend Shopping Campaign Ads
    ('ad-shopping-001', 'advertiser-amazon', 'http://localhost:9000/api/v1/media/creatives/shopping-banner.jpg', 'https://example.com/shopping', true, 40000, 4000, 400, 12, 6, NOW() - INTERVAL '5 days', NOW()),
    ('ad-shopping-002', 'advertiser-amazon', 'http://localhost:9000/api/v1/media/creatives/shopping-video.mp4', 'https://example.com/shopping', true, 40000, 4000, 400, 25, 6, NOW() - INTERVAL '5 days', NOW()),
    
    -- Food Delivery Campaign Ads
    ('ad-food-001', 'advertiser-zomato', 'http://localhost:9000/api/v1/media/creatives/food-banner.jpg', 'https://example.com/food', true, 25000, 2500, 250, 10, 5, NOW() - INTERVAL '3 days', NOW()),
    
    -- Fitness Center Campaign Ads
    ('ad-fitness-001', 'advertiser-cultfit', 'http://localhost:9000/api/v1/media/creatives/fitness-video.mp4', 'https://example.com/fitness', true, 35000, 3500, 350, 30, 7, NOW() - INTERVAL '7 days', NOW()),
    ('ad-fitness-002', 'advertiser-cultfit', 'http://localhost:9000/api/v1/media/creatives/fitness-banner.jpg', 'https://example.com/fitness', true, 35000, 3500, 350, 15, 7, NOW() - INTERVAL '7 days', NOW()),
    
    -- Universal Fallback Ads (No targeting rules - match ALL screens automatically)
    ('ad-universal-001', 'advertiser-default', 'http://localhost:9000/api/v1/media/creatives/default-banner.jpg', 'https://example.com/default', true, 100000, 10000, 1000, 15, 5, NOW() - INTERVAL '1 day', NOW()),
    ('ad-universal-002', 'advertiser-default', 'http://localhost:9000/api/v1/media/creatives/default-video.mp4', 'https://example.com/default', true, 100000, 10000, 1000, 30, 5, NOW() - INTERVAL '1 day', NOW()),
    ('ad-universal-003', 'advertiser-default', 'http://localhost:9000/api/v1/media/creatives/default-image.jpg', 'https://example.com/default', true, 100000, 10000, 1000, 10, 5, NOW() - INTERVAL '1 day', NOW())
ON CONFLICT (id) DO UPDATE SET
    advertiser_id = EXCLUDED.advertiser_id,
    creative_url = EXCLUDED.creative_url,
    target_url = EXCLUDED.target_url,
    is_active = EXCLUDED.is_active,
    max_plays = EXCLUDED.max_plays,
    daily_limit = EXCLUDED.daily_limit,
    hourly_limit = EXCLUDED.hourly_limit,
    duration_seconds = EXCLUDED.duration_seconds,
    weight = EXCLUDED.weight,
    updated_at = NOW();

-- ============================================
-- AD TARGETING RULES
-- ============================================
-- Add targeting rules for ads so they can be matched to screens
INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
VALUES 
    -- Summer Sale Ads - Target malls in Chennai and Mumbai
    ('ad-summer-sale-001', 'city', 'in', 'Chennai,Mumbai'),
    ('ad-summer-sale-001', 'venueType', 'eq', 'mall'),
    ('ad-summer-sale-002', 'city', 'in', 'Chennai,Mumbai'),
    ('ad-summer-sale-002', 'venueType', 'eq', 'mall'),
    
    -- Morning Coffee Ads - Target airports and offices
    ('ad-coffee-001', 'venueType', 'in', 'airport,office'),
    
    -- Premium Watch Ads - Target premium screens (high classification) in major cities
    ('ad-watch-001', 'classification', 'gte', '7'),
    ('ad-watch-001', 'city', 'in', 'Mumbai,Delhi,Bangalore'),
    ('ad-watch-002', 'classification', 'gte', '7'),
    ('ad-watch-002', 'city', 'in', 'Mumbai,Delhi,Bangalore'),
    
    -- Weekend Shopping Ads - Target malls
    ('ad-shopping-001', 'venueType', 'eq', 'mall'),
    ('ad-shopping-002', 'venueType', 'eq', 'mall'),
    
    -- Food Delivery Ads - Target food courts (via tags)
    ('ad-food-001', 'tag', 'eq', 'food_court'),
    
    -- Fitness Center Ads - Target malls and offices
    ('ad-fitness-001', 'venueType', 'in', 'mall,office'),
    ('ad-fitness-002', 'venueType', 'in', 'mall,office')
ON CONFLICT DO NOTHING;

-- ============================================
-- DELIVERY EVENTS (Analytics Data)
-- ============================================
-- Generate impression events for the last 7 days
-- This creates realistic analytics data

-- Helper function to generate random timestamps in the last 7 days
DO $$
DECLARE
    event_counter INTEGER := 0;
    screen_record RECORD;
    ad_record RECORD;
    random_time TIMESTAMPTZ;
    event_uuid UUID;
BEGIN
    -- Generate events for each screen-ad combination
    FOR screen_record IN SELECT id FROM screens WHERE is_online = true LOOP
        FOR ad_record IN SELECT id FROM ads WHERE is_active = true LOOP
            -- Generate 100-500 events per screen-ad pair over last 7 days
            FOR event_counter IN 1..(100 + floor(random() * 400)::INTEGER) LOOP
                random_time := NOW() - (random() * INTERVAL '7 days');
                event_uuid := uuid_generate_v4();
                
                -- Insert event
                INSERT INTO delivery_events (id, event_id, request_id, ad_id, event_type, occurred_at, created_at)
                VALUES (
                    event_uuid,
                    'event-' || event_uuid::TEXT,
                    'req-' || uuid_generate_v4()::TEXT,
                    ad_record.id,
                    'impression',
                    random_time,
                    random_time
                )
                ON CONFLICT (event_id) DO NOTHING;
                
                -- Add some event metadata
                IF random() > 0.7 THEN -- 30% of events have metadata
                    INSERT INTO event_metadata (event_id, metadata_key, metadata_value)
                    VALUES (
                        event_uuid,
                        'screenId',
                        screen_record.id
                    )
                    ON CONFLICT (event_id, metadata_key) DO NOTHING;
                END IF;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- ============================================
-- SUMMARY STATISTICS
-- ============================================
DO $$
BEGIN
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Demo Data Seeded Successfully!';
    RAISE NOTICE '========================================';
    RAISE NOTICE 'Screens: %', (SELECT COUNT(*) FROM screens);
    RAISE NOTICE 'Campaigns: %', (SELECT COUNT(*) FROM campaigns);
    RAISE NOTICE 'Creatives: %', (SELECT COUNT(*) FROM creatives);
    RAISE NOTICE 'Events: %', (SELECT COUNT(*) FROM delivery_events);
    RAISE NOTICE '========================================';
END $$;

-- Display summary
SELECT 
    'Screens' as table_name,
    COUNT(*) as record_count
FROM screens
UNION ALL
SELECT 
    'Campaigns' as table_name,
    COUNT(*) as record_count
FROM campaigns
UNION ALL
SELECT 
    'Creatives' as table_name,
    COUNT(*) as record_count
FROM creatives
UNION ALL
SELECT 
    'Delivery Events' as table_name,
    COUNT(*) as record_count
FROM delivery_events
UNION ALL
SELECT 
    'Screen Tags' as table_name,
    COUNT(*) as record_count
FROM screen_tags
UNION ALL
SELECT 
    'Targeting Rules' as table_name,
    COUNT(*) as record_count
FROM targeting_rules
ORDER BY table_name;

