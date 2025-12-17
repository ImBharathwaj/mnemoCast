-- Mnemocast Engine - PostgreSQL Database Schema
-- Run this script to initialize the database
-- This script is idempotent (safe to run multiple times on existing databases)

-- Create database (run as postgres user)
-- CREATE DATABASE mnemocast;
-- \c mnemocast;

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- ADS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS ads (
    id TEXT PRIMARY KEY,
    advertiser_id TEXT NOT NULL,
    creative_url TEXT NOT NULL,
    target_url TEXT,
    is_active BOOLEAN NOT NULL DEFAULT true,
    
    -- Budget fields
    max_plays INTEGER,
    daily_limit INTEGER,
    hourly_limit INTEGER,
    
    -- Frequency capping fields
    max_impressions_per_device INTEGER,
    max_impressions_per_user INTEGER,
    frequency_cap_window_hours INTEGER,
    
    -- OOH-specific fields
    duration_seconds INTEGER,
    
    -- Weight/priority for ad serving (higher weight = more likely to be selected)
    weight INTEGER NOT NULL DEFAULT 1,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for active ads (most common query)
CREATE INDEX IF NOT EXISTS idx_ads_is_active ON ads(is_active) WHERE is_active = true;

-- Index for advertiser
CREATE INDEX IF NOT EXISTS idx_ads_advertiser_id ON ads(advertiser_id);

-- ============================================
-- TARGETING RULES TABLE (for ads)
-- ============================================
CREATE TABLE IF NOT EXISTS targeting_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ad_id TEXT REFERENCES ads(id) ON DELETE CASCADE,
    campaign_id TEXT REFERENCES campaigns(id) ON DELETE CASCADE,
    rule_key TEXT NOT NULL,
    operator TEXT NOT NULL,
    rule_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((ad_id IS NOT NULL)::int + (campaign_id IS NOT NULL)::int = 1) -- Exactly one FK must be set
);

-- Index for ad_id (most common query)
CREATE INDEX IF NOT EXISTS idx_targeting_rules_ad_id ON targeting_rules(ad_id) WHERE ad_id IS NOT NULL;

-- Index for campaign_id
CREATE INDEX IF NOT EXISTS idx_targeting_rules_campaign_id ON targeting_rules(campaign_id) WHERE campaign_id IS NOT NULL;

