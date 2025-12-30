# MnemoCast Development Roadmap & Progress Tracking

> **Development roadmap extracted from Agency Pitch Document**  
> This document tracks all TBD features and their development progress.

---

## Table of Contents

1. [High Priority Features (Next 3 Months)](#high-priority-features-next-3-months)
2. [Medium Priority Features (3-6 Months)](#medium-priority-features-3-6-months)
3. [Low Priority Features (6-12 Months)](#low-priority-features-6-12-months)
4. [Progress Summary](#progress-summary)
5. [Development Guidelines](#development-guidelines)

---

## High Priority Features (Next 3 Months)

### 1. Creative Media Management

**Status:** 🚧 **Not Started**  
**Priority:** 🔴 **Critical**  
**Estimated Effort:** 3-4 weeks  
**Assignee:** TBD  
**Target Completion:** Q1 2025

#### Requirements

- [ ] Media upload API (video/image upload)
- [ ] Media storage integration (MinIO/S3)
- [ ] Media validation (format, size, duration)
- [ ] CDN integration for media delivery
- [ ] Media transcoding (multiple formats/resolutions)

#### Current State

- ✅ Media URL storage in database
- ✅ Media validation service (basic structure exists)
- ❌ Upload API not implemented
- ❌ Media storage not integrated
- ❌ CDN integration not implemented
- ❌ Transcoding not implemented

#### Acceptance Criteria

- [ ] API endpoint: `POST /admin/media/upload` accepts video/image files
- [ ] Files stored in MinIO/S3 bucket
- [ ] Media validation checks format, size, duration
- [ ] CDN URLs generated for media delivery
- [ ] Support for multiple video formats (MP4, WebM)
- [ ] Support for multiple image formats (JPG, PNG, WebP)
- [ ] Media metadata stored in database

#### Business Impact

**Critical for self-service ad creation** - Agencies need to upload their own creatives without manual intervention.

#### Technical Notes

- Use MinIO for local development, S3 for production
- Implement file size limits (e.g., 100MB for videos, 10MB for images)
- Validate video duration matches ad duration requirements
- Generate thumbnails for video previews

---

### 2. Campaign Management

**Status:** 🚧 **Not Started**  
**Priority:** 🔴 **Critical**  
**Estimated Effort:** 4-5 weeks  
**Assignee:** TBD  
**Target Completion:** Q1 2025

#### Requirements

- [ ] Campaign grouping (multiple ads per campaign)
- [ ] Campaign-level budget management
- [ ] Campaign performance aggregation
- [ ] Campaign scheduling (start/end dates)
- [ ] Campaign priority/weight management

#### Current State

- ✅ Campaign domain model exists (`Campaign.scala`)
- ✅ Campaign store interface exists (`CampaignStore.scala`)
- ❌ Campaign APIs not fully implemented
- ❌ Campaign-to-ad relationship not fully integrated
- ❌ Campaign-level budget not implemented
- ❌ Campaign scheduling not implemented

#### Acceptance Criteria

- [ ] API endpoint: `POST /admin/campaigns` creates campaign
- [ ] API endpoint: `GET /admin/campaigns` lists campaigns
- [ ] API endpoint: `PUT /admin/campaigns/{campaignId}` updates campaign
- [ ] Campaign can contain multiple ads
- [ ] Campaign-level budget limits (total, daily, hourly)
- [ ] Campaign start/end date scheduling
- [ ] Campaign performance aggregation (sum of all ads in campaign)
- [ ] Campaign priority/weight affects ad selection

#### Business Impact

**Essential for agency workflow** - Agencies need to group ads into campaigns and manage them as units.

#### Technical Notes

- Extend `Campaign` model with budget fields
- Create `CampaignService` for campaign-level operations
- Update `AdDeliveryService` to consider campaign-level budgets
- Implement campaign-to-ad relationship in database

---

### 3. User Authentication & Authorization

**Status:** 🚧 **Not Started**  
**Priority:** 🔴 **Critical**  
**Estimated Effort:** 3-4 weeks  
**Assignee:** TBD  
**Target Completion:** Q1 2025

#### Requirements

- [ ] User registration and login
- [ ] JWT token-based authentication
- [ ] Role-based access control (RBAC)
- [ ] Admin vs. agency vs. advertiser roles
- [ ] API key management for screens

#### Current State

- ✅ User model exists (`User.scala`)
- ✅ User store exists (`UserStore.scala`)
- ✅ Basic authentication service exists (`AuthService.scala`)
- ❌ Authentication not integrated into API routes
- ❌ Authorization not implemented
- ❌ JWT token generation/validation not implemented
- ❌ RBAC not implemented

#### Acceptance Criteria

- [ ] API endpoint: `POST /auth/register` creates user account
- [ ] API endpoint: `POST /auth/login` returns JWT token
- [ ] JWT token validation middleware
- [ ] Role-based route protection (admin routes require admin role)
- [ ] Roles: `admin`, `agency`, `advertiser`, `screen`
- [ ] API key generation for screens
- [ ] API key validation for screen endpoints

#### Business Impact

**Required for multi-tenant and security** - Cannot deploy to production without authentication and authorization.

#### Technical Notes

- Use JWT library (e.g., `jwt-scala`)
- Implement middleware for route protection
- Store API keys in database with expiration
- Hash passwords using bcrypt

---

### 4. Screen Interaction Tracking (Optional for Interactive Screens)

**Status:** 🚧 **Not Started**  
**Priority:** 🟡 **Medium**  
**Estimated Effort:** 1-2 weeks  
**Assignee:** TBD  
**Target Completion:** Q1 2025

#### Requirements

- [ ] Touch/interaction event tracking (for interactive OOH screens)
- [ ] Interaction analytics and reporting
- [ ] Screen engagement metrics

#### Current State

- ✅ Event model exists (`DeliveryEvent.scala`)
- ❌ Interaction tracking not implemented
- ❌ Interaction analytics not calculated

#### Acceptance Criteria

- [ ] API endpoint: `POST /api/v1/events/interaction` logs interaction
- [ ] Interaction types: `touch`, `swipe`, `tap`
- [ ] Interaction analytics endpoint: `GET /api/v1/analytics/interactions`
- [ ] Screen engagement metrics (interactions per screen)

#### Business Impact

**Optional feature for interactive OOH screens** - Only relevant for screens with touch capabilities.

#### Technical Notes

- Extend `DeliveryEvent` with `interactionType` field
- Track interactions per screen, not per device/user
- Calculate engagement rate (interactions / impressions)

---

## Medium Priority Features (3-6 Months)

### 5. Advanced Analytics

**Status:** 🚧 **Not Started**  
**Priority:** 🟡 **Medium**  
**Estimated Effort:** 3-4 weeks  
**Assignee:** TBD  
**Target Completion:** Q2 2025

#### Requirements

- [ ] Revenue tracking (CPM, cost per playout)
- [ ] Budget utilization analytics
- [ ] A/B testing framework
- [ ] Predictive analytics
- [ ] Export functionality (CSV, JSON, PDF)
- [ ] Screen-level performance comparison

#### Current State

- ✅ Basic analytics implemented (`AnalyticsService.scala`)
- ✅ Impression/playout tracking
- ✅ Budget vs. spend analysis (basic)
- ❌ Revenue tracking not implemented
- ❌ Export functionality not implemented
- ❌ A/B testing not implemented
- ❌ Predictive analytics not implemented

#### Acceptance Criteria

- [ ] Revenue calculation: CPM, cost per playout
- [ ] Budget utilization percentage per campaign/ad
- [ ] Export endpoint: `GET /api/v1/analytics/export?format=csv`
- [ ] A/B test creation and management
- [ ] Screen performance comparison dashboard
- [ ] PDF report generation

#### Business Impact

**Enhanced reporting for agencies** - Agencies need detailed reports and exports for clients.

#### Technical Notes

- Add revenue fields to `Ad` model (CPM, cost per playout)
- Use library for CSV/PDF export (e.g., `opencsv`, `pdfbox`)
- Implement A/B test framework with traffic splitting
- Use weighted selection for A/B test distribution

---

### 6. Multi-Tenant Support

**Status:** 🚧 **Not Started**  
**Priority:** 🟡 **Medium**  
**Estimated Effort:** 4-6 weeks  
**Assignee:** TBD  
**Target Completion:** Q2 2025

#### Requirements

- [ ] Tenant isolation (data separation)
- [ ] Tenant-specific configurations
- [ ] Tenant-level analytics
- [ ] Tenant billing and invoicing

#### Current State

- ❌ Multi-tenant features removed for MVP
- ✅ Single-tenant architecture (production-ready)
- ❌ Multi-tenant support not implemented

#### Acceptance Criteria

- [ ] `tenant_id` column added to all tables (ads, screens, campaigns, events)
- [ ] Tenant isolation in all queries
- [ ] Tenant-specific configuration storage
- [ ] Tenant-level analytics aggregation
- [ ] Tenant billing/invoicing system

#### Business Impact

**Required for SaaS model** - Cannot offer SaaS without multi-tenant support.

#### Technical Notes

- Add `tenant_id` to all domain models
- Update all store implementations to filter by tenant
- Create tenant management APIs
- Implement tenant-level data isolation

---

### 7. Real-Time Bidding (RTB)

**Status:** 🚧 **Not Started**  
**Priority:** 🟡 **Medium**  
**Estimated Effort:** 6-8 weeks  
**Assignee:** TBD  
**Target Completion:** Q2 2025

#### Requirements

- [ ] RTB protocol support (OpenRTB)
- [ ] Bid request/response handling
- [ ] Auction mechanism
- [ ] Dynamic pricing

#### Current State

- ❌ RTB not implemented
- ✅ Weighted selection (foundation for RTB)

#### Acceptance Criteria

- [ ] OpenRTB 2.5 protocol support
- [ ] Bid request parsing
- [ ] Bid response generation
- [ ] Auction mechanism (first-price, second-price)
- [ ] Dynamic pricing based on screen classification

#### Business Impact

**Advanced programmatic advertising** - Enables integration with DSPs and ad exchanges.

#### Technical Notes

- Use OpenRTB library or implement protocol manually
- Implement auction logic in `AdDeliveryService`
- Add bid floor/ceiling to ad model
- Integrate with external DSPs

---

### 8. Advanced Targeting

**Status:** 🚧 **Not Started**  
**Priority:** 🟡 **Medium**  
**Estimated Effort:** 2-3 weeks  
**Assignee:** TBD  
**Target Completion:** Q2 2025

#### Requirements

- [ ] Geographic radius targeting (lat/long) for screens
- [ ] Weather-based targeting
- [ ] Day-of-week targeting enhancements
- [ ] Screen traffic-based targeting (high-traffic vs. low-traffic screens)

#### Current State

- ✅ Basic geographic targeting (country, city, area, venue type)
- ✅ Screen tag targeting
- ✅ Time-based targeting (daypart/timeband)
- ✅ Screen classification targeting
- ❌ Radius targeting not implemented
- ❌ Weather-based targeting not implemented
- ❌ Traffic-based targeting not implemented

#### Acceptance Criteria

- [ ] Radius targeting: `{"key": "radius", "operator": "within", "value": "lat:12.9716,lng:77.5946,radius:5000"}`
- [ ] Weather API integration (temperature, conditions)
- [ ] Weather-based targeting rules
- [ ] Screen traffic level field (high/medium/low)
- [ ] Traffic-based targeting

#### Business Impact

**More precise screen-level targeting** - Enables location-based and context-aware campaigns.

#### Technical Notes

- Add `latitude` and `longitude` to `Screen` model
- Integrate weather API (e.g., OpenWeatherMap)
- Calculate distance between screen and target location
- Add traffic level field to screen model

---

## Low Priority Features (6-12 Months)

### 9. A/B Testing

**Status:** 🚧 **Not Started**  
**Priority:** 🟢 **Low**  
**Estimated Effort:** 3-4 weeks  
**Assignee:** TBD  
**Target Completion:** Q3 2025

#### Requirements

- [ ] A/B test creation and management
- [ ] Traffic splitting
- [ ] Statistical significance calculation
- [ ] Winner selection automation

#### Current State

- ❌ A/B testing not implemented
- ✅ Weighted selection (can be used for traffic splitting)

#### Acceptance Criteria

- [ ] API endpoint: `POST /admin/ab-tests` creates A/B test
- [ ] Traffic splitting (50/50, 70/30, etc.)
- [ ] Statistical significance calculation (chi-square test)
- [ ] Automatic winner selection based on performance

#### Business Impact

**Campaign optimization** - Agencies can test different creatives and select winners automatically.

#### Technical Notes

- Create `ABTest` domain model
- Use weighted selection for traffic splitting
- Implement statistical tests for significance
- Track performance per variant

---

### 10. Machine Learning Optimization

**Status:** 🚧 **Not Started**  
**Priority:** 🟢 **Low**  
**Estimated Effort:** 8-12 weeks  
**Assignee:** TBD  
**Target Completion:** Q4 2025

#### Requirements

- [ ] Predictive ad selection
- [ ] Performance prediction
- [ ] Budget optimization
- [ ] Audience segmentation

#### Current State

- ❌ ML not implemented
- ✅ Analytics data available for ML training

#### Acceptance Criteria

- [ ] ML model for ad selection prediction
- [ ] Performance prediction (impressions, engagement)
- [ ] Budget optimization recommendations
- [ ] Screen segmentation based on performance

#### Business Impact

**Advanced optimization** - ML can optimize ad selection and budget allocation automatically.

#### Technical Notes

- Use ML library (e.g., TensorFlow, scikit-learn)
- Train models on historical performance data
- Implement prediction API endpoints
- A/B test ML vs. rule-based selection

---

### 11. White-Label Dashboard

**Status:** 🚧 **Not Started**  
**Priority:** 🟢 **Low**  
**Estimated Effort:** 8-10 weeks  
**Assignee:** TBD  
**Target Completion:** Q3 2025

#### Requirements

- [ ] Web-based admin dashboard
- [ ] Campaign management UI
- [ ] Analytics visualization
- [ ] Creative upload interface
- [ ] Real-time monitoring

#### Current State

- ❌ Dashboard UI not implemented
- ✅ All backend APIs ready for dashboard integration

#### Acceptance Criteria

- [ ] React/Vue/Angular dashboard application
- [ ] Campaign creation/editing UI
- [ ] Analytics charts and graphs
- [ ] Media upload drag-and-drop interface
- [ ] Real-time dashboard updates

#### Business Impact

**Self-service for agencies** - Agencies can manage campaigns without API knowledge.

#### Technical Notes

- Use modern frontend framework (React recommended)
- Use charting library (e.g., Chart.js, D3.js)
- Implement WebSocket for real-time updates
- White-label customization (logos, colors)

---

### 12. Screen Client SDK

**Status:** 🚧 **Not Started**  
**Priority:** 🟢 **Low**  
**Estimated Effort:** 2-3 weeks  
**Assignee:** TBD  
**Target Completion:** Q3 2025

#### Requirements

- [ ] Screen client SDK for digital signage players
- [ ] Automatic playlist fetching
- [ ] Offline playlist caching
- [ ] Health monitoring and reporting

#### Current State

- ❌ SDK not implemented
- ✅ REST API ready for screen integration

#### Acceptance Criteria

- [ ] SDK library (Python/Node.js/Go)
- [ ] Automatic playlist refresh
- [ ] Offline playlist storage
- [ ] Health check reporting to server
- [ ] Error handling and retry logic

#### Business Impact

**Easier integration for digital signage systems** - Screen operators can integrate faster.

#### Technical Notes

- Create SDK repository
- Implement playlist fetching logic
- Add local caching (SQLite/file-based)
- Health check endpoint integration

---

## Progress Summary

### Overall Progress

| Priority | Total Features | Completed | In Progress | Not Started | Completion % |
|----------|---------------|-----------|-------------|-------------|--------------|
| **High** | 4 | 0 | 0 | 4 | 0% |
| **Medium** | 4 | 0 | 0 | 4 | 0% |
| **Low** | 4 | 0 | 0 | 4 | 0% |
| **Total** | 12 | 0 | 0 | 12 | 0% |

### Status Legend

- ✅ **Completed**: Feature fully implemented and tested
- 🚧 **In Progress**: Feature currently being developed
- ⏸️ **On Hold**: Feature paused (blocked or deprioritized)
- 🚧 **Not Started**: Feature not yet begun
- ❌ **Cancelled**: Feature cancelled (no longer needed)

### Priority Legend

- 🔴 **Critical**: Must have for MVP/production
- 🟡 **Medium**: Important but not blocking
- 🟢 **Low**: Nice to have, can be deferred

---

## Development Guidelines

### Feature Development Process

1. **Planning Phase**
   - Review requirements and acceptance criteria
   - Estimate effort and assign developer
   - Create feature branch: `feature/feature-name`
   - Update status to "In Progress"

2. **Development Phase**
   - Implement feature according to acceptance criteria
   - Write unit tests
   - Update API documentation
   - Code review

3. **Testing Phase**
   - Manual testing
   - Integration testing
   - Update test documentation

4. **Completion Phase**
   - Merge to main branch
   - Update status to "Completed"
   - Update progress summary
   - Deploy to staging/production

### Code Standards

- Follow Scala best practices
- Write unit tests for all new features
- Update API documentation (`docs/02-api-spec.md`)
- Add migration scripts for database changes
- Update domain model documentation (`docs/01-domain-model.md`)

### Database Migrations

- Create migration file: `infra/local-dev/postgres/migrations/XXX_feature_name.sql`
- Test migration on local database
- Document schema changes

### API Documentation

- Update `docs/02-api-spec.md` with new endpoints
- Include request/response examples
- Document error codes

### Testing Requirements

- Unit tests for all services
- Integration tests for API endpoints
- Test edge cases and error handling
- Performance testing for high-traffic endpoints

---

## Next Steps

### Immediate Actions (This Week)

1. **Prioritize Features**
   - Review business priorities
   - Assign developers to high-priority features
   - Set target completion dates

2. **Start High-Priority Features**
   - Creative Media Management (Critical)
   - Campaign Management (Critical)
   - User Authentication & Authorization (Critical)

3. **Setup Development Environment**
   - Ensure all developers have access
   - Setup MinIO for media storage testing
   - Prepare database migration scripts

### This Month

- Complete Creative Media Management
- Complete Campaign Management (partial)
- Start User Authentication & Authorization

### This Quarter

- Complete all High-Priority features
- Start Medium-Priority features
- Begin planning for Low-Priority features

---

## Notes

- **Last Updated**: 2025-01-XX
- **Next Review**: Weekly
- **Owner**: Development Team Lead

---

*This roadmap is a living document and should be updated regularly as features are completed or priorities change.*

