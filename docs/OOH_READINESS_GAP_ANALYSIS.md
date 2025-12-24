# OOH Ad Serving Readiness - Gap Analysis

## Current System Status

###  What Works
- Basic ad delivery (single ad at a time)
- Ad management (create, list, update)
- Budget management (maxPlays, dailyLimit, hourlyLimit)
- Frequency capping (per device/user)
- Impression tracking
- Analytics
- **OOH Screen Management** (Phase 1 )
- **OOH Playlist Generation** (Phase 1 )
- **Time-Based Targeting (Dayparts)** (Phase 1 )
- **Enhanced Location Targeting** (Phase 1 )

###  Critical Gaps for OOH

#### 1. Screen/Placement Management
**Status:**  **IMPLEMENTED**  
**Impact:** HIGH - Cannot identify or manage physical screens  
**Implemented:**
-  Screen registration API (`POST /api/v1/screens/register`)
-  Screen lookup API (`GET /api/v1/screens/{screenId}`)
-  Screen listing API (`GET /api/v1/screens`)
-  Screen heartbeat API (`PUT /api/v1/screens/{screenId}/heartbeat`)
-  Screen model with location, tags, metadata (`Screen`, `ScreenLocation`)
-  Screen store (`ScreenStore` interface, `RedisScreenStore` implementation)

#### 2. Playlist Generation
**Status:**  **IMPLEMENTED**  
**Impact:** CRITICAL - OOH requires playlists, not single ads  
**Implemented:**
-  Playlist API (`GET /api/v1/screens/{screenId}/playlist?durationMinutes=X`)
-  Playlist generation service (`PlaylistService`)
-  Duration-based ad selection
-  `durationSeconds` field added to `Ad` model
-  Weighted/priority-based scheduling (basic random selection implemented, advanced weighting pending)

#### 3. Campaign/Creative Separation
**Status:** Not implemented  
**Impact:** MEDIUM - Ads are standalone, not grouped  
**Required:**
- Campaign model (groups multiple creatives)
- Creative model (individual assets)
- Campaign-to-Creative relationship
- Campaign-level targeting and budget

#### 4. Time-Based Targeting (Dayparts)
**Status:**  **IMPLEMENTED**  
**Impact:** HIGH - Critical for OOH scheduling  
**Implemented:**
-  TimeBand model (start/end times)
-  Day-of-week targeting (via `daysOfWeek` field)
-  Time window matching logic (`TimeTargetingService`)
-  Timezone handling (via `timezone` field in `DeliveryRequest`)
-  Integration with `TargetingService` via `daypart`/`timeband` operator

#### 5. Enhanced Location Targeting
**Status:**  **IMPLEMENTED**  
**Impact:** MEDIUM - OOH needs city/area/venue targeting  
**Implemented:**
-  City targeting (via `city` field in `DeliveryRequest` and `ScreenLocation`)
-  Area/neighborhood targeting (via `area` field)
-  Venue type tags (via `venueType` field and screen `tags`)
-  Screen tag matching (stored in `Screen` model, available for targeting)
-  Extended `TargetingService` to support `city`, `area`, `venueType`, `screenId`, `timezone` keys

#### 6. Playlist Response Format
**Status:**  **IMPLEMENTED**  
**Impact:** CRITICAL - Current API returns single ad  
**Implemented:**
-  PlaylistResponse model (with `requestId`, `screenId`, `items`, `validForSeconds`, `totalDurationSeconds`)
-  PlaylistItem model (with `adId`, `creativeUrl`, `targetUrl`, `durationSeconds`, `impressionTrackingUrl`, `position`)
-  Multiple ads in sequence
-  ValidForSeconds field (default 5 minutes, configurable)

## Recommended Implementation Priority

### Phase 1: Core OOH Features (2-3 weeks)
1. **Screen Management**
   - Screen model and store
   - Registration and lookup APIs
   - Basic location/tag support

2. **Playlist Generation**
   - Playlist service
   - Duration-based selection
   - Basic priority/weighting

3. **Enhanced Targeting**
   - City/area targeting
   - Time-based targeting (dayparts)
   - Screen tag matching

### Phase 2: Advanced Features (1-2 weeks)
4. **Campaign/Creative Structure**
   - Campaign model
   - Creative model
   - Relationship management

5. **Advanced Scheduling**
   - Weighted random selection
   - Share-of-voice distribution
   - Advanced frequency capping

## Migration Path  **COMPLETED**

The current system has been extended rather than rebuilt:

1.  **Extended existing Ad model:**
   - Added `durationSeconds` field for playlist generation
   - Existing targeting rules now support OOH fields via extended keys

2.  **Added new models:**
   - `Screen` - Physical OOH display device/player
   - `ScreenLocation` - Location information (city, area, venueType, timezone)
   - `PlaylistResponse` - Playlist output format
   - `PlaylistItem` - Individual ad in playlist
   - `TimeBand` - Time window for daypart targeting
   - `CreateScreenRequest` - Screen registration request

3.  **Extended DeliveryRequest:**
   - Added `screenId`, `city`, `area`, `venueType`, `timezone` fields

4.  **Created new services:**
   - `PlaylistService` - Generates playlists for OOH screens
   - `TimeTargetingService` - Handles time-based targeting logic
   - Extended `TargetingService` - Supports OOH-specific targeting keys

5.  **Extended APIs:**
   - `AdRoutes` - Now accepts OOH parameters (screenId, city, area, venueType, timezone)
   - `ScreenRoutes` - New routes for screen management
   - `PlaylistRoutes` - New routes for playlist generation

## Implementation Summary