-- ============================================
-- CAMPAIGNS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS campaigns (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    advertiser_id TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',  -- active, paused, completed
    start_date TIMESTAMPTZ NOT NULL,
    end_date TIMESTAMPTZ NOT NULL,
    total_budget BIGINT,
    target_playouts BIGINT,
    priority INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for campaigns
CREATE INDEX IF NOT EXISTS idx_campaigns_status ON campaigns(status);
CREATE INDEX IF NOT EXISTS idx_campaigns_dates ON campaigns(start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_campaigns_status_dates ON campaigns(status, start_date, end_date) WHERE status = 'active';

-- ============================================
-- CREATIVES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS creatives (
    id TEXT PRIMARY KEY,
    campaign_id TEXT NOT NULL REFERENCES campaigns(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    creative_type TEXT NOT NULL DEFAULT 'video',  -- video, image, html
    creative_url TEXT NOT NULL,
    target_url TEXT,
    duration_seconds INTEGER NOT NULL,
    status TEXT NOT NULL DEFAULT 'active',  -- active, paused, deleted
    share_of_voice NUMERIC(5,4),  -- 0.0 to 1.0
    frequency_cap_per_screen INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for creatives
CREATE INDEX IF NOT EXISTS idx_creatives_campaign_id ON creatives(campaign_id);
CREATE INDEX IF NOT EXISTS idx_creatives_status ON creatives(status) WHERE status = 'active';

-- ============================================
-- CREATIVE METADATA TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS creative_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creative_id TEXT NOT NULL REFERENCES creatives(id) ON DELETE CASCADE,
    metadata_key TEXT NOT NULL,
    metadata_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(creative_id, metadata_key)
);

-- Index for creative_id
CREATE INDEX IF NOT EXISTS idx_creative_metadata_creative_id ON creative_metadata(creative_id);

-- ============================================
-- SCREENS TABLE (OOH)
-- ============================================
CREATE TABLE IF NOT EXISTS screens (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    
    -- Location fields
    country TEXT,
    city TEXT,
    area TEXT,
    venue_type TEXT,
    timezone TEXT,
    
    -- Status fields
    is_online BOOLEAN NOT NULL DEFAULT false,
    last_seen TIMESTAMPTZ,
    
    -- Classification for pay-per-attention model (1-10, higher = premium)
    -- Higher classified screens favor ads with higher weights
    classification INTEGER NOT NULL DEFAULT 1,
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for common queries
CREATE INDEX IF NOT EXISTS idx_screens_city ON screens(city);
CREATE INDEX IF NOT EXISTS idx_screens_area ON screens(area);
CREATE INDEX IF NOT EXISTS idx_screens_venue_type ON screens(venue_type);
CREATE INDEX IF NOT EXISTS idx_screens_is_online ON screens(is_online) WHERE is_online = true;

-- ============================================
-- SCREEN TAGS TABLE (OOH)
-- ============================================
CREATE TABLE IF NOT EXISTS screen_tags (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_id TEXT NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    tag TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(screen_id, tag)
);

-- Index for tag queries
CREATE INDEX IF NOT EXISTS idx_screen_tags_screen_id ON screen_tags(screen_id);
CREATE INDEX IF NOT EXISTS idx_screen_tags_tag ON screen_tags(tag);

-- ============================================
-- SCREEN METADATA TABLE (OOH)
-- ============================================
CREATE TABLE IF NOT EXISTS screen_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    screen_id TEXT NOT NULL REFERENCES screens(id) ON DELETE CASCADE,
    metadata_key TEXT NOT NULL,
    metadata_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(screen_id, metadata_key)
);

-- Index for screen_id queries
CREATE INDEX IF NOT EXISTS idx_screen_metadata_screen_id ON screen_metadata(screen_id);

-- Trigger to auto-update updated_at for screens (safe creation)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_trigger WHERE tgname = 'update_screens_updated_at'
    ) THEN
        CREATE TRIGGER update_screens_updated_at BEFORE UPDATE ON screens
            FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
    END IF;
END $$;

-- ============================================
-- DELIVERY EVENTS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS delivery_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id TEXT NOT NULL UNIQUE,
    request_id TEXT NOT NULL,
    ad_id TEXT NOT NULL REFERENCES ads(id) ON DELETE SET NULL,
    event_type TEXT NOT NULL, -- 'impression' (click tracking removed for OOH)
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for common queries
CREATE INDEX IF NOT EXISTS idx_delivery_events_ad_id ON delivery_events(ad_id);
CREATE INDEX IF NOT EXISTS idx_delivery_events_event_type ON delivery_events(event_type);
CREATE INDEX IF NOT EXISTS idx_delivery_events_occurred_at ON delivery_events(occurred_at);
CREATE INDEX IF NOT EXISTS idx_delivery_events_ad_id_occurred_at ON delivery_events(ad_id, occurred_at);

-- Composite index for budget queries
CREATE INDEX IF NOT EXISTS idx_delivery_events_ad_type_time ON delivery_events(ad_id, event_type, occurred_at);

-- ============================================
-- EVENT METADATA TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS event_metadata (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    event_id UUID NOT NULL REFERENCES delivery_events(id) ON DELETE CASCADE,
    metadata_key TEXT NOT NULL,
    metadata_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(event_id, metadata_key)
);

-- Index for event_id
CREATE INDEX IF NOT EXISTS idx_event_metadata_event_id ON event_metadata(event_id);

-- Index for device/user queries (frequency capping)
CREATE INDEX IF NOT EXISTS idx_event_metadata_key_value ON event_metadata(metadata_key, metadata_value) 
    WHERE metadata_key IN ('deviceId', 'userId');

-- ============================================
-- FUNCTIONS
-- ============================================

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Triggers to auto-update updated_at (idempotent: drop if exists, then create)
DROP TRIGGER IF EXISTS update_ads_updated_at ON ads;
CREATE TRIGGER update_ads_updated_at BEFORE UPDATE ON ads
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_campaigns_updated_at ON campaigns;
CREATE TRIGGER update_campaigns_updated_at BEFORE UPDATE ON campaigns
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_creatives_updated_at ON creatives;
CREATE TRIGGER update_creatives_updated_at BEFORE UPDATE ON creatives
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- VIEWS (for analytics)
-- ============================================

-- View for ad performance summary (impressions only, no clicks for OOH)
CREATE OR REPLACE VIEW ad_performance AS
SELECT 
    ad_id,
    COUNT(*) FILTER (WHERE event_type = 'impression') as impressions
FROM delivery_events
GROUP BY ad_id;
