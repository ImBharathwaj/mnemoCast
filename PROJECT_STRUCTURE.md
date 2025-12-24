# MnemoCast Project Structure

Complete documentation of the MnemoCast ad serving engine project structure, files, and their usage.

**Last Updated:** 2025-12-19  
**Project Type:** Scala/TypeScript Ad Serving Engine with React Dashboard

---

## Project Overview

MnemoCast is a smart ad serving engine built with:
- **Backend:** Scala 2.13 + Pekko HTTP
- **Frontend:** React + TypeScript
- **Storage:** PostgreSQL + Redis (hybrid)
- **Media Storage:** MinIO / Local File System

---

## Root Directory Structure

```
mnemoCast/
├── backend/              # Scala backend application
├── dashboard/            # React TypeScript frontend
├── docs/                 # Project documentation
├── infra/                # Infrastructure configuration
├── scripts/              # Utility scripts
├── storage/              # Local file storage (uploads)
├── README.md             # Main project README
└── PROJECT_STRUCTURE.md  # This file
```

---

## Backend (`/backend`)

### Overview
Scala-based ad serving engine using Pekko HTTP. Organized in a modular architecture with three main modules.

### Structure

```
backend/
├── build.sbt                    # SBT build configuration
├── README.md                    # Backend-specific README
├── project/                     # SBT project configuration
│   └── build.properties         # SBT version
├── modules/                     # Main application modules
│   ├── engine-api/              # HTTP API layer
│   ├── engine-domain/           # Domain models & business logic
│   ├── engine-infra/            # Infrastructure (storage, services)
│   └── shared-kernel/           # Shared utilities
├── storage/                     # Local file storage
│   └── uploads/
│       └── creatives/           # Uploaded ad creatives
└── target/                      # SBT build output (compiled classes, JARs)
```

### Build Configuration (`build.sbt`)

**Purpose:** Defines project structure, dependencies, and module relationships.

**Key Dependencies:**
- **Pekko HTTP** (1.0.1) - HTTP server framework
- **Circe** (0.14.7) - JSON serialization
- **Jedis** (5.1.0) - Redis client
- **PostgreSQL** (42.7.1) - Database driver
- **MinIO** (8.5.7) - Object storage client
- **JWT** (9.4.5) - Authentication tokens
- **BCrypt** (0.4) - Password hashing

**Modules:**
1. `engine-domain` - Pure domain models (no dependencies)
2. `engine-infra` - Infrastructure layer (depends on domain)
3. `engine-api` - HTTP API layer (depends on domain + infra)

---

## Module: `engine-domain`

**Purpose:** Core domain models and business logic. Pure Scala with no external dependencies (except Circe for JSON).

### Structure

```
engine-domain/
└── src/main/scala/mnemocast/engine/domain/
    ├── model/                   # Domain models (case classes)
    │   ├── Ad.scala             # Ad entity with targeting rules
    │   ├── Campaign.scala       # Campaign entity
    │   ├── Creative.scala       # Creative media entity
    │   ├── Screen.scala         # Digital screen entity
    │   ├── User.scala           # User account entity
    │   ├── TargetingRule.scala  # Targeting rule model
    │   ├── DeliveryRequest.scala # Ad delivery request
    │   ├── DeliveryResponse.scala # Ad delivery response
    │   ├── DeliveryEvents.scala  # Event tracking models
    │   ├── Playlist.scala       # Playlist model
    │   ├── Metrics.scala        # Analytics metrics
    │   ├── Analytics.scala      # Analytics data models
    │   ├── Health.scala         # Health check model
    │   ├── Decision.scala       # Decision tracking
    │   ├── TimeBand.scala       # Time-based targeting
    │   └── Request/Response models:
    │       ├── CreateAdRequest.scala
    │       ├── CreateCampaignRequest.scala
    │       ├── CreateCreativeRequest.scala
    │       ├── CreateScreenRequest.scala
    │       ├── MediaUploadResponse.scala
    │       └── ErrorResponse.scala
    └── services/                 # Pure business logic services
        ├── TargetingService.scala    # Targeting rule evaluation
        └── TimeTargetingService.scala # Time-based targeting logic
```

### Key Files

#### `model/Ad.scala`
**Purpose:** Core ad entity with targeting, budget, and frequency cap rules.

**Fields:**
- `id`, `advertiserId`, `creativeUrl`, `targetUrl`
- `isActive`, `maxPlays`, `dailyLimit`, `hourlyLimit`
- `targetingRules: List[TargetingRule]`
- `weight` (for weighted selection)
- `durationSeconds`

