# Tenant-Based Isolation Plan

**Document Version:** 1.0  
**Last Updated:** 2025-12-23  
**Status:** Planning Phase

---

## Executive Summary

This document outlines the plan to implement tenant-based isolation for the MnemoCast ad serving platform. Multi-tenancy will enable the platform to serve multiple agencies/organizations (tenants) while ensuring complete data isolation, security, and independent configuration per tenant.

**Key Use Case:** Agencies/tenants need to self-manage their own campaigns, ads, creatives, users, and analytics without system admin intervention. System admins manage the overall platform, while tenant admins manage their agency's resources.

---

## Table of Contents

1. [Overview](#overview)
2. [Goals and Requirements](#goals-and-requirements)
3. [Architecture Design](#architecture-design)
4. [Database Schema Changes](#database-schema-changes)
5. [Domain Model Changes](#domain-model-changes)
6. [API Changes](#api-changes)
7. [Authentication & Authorization](#authentication--authorization)
8. [Data Isolation Strategy](#data-isolation-strategy)
9. [Implementation Phases](#implementation-phases)
10. [Migration Strategy](#migration-strategy)
11. [Testing Strategy](#testing-strategy)
12. [Security Considerations](#security-considerations)
13. [Performance Considerations](#performance-considerations)

---

## Overview

### Current State

- **Single-tenant architecture**: All data is global, no tenant concept
- **User authentication**: Basic JWT-based auth with roles (user, admin, advertiser)
- **No data isolation**: All screens, campaigns, ads, and creatives are accessible to all users
- **Global resources**: No organization/tenant boundaries

### Target State

- **Multi-tenant architecture**: Each agency/organization (tenant) operates in complete isolation
- **Tenant-scoped data**: All entities (screens, campaigns, ads, creatives, users) belong to a tenant
- **Tenant-aware authentication**: Users are associated with tenants, JWT tokens include tenant context
- **Data isolation**: Complete separation of data between tenants at database and application level
- **Role hierarchy**: 
  - **System Admin (Super Admin)**: Manages entire platform, all tenants, system configuration
  - **Tenant Admin**: Manages their agency's campaigns, ads, creatives, users, analytics
  - **Tenant User**: Basic access to their tenant's resources
- **Tenant self-service**: Agencies can independently manage their campaigns, ads, creatives, and users
- **Tenant management**: System admins create/manage tenants; tenant admins manage tenant resources

---

## Goals and Requirements

### Functional Requirements

1. **Tenant Creation & Management**
   - Create, read, update, delete tenants
   - Tenant configuration (name, domain, settings)
   - Tenant status (active, suspended, deleted)

2. **User-Tenant Association & Role Hierarchy**
   - **System Admin (Super Admin)**: 
     - Manages entire platform
     - Creates/manages tenants
     - Can access all tenants' data
     - System-level configuration
   - **Tenant Admin**: 
     - Manages their agency's campaigns, ads, creatives
     - Creates/manages users within their tenant
     - Views analytics for their tenant
     - Cannot access other tenants' data
   - **Tenant User**: 
     - Basic access to view campaigns, ads, creatives
     - Limited permissions within their tenant
   - Users belong to one tenant (MVP), can belong to multiple tenants (future)

3. **Data Isolation**
   - All entities must be tenant-scoped:
     - Screens
     - Campaigns
     - Ads
     - Creatives
     - Users (except super admins)
     - Events/Analytics
   - Users can only access data from their tenant(s)

4. **API Isolation & Tenant Self-Service**
   - All API endpoints must be tenant-aware
   - Screen endpoints automatically filter by tenant
   - **Tenant Admin APIs**: Allow tenant admins to:
     - Create/edit/delete campaigns for their tenant
     - Create/edit/delete ads for their tenant
     - Create/edit/delete creatives for their tenant
     - Manage users within their tenant
     - View analytics for their tenant
   - **System Admin APIs**: Allow system admins to:
     - Create/manage tenants
     - Access all tenants' data
     - System-level configuration

5. **Screen Authentication**
   - Screen passkeys are tenant-scoped
   - Screens can only authenticate within their tenant

### Non-Functional Requirements

1. **Security**
   - No data leakage between tenants
   - Tenant context validation on every request
   - Audit logging per tenant

2. **Performance**
   - Efficient tenant-based queries (indexed)
   - Minimal performance overhead from tenant filtering
   - Scalable to hundreds of tenants

3. **Backward Compatibility**
   - Migration path for existing data
   - Support for "default tenant" during transition

---

## Architecture Design

### Tenant Model

```
Tenant (Organization)
├── id: UUID (primary key)
├── name: String (e.g., "Acme Corporation")
├── slug: String (unique, URL-friendly, e.g., "acme-corp")
├── domain: String? (optional custom domain)
├── status: Enum (active, suspended, deleted)
├── settings: JSONB (tenant-specific configuration)
├── created_at: Timestamp
└── updated_at: Timestamp
```

### Data Isolation Approach

**Strategy: Shared Database, Tenant ID Column**

- **Pros:**
  - Simple to implement
  - Easy to maintain
  - Efficient for moderate number of tenants (< 1000)
  - Single database instance

- **Cons:**
  - Requires careful query filtering
  - Risk of data leakage if queries miss tenant filter

- **Mitigation:**
  - Middleware to inject tenant context
  - Store-level tenant filtering
  - Database row-level security (optional, future)

### Tenant Context Flow

```
Request → Extract Tenant ID → Validate Tenant Access → Filter Data → Response
```

1. **Extract Tenant ID**: From JWT token, subdomain, or header
2. **Validate Tenant Access**: User has access to this tenant
3. **Filter Data**: All queries automatically include tenant_id filter
4. **Response**: Return only tenant-scoped data

---

## Database Schema Changes

### 1. Create `tenants` Table

```sql
CREATE TABLE IF NOT EXISTS tenants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name TEXT NOT NULL,
    slug TEXT NOT NULL UNIQUE,  -- URL-friendly identifier (e.g., "acme-corp")
    domain TEXT,                -- Optional custom domain
    status TEXT NOT NULL DEFAULT 'active',  -- active, suspended, deleted
    settings JSONB DEFAULT '{}',  -- Tenant-specific configuration
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_tenants_slug ON tenants(slug);
CREATE INDEX idx_tenants_status ON tenants(status) WHERE status = 'active';
CREATE INDEX idx_tenants_domain ON tenants(domain) WHERE domain IS NOT NULL;
```

### 2. Add `tenant_id` to All Entity Tables

#### Users Table

```sql
ALTER TABLE users ADD COLUMN IF NOT EXISTS tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS is_super_admin BOOLEAN NOT NULL DEFAULT false;

-- Super admins don't need tenant_id (can be NULL)
-- Regular users must have tenant_id
CREATE INDEX idx_users_tenant_id ON users(tenant_id) WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_users_is_super_admin ON users(is_super_admin) WHERE is_super_admin = true;
```

#### Screens Table

```sql
ALTER TABLE screens ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_screens_tenant_id ON screens(tenant_id);
CREATE INDEX idx_screens_tenant_id_passkey ON screens(tenant_id, passkey);
```

#### Campaigns Table

```sql
ALTER TABLE campaigns ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_campaigns_tenant_id ON campaigns(tenant_id);
CREATE INDEX idx_campaigns_tenant_status_dates ON campaigns(tenant_id, status, start_date, end_date) WHERE status = 'active';
```

#### Ads Table

```sql
ALTER TABLE ads ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_ads_tenant_id ON ads(tenant_id);
CREATE INDEX idx_ads_tenant_is_active ON ads(tenant_id, is_active) WHERE is_active = true;
```

#### Creatives Table

```sql
ALTER TABLE creatives ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_creatives_tenant_id ON creatives(tenant_id);
CREATE INDEX idx_creatives_tenant_campaign_id ON creatives(tenant_id, campaign_id);
```

#### Delivery Events Table

```sql
ALTER TABLE delivery_events ADD COLUMN IF NOT EXISTS tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE;

CREATE INDEX idx_delivery_events_tenant_id ON delivery_events(tenant_id);
CREATE INDEX idx_delivery_events_tenant_ad_id ON delivery_events(tenant_id, ad_id);
CREATE INDEX idx_delivery_events_tenant_occurred_at ON delivery_events(tenant_id, occurred_at);
```

### 3. Create User-Tenant Roles Table (Many-to-Many)

For users who belong to multiple tenants with different roles:

```sql
CREATE TABLE IF NOT EXISTS user_tenant_roles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'user',  -- user, admin, advertiser
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(user_id, tenant_id)
);

CREATE INDEX idx_user_tenant_roles_user_id ON user_tenant_roles(user_id);
CREATE INDEX idx_user_tenant_roles_tenant_id ON user_tenant_roles(tenant_id);
```

**Note:** For MVP, we can start with single tenant per user (tenant_id in users table). The user_tenant_roles table can be added later for multi-tenant users.

### 4. Role Hierarchy

**System Roles:**
- `super_admin`: System-level admin, can manage all tenants
- `tenant_admin`: Agency admin, manages their tenant's resources
- `tenant_user`: Regular user within a tenant
- `advertiser`: Advertiser role within a tenant (future)

**Role Permissions:**

| Role | Manage Tenants | Manage Tenant Resources | View All Tenants | View Own Tenant |
|------|---------------|------------------------|------------------|-----------------|
| super_admin | ✅ | ✅ (all tenants) | ✅ | ✅ |
| tenant_admin | ❌ | ✅ (own tenant only) | ❌ | ✅ |
| tenant_user | ❌ | ❌ (view only) | ❌ | ✅ |

---

## Domain Model Changes

### 1. Create Tenant Model

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Tenant.scala`

```scala
package mnemocast.engine.domain.model

import java.time.Instant
import java.util.UUID

import io.circe.Codec
import io.circe.generic.semiauto._

final case class Tenant(
  id: UUID,
  name: String,
  slug: String,  // URL-friendly identifier
  domain: Option[String] = None,
  status: String = "active",  // active, suspended, deleted
  settings: Map[String, String] = Map.empty,
  createdAt: Instant,
  updatedAt: Instant
)

object Tenant {
  implicit val tenantCodec: Codec[Tenant] = deriveCodec[Tenant]
}
```

### 2. Update User Model

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/User.scala`

```scala
final case class User(
  id: UUID,
  email: String,
  username: String,
  passwordHash: String,
  fullName: Option[String] = None,
  role: String = "tenant_user",  // super_admin, tenant_admin, tenant_user, advertiser
  tenantId: Option[UUID] = None,  // NULL for super admins, required for tenant users/admins
  isSuperAdmin: Boolean = false,  // Super admins can access all tenants
  isActive: Boolean = true,
  emailVerified: Boolean = false,
  lastLogin: Option[Instant] = None,
  createdAt: Instant,
  updatedAt: Instant
)
```

### 3. Update Screen Model

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Screen.scala`

```scala
final case class Screen(
  id: String,
  name: String,
  tenantId: UUID,  // Required: screens belong to a tenant
  location: ScreenLocation,
  tags: List[String] = List.empty,
  metadata: Map[String, String] = Map.empty,
  classification: Int = 1,
  width: Option[Int] = None,
  height: Option[Int] = None,
  isAudible: Boolean = false,
  isOnline: Boolean = false,
  lastSeen: Option[Instant] = None,
  passkey: String,
  createdAt: Instant,
  updatedAt: Instant
)
```

### 4. Update Campaign Model

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Campaign.scala`

```scala
final case class Campaign(
  id: String,
  name: String,
  advertiserId: String,
  tenantId: UUID,  // Required: campaigns belong to a tenant
  status: String = "active",
  startDate: Instant,
  endDate: Instant,
  totalBudget: Option[Long] = None,
  targetPlayouts: Option[Long] = None,
  targetingRules: List[TargetingRule] = List.empty,
  priority: Int = 1,
  createdAt: Instant,
  updatedAt: Instant
)
```

### 5. Update Ad Model

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Ad.scala`

```scala
final case class Ad(
  id: String,
  advertiserId: String,
  tenantId: UUID,  // Required: ads belong to a tenant
  creativeUrl: String,
  targetUrl: Option[String],
  targetingRules: List[TargetingRule],
  isActive: Boolean,
  // ... rest of fields unchanged
)
```

### 6. Update Creative Model

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Creative.scala`

```scala
final case class Creative(
  id: String,
  campaignId: String,
  tenantId: UUID,  // Required: creatives belong to a tenant
  name: String,
  creativeType: String = "video",
  creativeUrl: String,
  targetUrl: Option[String],
  durationSeconds: Int,
  status: String = "active",
  shareOfVoice: Option[Double] = None,
  frequencyCapPerScreen: Option[Int] = None,
  createdAt: Instant,
  updatedAt: Instant
)
```

---

## API Changes

### 1. Tenant Management Endpoints

**New Routes:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/TenantRoutes.scala`

**System Admin Endpoints (Super Admin Only):**
```
POST   /api/v1/admin/tenants              - Create tenant
GET    /api/v1/admin/tenants               - List all tenants
GET    /api/v1/admin/tenants/{tenantId}    - Get tenant details
PUT    /api/v1/admin/tenants/{tenantId}    - Update tenant
DELETE /api/v1/admin/tenants/{tenantId}    - Delete tenant (soft delete)
```

**Tenant Admin/User Endpoints:**
```
GET    /api/v1/tenants/current            - Get current user's tenant
GET    /api/v1/tenants/current/settings   - Get current tenant settings (tenant admin only)
PUT    /api/v1/tenants/current/settings   - Update current tenant settings (tenant admin only)
```

### 2. Update Existing Endpoints

All existing endpoints must include tenant context:

#### Screen Endpoints

```
POST   /api/v1/screens/register
  - Request body must include tenantId (or extract from authenticated user)
  - Response: Screen with tenantId

GET    /api/v1/screens
  - Automatically filter by user's tenant(s)
  - Super admins can specify ?tenantId=xxx

GET    /api/v1/screens/{screenId}
  - Verify screen belongs to user's tenant

PUT    /api/v1/screens/{screenId}
  - Verify screen belongs to user's tenant

PUT    /api/v1/screens/{screenId}/heartbeat
  - Screen authentication already validates tenant via passkey
```

#### Campaign Endpoints (Tenant Self-Service)

**Tenant Admin/User Endpoints:**
```
POST   /api/v1/campaigns
  - Create campaign (tenant admin only)
  - tenantId automatically extracted from authenticated user's tenant

GET    /api/v1/campaigns
  - List campaigns (filtered by user's tenant)
  - Tenant admin: sees all tenant campaigns
  - Tenant user: sees active campaigns only

GET    /api/v1/campaigns/{campaignId}
  - Get campaign details
  - Verify campaign belongs to user's tenant

PUT    /api/v1/campaigns/{campaignId}
  - Update campaign (tenant admin only)
  - Verify campaign belongs to user's tenant

DELETE /api/v1/campaigns/{campaignId}
  - Delete campaign (tenant admin only)
  - Verify campaign belongs to user's tenant
```

**System Admin Endpoints:**
```
GET    /api/v1/admin/campaigns?tenantId={tenantId}
  - List campaigns for specific tenant (super admin only)
```

#### Ad Endpoints (Tenant Self-Service)

**Tenant Admin/User Endpoints:**
```
POST   /api/v1/ads
  - Create ad (tenant admin only)
  - tenantId automatically extracted from authenticated user's tenant

GET    /api/v1/ads
  - List ads (filtered by user's tenant)
  - Tenant admin: sees all tenant ads
  - Tenant user: sees active ads only

GET    /api/v1/ads/{adId}
  - Get ad details
  - Verify ad belongs to user's tenant

PUT    /api/v1/ads/{adId}
  - Update ad (tenant admin only)
  - Verify ad belongs to user's tenant

DELETE /api/v1/ads/{adId}
  - Delete ad (tenant admin only)
  - Verify ad belongs to user's tenant
```

**Screen Ad Delivery (Tenant-Aware):**
```
GET    /api/v1/screens/{screenId}/ads/deliver
  - Screen's tenantId used to filter ads
  - Only returns ads from screen's tenant
```

**System Admin Endpoints:**
```
GET    /api/v1/admin/ads?tenantId={tenantId}
  - List ads for specific tenant (super admin only)
```

#### Analytics Endpoints (Tenant Self-Service)

**Tenant Admin/User Endpoints:**
```
GET    /api/v1/analytics/dashboard
  - Dashboard metrics (filtered by user's tenant)
  - Tenant admin: full analytics
  - Tenant user: limited analytics

GET    /api/v1/analytics/campaigns
  - Campaign performance (filtered by tenant)

GET    /api/v1/analytics/ads
  - Ad performance (filtered by tenant)

GET    /api/v1/analytics/screens
  - Screen analytics (filtered by tenant)
```

**System Admin Endpoints:**
```
GET    /api/v1/admin/analytics/dashboard?tenantId={tenantId}
  - Dashboard metrics for specific tenant (super admin only)
```

### 3. Tenant Context Extraction

**Strategy 1: JWT Token (Primary)**

- JWT token includes `tenantId` claim
- Middleware extracts tenantId from token
- All queries automatically filtered by tenantId

**Strategy 2: Subdomain (Future)**

- Extract tenant from subdomain (e.g., `acme-corp.mnemocast.com`)
- Map subdomain to tenant slug

**Strategy 3: Header (Optional)**

- `X-Tenant-Id` header for API clients
- Lower priority than JWT token

---

## Authentication & Authorization

### 1. JWT Token Structure

**Current:**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "role": "admin",
  "exp": 1234567890
}
```

**Updated:**
```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "role": "admin",
  "tenantId": "uuid",  // User's primary tenant
  "tenantIds": ["uuid1", "uuid2"],  // All tenants user has access to (if multi-tenant)
  "isSuperAdmin": false,
  "exp": 1234567890
}
```

### 2. Login Flow

```
1. User authenticates with email/password
2. System validates credentials
3. System retrieves user's tenant(s)
4. JWT token generated with tenant context
5. Token returned to client
```

### 3. Tenant Validation Middleware

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/middleware/TenantMiddleware.scala`

```scala
object TenantMiddleware {
  def requireTenant(tenantId: UUID): Directive1[UUID] = {
    extractRequest.flatMap { request =>
      // Extract tenantId from JWT token
      val tokenTenantId = extractTenantFromToken(request)
      
      // Validate user has access to this tenant
      if (isSuperAdmin || tokenTenantId.contains(tenantId)) {
        provide(tenantId)
      } else {
        reject(AuthorizationFailedRejection)
      }
    }
  }
  
  def extractTenantFromRequest: Directive1[UUID] = {
    // Extract from JWT token (primary)
    // Fallback to header if needed
  }
}
```

### 4. Screen Authentication

Screen passkeys are tenant-scoped:

```
1. Screen registers with tenantId
2. Passkey generated and stored with tenantId
3. Screen authenticates with X-Screen-Id and X-Screen-Passkey
4. System validates passkey and extracts tenantId
5. All ad delivery queries filtered by tenantId
```

---

## Data Isolation Strategy

### 1. Store-Level Filtering

All stores must filter by tenantId:

**Example: AdStore**

```scala
trait AdStore {
  def listActive(tenantId: UUID): Future[List[Ad]]
  def getById(id: String, tenantId: UUID): Future[Option[Ad]]
  def upsert(ad: Ad): Future[Unit]  // Ad.tenantId must be set
}
```

### 2. Query-Level Filtering

All database queries include tenant_id:

```sql
-- Before
SELECT * FROM ads WHERE is_active = true;

-- After
SELECT * FROM ads WHERE is_active = true AND tenant_id = $1;
```

### 3. Service-Level Filtering

Services automatically inject tenant context:

```scala
class AdDeliveryService(adStore: AdStore, tenantId: UUID) {
  def deliver(request: DeliveryRequest): Future[Option[DeliveryResponse]] = {
    // All queries automatically filtered by tenantId
    adStore.listActive(tenantId).flatMap { ads =>
      // ... delivery logic
    }
  }
}
```

### 4. Row-Level Security (Future Enhancement)

PostgreSQL Row-Level Security (RLS) can provide additional isolation:

```sql
ALTER TABLE ads ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation_policy ON ads
  FOR ALL
  USING (tenant_id = current_setting('app.current_tenant_id')::UUID);
```

---

## Implementation Phases

### Phase 1: Foundation (Week 1-2)

**Goal:** Create tenant model and database schema

**Tasks:**
1. Create `tenants` table migration
2. Create `Tenant` domain model
3. Create `TenantStore` interface and implementations (Redis, Postgres)
4. Create tenant management API endpoints (super admin only)
5. Seed default tenant for existing data

**Deliverables:**
- Database migration script
- Tenant domain model
- TenantStore implementations
- Basic tenant CRUD APIs

### Phase 2: User-Tenant Association (Week 2-3)

**Goal:** Associate users with tenants and implement role hierarchy

**Tasks:**
1. Add `tenant_id` and `is_super_admin` to users table (via migration)
2. Update User domain model with role hierarchy
3. Update authentication to include tenant in JWT
4. Update login endpoint to return tenant context
5. Create user-tenant management endpoints
6. Implement role-based access control (RBAC):
   - Super admin: full system access
   - Tenant admin: manage their tenant's resources
   - Tenant user: view-only access

**Deliverables:**
- Updated User model
- JWT token with tenant context
- User-tenant association APIs
- RBAC middleware

### Phase 3: Entity Tenant Scoping & Tenant Self-Service APIs (Week 3-5)

**Goal:** Add tenant_id to all entities and enable tenant self-service

**Tasks:**
1. Add `tenant_id` column to screens, campaigns, ads, creatives, events tables (via migration)
2. Update all domain models with tenantId
3. Update all stores to filter by tenantId
4. Update all service methods to require tenantId
5. Update all API routes to extract and validate tenantId
6. **Create tenant self-service APIs:**
   - Tenant admins can create/edit/delete campaigns
   - Tenant admins can create/edit/delete ads
   - Tenant admins can create/edit/delete creatives
   - Tenant admins can manage users within their tenant
   - All operations automatically scoped to tenant

**Deliverables:**
- Database migrations for all entities
- Updated domain models
- Tenant-aware stores and services
- Tenant-validated API routes
- Tenant self-service API endpoints

### Phase 4: Screen Authentication (Week 5-6)

**Goal:** Make screen authentication tenant-aware

**Tasks:**
1. Update screen registration to require tenantId
2. Update screen passkey validation to check tenant
3. Update ad delivery to filter by screen's tenant
4. Update screen heartbeat to validate tenant

**Deliverables:**
- Tenant-aware screen authentication
- Tenant-scoped ad delivery

### Phase 5: Analytics & Reporting (Week 6-7)

**Goal:** Tenant-scoped analytics

**Tasks:**
1. Update analytics queries to filter by tenantId
2. Update dashboard endpoints to show tenant-specific data
3. Add tenant-level analytics aggregation

**Deliverables:**
- Tenant-scoped analytics
- Tenant dashboard views

### Phase 6: Testing & Migration (Week 7-8)

**Goal:** Test and migrate existing data

**Tasks:**
1. Create test suite for tenant isolation
2. Create data migration script for existing data
3. Test tenant isolation (no data leakage)
4. Performance testing with multiple tenants
5. Documentation updates

**Deliverables:**
- Test suite
- Migration script
- Performance benchmarks
- Updated documentation

---

## Migration Strategy

### 1. Create Default Tenant

```sql
-- Create default tenant for existing data
INSERT INTO tenants (id, name, slug, status)
VALUES ('00000000-0000-0000-0000-000000000000', 'Default Tenant', 'default', 'active');
```

### 2. Migrate Existing Data

```sql
-- Migrate all existing data to default tenant
UPDATE users SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;
UPDATE screens SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;
UPDATE campaigns SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;
UPDATE ads SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;
UPDATE creatives SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;
UPDATE delivery_events SET tenant_id = '00000000-0000-0000-0000-000000000000' WHERE tenant_id IS NULL;
```

### 3. Make tenant_id NOT NULL

After migration, add NOT NULL constraints:

```sql
ALTER TABLE screens ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE campaigns ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE ads ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE creatives ALTER COLUMN tenant_id SET NOT NULL;
ALTER TABLE delivery_events ALTER COLUMN tenant_id SET NOT NULL;
```

### 4. Migration Scripts

Two migration scripts have been created:

1. **`001_add_tenant_isolation.sql`**: 
   - Creates tenants table
   - Adds tenant_id to all entity tables
   - Creates indexes
   - Migrates existing data to default tenant
   - Creates views for tenant analytics

2. **`002_seed_default_tenant_and_admin.sql`**:
   - Creates default tenant
   - Creates super admin user
   - Creates sample tenant and tenant admin for demo

**Migration Order:**
1. Run `001_add_tenant_isolation.sql` first
2. Run `002_seed_default_tenant_and_admin.sql` second
3. Verify data migration
4. Uncomment NOT NULL constraints in `001_add_tenant_isolation.sql` if needed

---

## Testing Strategy

### 1. Unit Tests

- Tenant model serialization/deserialization
- Tenant store operations
- Tenant validation logic

### 2. Integration Tests

- Tenant creation and management
- User-tenant association
- Data isolation between tenants
- Cross-tenant access prevention

### 3. Security Tests

- Verify no data leakage between tenants
- Verify tenant validation on all endpoints
- Verify super admin can access all tenants
- Verify regular users cannot access other tenants

### 4. Performance Tests

- Query performance with tenant filtering
- Index effectiveness
- Concurrent tenant operations

### Test Scenarios

**Scenario 1: Tenant Isolation**
```
1. Create Tenant A and Tenant B
2. Create screens in Tenant A
3. Create screens in Tenant B
4. Login as Tenant A user
5. List screens → Should only see Tenant A screens
6. Try to access Tenant B screen → Should fail
```

**Scenario 2: Super Admin**
```
1. Create Tenant A and Tenant B
2. Create screens in both tenants
3. Login as super admin
4. List screens → Should see all screens
5. Can access screens from any tenant
```

**Scenario 3: Screen Authentication**
```
1. Create Tenant A
2. Register screen in Tenant A
3. Authenticate screen with passkey
4. Request ads → Should only get Tenant A ads
5. Try to access Tenant B screen passkey → Should fail
```

---

## Security Considerations

### 1. Data Leakage Prevention

- **Mandatory tenant filtering**: All queries must include tenant_id
- **Middleware validation**: Validate tenant access on every request
- **Store-level enforcement**: Stores should not allow queries without tenantId
- **Database constraints**: Foreign keys ensure referential integrity

### 2. Tenant ID Validation

- Validate tenantId exists and is active
- Validate user has access to tenant
- Reject requests with invalid tenantId

### 3. Super Admin Access

- Super admins can access all tenants
- Super admin status must be explicitly checked
- Audit logging for super admin actions

### 4. Screen Authentication

- Screen passkeys are tenant-scoped
- Validate screen belongs to tenant before ad delivery
- Prevent cross-tenant screen access

### 5. Audit Logging

- Log all tenant-related operations
- Log cross-tenant access attempts
- Log tenant creation/deletion

---

## Performance Considerations

### 1. Indexing

All tenant_id columns must be indexed:

```sql
CREATE INDEX idx_screens_tenant_id ON screens(tenant_id);
CREATE INDEX idx_campaigns_tenant_id ON campaigns(tenant_id);
CREATE INDEX idx_ads_tenant_id ON ads(tenant_id);
-- etc.
```

### 2. Composite Indexes

For common query patterns:

```sql
CREATE INDEX idx_ads_tenant_is_active ON ads(tenant_id, is_active) WHERE is_active = true;
CREATE INDEX idx_campaigns_tenant_status_dates ON campaigns(tenant_id, status, start_date, end_date);
```

### 3. Query Optimization

- Use parameterized queries with tenant_id
- Avoid full table scans
- Use EXPLAIN ANALYZE to verify index usage

### 4. Caching

- Cache tenant information (Redis)
- Cache tenant-scoped data with tenant prefix
- Invalidate cache on tenant updates

---

## Future Enhancements

### 1. Multi-Tenant Users

- Users can belong to multiple tenants
- Different roles per tenant
- User-tenant-role mapping table

### 2. Subdomain-Based Tenant Resolution

- Extract tenant from subdomain
- Automatic tenant context from URL

### 3. Tenant Customization

- Custom branding per tenant
- Custom domain per tenant
- Tenant-specific feature flags

### 4. Row-Level Security (RLS)

- PostgreSQL RLS for additional isolation
- Automatic tenant filtering at database level

### 5. Tenant Analytics

- Tenant-level usage metrics
- Tenant billing/usage tracking
- Tenant performance dashboards

---

## Acceptance Criteria

### Phase 1 Complete When:

- [ ] Tenants table created and seeded
- [ ] Tenant CRUD APIs functional
- [ ] Tenant domain model implemented
- [ ] TenantStore implementations working

### Phase 2 Complete When:

- [ ] Users associated with tenants
- [ ] JWT tokens include tenant context
- [ ] Login returns tenant information
- [ ] User-tenant management APIs functional

### Phase 3 Complete When:

- [ ] All entities have tenant_id
- [ ] All stores filter by tenant
- [ ] All services require tenant context
- [ ] All API routes validate tenant

### Phase 4 Complete When:

- [ ] Screen registration requires tenant
- [ ] Screen authentication validates tenant
- [ ] Ad delivery filtered by tenant
- [ ] No cross-tenant data access

### Phase 5 Complete When:

- [ ] Analytics filtered by tenant
- [ ] Dashboard shows tenant-specific data
- [ ] No data leakage between tenants

### Phase 6 Complete When:

- [ ] All tests passing
- [ ] Existing data migrated
- [ ] Performance benchmarks met
- [ ] Documentation updated

---

## Risks and Mitigations

### Risk 1: Data Leakage

**Mitigation:**
- Comprehensive testing
- Code review for all queries
- Middleware validation
- Database constraints

### Risk 2: Performance Degradation

**Mitigation:**
- Proper indexing
- Query optimization
- Performance testing
- Caching strategy

### Risk 3: Migration Complexity

**Mitigation:**
- Phased migration
- Rollback plan
- Data backup before migration
- Testing in staging first

### Risk 4: Breaking Changes

**Mitigation:**
- Backward compatibility during transition
- Versioned APIs
- Clear migration guide
- Support for default tenant

---

## Tenant Self-Service Summary

### Key Capabilities for Agencies/Tenants

Once implemented, tenant admins (agency managers) will be able to:

1. **Manage Campaigns**
   - Create, edit, delete campaigns for their agency
   - Set campaign budgets, dates, targeting rules
   - View campaign performance analytics

2. **Manage Ads**
   - Create, edit, delete ads for their agency
   - Configure ad targeting, budgets, frequency caps
   - View ad performance metrics

3. **Manage Creatives**
   - Upload and manage creative assets
   - Associate creatives with campaigns
   - View creative performance

4. **Manage Users**
   - Create and manage users within their tenant
   - Assign roles (tenant_admin, tenant_user)
   - Deactivate users

5. **View Analytics**
   - Dashboard with agency-specific metrics
   - Campaign performance reports
   - Ad performance reports
   - Screen analytics (if screens belong to tenant)

### System Admin vs Tenant Admin

| Feature | System Admin | Tenant Admin |
|---------|--------------|--------------|
| Create/Manage Tenants | ✅ | ❌ |
| Access All Tenants | ✅ | ❌ |
| Manage Own Tenant Resources | ✅ | ✅ |
| Manage Users in Own Tenant | ✅ | ✅ |
| View All Tenant Analytics | ✅ | ❌ |
| View Own Tenant Analytics | ✅ | ✅ |
| System Configuration | ✅ | ❌ |

---

## Conclusion

This plan provides a comprehensive roadmap for implementing tenant-based isolation in MnemoCast with full tenant self-service capabilities. The phased approach ensures minimal disruption while providing complete data isolation, security, and agency independence.

**Key Benefits:**
- **Agencies can self-manage**: No need for system admin intervention for day-to-day operations
- **Complete data isolation**: Each agency's data is completely separate
- **Scalable**: Supports hundreds of agencies/tenants
- **Secure**: Role-based access control prevents unauthorized access

**Next Steps:**
1. Review and approve this plan
2. Run migration scripts: `001_add_tenant_isolation.sql` and `002_seed_default_tenant_and_admin.sql`
3. Create detailed task breakdown for Phase 1
4. Begin implementation of Phase 1
5. Set up testing infrastructure

---

**Document Status:** Ready for Review  
**Approved By:** _Pending_  
**Implementation Start Date:** _TBD_

**Migration Files Created:**
- `infra/local-dev/postgres/migrations/001_add_tenant_isolation.sql` (322 lines)
- `infra/local-dev/postgres/migrations/002_seed_default_tenant_and_admin.sql` (104 lines)

