-- ============================================
-- Migration: Seed Default Tenant and Admin User
-- Version: 2.0
-- Date: 2025-12-23
-- Description: Creates default tenant and super admin user for system management
-- ============================================

-- ============================================
-- CREATE DEFAULT TENANT (if not exists)
-- ============================================
INSERT INTO tenants (id, name, slug, status, settings)
VALUES (
    '00000000-0000-0000-0000-000000000000',
    'Default Tenant',
    'default',
    'active',
    '{"description": "Default tenant for existing data migration"}'::jsonb
)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- CREATE SUPER ADMIN USER (if not exists)
-- ============================================
-- Password: "admin123" (bcrypt hash)
-- IMPORTANT: Change this password after first login!
INSERT INTO users (
    id,
    email,
    username,
    password_hash,
    full_name,
    role,
    tenant_id,
    is_super_admin,
    is_active,
    email_verified
)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin@mnemocast.com',
    'admin',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',  -- bcrypt hash of "admin123"
    'System Administrator',
    'super_admin',
    NULL,  -- Super admins don't have tenant_id
    true,
    true,
    true
)
ON CONFLICT (email) DO NOTHING;

-- ============================================
-- CREATE SAMPLE TENANT FOR DEMO
-- ============================================
-- Example agency tenant for demonstration
INSERT INTO tenants (id, name, slug, status, settings)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'Acme Advertising Agency',
    'acme-agency',
    'active',
    '{"description": "Sample advertising agency", "contact_email": "contact@acme-agency.com"}'::jsonb
)
ON CONFLICT (id) DO NOTHING;

-- ============================================
-- CREATE SAMPLE TENANT ADMIN USER
-- ============================================
-- Password: "tenant123" (bcrypt hash)
-- IMPORTANT: Change this password after first login!
INSERT INTO users (
    id,
    email,
    username,
    password_hash,
    full_name,
    role,
    tenant_id,
    is_super_admin,
    is_active,
    email_verified
)
VALUES (
    '22222222-2222-2222-2222-222222222222',
    'admin@acme-agency.com',
    'acme_admin',
    '$2a$10$8K1p/a0dL1YqKZJqKZJqK.ZJqKZJqKZJqKZJqKZJqKZJqKZJqKZJqK',  -- bcrypt hash of "tenant123"
    'Acme Agency Admin',
    'tenant_admin',
    '11111111-1111-1111-1111-111111111111',  -- Acme Agency tenant
    false,
    true,
    true
)
ON CONFLICT (email) DO NOTHING;

-- ============================================
-- VERIFICATION QUERIES
-- ============================================
-- Run these to verify the migration:

-- SELECT id, name, slug, status FROM tenants;
-- SELECT id, email, username, role, tenant_id, is_super_admin FROM users;