#### `model/Screen.scala`
**Purpose:** Digital screen/display device entity.

**Fields:**
- `id`, `name`, `location` (ScreenLocation)
- `tags`, `metadata`, `classification` (1-10)
- `width`, `height`, `isAudible`, `isOnline`
- `lastSeen`, `passkey` (for authentication)
- `createdAt`, `updatedAt`

#### `services/TargetingService.scala`
**Purpose:** Evaluates whether ads match delivery requests based on targeting rules.

**Key Methods:**
- `matches(ad: Ad, request: DeliveryRequest): Boolean`
- `evaluateRule(rule: TargetingRule, request: DeliveryRequest): Boolean`
- `extractRequestValue(key: String, request: DeliveryRequest): Option[String]`

**Supported Operators:**
- `eq` - Equals (case-insensitive)
- `in` - List membership
- `gte`, `lte`, `gt`, `lt` - Numeric comparisons
- `daypart` / `timeband` - Time-based targeting

---

## Module: `engine-infra`

**Purpose:** Infrastructure layer - storage implementations, services, and external integrations.

### Structure

```
engine-infra/
└── src/main/scala/mnemocast/engine/infra/
    ├── store/                   # Data store interfaces & implementations
    │   ├── AdStore.scala        # Ad storage interface
    │   ├── CampaignStore.scala  # Campaign storage interface
    │   ├── CreativeStore.scala  # Creative storage interface
    │   ├── ScreenStore.scala    # Screen storage interface
    │   ├── EventStore.scala     # Event storage interface
    │   ├── UserStore.scala      # User storage interface
    │   ├── DecisionStore.scala  # Decision tracking interface
    │   ├── redis/               # Redis implementations
    │   │   ├── RedisClient.scala
    │   │   ├── RedisAdStore.scala
    │   │   ├── RedisCampaignStore.scala
    │   │   ├── RedisCreativeStore.scala
    │   │   ├── RedisScreenStore.scala
    │   │   ├── RedisEventStore.scala
    │   │   └── RedisDecisionStore.scala
    │   ├── postgres/            # PostgreSQL implementations
    │   │   ├── PostgresClient.scala
    │   │   ├── PostgresAdStore.scala
    │   │   ├── PostgresCampaignStore.scala
    │   │   ├── PostgresCreativeStore.scala
    │   │   ├── PostgresScreenStore.scala
    │   │   ├── PostgresEventStore.scala
    │   │   └── PostgresUserStore.scala
    │   └── Hybrid*Store.scala  # Hybrid Redis+Postgres implementations
    ├── services/               # Business services
    │   ├── AdDeliveryService.scala      # Core ad delivery logic
    │   ├── TargetingService.scala       # (Re-exported from domain)
    │   ├── BudgetService.scala          # Budget constraint checking
    │   ├── FrequencyCapService.scala    # Frequency capping logic
    │   ├── CampaignBudgetService.scala   # Campaign-level budget
    │   ├── CampaignPlaylistService.scala # Playlist management
    │   ├── AnalyticsService.scala       # Analytics aggregation
    │   ├── MetricsService.scala         # Metrics collection
    │   ├── HealthService.scala          # Health check service
    │   ├── PlaylistService.scala         # Playlist operations
    │   ├── AuthService.scala            # User authentication
    │   ├── JwtService.scala             # JWT token generation/validation
    │   ├── PasswordService.scala        # Password hashing (BCrypt)
    │   ├── PasskeyService.scala         # Screen passkey generation
    │   └── MediaValidator.scala         # Media file validation
    ├── storage/                # Media storage implementations
    │   ├── MediaStorage.scala          # Storage interface
    │   ├── LocalFileStorage.scala      # Local filesystem storage
    │   └── MinIOStorage.scala           # MinIO object storage
    └── SeedDemoAds.scala       # Demo data seeding utility
```

### Key Files

#### `services/AdDeliveryService.scala`
**Purpose:** Core ad delivery engine. Filters ads through 3 stages and selects one.

**Filtering Pipeline:**
1. **Targeting Filter** - Matches ads to screen context
2. **Budget Filter** - Checks budget constraints
3. **Frequency Cap Filter** - Checks impression limits

**Key Methods:**
- `deliver(request: DeliveryRequest): Future[Option[DeliveryResponse]]`
- `pickAd(ads: List[Ad], screenClassification: Int): Option[Ad]` (weighted selection)

