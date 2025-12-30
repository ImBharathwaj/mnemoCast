# Playlist Generation Logic

> This document defines the complete logic and algorithm for generating playlists of ads for OOH screens in the MnemoCast ad serving engine.

---

## Table of Contents

1. [Overview](#overview)
2. [Input/Output Models](#inputoutput-models)
3. [Playlist Generation Algorithm](#playlist-generation-algorithm)
4. [Ad Eligibility Filtering](#ad-eligibility-filtering)
5. [Weighted Selection](#weighted-selection)
6. [Duration Filling Strategy](#duration-filling-strategy)
7. [Screen Classification Boost](#screen-classification-boost)
8. [Examples](#examples)
9. [Edge Cases](#edge-cases)
10. [Performance Considerations](#performance-considerations)

---

## Overview

The playlist generation service creates a sequence of ads (playlist) for an OOH screen that:

- **Fills a requested duration** (e.g., 5 minutes, 10 minutes)
- **Respects targeting rules** (location, time, platform, etc.)
- **Honors budget constraints** (total plays, daily limits, hourly limits)
- **Respects frequency capping** (impressions per device/user)
- **Favors higher-weight ads** (weighted selection)
- **Boosts ads for premium screens** (screen classification-based weighting)

### Key Characteristics

- **Dynamic**: Playlists are generated on-demand for each request
- **Context-aware**: Different screens receive different playlists based on location, time, etc.
- **Weighted**: Higher-weight ads are more likely to be selected
- **Repetitive**: Ads can appear multiple times in a playlist if needed to fill duration
- **Duration-flexible**: Playlist fills approximately to requested duration (may exceed slightly)

---

## Input/Output Models

### PlaylistRequest

Not a separate API model, but derived from `DeliveryRequest`:

| Field | Type | Description |
|-------|------|-------------|
| `screenId` | String (optional) | Screen identifier requesting the playlist |
| `durationMinutes` | Int | Desired playlist duration in minutes |
| `country` | String (optional) | Country for geo-targeting |
| `city` | String (optional) | City for geo-targeting |
| `area` | String (optional) | Area/neighborhood for geo-targeting |
| `venueType` | String (optional) | Venue type (e.g., "mall", "airport") |
| `screenTags` | List[String] | Screen tags for tag-based targeting |
| `timezone` | String (optional) | IANA timezone identifier |
| `deviceId` | String (optional) | Device identifier for frequency capping |
| `userId` | String (optional) | User identifier for frequency capping |
| `timestamp` | Instant | Request timestamp |

### PlaylistResponse

The output model containing the generated playlist:

```scala
final case class PlaylistResponse(
  requestId: String,              // Original request ID
  screenId: Option[String],       // Screen identifier
  items: List[PlaylistItem],      // Ordered list of ads to play
  validForSeconds: Int,           // Playlist validity TTL (default: duration in seconds)
  totalDurationSeconds: Int       // Total playlist duration
)
```

### PlaylistItem

A single ad in the playlist:

```scala
final case class PlaylistItem(
  adId: String,                        // Ad identifier
  creativeUrl: String,                 // URL to creative asset
  targetUrl: Option[String],           // Optional click-through URL
  durationSeconds: Int,                // Duration of this ad
  impressionTrackingUrl: Option[String], // Impression tracking URL
  position: Int = 0                    // Position in playlist (0-based)
)
```

---

## Playlist Generation Algorithm

### High-Level Flow

```
Algorithm: PlaylistService.generatePlaylist(request, durationMinutes)

1. Calculate target duration in seconds
2. Fetch screen classification (if screenId provided)
3. Fetch all active ads
4. Filter by targeting rules
5. Filter by budget constraints
6. Filter by frequency capping
7. Build playlist using weighted selection
8. Return playlist response
```

### Detailed Algorithm

```
Algorithm: PlaylistService.generatePlaylist(request, durationMinutes)

Input:
  - request: DeliveryRequest (contains screenId, location, deviceId, userId, etc.)
  - durationMinutes: Int (desired playlist duration in minutes)

Output:
  - Future[Option[PlaylistResponse]]

Steps:

1. CALCULATE TARGET DURATION
   - targetDurationSeconds = durationMinutes * 60

2. FETCH SCREEN CLASSIFICATION
   - If screenId provided and ScreenStore available:
     - Fetch screen from ScreenStore
     - Extract screen.classification (1-10, default: 1)
   - Else:
     - Use default classification: 1

3. FETCH ALL ACTIVE ADS
   - Query AdStore.listActive()
   - Returns: List[Ad] where ad.isActive == true

4. FILTER BY TARGETING RULES
   - For each ad:
     - Evaluate TargetingService.matches(ad, request)
     - All targeting rules must pass (AND logic)
     - Empty targeting rules = match all requests
   - Result: targetingEligible = ads where all targeting rules pass

5. FILTER BY BUDGET CONSTRAINTS
   - For each ad in targetingEligible:
     - Check maxPlays: totalImpressions < ad.maxPlays (if set)
     - Check dailyLimit: dailyImpressions < ad.dailyLimit (if set)
     - Check hourlyLimit: hourlyImpressions < ad.hourlyLimit (if set)
     - All budget checks must pass (AND logic)
   - Result: budgetEligible = ads where BudgetService.isWithinBudget(ad) == true

6. FILTER BY FREQUENCY CAPPING
   - For each ad in budgetEligible:
     - Calculate windowStart = now() - frequencyCapWindowHours (default: 24h)
     - Check device cap: deviceImpressions < ad.maxImpressionsPerDevice (if set)
     - Check user cap: userImpressions < ad.maxImpressionsPerUser (if set)
     - Both checks must pass (AND logic)
   - Result: eligibleAds = ads where FrequencyCapService.canShow(ad, request) == true

7. BUILD PLAYLIST
   - If eligibleAds.isEmpty:
     - Return None (no ads available)
   
   - Filter ads with duration:
     - adsWithDuration = eligibleAds.filter(_.durationSeconds.isDefined)
     - If adsWithDuration.isEmpty:
       - Return None (no ads with duration specified)
   
   - Build weighted pool:
     - For each ad, calculate effectiveWeight = baseWeight * screenClassification
     - Add ad to pool (effectiveWeight) times
   
   - Fill playlist:
     - currentDuration = 0
     - items = []
     - position = 0
     - maxIterations = (targetDurationSeconds / 5) + 10
     
     - While currentDuration < targetDurationSeconds AND iterations < maxIterations:
       * Select random ad from weighted pool
       * Get ad duration (or default 30 seconds)
       * Create PlaylistItem:
         - adId = selectedAd.id
         - creativeUrl = selectedAd.creativeUrl
         - targetUrl = selectedAd.targetUrl
         - durationSeconds = adDuration
         - impressionTrackingUrl = "{baseUrl}/api/v1/events/impression?adId={adId}&requestId={requestId}&position={position}"
         - position = position
       * Append item to playlist
       * currentDuration += adDuration
       * position += 1
       * iterations += 1
   
   - Return PlaylistResponse:
     - requestId = request.requestId
     - screenId = request.screenId
     - items = generated playlist items
     - validForSeconds = targetDurationSeconds
     - totalDurationSeconds = currentDuration
```

### Algorithm Complexity

- **Time Complexity**: O(N × M + D/W) where:
  - N = number of active ads
  - M = average number of targeting rules per ad
  - D = target duration in seconds
  - W = average ad weight
- **Space Complexity**: O(N × W + D/W) where:
  - N × W = size of weighted pool
  - D/W = number of items in playlist

---

## Ad Eligibility Filtering

Playlist generation uses the same filtering logic as single ad delivery. Ads must pass:

### 1. Basic Status
- `ad.isActive == true`

### 2. Targeting Rules
- All targeting rules must pass (AND logic)
- See [AD_REQUIREMENTS_MATRIX_AND_ALGORITHM.md](./AD_REQUIREMENTS_MATRIX_AND_ALGORITHM.md#targeting-rules) for details

### 3. Budget Constraints
- Total plays: `totalImpressions < maxPlays` (if set)
- Daily limit: `dailyImpressions < dailyLimit` (if set)
- Hourly limit: `hourlyImpressions < hourlyLimit` (if set)

### 4. Frequency Capping
- Device cap: `deviceImpressions < maxImpressionsPerDevice` (if set)
- User cap: `userImpressions < maxImpressionsPerUser` (if set)
- Time window: Last N hours (default: 24 hours)

### 5. Duration Requirement
- `ad.durationSeconds.isDefined` (ads without duration are excluded from playlists)

---

## Weighted Selection

### Weight Calculation

Ads are selected using **weighted random selection** with screen classification boost:

```
effectiveWeight = max(1, ad.weight) × max(1, screenClassification)
```

Where:
- `ad.weight`: Ad's base weight (default: 1)
- `screenClassification`: Screen classification (1-10, default: 1)

### Weighted Pool Construction

For playlist generation, a weighted pool is built once at the start:

1. For each eligible ad with duration:
   - Calculate `effectiveWeight = baseWeight × screenClassification`
   - Add the ad to the pool `effectiveWeight` times

2. Example:
   - Ad1: weight=2, screen=3 → appears 6 times in pool
   - Ad2: weight=5, screen=3 → appears 15 times in pool
   - Ad3: weight=1, screen=3 → appears 3 times in pool
   - Total pool size: 24 entries

3. Selection:
   - Each ad selection picks a random entry from the weighted pool
   - Higher-weight ads are selected more frequently

### Probability Distribution

Probability of selecting ad `i`:

```
P(ad_i) = effectiveWeight_i / Σ(effectiveWeight_j)
```

The same ad can appear multiple times in a playlist (repetition allowed).

---

## Duration Filling Strategy

### Goal

Fill the playlist to approximately match the requested duration.

### Strategy

1. **Iterative Selection**:
   - Repeatedly select ads from the weighted pool
   - Add each selected ad to the playlist
   - Accumulate total duration
   - Continue until `currentDuration >= targetDurationSeconds`

2. **Duration Handling**:
   - Each ad's `durationSeconds` is used (must be defined)
   - If ad duration is missing, default to 30 seconds
   - Total duration may exceed target (no truncation)

3. **Iteration Limit**:
   - Maximum iterations = `(targetDurationSeconds / 5) + 10`
   - Prevents infinite loops if ads are very short
   - Example: For 5 minutes (300s), max iterations = 70

4. **Duration Calculation**:
   - `totalDurationSeconds` = sum of all item durations
   - May be slightly more than requested (within last ad's duration)

### Example

**Request**: 5 minutes (300 seconds)

**Eligible Ads**:
- Ad1: 30 seconds
- Ad2: 60 seconds
- Ad3: 15 seconds

**Playlist Generation**:
1. Select Ad2 (60s) → duration = 60s
2. Select Ad1 (30s) → duration = 90s
3. Select Ad2 (60s) → duration = 150s
4. Select Ad1 (30s) → duration = 180s
5. Select Ad2 (60s) → duration = 240s
6. Select Ad3 (15s) → duration = 255s
7. Select Ad1 (30s) → duration = 285s
8. Select Ad3 (15s) → duration = 300s ✓

**Result**: 8 items, total duration = 300 seconds

---

## Screen Classification Boost

### Purpose

Higher-classified screens (premium locations) boost the effective weight of ads, creating a **pay-per-attention model**.

### How It Works

- **Screen Classification**: Integer from 1-10 (1 = standard, 10 = premium)
- **Effective Weight**: `ad.weight × screenClassification`

### Examples

**Standard Screen (Classification = 1)**:
- Ad with weight 5 → effectiveWeight = 5 × 1 = 5
- Ad with weight 10 → effectiveWeight = 10 × 1 = 10
- Selection ratio: 10:5 (2:1)

**Premium Screen (Classification = 5)**:
- Ad with weight 5 → effectiveWeight = 5 × 5 = 25
- Ad with weight 10 → effectiveWeight = 10 × 5 = 50
- Selection ratio: 50:25 (2:1, but both ads appear more frequently overall)

**Ultra-Premium Screen (Classification = 10)**:
- Ad with weight 5 → effectiveWeight = 5 × 10 = 50
- Ad with weight 10 → effectiveWeight = 10 × 10 = 100
- Selection ratio: 100:50 (2:1, even higher frequency)

### Business Model

This enables:
- **Premium screens** favor premium (high-weight) ads
- **Advertisers** can pay more for higher-weight ads
- **Premium locations** naturally show more valuable content
- **Fair distribution** maintained (relative weights preserved)

---

## Examples

### Example 1: Basic Playlist Generation

**Request**:
```
screenId = "screen-123"
durationMinutes = 5
city = "Mumbai"
country = "India"
```

**Eligible Ads**:
- Ad1: weight=3, duration=30s, targeting: city="Mumbai" ✓
- Ad2: weight=5, duration=60s, targeting: city="Mumbai" ✓
- Ad3: weight=1, duration=15s, targeting: city="Mumbai" ✓

**Screen Classification**: 1 (standard)

**Weighted Pool**:
- Ad1: 3 entries
- Ad2: 5 entries
- Ad3: 1 entry
- Total: 9 entries

**Playlist Generation** (target: 300 seconds):
1. Ad2 (60s) → 60s
2. Ad2 (60s) → 120s
3. Ad1 (30s) → 150s
4. Ad2 (60s) → 210s
5. Ad1 (30s) → 240s
6. Ad2 (60s) → 300s ✓

**Result**: 6 items, 300 seconds, Ad2 appears most frequently (weight=5)

---

### Example 2: Premium Screen Playlist

**Request**:
```
screenId = "premium-screen-456"
durationMinutes = 10
city = "Chennai"
```

**Eligible Ads**:
- Ad1: weight=2, duration=30s
- Ad2: weight=5, duration=60s
- Ad3: weight=8, duration=45s

**Screen Classification**: 7 (premium)

**Weighted Pool**:
- Ad1: 2 × 7 = 14 entries
- Ad2: 5 × 7 = 35 entries
- Ad3: 8 × 7 = 56 entries
- Total: 105 entries

**Selection Probabilities**:
- Ad1: 14/105 = 13.3%
- Ad2: 35/105 = 33.3%
- Ad3: 56/105 = 53.3%

**Result**: Ad3 (highest weight) appears most frequently due to premium screen boost

---

### Example 3: Limited Eligible Ads

**Request**:
```
screenId = "screen-789"
durationMinutes = 15  // 900 seconds
city = "Delhi"
```

**Eligible Ads**:
- Ad1: weight=1, duration=30s (only ad matching targeting)

**Screen Classification**: 1

**Playlist Generation**:
- Since only one ad is eligible, it will be repeated
- Ad1 selected repeatedly until duration filled
- Result: ~30 items (Ad1 repeated 30 times)

**Note**: Repetition is allowed and expected when few ads are eligible

---

### Example 4: Empty Playlist (No Eligible Ads)

**Request**:
```
screenId = "screen-999"
durationMinutes = 5
city = "Bangalore"
```

**Eligible Ads**: None (all ads filtered out by targeting/budget/frequency)

**Result**: `None` (no playlist generated)

---

## Edge Cases

### Case 1: No Ads with Duration

**Scenario**: Eligible ads exist but none have `durationSeconds` defined

**Behavior**:
- Returns `None`
- Logs: "No ads with duration specified for playlist"

---

### Case 2: Very Short Ads

**Scenario**: All eligible ads have very short durations (e.g., 5 seconds)

**Behavior**:
- Playlist generation continues normally
- More items in playlist to fill duration
- Iteration limit prevents infinite loops

---

### Case 3: Very Long Ads

**Scenario**: Eligible ads have very long durations (e.g., 300 seconds)

**Behavior**:
- Single ad may exceed target duration
- Playlist includes the ad anyway (no truncation)
- `totalDurationSeconds` may be much larger than requested

---

### Case 4: Exact Duration Match

**Scenario**: Playlist duration exactly matches target

**Behavior**:
- Normal completion
- `totalDurationSeconds == targetDurationSeconds`

---

### Case 5: Duration Slightly Exceeded

**Scenario**: Last selected ad causes duration to exceed target

**Behavior**:
- Playlist includes the last ad
- `totalDurationSeconds > targetDurationSeconds` (within last ad's duration)
- This is expected and acceptable

---

### Case 6: All Ads Filtered Out

**Scenario**: All ads filtered out by targeting, budget, or frequency capping

**Behavior**:
- Returns `None`
- No playlist generated

---

### Case 7: Single Eligible Ad

**Scenario**: Only one ad is eligible for the screen

**Behavior**:
- Ad is repeated to fill duration
- Same ad appears multiple times in playlist
- Repetition is allowed

---

### Case 8: Iteration Limit Reached

**Scenario**: Many very short ads cause iteration limit to be reached

**Behavior**:
- Loop terminates at max iterations
- Playlist returned with available items
- Duration may be less than requested (unlikely in practice)

---

## Performance Considerations

### Optimization Strategies

1. **Weighted Pool Caching** (Future):
   - Cache weighted pool for eligible ads
   - Rebuild only when ads change

2. **Parallel Filtering**:
   - Budget and frequency cap checks can run in parallel
   - Reduces total filtering time

3. **Early Termination**:
   - Stop filtering if no ads remain after any step
   - Return `None` immediately

4. **Duration Pre-calculation**:
   - Pre-calculate average ad duration
   - Estimate playlist size before generation

### Database Indexes

Key indexes for performance:
- `ads.is_active`
- `ads.duration_seconds` (for filtering ads with duration)
- `delivery_events.ad_id`
- `delivery_events.occurred_at` (for budget/frequency calculations)

### Monitoring

Key metrics to track:
- **Playlist generation time**: Average time to generate playlist
- **Filter rates**: How many ads pass each filter stage
- **Playlist sizes**: Average number of items per playlist
- **Duration accuracy**: Difference between requested and actual duration
- **Repetition rates**: How often ads are repeated in playlists

### Scalability

For high-volume scenarios:
- Consider caching eligible ads per screen/location
- Pre-compute weighted pools for common screen classifications
- Batch budget/frequency checks
- Use streaming for very long playlists (future enhancement)

---

## API Usage

### Request

```
GET /api/v1/screens/{screenId}/playlist?durationMinutes=5

Query Parameters:
  - durationMinutes: Int (required) - Desired playlist duration
  - Additional targeting parameters (optional):
    - country
    - city
    - area
    - venueType
    - timezone
```

### Response

```json
{
  "requestId": "req-123",
  "screenId": "screen-456",
  "items": [
    {
      "adId": "ad-001",
      "creativeUrl": "https://example.com/creative1.mp4",
      "targetUrl": "https://example.com/landing",
      "durationSeconds": 30,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-001&requestId=req-123&position=0",
      "position": 0
    },
    {
      "adId": "ad-002",
      "creativeUrl": "https://example.com/creative2.mp4",
      "targetUrl": null,
      "durationSeconds": 60,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-002&requestId=req-123&position=1",
      "position": 1
    }
  ],
  "validForSeconds": 300,
  "totalDurationSeconds": 300
}
```

---

## Conclusion

The playlist generation algorithm ensures:

1. **Smart Selection**: Weighted random selection favors higher-weight ads
2. **Context Awareness**: Playlists adapt to screen location, time, and classification
3. **Fair Distribution**: Relative ad weights are preserved while boosting for premium screens
4. **Duration Compliance**: Playlists fill approximately to requested duration
5. **Business Alignment**: Premium screens naturally favor premium (high-weight) ads

This creates a robust, scalable playlist generation system that balances advertiser requirements with fair distribution while enabling a pay-per-attention business model.

