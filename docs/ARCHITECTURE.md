#  Mnemocast Architecture

**Version:** 1.0.0  
**Last Updated:** December 2024

---

##  System Overview

Mnemocast is an enterprise-ready OOH (Out-of-Home) ad serving platform built with a microservices-ready architecture, designed for scalability, reliability, and performance.

```
┌─────────────────────────────────────────────────────────────┐
│                      Client Applications                      │
│  (Web Dashboard, Mobile Apps, Screen Players, API Clients)  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        │ HTTP/REST API
                        │
┌───────────────────────▼─────────────────────────────────────┐
│                    API Gateway Layer                         │
│  ┌──────────────────────────────────────────────────────┐   │
│  │  Pekko HTTP Server (Port 8080)                      │   │
│  │  - Request Routing                                   │   │
│  │  - CORS Handling                                     │   │
│  │  - Request Logging                                   │   │
│  │  - Health Checks                                     │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│   Domain     │ │   Infra      │ │    API      │
│   Layer      │ │   Layer      │ │   Layer     │
│              │ │              │ │             │
│ - Models     │ │ - Stores     │ │ - Routes    │
│ - Services   │ │ - Storage    │ │ - JSON      │
│ - Logic      │ │ - Clients    │ │ - HTTP      │
└───────┬──────┘ └──────┬──────┘ └──────┬──────┘
        │               │               │
        └───────────────┼───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
┌───────▼──────┐ ┌──────▼──────┐ ┌──────▼──────┐
│    Redis     │ │  PostgreSQL │ │    MinIO    │
│   (Cache)    │ │  (Database) │ │  (Storage)  │
└──────────────┘ └─────────────┘ └─────────────┘
```

---

##  Component Architecture

### **1. Domain Layer** (`engine-domain`)
**Purpose:** Pure business logic, no dependencies on infrastructure

**Components:**
- **Models:** `Campaign`, `Creative`, `Screen`, `Playlist`, `DeliveryEvent`
- **Services:** Pure business logic (targeting, playlist generation)
- **No Dependencies:** No HTTP, no database, no external services

**Key Files:**
- `model/Campaign.scala`
- `model/Creative.scala`
- `model/Screen.scala`
- `model/Analytics.scala`
- `model/Health.scala`
- `model/Metrics.scala`
- `model/ErrorResponse.scala`

---

### **2. Infrastructure Layer** (`engine-infra`)
**Purpose:** External integrations and data persistence

**Components:**

#### **Stores (Data Access)**
- `AdStore` - Ad data access
- `CampaignStore` - Campaign data access
- `CreativeStore` - Creative data access
- `ScreenStore` - Screen data access
- `EventStore` - Event logging
- `DecisionStore` - Decision tracking

**Storage Strategies:**
- **Redis:** Fast, in-memory (default)
- **PostgreSQL:** Persistent, ACID-compliant
- **Hybrid:** Redis cache + PostgreSQL persistence (recommended)

#### **Services**
- `AnalyticsService` - Analytics calculations
- `HealthService` - Health check logic
- `MetricsService` - Metrics collection
- `PlaylistService` - Playlist generation
- `AdDeliveryService` - Ad delivery logic
- `BudgetService` - Budget management
- `FrequencyCapService` - Frequency capping

#### **Storage**
- `MediaStorage` - Media file storage (MinIO/Local)
- `MediaValidator` - Media validation

**Key Files:**
- `store/redis/` - Redis implementations
- `store/postgres/` - PostgreSQL implementations
- `store/Hybrid*.scala` - Hybrid implementations
- `services/AnalyticsService.scala`
- `services/HealthService.scala`
- `services/MetricsService.scala`

---

### **3. API Layer** (`engine-api`)
**Purpose:** HTTP API, routing, JSON serialization

**Components:**

#### **Routes**
- `HealthRoutes` - `/api/v1/health`, `/api/v1/ready`, `/api/v1/live`
- `MetricsRoutes` - `/api/v1/metrics`
- `AnalyticsRoutes` - `/api/v1/analytics/*`
- `CampaignRoutes` - `/api/v1/campaigns/*`
- `CreativeRoutes` - `/api/v1/creatives/*`
- `ScreenRoutes` - `/api/v1/screens/*`
- `PlaylistRoutes` - `/api/v1/screens/{id}/playlist`
- `EventRoutes` - `/api/v1/events/*`
- `MediaRoutes` - `/api/v1/media/*`
- `DocsRoutes` - `/api/docs` (Swagger UI)

#### **Middleware**
- Request logging
- Response time tracking
- Error handling
- CORS support

**Key Files:**
- `HttpServer.scala` - Main server entry point
- `routes/*.scala` - Route definitions
- `json/JsonSupport.scala` - JSON marshalling

---

##  Data Flow

### **Playlist Generation Flow**
```
1. Screen requests playlist
   GET /api/v1/screens/{screenId}/playlist
   
2. API Layer (PlaylistRoutes)
   - Validates screen exists
   - Extracts screen metadata
   
3. Domain Layer (PlaylistService)
   - Finds matching campaigns (targeting rules)
   - Filters by budget and frequency caps
   - Applies weight-based selection
   - Generates playlist
   
4. Infrastructure Layer
   - Reads campaigns from Store
   - Reads creatives from Store
   - Checks budget from EventStore
   - Applies frequency caps
   
5. Response
   - Returns playlist JSON
   - Screen plays creatives
```

### **Analytics Flow**
```
1. Request analytics
   GET /api/v1/analytics/dashboard
   
2. API Layer (AnalyticsRoutes)
   - Validates parameters
   - Calls AnalyticsService
   
3. Domain Layer (AnalyticsService)
   - Aggregates events from EventStore
   - Calculates metrics
   - Formats response
   
4. Response
   - Returns analytics JSON
   - Frontend displays charts
```