### Phase 1: Core OOH Features  **COMPLETED**
1.  **Screen Management**
   - Screen model and store (`Screen`, `ScreenLocation`, `ScreenStore`, `RedisScreenStore`)
   - Registration and lookup APIs (`POST /api/v1/screens/register`, `GET /api/v1/screens/{screenId}`)
   - Basic location/tag support (city, area, venueType, tags, timezone)

2.  **Playlist Generation**
   - Playlist service (`PlaylistService`)
   - Duration-based selection
   - Playlist models (`PlaylistResponse`, `PlaylistItem`)
   - API endpoint (`GET /api/v1/screens/{screenId}/playlist`)

3.  **Enhanced Targeting**
   - City/area targeting (via `DeliveryRequest` extensions)
   - Time-based targeting (dayparts via `TimeBand` and `TimeTargetingService`)
   - Screen tag matching (stored in `Screen` model)
   - Extended `TargetingService` for OOH-specific fields

### Phase 2: Advanced Features  **PENDING**
4. **Campaign/Creative Structure**
   - Campaign model (groups multiple creatives)
   - Creative model (individual assets)
   - Campaign-to-Creative relationship
   - Campaign-level targeting and budget

5. **Advanced Scheduling**
   - Weighted random selection (currently basic random)
   - Share-of-voice distribution
   - Advanced frequency capping

## Conclusion

**Current System:**  Suitable for both conventional ad serving (mobile/web) and **basic OOH ad serving**  
**OOH Readiness:** ~85% - Core OOH features implemented, advanced features pending

**Minimum Viable OOH System Status:**
-  Screen management (IMPLEMENTED)
-  Playlist generation (IMPLEMENTED)
-  Time-based targeting (IMPLEMENTED)
-  Enhanced location targeting (IMPLEMENTED)
-  Campaign/Creative separation (PENDING - Phase 2)
-  Advanced scheduling/weighting (PENDING - Phase 2)

**Key Implemented Features:**
- Screen registration and management with location data
- Playlist generation with duration-based selection
- Time-based targeting (dayparts) with timezone support
- Enhanced location targeting (city, area, venue type)
- Screen tag matching
- Full integration with existing budget and frequency capping systems

**Remaining Work for Full OOH System:**
- Campaign/Creative model separation (1-2 weeks)
- Advanced scheduling algorithms (weighted selection, SOV distribution) (1 week)

## Implementation Details

### API Endpoints Added

#### Screen Management
- `POST /api/v1/screens/register` - Register a new screen
  - Body: `CreateScreenRequest` (id, name, location, tags, metadata)
  - Returns: `Screen` object
  
- `GET /api/v1/screens/{screenId}` - Get screen details
  - Returns: `Screen` object or 404 if not found
  
- `GET /api/v1/screens` - List all screens
  - Returns: List of `Screen` objects
  
- `PUT /api/v1/screens/{screenId}/heartbeat` - Update screen heartbeat
  - Updates `lastSeen` timestamp and sets `isOnline = true`

#### Playlist Generation
- `GET /api/v1/screens/{screenId}/playlist?durationMinutes=X` - Generate playlist
  - Query param: `durationMinutes` (default: 3)
  - Returns: `PlaylistResponse` with list of `PlaylistItem` objects
  - Automatically fetches screen location context for targeting

#### Extended Ad Delivery
- `GET /ads/deliver` - Now supports OOH parameters:
  - Query params: `screenId`, `city`, `area`, `venueType`, `timezone`
  - These parameters are used for enhanced targeting

### Models Added

**Screen Models:**
- `Screen` - Main screen entity with location, tags, metadata, online status
- `ScreenLocation` - Location info (country, city, area, venueType, timezone)
- `CreateScreenRequest` - Request model for screen registration

**Playlist Models:**
- `PlaylistResponse` - Contains playlist items, total duration, validity
- `PlaylistItem` - Individual ad in playlist with position and duration

**Targeting Models:**
- `TimeBand` - Time window with start/end times and optional days-of-week

### Services Added

- `PlaylistService` - Generates playlists by:
  1. Filtering ads using targeting rules (including time-based)
  2. Filtering by budget constraints
  3. Filtering by frequency capping
  4. Selecting ads until target duration is reached
  
- `TimeTargetingService` - Handles time-based targeting:
  - Parses time bands from targeting rules
  - Matches current time against time bands
  - Supports timezone-aware matching
  - Supports day-of-week filtering

- Extended `TargetingService` - Now supports:
  - OOH location keys: `city`, `area`, `venueType`, `screenId`, `timezone`
  - Time-based operator: `daypart` or `timeband` with format `"HH:mm-HH:mm"` or `"HH:mm-HH:mm,day1,day2"`

### Storage

- `ScreenStore` - Interface for screen storage
- `RedisScreenStore` - Redis implementation (default for MVP)
- Screen data stored in Redis with keys: `screens:{screenId}`

### Usage Examples

**Register a Screen:**
```json
POST /api/v1/screens/register
{
  "id": "screen-001",
  "name": "Phoenix Mall - Food Court Screen 1",
  "location": {
    "country": "IN",
    "city": "Chennai",
    "area": "Phoenix Mall",
    "venueType": "mall",
    "timezone": "Asia/Kolkata"
  },
  "tags": ["mall", "food-court", "premium"],
  "metadata": {}
}
```

**Generate Playlist:**
```
GET /api/v1/screens/screen-001/playlist?durationMinutes=5
```

**Create Ad with Time-Based Targeting:**
```json
POST /admin/ads
{
  "advertiserId": "brand-x",
  "creativeUrl": "https://cdn.example.com/ad.mp4",
  "durationSeconds": 30,
  "targetingRules": [
    {"key": "city", "operator": "eq", "value": "Chennai"},
    {"key": "venueType", "operator": "eq", "value": "mall"},
    {"key": "time", "operator": "daypart", "value": "09:00-17:00,monday,friday"}
  ],
  "isActive": true
}
```

