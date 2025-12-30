-- ============================================
-- Migration: Add Tenant-Based Isolation
-- Version: 1.0
-- Date: 2025-12-23
-- Description: Adds multi-tenancy support to MnemoCast platform
--              Enables agencies/tenants to self-manage their campaigns, ads, and users
-- ============================================

-- Enable UUID extension if not already enabled
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================
-- TENANTS TABLE
-- ============================================
-- Represents agencies/organizations that use the platform
-- Each tenant operates in complete isolation
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,  -- URL-friendly identifier (e.g., "acme-corp")
    domain TEXT,                -- Optional custom domain (e.g., "acme.mnemocast.com")
    status TEXT NOT NULL DEFAULT 'active',  -- active, suspended, deleted
    settings JSONB DEFAULT '{}',  -- Tenant-specific configuration
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Indexes for tenants
CREATE INDEX IF NOT EXISTS idx_tenants_slug ON tenants(slug);
CREATE INDEX IF NOT EXISTS idx_tenants_status ON tenants(status) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_tenants_domain ON tenants(domain) WHERE domain IS NOT NULL;

-- ============================================
-- UPDATE USERS TABLE
-- ============================================
-- Add tenant association and role hierarchy
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_super_admin BOOLEAN NOT NULL DEFAULT false;

-- Super admins don't need tenant_id (can be NULL)
-- Regular users (tenant_admin, tenant_user) must have tenant_id
-- Update role enum to support: super_admin, tenant_admin, tenant_user, advertiser
-- Note: PostgreSQL doesn't have native enums in this setup, so we use TEXT with constraints

-- Indexes for users
CREATE INDEX IF NOT EXISTS idx_users_tenant_id ON users(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_is_super_admin ON users(is_super_admin) WHERE is_super_admin = true;
CREATE INDEX IF NOT EXISTS idx_users_tenant_role ON users(tenant_id, role) WHERE tenant_id IS NOT NULL;

-- ============================================
-- UPDATE SCREENS TABLE
-- ============================================
-- Screens belong to a tenant (agency)
ALTER TABLE screens ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;

-- Make tenant_id required for screens (after migration)
-- For now, allow NULL during migration, then update and set NOT NULL

-- Indexes for screens
CREATE INDEX IF NOT EXISTS idx_screens_tenant_id ON screens(tenant_id);
CREATE INDEX IF NOT EXISTS idx_screens_tenant_id_passkey ON screens(tenant_id, passkey);
CREATE INDEX IF NOT EXISTS idx_screens_tenant_is_online ON screens(tenant_id, is_online) WHERE is_online = true;

-- ============================================
-- UPDATE CAMPAIGNS TABLE
-- ============================================
-- Campaigns belong to a tenant (agency)
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;

-- Indexes for campaigns
CREATE INDEX IF NOT EXISTS idx_campaigns_tenant_id ON campaigns(tenant_id);
CREATE INDEX IF NOT EXISTS idx_campaigns_tenant_status_dates ON campaigns(tenant_id, status, start_date, end_date) WHERE status = 'active';
CREATE INDEX IF NOT EXISTS idx_campaigns_tenant_advertiser ON campaigns(tenant_id, advertiser_id);

-- ============================================
-- UPDATE ADS TABLE
-- ============================================
-- Ads belong to a tenant (agency)
ALTER TABLE ads ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;

-- Indexes for ads
CREATE INDEX IF NOT EXISTS idx_ads_tenant_id ON ads(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ads_tenant_is_active ON ads(tenant_id, is_active) WHERE is_active = true;
CREATE INDEX IF NOT EXISTS idx_ads_tenant_advertiser ON ads(tenant_id, advertiser_id);

-- ============================================
-- UPDATE CREATIVES TABLE
-- ============================================
-- Creatives belong to a tenant (agency)
ALTER TABLE creatives ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;

-- Indexes for creatives
CREATE INDEX IF NOT EXISTS idx_creatives_tenant_id ON creatives(tenant_id);
CREATE INDEX IF NOT EXISTS idx_creatives_tenant_campaign_id ON creatives(tenant_id, campaign_id);
CREATE INDEX IF NOT EXISTS idx_creatives_tenant_status ON creatives(tenant_id, status) WHERE status = 'active';

-- ============================================
-- UPDATE DELIVERY EVENTS TABLE
-- ============================================
-- Events belong to a tenant (for analytics isolation)
ALTER TABLE delivery_events ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;

-- Indexes for delivery_events
CREATE INDEX IF NOT EXISTS idx_delivery_events_tenant_id ON delivery_events(tenant_id);
CREATE INDEX IF NOT EXISTS idx_delivery_events_tenant_ad_id ON delivery_events(tenant_id, ad_id);
CREATE INDEX IF NOT EXISTS idx_delivery_events_tenant_occurred_at ON delivery_events(tenant_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_delivery_events_tenant_type_time ON delivery_events(tenant_id, event_type, occurred_at);

-- ============================================
-- USER-TENANT ROLES TABLE (Many-to-Many)
-- ============================================
-- For future: users who belong to multiple tenants with different roles
-- For MVP, we use tenant_id in users table (single tenant per user)
CREATE TABLE IF NOT EXISTS user_tenant_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'tenant_user',  -- tenant_admin, tenant_user, advertiser
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, tenant_id)
);

-- Indexes for user_tenant_roles
CREATE INDEX IF NOT EXISTS idx_user_tenant_roles_user_id ON user_tenant_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_tenant_roles_tenant_id ON user_tenant_roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_user_tenant_roles_role ON user_tenant_roles(tenant_id, role);

-- Trigger to auto-update updated_at for user_tenant_roles
DROP TRIGGER IF EXISTS update_user_tenant_roles_updated_at ON user_tenant_roles;
CREATE TRIGGER update_user_tenant_roles_updated_at BEFORE UPDATE ON user_tenant_roles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- TENANT SETTINGS TABLE (Optional - for tenant-specific configuration)
-- ============================================
CREATE TABLE IF NOT EXISTS tenant_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    setting_key TEXT NOT NULL,
    setting_value TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(tenant_id, setting_key)
);