#### `store/redis/RedisAdStore.scala`
**Purpose:** Redis-based ad storage using JSON serialization.

**Operations:**
- `upsert(ad: Ad): Future[Unit]`
- `getById(id: String): Future[Option[Ad]]`
- `listActive(): Future[List[Ad]]`
- `listAll(): Future[List[Ad]]`

#### `store/postgres/PostgresAdStore.scala`
**Purpose:** PostgreSQL-based ad storage with relational data.

**Features:**
- Persistent storage
- Foreign key relationships
- Transaction support

#### `services/BudgetService.scala`
**Purpose:** Validates ad budget constraints (max_plays, daily_limit, hourly_limit).

#### `services/FrequencyCapService.scala`
**Purpose:** Enforces frequency capping (max impressions per device/user).

#### `services/PasskeyService.scala`
**Purpose:** Generates cryptographically secure passkeys for screen authentication.

**Method:**
- `generatePasskey(): String` - Returns Base64-encoded 32-byte random passkey

---

## Module: `engine-api`

**Purpose:** HTTP API layer using Pekko HTTP. Defines routes, middleware, and request/response handling.

### Structure

```
engine-api/
└── src/main/scala/mnemocast/engine/api/
    ├── HttpServer.scala         # Main server entry point
    ├── json/
    │   └── JsonSupport.scala     # Circe JSON codecs
    ├── middleware/
    │   ├── AuthMiddleware.scala      # JWT user authentication
    │   └── ScreenAuthMiddleware.scala # Screen passkey authentication
    ├── routes/                  # HTTP route definitions
    │   ├── AdRoute.scala            # Generic ad delivery
    │   ├── AdminAdRoutes.scala      # Admin ad management
    │   ├── ScreenAdRoutes.scala     # Screen-specific ad delivery
    │   ├── ScreenRoutes.scala       # Screen CRUD operations
    │   ├── CampaignRoutes.scala     # Campaign management
    │   ├── CreativeRoutes.scala     # Creative management
    │   ├── PlaylistRoutes.scala     # Playlist management
    │   ├── AnalyticsRoutes.scala    # Analytics endpoints
    │   ├── MetricsRoutes.scala      # Metrics endpoints
    │   ├── EventRoutes.scala        # Event tracking
    │   ├── MediaRoutes.scala        # Media upload/download
    │   ├── AuthRoutes.scala         # Authentication endpoints
    │   ├── HealthRoutes.scala       # Health check
    │   └── DocsRoutes.scala         # API documentation
    └── RequestLogging.scala     # Request logging utility
```

### Key Files

#### `HttpServer.scala`
**Purpose:** Main application entry point. Sets up HTTP server, routes, CORS, and dependency injection.

**Key Responsibilities:**
- Initialize storage (Redis/Postgres/Hybrid)
- Wire up services and stores
- Configure CORS headers
- Set up route handlers
- Start HTTP server on port 8080

**Storage Strategy:**
- `STORAGE_STRATEGY` environment variable
- Options: `redis`, `postgres`, `hybrid` (default: `redis`)

#### `middleware/AuthMiddleware.scala`
**Purpose:** JWT-based authentication for admin/dashboard endpoints.

**Usage:**
```scala
requireAuth { user =>
  // Protected route
}
```

#### `middleware/ScreenAuthMiddleware.scala`
**Purpose:** Passkey-based authentication for screen endpoints.

**Headers Required:**
- `X-Screen-Id`
- `X-Screen-Passkey`

**Usage:**
```scala
ScreenAuthMiddleware.authenticate(screenStore) { screen =>
  // Protected route
}
```

#### `routes/ScreenAdRoutes.scala`
**Purpose:** Ad delivery endpoints for screen clients.

**Endpoints:**
- `GET /api/v1/screens/{screenId}/ads/deliver` - Single ad delivery
- `GET /api/v1/screens/{screenId}/ads/batch?count=N` - Batch ad delivery

**Response Format:**
```json
{
  "id": "ad-123",
  "title": "Ad Title",
  "type": "image|video",
  "contentUrl": "http://...",
  "duration": 30,
  "startTime": "2025-12-19T12:00:00Z",
  "endTime": "2025-12-19T12:00:30Z",
  "priority": 5,
  "metadata": {
    "campaignId": "campaign-123",
    "targetAudience": "all"
  }
}
```

#### `routes/ScreenRoutes.scala`
**Purpose:** Screen management endpoints.

