-- Seed Sample Campaigns for Mnemocast Engine
-- Run this after init.sql to populate the database with sample data
-- This script is idempotent (safe to run multiple times)

-- Insert sample campaigns (only if they don't exist)
INSERT INTO campaigns (id, name, advertiser_id, status, start_date, end_date, total_budget, target_playouts, priority, created_at, updated_at)
VALUES 
    (
        'campaign-morning-rush',
        'Morning Rush Hour',
        'advertiser-001',
        'active',
        NOW() - INTERVAL '1 day',
        NOW() + INTERVAL '30 days',
        1000000,
        50000,
        80,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO campaigns (id, name, advertiser_id, status, start_date, end_date, total_budget, target_playouts, priority, created_at, updated_at)
VALUES 
    (
        'campaign-weekend-shopping',
        'Weekend Shopping Campaign',
        'advertiser-002',
        'active',
        NOW() - INTERVAL '2 days',
        NOW() + INTERVAL '60 days',
        2000000,
        100000,
        90,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO campaigns (id, name, advertiser_id, status, start_date, end_date, total_budget, target_playouts, priority, created_at, updated_at)
VALUES 
    (
        'campaign-premium-brand',
        'Premium Brand Awareness',
        'advertiser-003',
        'active',
        NOW() - INTERVAL '7 days',
        NOW() + INTERVAL '90 days',
        5000000,
        250000,
        95,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

INSERT INTO campaigns (id, name, advertiser_id, status, start_date, end_date, total_budget, target_playouts, priority, created_at, updated_at)
VALUES 
    (
        'campaign-evening-entertainment',
        'Evening Entertainment',
        'advertiser-001',
        'active',
        NOW(),
        NOW() + INTERVAL '45 days',
        1500000,
        75000,
        75,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

-- Verify inserted campaigns
SELECT id, name, status, advertiser_id, start_date, end_date FROM campaigns ORDER BY created_at;