-- Indexes for tenant_settings
CREATE INDEX IF NOT EXISTS idx_tenant_settings_tenant_id ON tenant_settings(tenant_id);

-- Trigger to auto-update updated_at for tenant_settings
DROP TRIGGER IF EXISTS update_tenant_settings_updated_at ON tenant_settings;
CREATE TRIGGER update_tenant_settings_updated_at BEFORE UPDATE ON tenant_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- TRIGGER TO AUTO-UPDATE TENANTS UPDATED_AT
-- ============================================
DROP TRIGGER IF EXISTS update_tenants_updated_at ON tenants;
CREATE TRIGGER update_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================
-- CREATE DEFAULT TENANT FOR EXISTING DATA
-- ============================================
-- This allows migration of existing data to a default tenant
INSERT INTO tenants (id, name, slug, status, settings)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'Default Tenant',
    'default',
    'active',
    '{}'::jsonb
)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- MIGRATE EXISTING DATA TO DEFAULT TENANT
-- ============================================
-- Update all existing entities to belong to default tenant
-- This ensures backward compatibility during migration

UPDATE users 
SET tenant_id = '00000000-0000-0000-0000-000000000000' 
WHERE tenant_id IS NULL AND is_super_admin = false;

UPDATE screens 
SET tenant_id = '00000000-0000-0000-0000-000000000000' 
WHERE tenant_id IS NULL;

UPDATE campaigns 
SET tenant_id = '00000000-0000-0000-0000-000000000000' 
WHERE tenant_id IS NULL;

UPDATE ads 
SET tenant_id = '00000000-0000-0000-0000-000000000000' 
WHERE tenant_id IS NULL;

UPDATE creatives 
SET tenant_id = '00000000-0000-0000-0000-000000000000' 
WHERE tenant_id IS NULL;

UPDATE delivery_events 
SET tenant_id = '00000000-0000-0000-0000-000000000000' 
WHERE tenant_id IS NULL;

-- ============================================
-- ADD NOT NULL CONSTRAINTS (After Migration)
-- ============================================
-- Once all data is migrated, we can add NOT NULL constraints
-- Commented out for now - uncomment after verifying migration

-- ALTER TABLE screens ALTER COLUMN tenant_id SET NOT NULL;
-- ALTER TABLE campaigns ALTER COLUMN tenant_id SET NOT NULL;
-- ALTER TABLE ads ALTER COLUMN tenant_id SET NOT NULL;
-- ALTER TABLE creatives ALTER COLUMN tenant_id SET NOT NULL;
-- ALTER TABLE delivery_events ALTER COLUMN tenant_id SET NOT NULL;

