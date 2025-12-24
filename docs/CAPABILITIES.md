# Mnemocast Engine — System Capabilities

A comprehensive overview of what the Mnemocast Ad Serving Engine can do.

---

##  Core Capabilities

### 1. Intelligent Ad Delivery

**What it does:**
- Serves ads to clients in real-time via HTTP API
- Selects the most appropriate ad from eligible candidates
- Filters ads based on multiple criteria before selection
- Returns ad metadata including creative URLs and tracking endpoints

**How it works:**
1. Client requests an ad with context (device, user, location, platform)
2. System evaluates all active ads against targeting rules
3. System checks budget constraints and frequency caps
4. System randomly selects from eligible ads
5. Returns ad details with tracking URLs

**Key Features:**
-  Real-time ad serving (< 100ms typical response time)
-  Multi-criteria filtering (targeting + budget + frequency)
-  Automatic ad exclusion when constraints are met
-  Tracking URL generation for impressions and clicks

---

### 2. Advanced Targeting

**What it does:**
- Filters ads based on request context
- Supports multiple targeting criteria simultaneously
- Uses flexible rule-based matching

**Supported Targeting Criteria:**
- **Geographic**: Country-based targeting
- **Platform**: Device platform (Android, iOS, Web)
- **Device**: Specific device targeting
- **User**: User-specific targeting
- **Application**: App-specific targeting

**Targeting Operators:**
- **`eq`**: Exact match (case-insensitive)
- **`in`**: List membership (comma-separated values)

**Example Use Cases:**
- Show ads only to users in India: `{"key": "country", "operator": "eq", "value": "IN"}`
- Show ads on mobile platforms: `{"key": "platform", "operator": "in", "value": "android,ios"}`
- Target specific device: `{"key": "deviceId", "operator": "eq", "value": "device-123"}`

**Behavior:**
- Multiple rules use AND logic (all must pass)
- No targeting rules = show everywhere (default)
- Rules are evaluated in real-time for each request

---

### 3. Budget Management & Control

**What it does:**
- Enforces spending limits at multiple time granularities
- Automatically prevents over-delivery
- Tracks budget consumption in real-time
- Automatically excludes ads when budgets are exhausted

**Budget Types:**

1. **Total Budget (`maxPlays`)**
   - Global limit across all time
   - Never resets
   - Example: "Show this ad 10,000 times total"

2. **Daily Budget (`dailyLimit`)**
   - Maximum plays per day
   - Resets at start of each day (UTC)
   - Example: "Show this ad 500 times per day"

3. **Hourly Budget (`hourlyLimit`)**
   - Maximum plays per hour
   - Resets at start of each hour (UTC)
   - Example: "Show this ad 50 times per hour"

**Key Features:**
-  Multiple budget constraints per ad
-  Automatic budget exhaustion handling
-  Real-time budget checking
-  UTC-based time calculations
-  Optional budgets (null = unlimited)

**Use Cases:**
- Control total campaign spend: `maxPlays: 10000`
- Distribute delivery over time: `dailyLimit: 100, hourlyLimit: 10`
- Prevent budget overruns: Automatic exclusion when limits reached

---

### 4. Frequency Capping

**What it does:**
- Limits how often an ad is shown to the same device or user
- Prevents ad fatigue and improves user experience
- Tracks impressions per device/user within time windows

**Capping Types:**

1. **Per-Device Capping (`maxImpressionsPerDevice`)**
   - Maximum impressions per device
   - Example: "Show this ad max 5 times per device"

2. **Per-User Capping (`maxImpressionsPerUser`)**
   - Maximum impressions per user
   - Example: "Show this ad max 3 times per user"

3. **Time Window (`frequencyCapWindowHours`)**
   - Rolling time window for frequency calculation
   - Default: 24 hours
   - Example: "Within the last 24 hours"