**Endpoints:**
- `POST /api/v1/screens/register` - Register new screen (returns passkey)
- `PUT /api/v1/screens/{screenId}/heartbeat` - Screen heartbeat
- `GET /api/v1/screens/{screenId}` - Get screen details
- `GET /api/v1/screens/{screenId}/status` - Get screen status
- `PUT /api/v1/screens/{screenId}` - Update screen (admin)
- `DELETE /api/v1/screens/{screenId}` - Delete screen (admin)
- `GET /api/v1/screens` - List all screens (admin)

---

## Dashboard (`/dashboard`)

**Purpose:** React TypeScript frontend for admin management and analytics.

### Structure

```
dashboard/
├── package.json              # NPM dependencies
├── tsconfig.json             # TypeScript configuration
├── tailwind.config.js        # Tailwind CSS configuration
├── public/                   # Static assets
│   ├── index.html
│   ├── favicon.ico
│   └── manifest.json
├── src/
│   ├── App.tsx               # Main app component
│   ├── index.tsx             # React entry point
│   ├── index.css             # Global styles
│   ├── components/           # Reusable components
│   │   ├── Layout.tsx        # Main layout wrapper
│   │   ├── ProtectedRoute.tsx # Auth-protected routes
│   │   ├── ErrorBoundary.tsx  # Error handling
│   │   ├── FileUpload.tsx     # File upload component
│   │   ├── Logo.tsx           # Logo component
│   │   ├── Icons.tsx           # Icon components
│   │   └── Modal components:
│   │       ├── CreateCampaignModal.tsx
│   │       ├── CreateCreativeModal.tsx
│   │       ├── CreateScreenModal.tsx
│   │       ├── EditCampaignModal.tsx
│   │       ├── EditCreativeModal.tsx
│   │       └── EditScreenModal.tsx
│   ├── pages/                # Page components
│   │   ├── Login.tsx         # Login page
│   │   ├── Signup.tsx        # Signup page
│   │   ├── Dashboard.tsx     # Main dashboard
│   │   ├── Campaigns.tsx     # Campaign list
│   │   ├── CampaignDetail.tsx # Campaign details
│   │   ├── Creatives.tsx     # Creative list
│   │   ├── Screens.tsx       # Screen list
│   │   ├── Analytics.tsx     # Analytics dashboard
│   │   ├── Playlist.tsx       # Playlist management
│   │   ├── MediaUpload.tsx   # Media upload page
│   │   └── Monitoring.tsx     # System monitoring
│   ├── services/             # API client services
│   │   ├── api.ts            # API client (axios)
│   │   └── authService.ts    # Authentication service
│   ├── contexts/             # React contexts
│   │   └── AuthContext.tsx   # Authentication context
│   ├── types/                # TypeScript type definitions
│   │   ├── index.ts          # Shared types
│   │   └── auth.ts           # Auth types
│   └── utils/                # Utility functions
│       └── urlTransform.ts   # URL transformation utilities
├── build/                    # Production build output
└── node_modules/             # NPM dependencies
```

### Key Files

#### `src/App.tsx`
**Purpose:** Main application component with routing.

#### `src/services/api.ts`
**Purpose:** Axios-based API client with authentication headers.

#### `src/contexts/AuthContext.tsx`
**Purpose:** React context for authentication state management.

---

## Documentation (`/docs`)

**Purpose:** Comprehensive project documentation.

### Key Documentation Files

- `01-domain-model.md` - Domain model reference
- `02-api-spec.md` - Complete API specification
- `ARCHITECTURE.md` - System architecture
- `RUNNING.md` - Setup and running instructions
- `CURL_TESTING.md` - API testing guide
- `WHY_NO_ADS_DIAGNOSTIC.md` - Troubleshooting guide
- `SCREEN_AUTHENTICATION_IMPLEMENTATION.md` - Screen auth docs
- `ENHANCED_AD_RESPONSE.md` - Ad response format docs
- `POSTGRES_SETUP.md` - Database setup guide
- `MINIO_CONFIGURATION.md` - MinIO setup guide

---

## Infrastructure (`/infra`)

**Purpose:** Infrastructure configuration files.

### Structure

```
infra/
├── docker/                   # Docker configurations
├── k8s/                      # Kubernetes configurations
└── local-dev/                # Local development setup
    └── postgres/
        ├── init.sql          # Database schema initialization
        ├── seed_campaigns.sql # Campaign seed data
        └── seed_demo_data.sql # Complete demo data seed
```

