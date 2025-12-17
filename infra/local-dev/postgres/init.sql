-- Mnemocast Engine - PostgreSQL Database Schema
-- Run this script to initialize the database

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
    
    -- Timestamps
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for active ads (most common query)
CREATE INDEX IF NOT EXISTS idx_ads_is_active ON ads(is_active) WHERE is_active = true;

-- Index for advertiser
CREATE INDEX IF NOT EXISTS idx_ads_advertiser_id ON ads(advertiser_id);

-- ============================================
-- TARGETING RULES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS targeting_rules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    ad_id TEXT NOT NULL REFERENCES ads(id) ON DELETE CASCADE,
    rule_key TEXT NOT NULL,
    operator TEXT NOT NULL,
    rule_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Index for ad_id (most common query)
CREATE INDEX IF NOT EXISTS idx_targeting_rules_ad_id ON targeting_rules(ad_id);

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

-- Trigger to auto-update updated_at
CREATE TRIGGER update_ads_updated_at BEFORE UPDATE ON ads
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
