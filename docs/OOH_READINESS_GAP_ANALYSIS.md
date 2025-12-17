# OOH Ad Serving Readiness - Gap Analysis

## Current System Status

### ✅ What Works
- Basic ad delivery (single ad at a time)
- Ad management (create, list, update)
- Budget management (maxPlays, dailyLimit, hourlyLimit)
- Frequency capping (per device/user)
- Impression tracking
- Analytics

### ❌ Critical Gaps for OOH

#### 1. Screen/Placement Management
**Status:** Not implemented  
**Impact:** HIGH - Cannot identify or manage physical screens  
**Required:**
- Screen registration API (`POST /api/v1/screens/register`)
- Screen lookup API (`GET /api/v1/screens/{screenId}`)
- Screen model with location, tags, metadata
- Screen store (in-memory or Postgres)

#### 2. Playlist Generation
**Status:** Not implemented  
**Impact:** CRITICAL - OOH requires playlists, not single ads  
**Required:**
- Playlist API (`GET /api/v1/screens/{screenId}/playlist?durationMinutes=X`)
- Playlist generation service
- Duration-based ad selection
- Weighted/priority-based scheduling

#### 3. Campaign/Creative Separation
**Status:** Not implemented  
**Impact:** MEDIUM - Ads are standalone, not grouped  
**Required:**
- Campaign model (groups multiple creatives)
- Creative model (individual assets)
- Campaign-to-Creative relationship
- Campaign-level targeting and budget

#### 4. Time-Based Targeting (Dayparts)
**Status:** Not implemented  
**Impact:** HIGH - Critical for OOH scheduling  
**Required:**
- TimeBand model (start/end times)
- Day-of-week targeting
- Time window matching logic
- Timezone handling

#### 5. Enhanced Location Targeting
**Status:** Partial (only country)  
**Impact:** MEDIUM - OOH needs city/area/venue targeting  
**Required:**
- City targeting
- Area/neighborhood targeting
- Venue type tags (mall, airport, transit, etc.)
- Screen tag matching

#### 6. Playlist Response Format
**Status:** Not implemented  
**Impact:** CRITICAL - Current API returns single ad  
**Required:**
- PlaylistResponse model
- PlaylistItem model
- Multiple ads in sequence
- ValidForSeconds field

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

## Migration Path

The current system can be extended rather than rebuilt:

1. **Keep existing Ad model** but add:
   - Screen ID reference
   - Time-based targeting fields
   - Enhanced location fields

2. **Add new models:**
   - Screen
   - Playlist
   - Campaign (optional, can start with ads)

3. **Extend AdDeliveryService** to:
   - Accept screen context
   - Generate playlists instead of single ads
   - Support time-based filtering

## Conclusion

**Current System:** Suitable for conventional ad serving (mobile/web)  
**OOH Readiness:** ~40% - Core infrastructure exists but missing OOH-specific features

**Minimum Viable OOH System Requires:**
- Screen management ✅ (needs implementation)
- Playlist generation ✅ (needs implementation)
- Time-based targeting ✅ (needs implementation)
- Enhanced location targeting ✅ (needs implementation)

**Estimated Effort:** 3-5 weeks to reach MVP OOH capability

