# OOH Ad Serving MVP - Pending Items

## Overview

**Current Status:** ~85% Complete - Core OOH features implemented, advanced features pending

**Phase 1 (Core OOH Features):** ✅ **COMPLETED**
**Phase 2 (Advanced Features):** ⚠️ **PENDING**

---

## ⚠️ Pending Items for OOH MVP

### 1. Campaign/Creative Separation ⚠️ **HIGH PRIORITY**

**Status:** Not implemented  
**Impact:** MEDIUM - Currently ads are standalone, not grouped into campaigns  
**Why it matters:** Real-world OOH advertising uses campaigns that contain multiple creatives. This is essential for:
- Managing multiple ad variations under one campaign
- Campaign-level budget and targeting
- Better organization and reporting

**Required Implementation:**

#### 1.1 Campaign Model
```scala
case class Campaign(
  id: String,
  name: String,
  advertiserId: String,
  status: CampaignStatus, // active, paused, completed
  startDate: Instant,
  endDate: Instant,
  totalBudget: Option[Long],
  targetPlayouts: Option[Long],
  targetingRules: List[TargetingRule], // Campaign-level targeting
  priority: Int, // For weighted selection
  createdAt: Instant,
  updatedAt: Instant
)
```

#### 1.2 Creative Model
```scala
case class Creative(
  id: String,
  campaignId: String, // FK to Campaign
  name: String,
  creativeType: String, // video, image, html
  creativeUrl: String,
  durationSeconds: Int,
  status: CreativeStatus, // active, paused, deleted
  shareOfVoice: Option[Double], // 0.0 to 1.0 for SOV distribution
  frequencyCapPerScreen: Option[Int], // Plays per day per screen
  metadata: Map[String, String],
  createdAt: Instant,
  updatedAt: Instant
)
```

#### 1.3 Campaign-to-Creative Relationship
- One Campaign → Many Creatives
- Store relationship in database
- Load creatives when campaign is selected

#### 1.4 Campaign-Level Targeting and Budget
- Move targeting rules to campaign level (creatives inherit)
- Campaign-level budget enforcement
- Campaign active window (startDate/endDate) checking

**Estimated Effort:** 1-2 weeks

---

### 2. Advanced Scheduling Algorithms ⚠️ **MEDIUM PRIORITY**

**Status:** Partially implemented (basic random selection only)  
**Impact:** MEDIUM - Current playlist generation uses simple random selection  
**Why it matters:** Production OOH systems need:
- Weighted selection based on campaign priority
- Share-of-voice (SOV) distribution
- Fair rotation algorithms

**Required Implementation:**

#### 2.1 Weighted Random Selection
- Use campaign/creative priority/weight for selection
- Higher priority = more likely to be selected
- Implement weighted random algorithm

#### 2.2 Share-of-Voice (SOV) Distribution
- Track SOV per campaign/creative in time windows
- Ensure fair distribution based on SOV targets
- Implement SOV counters (can use Redis for fast updates)

#### 2.3 Advanced Frequency Capping
- Per-screen frequency capping (already partially supported)
- Per-placement frequency capping
- Time-window based frequency caps (e.g., max 3 per hour)

**Current Implementation:**
- ✅ Basic random selection in `PlaylistService`
- ✅ Frequency capping per device/user (exists)
- ⚠️ No weighted selection
- ⚠️ No SOV distribution
- ⚠️ No per-screen frequency capping in playlist context

**Estimated Effort:** 1 week

---

### 3. Placement/Zone Management ⚠️ **LOW PRIORITY (Optional for MVP)**

**Status:** Not implemented  
**Impact:** LOW - Screens can work standalone, but placements provide better organization  
**Why it matters:** 
- Group screens into logical zones (e.g., "Phoenix Mall - Food Court")
- Placement-level targeting and analytics
- Better screen organization

**Required Implementation:**
- Placement model (groups screens)
- Screen-to-Placement relationship
- Placement-level targeting

**Note:** This can be deferred to post-MVP as screens work standalone.

**Estimated Effort:** 3-5 days

---

### 4. Decision Logging ⚠️ **MEDIUM PRIORITY**