-- ============================================
-- ROLE VALIDATION FUNCTION
-- ============================================
-- Helper function to validate user roles
CREATE OR REPLACE FUNCTION validate_user_role(role_text TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN role_text IN ('super_admin', 'tenant_admin', 'tenant_user', 'advertiser', 'admin', 'user');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ============================================
-- TENANT STATUS VALIDATION FUNCTION
-- ============================================
-- Helper function to validate tenant status
CREATE OR REPLACE FUNCTION validate_tenant_status(status_text TEXT)
RETURNS BOOLEAN AS $$
BEGIN
    RETURN status_text IN ('active', 'suspended', 'deleted');
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- ============================================
-- VIEWS FOR TENANT ANALYTICS
-- ============================================

-- View for tenant ad performance
CREATE OR REPLACE VIEW tenant_ad_performance AS
SELECT 
    t.id as tenant_id,
    t.name as tenant_name,
    ad.id as ad_id,
    ad.advertiser_id,
    COUNT(de.id) FILTER (WHERE de.event_type = 'impression') as impressions,
    COUNT(DISTINCT de.id) as total_events
FROM tenants t
LEFT JOIN ads ad ON ad.tenant_id = t.id
LEFT JOIN delivery_events de ON de.ad_id = ad.id AND de.tenant_id = t.id
WHERE t.status = 'active'
GROUP BY t.id, t.name, ad.id, ad.advertiser_id;

-- View for tenant campaign performance
CREATE OR REPLACE VIEW tenant_campaign_performance AS
SELECT 
    t.id as tenant_id,
    t.name as tenant_name,
    c.id as campaign_id,
    c.name as campaign_name,
    COUNT(DISTINCT ad.id) as total_ads,
    COUNT(de.id) FILTER (WHERE de.event_type = 'impression') as total_impressions
FROM tenants t
LEFT JOIN campaigns c ON c.tenant_id = t.id
LEFT JOIN ads ad ON ad.advertiser_id = c.advertiser_id AND ad.tenant_id = t.id
LEFT JOIN delivery_events de ON de.ad_id = ad.id AND de.tenant_id = t.id
WHERE t.status = 'active' AND c.status = 'active'
GROUP BY t.id, t.name, c.id, c.name;

-- View for tenant screen statistics
CREATE OR REPLACE VIEW tenant_screen_stats AS
SELECT 
    t.id as tenant_id,
    t.name as tenant_name,
    COUNT(s.id) as total_screens,
    COUNT(s.id) FILTER (WHERE s.is_online = true) as online_screens,
    COUNT(s.id) FILTER (WHERE s.is_online = false) as offline_screens
FROM tenants t
LEFT JOIN screens s ON s.tenant_id = t.id
WHERE t.status = 'active'
GROUP BY t.id, t.name;

-- ============================================
-- COMMENTS FOR DOCUMENTATION
-- ============================================

COMMENT ON TABLE tenants IS 'Agencies/organizations using the platform. Each tenant operates in complete isolation.';
COMMENT ON COLUMN tenants.slug IS 'URL-friendly identifier for the tenant (e.g., "acme-corp")';
COMMENT ON COLUMN tenants.domain IS 'Optional custom domain for the tenant (e.g., "acme.mnemocast.com")';
COMMENT ON COLUMN tenants.status IS 'Tenant status: active (operational), suspended (temporarily disabled), deleted (soft delete)';
COMMENT ON COLUMN tenants.settings IS 'JSONB field for tenant-specific configuration (feature flags, limits, etc.)';

COMMENT ON COLUMN users.tenant_id IS 'Tenant this user belongs to. NULL for super admins. Required for tenant_admin and tenant_user.';
COMMENT ON COLUMN users.is_super_admin IS 'True if user is a system-level super admin with access to all tenants';
COMMENT ON COLUMN users.role IS 'User role: super_admin (system), tenant_admin (agency admin), tenant_user (regular user), advertiser';

COMMENT ON COLUMN screens.tenant_id IS 'Tenant (agency) that owns this screen. Required.';
COMMENT ON COLUMN campaigns.tenant_id IS 'Tenant (agency) that owns this campaign. Required.';
COMMENT ON COLUMN ads.tenant_id IS 'Tenant (agency) that owns this ad. Required.';
COMMENT ON COLUMN creatives.tenant_id IS 'Tenant (agency) that owns this creative. Required.';
COMMENT ON COLUMN delivery_events.tenant_id IS 'Tenant (agency) for this event. Required for analytics isolation.';

-- ============================================
-- MIGRATION COMPLETE
-- ============================================
-- This migration adds tenant-based isolation to the MnemoCast platform.
-- 
-- Next steps:
-- 1. Verify all data migrated correctly
-- 2. Uncomment NOT NULL constraints after verification
-- 3. Update application code to use tenant_id in all queries
-- 4. Test tenant isolation (no data leakage)
-- 5. Create tenant management APIs
--
-- Rollback: If needed, you can drop tenant_id columns and the tenants table
-- However, this will lose tenant associations, so backup first!