**Key Features:**
-  Device-level and user-level capping
-  Configurable time windows
-  Both caps must be satisfied (AND logic)
-  Real-time frequency checking
-  Automatic exclusion when caps reached

**Use Cases:**
- Prevent ad spam: `maxImpressionsPerDevice: 3, frequencyCapWindowHours: 24`
- Improve user experience: Limit repeated ads to same user
- Optimize campaign reach: Ensure ads are seen by more unique users

---

### 5. Analytics & Performance Reporting

**What it does:**
- Calculates performance metrics for ads and campaigns
- Provides real-time analytics dashboards
- Supports time-range filtering
- Identifies top-performing ads

**Metrics Calculated:**

1. **Impressions**
   - Total number of times ad was shown
   - Filtered by time range if specified

2. **Impression Metrics**
   - Total number of impressions per ad
   - Filtered by time range if specified

**Analytics Endpoints:**

1. **Ad Performance** (`/api/v1/analytics/ads/{adId}`)
   - Metrics for a specific ad
   - Optional time range filtering
   - Returns: impressions

2. **Campaign Performance** (`/api/v1/analytics/campaigns`)
   - Metrics for all campaigns (ads)
   - Optional time range filtering
   - Returns: Aggregated metrics per campaign

3. **Dashboard** (`/api/v1/analytics/dashboard`)
   - Overall system metrics
   - Top performing ads
   - Recent activity feed
   - Returns: Summary with key KPIs

**Key Features:**
-  Real-time metric calculation
-  Time-range filtering (start/end dates)
-  Top performers identification
-  Recent activity tracking
-  JSON API responses

**Use Cases:**
- Monitor campaign performance: Track impressions
- Optimize ad selection: Identify high-performing ads
- Generate reports: Export metrics for stakeholders
- Make data-driven decisions: Use impression metrics to improve campaigns

---

### 7. Event Logging & Storage

**What it does:**
- Logs all ad delivery events (impressions, clicks)
- Stores events with full metadata
- Enables historical analysis
- Supports querying by ad, time, device, user

**Event Types:**
- **Impression**: Ad was shown to a user
- **Click**: User clicked on an ad

**Event Metadata:**
- Device ID
- User ID
- IP Address
- Country
- Platform
- Timestamp
- Request ID

**Key Features:**
-  Immutable event log (append-only)
-  Full metadata capture
-  Fast queries by ad ID
-  Time-based filtering
-  Device/user-based queries for frequency capping

**Use Cases:**
- Audit trail: Track all ad deliveries
- Debugging: Investigate delivery issues
- Analytics: Calculate metrics from events
- Compliance: Maintain delivery records

---

### 8. Admin & Management

**What it does:**
- Create and manage ads via REST API
- List and query ads
- View event history
- Control ad activation status

**Admin Capabilities:**

1. **Ad Creation** (`POST /admin/ads`)
   - Create new ads with full configuration
   - Set targeting rules
   - Configure budgets and frequency caps
   - Set active/inactive status

2. **Ad Listing** (`GET /admin/ads`)
   - List all ads or only active ads
   - View ad configurations
   - Filter by active status

3. **Event History** (`GET /admin/ads/{adId}/events`)
   - View events for specific ad
   - Limit results for pagination
   - Debug delivery issues

**Key Features:**
-  RESTful API for management
-  JSON request/response format
-  Flexible ad configuration
-  Active/inactive status control
-  Event history access

---

##  Technical Capabilities

### Performance
- **Response Time**: < 100ms typical for ad delivery
- **Throughput**: Handles concurrent requests efficiently
- **Scalability**: Redis-based storage for fast access
- **Real-time**: All checks and calculations happen in real-time

### Reliability
- **Budget Enforcement**: Guaranteed budget limits (no over-delivery)
- **Frequency Capping**: Accurate per-device/user tracking
- **Event Logging**: All events are logged (no data loss)
- **Error Handling**: Graceful handling of edge cases