### Key Files

#### `local-dev/postgres/init.sql`
**Purpose:** PostgreSQL schema definition.

**Tables:**
- `users` - User accounts
- `screens` - Digital screens
- `campaigns` - Advertising campaigns
- `ads` - Advertisements
- `creatives` - Creative media
- `targeting_rules` - Ad targeting rules
- `screen_tags` - Screen tags
- `screen_metadata` - Screen metadata
- `delivery_events` - Event tracking
- `playlists` - Playlists
- `playlist_items` - Playlist items

#### `local-dev/postgres/seed_demo_data.sql`
**Purpose:** Seeds database with demo data for testing.

**Includes:**
- Sample users
- Sample screens with passkeys
- Sample campaigns
- Sample ads with targeting rules
- Universal fallback ads (no targeting)

---

## Scripts (`/scripts`)

**Purpose:** Utility scripts for development and deployment.

### Scripts

- `dev-run.sh` - Development server startup
- `health-check.sh` - Health check script
- `setup-postgres.sh` - PostgreSQL setup
- `seed-demo-data.sh` - Seed demo data
- `seed-comprehensive-demo.sh` - Comprehensive seeding
- `test-ooh-endpoints.sh` - OOH endpoint testing
- `load-test-k6.js` - Load testing script (k6)

---

## Storage (`/storage`)

**Purpose:** Local file storage for uploaded media.

### Structure

```
storage/
└── uploads/
    └── creatives/            # Uploaded ad creatives
```

**Note:** In production, use MinIO or cloud storage. Local storage is for development only.

---

## Authentication Architecture

### User Authentication (Admin/Dashboard)
- **Method:** JWT tokens
- **Middleware:** `AuthMiddleware`
- **Endpoints:** All `/admin/*` and dashboard endpoints
- **Header:** `Authorization: Bearer <token>`

### Screen Authentication (Screen Clients)
- **Method:** Passkey-based
- **Middleware:** `ScreenAuthMiddleware`
- **Endpoints:** Screen-specific endpoints (`/api/v1/screens/{id}/*`)
- **Headers:**
  - `X-Screen-Id: <screen-id>`
  - `X-Screen-Passkey: <passkey>`

---

## Running the Application

### Backend
```bash
cd backend
sbt run
```

**Environment Variables:**
- `STORAGE_STRATEGY` - `redis` | `postgres` | `hybrid` (default: `redis`)

### Frontend
```bash
cd dashboard
npm install
npm start
```

### Database Setup
```bash
# Initialize PostgreSQL
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql

# Seed demo data
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/seed_demo_data.sql
```

---

## Data Flow

### Ad Delivery Flow

1. **Screen Request** → `ScreenAdRoutes.deliver()`
2. **Authentication** → `ScreenAuthMiddleware.authenticate()`
3. **Build Request** → `DeliveryRequest` with screen context
4. **Ad Delivery** → `AdDeliveryService.deliver()`
5. **Filtering:**
   - Targeting → `TargetingService.matches()`
   - Budget → `BudgetService.isWithinBudget()`
   - Frequency Cap → `FrequencyCapService.canShow()`
6. **Selection** → Weighted random selection
7. **Response** → `ScreenClientAdResponse` JSON
8. **Event Logging** → `EventStore.append()`

---

## Key Design Patterns

1. **Layered Architecture**
   - Domain (models, business logic)
   - Infrastructure (storage, services)
   - API (HTTP routes, middleware)

2. **Dependency Injection**
   - Manual DI in `HttpServer.scala`
   - Services injected via constructors

3. **Repository Pattern**
   - Store interfaces (`AdStore`, `ScreenStore`, etc.)
   - Multiple implementations (Redis, Postgres, Hybrid)

4. **Service Layer**
   - Business logic in services
   - Pure functions where possible

5. **Middleware Pattern**
   - Authentication middleware
   - CORS handling
   - Request logging

---

## Notes

- **Target Directories:** `target/` directories contain compiled classes and are generated by SBT
- **Build Output:** JAR files are in `target/scala-2.13/`
- **Storage Strategy:** Can switch between Redis, Postgres, or Hybrid via environment variable
- **Media Storage:** Supports both local filesystem and MinIO object storage
- **Authentication:** Dual authentication system (JWT for users, passkey for screens)

---

**Generated:** 2025-12-19  
**Project Version:** 0.1.0-SNAPSHOT