---

##  Data Storage

### **Redis (Cache Layer)**
- **Purpose:** Fast data access, caching
- **Data:** Campaigns, Creatives, Screens (hot data)
- **TTL:** Configurable per key
- **Use Cases:** Read-heavy operations, real-time data

### **PostgreSQL (Persistent Storage)**
- **Purpose:** Data durability, complex queries
- **Data:** All entities, events, decisions
- **Use Cases:** Analytics queries, data persistence, reporting

### **MinIO (Object Storage)**
- **Purpose:** Media file storage
- **Data:** Creative media files (images, videos)
- **Compatibility:** S3-compatible API
- **Use Cases:** Media upload, media serving

---

##  API Endpoints

### **Core Endpoints**
- `GET /api/v1/screens/{id}/playlist` - Generate playlist
- `POST /api/v1/screens/register` - Register screen
- `POST /api/v1/events/play` - Record play event

### **Management Endpoints**
- `GET /api/v1/campaigns` - List campaigns
- `POST /api/v1/campaigns` - Create campaign
- `GET /api/v1/creatives` - List creatives
- `POST /api/v1/creatives` - Create creative

### **Analytics Endpoints**
- `GET /api/v1/analytics/dashboard` - Dashboard metrics
- `GET /api/v1/analytics/campaigns/roi` - ROI metrics
- `GET /api/v1/analytics/screens` - Screen performance
- `GET /api/v1/analytics/creatives` - Creative performance
- `GET /api/v1/analytics/geographic` - Geographic analytics

### **Monitoring Endpoints**
- `GET /api/v1/health` - System health
- `GET /api/v1/metrics` - System metrics
- `GET /api/v1/ready` - Readiness probe
- `GET /api/v1/live` - Liveness probe

### **Documentation**
- `GET /api/docs` - Swagger UI
- `GET /api/docs/openapi.yaml` - OpenAPI spec

---

##  Frontend Architecture

### **Technology Stack**
- **React 18** - UI framework
- **TypeScript** - Type safety
- **Chart.js** - Data visualization
- **Tailwind CSS** - Styling
- **React Router** - Navigation

### **Pages**
- **Dashboard** - Overview metrics
- **Campaigns** - Campaign management
- **Creatives** - Creative management
- **Screens** - Screen management
- **Analytics** - Analytics dashboard (7 tabs)
- **Monitoring** - System monitoring

### **API Integration**
- Centralized API client (`services/api.ts`)
- Error handling
- Request/response interceptors

---

##  Security

### **Current Implementation**
- CORS configuration
- Request logging with IP tracking
- Input validation
- Media file validation

### **Future Enhancements**
- API key authentication
- Rate limiting (Redis-based)
- JWT tokens
- Role-based access control

---

##  Performance Characteristics

### **Response Times**
- Health check: < 10ms
- Playlist generation: < 100ms
- Analytics queries: < 200ms
- Campaign list: < 100ms

### **Throughput**
- Playlist requests: ~2,500/sec
- Health checks: ~30,000/sec
- Analytics queries: ~1,200/sec

### **Scalability**
- Horizontal scaling supported
- Stateless API design
- Shared state via Redis/PostgreSQL
- Load balancing ready

---

##  Deployment Architecture

### **Development**
```
Local Machine
├── Backend (sbt run)
├── Frontend (npm start)
├── Redis (Docker)
├── PostgreSQL (Docker)
└── MinIO (Docker)
```

### **Production**
```
Kubernetes Cluster
├── Backend Pods (3+ replicas)
├── Frontend Pods (2+ replicas)
├── Redis Cluster
├── PostgreSQL (Primary + Replicas)
└── MinIO Cluster
```

---

##  Technology Stack

### **Backend**
- **Language:** Scala 2.13
- **Framework:** Pekko HTTP 1.0
- **JSON:** Circe
- **Database:** PostgreSQL (via JDBC)
- **Cache:** Redis (via Jedis)
- **Storage:** MinIO (S3-compatible)

### **Frontend**
- **Language:** TypeScript
- **Framework:** React 18
- **Charts:** Chart.js
- **Styling:** Tailwind CSS
- **Build:** Create React App

### **Infrastructure**
- **Containerization:** Docker
- **Orchestration:** Kubernetes
- **Monitoring:** Built-in metrics endpoint
- **Documentation:** Swagger/OpenAPI

---

##  Scalability Considerations

### **Horizontal Scaling**
-  Stateless API design
-  Shared state via Redis/PostgreSQL
-  Load balancer ready
-  Kubernetes deployment ready

### **Vertical Scaling**
-  Efficient resource usage
-  Connection pooling
-  Async request handling

### **Database Scaling**
-  Read replicas for PostgreSQL
-  Redis cluster support
-  Query optimization

---

##  Monitoring & Observability

### **Health Checks**
- Component-level health
- System status aggregation
- Kubernetes probes

### **Metrics**
- Request counts
- Response times (avg, p95, p99)
- Error rates
- Active entities count

### **Logging**
- Structured logging
- Request ID tracking
- Log levels (INFO, WARN, ERROR)

---

##  Additional Documentation

- **API Documentation:** `/api/docs` (Swagger UI)
- **Performance Benchmarks:** `docs/PERFORMANCE_BENCHMARKS.md`
- **Deployment Guide:** `docs/DEPLOYMENT_GUIDE.md`
- **Demo Script:** `docs/DEMO_SCRIPT.md`

---

**Last Updated:** December 2024