### Flexibility
- **Optional Constraints**: All budget/frequency fields are optional
- **Configurable Rules**: Flexible targeting rule system
- **Time Filtering**: Analytics support custom time ranges
- **Extensible**: Easy to add new features

### Data Management
- **Redis Storage**: Fast in-memory storage for ads and events
- **JSON Format**: Human-readable data format
- **Event Queries**: Fast queries by ad, time, device, user
- **Metadata Capture**: Rich metadata for all events

---

##  System Metrics & Monitoring

### What the System Tracks

1. **Delivery Metrics**
   - Total ads delivered
   - Active vs inactive ads
   - Delivery success rate

2. **Performance Metrics**
   - Impressions per ad
   - Top performing ads

3. **Budget Metrics**
   - Budget consumption
   - Budget exhaustion events
   - Daily/hourly delivery rates

4. **Frequency Metrics**
   - Impressions per device
   - Impressions per user
   - Frequency cap hits

---

##  Use Cases & Scenarios

### Scenario 1: Controlled Campaign Launch
**Goal**: Launch a campaign with controlled spending

**Configuration:**
```json
{
  "maxPlays": 10000,
  "dailyLimit": 1000,
  "hourlyLimit": 100
}
```

**Result**: Campaign delivers exactly 10,000 impressions total, max 1,000 per day, max 100 per hour

---

### Scenario 2: Prevent Ad Fatigue
**Goal**: Ensure users don't see the same ad too often

**Configuration:**
```json
{
  "maxImpressionsPerDevice": 3,
  "maxImpressionsPerUser": 2,
  "frequencyCapWindowHours": 24
}
```

**Result**: Each device sees ad max 3 times, each user max 2 times, within 24-hour window

---

### Scenario 3: Geographic Targeting
**Goal**: Show ads only to users in specific countries

**Configuration:**
```json
{
  "targetingRules": [
    {"key": "country", "operator": "in", "value": "IN,US,UK"}
  ]
}
```

**Result**: Ad only shown to users in India, USA, or UK

---

### Scenario 4: Performance Optimization
**Goal**: Identify and focus on high-performing ads

**Process:**
1. Run campaigns with multiple ads
2. Query analytics dashboard: `GET /api/v1/analytics/dashboard`
3. Identify top performers by impressions
4. Increase budget for high-performing ads
5. Pause or reduce budget for low-performing ads

---

### Scenario 5: Multi-Platform Campaign
**Goal**: Show ads on mobile platforms only

**Configuration:**
```json
{
  "targetingRules": [
    {"key": "platform", "operator": "in", "value": "android,ios"}
  ]
}
```

**Result**: Ad only shown on Android and iOS devices

---

##  What Makes This System Powerful

1. **Intelligent Filtering**: Multi-layer filtering ensures right ad to right user
2. **Budget Control**: Automatic budget enforcement prevents over-spending
3. **User Experience**: Frequency capping prevents ad fatigue
4. **Data-Driven**: Analytics enable optimization decisions
5. **Real-Time**: All operations happen in real-time
6. **Flexible**: Optional constraints allow maximum flexibility
7. **Scalable**: Redis-based architecture supports growth
8. **Complete**: End-to-end solution from delivery to analytics

---

##  Current Limitations (MVP)

- Single ad per campaign (no campaign grouping yet)
- No advanced targeting (time-based, geographic coordinates)
- No revenue/ROI tracking
- No export functionality (CSV/JSON)
- No weighted ad selection (pure random)
- No A/B testing support

**Note**: See `docs/Epic3.md` for planned enhancements.

---

##  Getting Started

1. **Quick Start**: See `QUICKSTART.md`
2. **API Reference**: See `docs/02-api-spec.md`
3. **Features**: See `docs/FEATURES.md`
4. **Running Guide**: See `docs/RUNNING.md`

---

**The Mnemocast Engine is a production-ready ad serving platform with intelligent targeting, budget control, frequency capping, and comprehensive analytics.**

