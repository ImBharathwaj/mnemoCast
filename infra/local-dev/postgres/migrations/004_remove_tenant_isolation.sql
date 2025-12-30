-- ============================================
-- Migration: Remove Tenant-Based Isolation (MVP Simplification)
-- Version: 1.0
-- Date: 2025-12-25
-- Description: Removes multi-tenancy support for MVP - focusing on single tenant
--              This simplifies the codebase and database schema for initial release
-- ============================================

-- ============================================
-- REMOVE TENANT_ID COLUMNS FROM ALL TABLES
-- ============================================

-- Drop indexes that reference tenant_id first
DROP INDEX IF EXISTS idx_users_tenant_id;
DROP INDEX IF EXISTS idx_users_tenant_role;
DROP INDEX IF EXISTS idx_screens_tenant_id;
DROP INDEX IF EXISTS idx_screens_tenant_id_passkey;
DROP INDEX IF EXISTS idx_screens_tenant_is_online;
DROP INDEX IF EXISTS idx_campaigns_tenant_id;
DROP INDEX IF EXISTS idx_campaigns_tenant_status_dates;
DROP INDEX IF EXISTS idx_campaigns_tenant_advertiser;
DROP INDEX IF EXISTS idx_ads_tenant_id;
DROP INDEX IF EXISTS idx_ads_tenant_is_active;
DROP INDEX IF EXISTS idx_ads_tenant_advertiser;
DROP INDEX IF EXISTS idx_creatives_tenant_id;
DROP INDEX IF EXISTS idx_creatives_tenant_campaign_id;
DROP INDEX IF EXISTS idx_creatives_tenant_status;
DROP INDEX IF EXISTS idx_delivery_events_tenant_ad_id;
DROP INDEX IF EXISTS idx_delivery_events_tenant_device_id;
DROP INDEX IF EXISTS idx_delivery_events_tenant_user_id;

-- Remove tenant_id columns from all tables
ALTER TABLE users DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE users DROP COLUMN IF EXISTS is_super_admin;
ALTER TABLE screens DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE campaigns DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE ads DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE creatives DROP COLUMN IF EXISTS tenant_id;
ALTER TABLE delivery_events DROP COLUMN IF EXISTS tenant_id;

-- Drop tenants table and related views
DROP VIEW IF EXISTS tenant_ad_performance;
DROP VIEW IF EXISTS tenant_campaign_performance;
DROP TABLE IF EXISTS tenants CASCADE;

-- Note: This migration removes all tenant-related functionality.
-- For MVP, the system operates as a single-tenant application.
-- Multi-tenancy can be re-added later if needed.