**Status:** Not implemented  
**Impact:** MEDIUM - Currently only playout events are logged, not decisions  
**Why it matters:**
- Audit trail of what was selected vs. what was actually played
- Debugging playlist generation issues
- Analytics on selection vs. playout

**Required Implementation:**
- `Decision` model to log playlist generation decisions
- Store decision when playlist is generated
- Link decisions to playout events
- API to query decisions

**Estimated Effort:** 2-3 days

---

### 5. Enhanced Playlist Features ⚠️ **LOW PRIORITY**

**Status:** Basic implementation exists  
**Impact:** LOW - Current playlist generation works but could be enhanced  

**Potential Enhancements:**
- Playlist caching (avoid regenerating same playlist multiple times)
- Playlist versioning
- Playlist validation (ensure no gaps, proper sequencing)
- Support for filler content when no ads available

**Estimated Effort:** 2-3 days

---

### 6. Screen Tag Targeting ⚠️ **PARTIALLY IMPLEMENTED**

**Status:** Screen tags are stored but not fully used in targeting  
**Impact:** LOW - Can be worked around with venueType targeting  

**Current State:**
- ✅ Screen tags are stored in `Screen` model
- ✅ Tags are persisted in database
- ⚠️ Targeting rules don't support screen tag matching yet

**Required Implementation:**
- Add "screenTag" or "tag" key to targeting rules
- Implement tag matching in `TargetingService`
- Support "in" operator for multiple tags

**Estimated Effort:** 1 day

---

### 7. Testing & Documentation ⚠️ **ONGOING**

**Status:** Partial  
**Impact:** HIGH - Essential for production readiness  

**Required:**
- Unit tests for new OOH features
- Integration tests for playlist generation
- API documentation updates
- End-to-end test scenarios
- Performance testing for playlist generation

**Estimated Effort:** 1 week

---

## Priority Ranking for MVP Completion

### Must Have (Blocking MVP):
1. **Campaign/Creative Separation** - Essential for real-world usage
2. **Decision Logging** - Important for debugging and analytics
3. **Testing & Documentation** - Required for production readiness

### Should Have (Important but not blocking):
4. **Advanced Scheduling (Weighted Selection)** - Improves fairness and control
5. **Screen Tag Targeting** - Completes the targeting feature set

### Nice to Have (Post-MVP):
6. **Placement/Zone Management** - Can be added later
7. **SOV Distribution** - Advanced feature, can be added post-MVP
8. **Enhanced Playlist Features** - Optimization features

---

## Recommended MVP Completion Plan

### Week 1-2: Campaign/Creative Structure
- Implement Campaign and Creative models
- Create CampaignStore and CreativeStore
- Update PlaylistService to work with campaigns/creatives
- Migrate existing ads to creatives (or support both)

### Week 3: Advanced Scheduling
- Implement weighted random selection
- Add priority/weight fields to campaigns
- Update playlist generation algorithm

### Week 4: Decision Logging & Testing
- Implement Decision model and storage
- Add decision logging to PlaylistService
- Write comprehensive tests
- Update documentation

**Total Estimated Time:** 3-4 weeks to complete MVP

---

## Current MVP Capabilities (What Works Now)

✅ **Fully Functional:**
- Screen registration and management
- Playlist generation with duration-based selection
- Time-based targeting (dayparts) with timezone support
- Enhanced location targeting (city, area, venue type)
- Budget management (maxPlays, dailyLimit, hourlyLimit)
- Frequency capping (per device/user)
- Postgres storage for all OOH features
- Hybrid storage strategy (Postgres + Redis cache)

⚠️ **Works but Limited:**
- Playlist generation (basic random selection, no weighting)
- Ad targeting (works but ads are standalone, not grouped in campaigns)

---

## Summary

**For a production-ready OOH MVP, you need:**

1. ✅ Core infrastructure (DONE)
2. ⚠️ Campaign/Creative structure (PENDING - 1-2 weeks)
3. ⚠️ Advanced scheduling (PENDING - 1 week)
4. ⚠️ Decision logging (PENDING - 2-3 days)
5. ⚠️ Testing & documentation (PENDING - 1 week)

**Total remaining effort:** ~3-4 weeks to reach full MVP

**Current system can serve basic OOH needs** but would benefit from campaign/creative structure for real-world usage.

