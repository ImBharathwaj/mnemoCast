# Ad Requirements Matrix & Selection Algorithm

> This document defines the complete requirements matrix and algorithm for ad selection in the MnemoCast ad serving engine.
> 
> **Note:** This document describes the MVP single-tenant implementation. All ads and screens operate within a single tenant context.

---

## Table of Contents

1. [Ad Requirements Matrix](#ad-requirements-matrix)
2. [Selection Algorithm](#selection-algorithm)
3. [Targeting Rules](#targeting-rules)
4. [Budget Constraints](#budget-constraints)
5. [Frequency Capping](#frequency-capping)
6. [Weighted Selection](#weighted-selection)
7. [Examples](#examples)

---

## Ad Requirements Matrix

The following matrix defines all requirements that an ad must satisfy to be eligible for delivery:

| Requirement Category | Field | Type | Required | Description | Evaluation |
|---------------------|-------|------|----------|------------|------------|
| **Basic Status** | `isActive` | Boolean | Yes | Ad must be active/enabled | `ad.isActive == true` |
| **Targeting** | `targetingRules` | List[TargetingRule] | Optional | All targeting rules must pass (AND logic) | See [Targeting Rules](#targeting-rules) |
| **Budget - Total** | `maxPlays` | Int | Optional | Total impressions across all screens | `totalImpressions < maxPlays` |
| **Budget - Daily** | `dailyLimit` | Int | Optional | Maximum impressions per day | `dailyImpressions < dailyLimit` |
| **Budget - Hourly** | `hourlyLimit` | Int | Optional | Maximum impressions per hour | `hourlyImpressions < hourlyLimit` |
| **Frequency - Device** | `maxImpressionsPerDevice` | Int | Optional | Max impressions per device in window | `deviceImpressions < maxImpressionsPerDevice` |
| **Frequency - User** | `maxImpressionsPerUser` | Int | Optional | Max impressions per user in window | `userImpressions < maxImpressionsPerUser` |
| **Frequency - Window** | `frequencyCapWindowHours` | Int | Optional | Time window for frequency cap (default: 24h) | Used to calculate window start |

### Requirement Evaluation Order

Requirements are evaluated in the following order (early termination if any fails):

1. **Basic Status** (implicit - only active ads are fetched)
2. **Targeting Rules** (Step 1)
3. **Budget Constraints** (Step 2)
4. **Frequency Capping** (Step 3)
5. **Weighted Selection** (Step 4 - final selection)

---

## Selection Algorithm

### Overview

The ad selection algorithm follows a **filter-then-select** approach:

1. **Filter** ads through multiple requirement checks (targeting, budget, frequency)
2. **Select** one ad from eligible candidates using weighted random selection

### Detailed Algorithm

```
Algorithm: AdDeliveryService.deliver(request)

Input:
  - request: DeliveryRequest (contains screenId, deviceId, userId, location, etc.)

Output:
  - Option[DeliveryResponse] (Some(response) if ad found, None otherwise)

Steps:

1. FETCH SCREEN INFORMATION
   - If screenId provided:
     - Fetch screen from ScreenStore
     - Extract screen.classification (1-10, default: 1)
   - Else:
     - Use default classification: 1

2. FETCH ALL ACTIVE ADS
   - Query AdStore.listActive()
   - Returns: List[Ad] where ad.isActive == true

3. FILTER BY TARGETING RULES (Step 1)
   - For each ad in the active ads list:
     - If ad.targetingRules.isEmpty:
       - Ad matches (no targeting = show everywhere)
     - Else:
       - Evaluate ALL rules using AND logic
       - Rule evaluation:
         * Extract request field by rule.key
         * Compare using rule.operator (eq, in, gte, lte, gt, lt, daypart, timeband)
         * All rules must pass
   
   - Result: targetingEligible = ads where TargetingService.matches(ad, request) == true
   - Log: "After targeting filter: X/Y ads eligible"

5. FILTER BY BUDGET CONSTRAINTS (Step 2)
   - For each ad in targetingEligible:
     - Check maxPlays: totalImpressions < ad.maxPlays (if set)
     - Check dailyLimit: dailyImpressions < ad.dailyLimit (if set)
     - Check hourlyLimit: hourlyImpressions < ad.hourlyLimit (if set)
     - All budget checks must pass (AND logic)
   
   - Result: budgetEligible = ads where BudgetService.isWithinBudget(ad) == true
   - Log: "After budget filter: X/Y ads eligible"

6. FILTER BY FREQUENCY CAPPING (Step 3)
   - For each ad in budgetEligible:
     - Calculate windowStart = now() - frequencyCapWindowHours (default: 24h)
     - Check device cap: deviceImpressions < ad.maxImpressionsPerDevice (if set)
     - Check user cap: userImpressions < ad.maxImpressionsPerUser (if set)
     - Both checks must pass (AND logic)
   
   - Result: eligible = ads where FrequencyCapService.canShow(ad, request) == true
   - Log: "After frequency cap filter: X/Y ads eligible"

7. WEIGHTED SELECTION (Step 4)
   - If eligible.isEmpty:
     - Return None
     - Log: "ERROR: NO ELIGIBLE ADS FOUND AFTER ALL FILTERS"
   
   - Else:
     - Calculate effectiveWeight for each ad:
       * baseWeight = max(1, ad.weight)
       * effectiveWeight = baseWeight * max(1, screenClassification)
       * Example: ad.weight=3, screenClassification=5 → effectiveWeight=15
     
     - Build weighted pool:
       * For each ad, add it to pool (effectiveWeight) times
       * Example: ad1 (weight=2, screen=3) → appears 6 times
       *          ad2 (weight=5, screen=3) → appears 15 times
     
     - Random selection:
       * Select random index from weighted pool
       * Return selected ad
     
     - Log: "SUCCESS: Selected ad: {adId} (weight: {weight}, screen classification: {classification})"

8. BUILD RESPONSE
   - Create DeliveryResponse:
     * requestId: from request
     * adId: selected ad.id
     * creativeUrl: ad.creativeUrl
     * targetUrl: ad.targetUrl
     * impressionTrackingUrl: "{baseUrl}/api/v1/events/impression?adId={adId}&requestId={requestId}"
   
   - Log impression event to EventStore
   - Return Some(response)
```

### Algorithm Complexity

- **Time Complexity**: O(N × M) where:
  - N = number of active ads
  - M = average number of targeting rules per ad
- **Space Complexity**: O(N × W) where:
  - N = number of eligible ads
  - W = maximum effective weight (for weighted pool)

---

## Targeting Rules

### Rule Structure

```scala
TargetingRule(
  key: String,      // Field to match (e.g., "country", "city", "platform")
  operator: String, // Comparison operator (eq, in, gte, lte, gt, lt, daypart, timeband)
  value: String     // Value to compare against
)
```

### Supported Keys

| Key | Request Field | Type | Description |
|-----|---------------|------|-------------|
| `country` | `request.country` | String | Country code (e.g., "IN", "US") |
| `platform` | `request.platform` | String | Platform (e.g., "android", "ios", "web") |
| `deviceId` | `request.deviceId` | String | Device identifier |
| `userId` | `request.userId` | String | User identifier |
| `appId` | `request.appId` | String | Application identifier |
| `screenId` | `request.screenId` | String | Screen/player identifier (OOH) |
| `city` | `request.city` | String | City name (OOH) |
| `area` | `request.area` | String | Area/neighborhood (OOH) |
| `venueType` | `request.venueType` | String | Venue type (e.g., "mall", "airport") |
| `screenTag` / `tag` | `request.screenTags` | List[String] | Screen tags (OOH, special handling) |
| `timezone` | `request.timezone` | String | IANA timezone identifier |
| `classification` | `request.screenClassification` | Int | Screen classification (1-10) |

### Supported Operators

| Operator | Description | Example | Evaluation |
|----------|-------------|---------|------------|
| `eq` | Equals (case-insensitive) | `country eq "IN"` | `request.country == "IN"` |
| `in` | List membership | `platform in "android,ios"` | `request.platform` in `["android", "ios"]` |
| `gte` | Greater than or equal (numeric) | `classification gte "5"` | `request.classification >= 5` |
| `lte` | Less than or equal (numeric) | `classification lte "7"` | `request.classification <= 7` |
| `gt` | Greater than (numeric) | `classification gt "3"` | `request.classification > 3` |
| `lt` | Less than (numeric) | `classification lt "10"` | `request.classification < 10` |
| `daypart` / `timeband` | Time-based targeting | `daypart "09:00-17:00"` | Current time within band |

### Rule Evaluation Logic

- **AND Logic**: All rules must pass for ad to match
- **Empty Rules**: If `ad.targetingRules.isEmpty`, ad matches all requests
- **Missing Fields**: If request field is missing (None), rule fails
- **Case Insensitive**: String comparisons are case-insensitive

### Special Handling: Screen Tags

For `screenTag` or `tag` keys:
- Rule value: comma-separated list (e.g., `"mall,food_court"`)
- Request value: list of screen tags (e.g., `["mall", "gym"]`)
- Evaluation: Check if any request tag matches any rule tag (OR logic within tags)

Example:
- Rule: `tag in "mall,food_court"`
- Request tags: `["mall", "gym"]`
- Result: **PASS** (mall matches)

---

## Budget Constraints

### Budget Types

| Budget Type | Field | Description | Check |
|-------------|-------|-------------|-------|
| **Total Plays** | `maxPlays` | Maximum total impressions across all screens | `totalImpressions < maxPlays` |
| **Daily Limit** | `dailyLimit` | Maximum impressions per day (UTC) | `dailyImpressions < dailyLimit` |
| **Hourly Limit** | `hourlyLimit` | Maximum impressions per hour (UTC) | `hourlyImpressions < hourlyLimit` |

### Budget Evaluation

- **All checks must pass** (AND logic)
- If budget field is `None`, that check is skipped (no limit)
- Time windows:
  - Daily: Start of current day (UTC 00:00:00)
  - Hourly: Start of current hour (UTC HH:00:00)

### Example

```scala
Ad(
  maxPlays = Some(1000),      // Total limit: 1000 impressions
  dailyLimit = Some(100),     // Daily limit: 100 impressions/day
  hourlyLimit = Some(10)      // Hourly limit: 10 impressions/hour
)
```

Evaluation:
- Check 1: `totalImpressions < 1000` ✓
- Check 2: `dailyImpressions < 100` ✓
- Check 3: `hourlyImpressions < 10` ✓
- Result: **PASS** (all checks pass)

---

## Frequency Capping

### Frequency Cap Types

| Cap Type | Field | Description | Check |
|----------|-------|-------------|-------|
| **Device Cap** | `maxImpressionsPerDevice` | Max impressions per device in window | `deviceImpressions < maxImpressionsPerDevice` |
| **User Cap** | `maxImpressionsPerUser` | Max impressions per user in window | `userImpressions < maxImpressionsPerUser` |
| **Window** | `frequencyCapWindowHours` | Time window size (default: 24 hours) | Used to calculate window start |

### Frequency Cap Evaluation

- **Both checks must pass** (AND logic)
- If cap field is `None`, that check is skipped (no limit)
- Window calculation:
  - `windowStart = now() - frequencyCapWindowHours`
  - Default: 24 hours if not specified
- Device check: Only if `request.deviceId` is provided
- User check: Only if `request.userId` is provided

### Example

```scala
Ad(
  maxImpressionsPerDevice = Some(5),      // Max 5 impressions per device
  maxImpressionsPerUser = Some(10),      // Max 10 impressions per user
  frequencyCapWindowHours = Some(24)      // Within 24 hours
)
```

Request: `deviceId="device123", userId="user456"`

Evaluation:
- Window: Last 24 hours
- Check 1: `deviceImpressions("device123") < 5` ✓
- Check 2: `userImpressions("user456") < 10` ✓
- Result: **PASS** (both checks pass)

---

## Weighted Selection

### Weight Calculation

The final selection uses **weighted random selection** with screen classification boost:

```
effectiveWeight = max(1, ad.weight) × max(1, screenClassification)
```

Where:
- `ad.weight`: Ad's base weight (default: 1)
- `screenClassification`: Screen classification (1-10, default: 1)

### Selection Algorithm

1. **Build Weighted Pool**:
   - For each eligible ad, add it to pool `effectiveWeight` times
   - Example:
     - Ad1: weight=2, screen=3 → appears 6 times
     - Ad2: weight=5, screen=3 → appears 15 times
     - Ad3: weight=1, screen=3 → appears 3 times

2. **Random Selection**:
   - Select random index from weighted pool
   - Return ad at that index

### Probability Distribution

Probability of selecting ad `i`:

```
P(ad_i) = effectiveWeight_i / Σ(effectiveWeight_j)
```

Example:
- Ad1: effectiveWeight=6 → P=6/24=25%
- Ad2: effectiveWeight=15 → P=15/24=62.5%
- Ad3: effectiveWeight=3 → P=3/24=12.5%

### Screen Classification Boost

Higher classified screens boost ad weights proportionally:
- **Screen Classification 1**: No boost (weight × 1)
- **Screen Classification 5**: 5× boost (weight × 5)
- **Screen Classification 10**: 10× boost (weight × 10)

This creates a **pay-per-attention model** where premium screens favor premium (high-weight) ads.

---

## Examples

### Example 1: Simple Ad with No Targeting

**Ad:**
```scala
Ad(
  id = "ad1",
  isActive = true,
  targetingRules = List.empty,  // No targeting = show everywhere
  maxPlays = Some(1000),
  weight = 1
)
```

**Request:**
```
screenId = "screen123"
country = "IN"
city = "Mumbai"
```

**Evaluation:**
1. ✅ Targeting: Pass (no rules = match all)
3. ✅ Budget: Pass (assuming < 1000 impressions)
4. ✅ Frequency: Pass (assuming no caps)
5. ✅ Selection: Selected (weight=1)

**Result:** ✅ Ad delivered

---

### Example 2: Ad with Targeting Rules

**Ad:**
```scala
Ad(
  id = "ad2",
  isActive = true,
  targetingRules = List(
    TargetingRule("country", "eq", "IN"),
    TargetingRule("city", "in", "Mumbai,Chennai"),
    TargetingRule("platform", "in", "android,ios")
  ),
  maxPlays = Some(500),
  weight = 3
)
```

**Request:**
```
country = "IN"
city = "Mumbai"
platform = "android"
```

**Evaluation:**
1. ✅ Targeting:
   - Rule 1: `country eq "IN"` → ✅ Pass
   - Rule 2: `city in "Mumbai,Chennai"` → ✅ Pass
   - Rule 3: `platform in "android,ios"` → ✅ Pass
2. ✅ Budget: Pass
4. ✅ Frequency: Pass
5. ✅ Selection: Selected (weight=3)

**Result:** ✅ Ad delivered

---

### Example 3: Ad Rejected by Targeting

**Ad:**
```scala
Ad(
  id = "ad3",
  isActive = true,
  targetingRules = List(
    TargetingRule("country", "eq", "US")  // Requires US
  ),
  weight = 5
)
```

**Request:**
```
country = "IN"  // India, not US
```

**Evaluation:**
1. ❌ Targeting:
   - Rule 1: `country eq "US"` → ❌ Fail (request is "IN")
3. ❌ **REJECTED** (targeting failed)

**Result:** ❌ Ad not delivered

---

### Example 4: Ad Rejected by Budget

**Ad:**
```scala
Ad(
  id = "ad4",
  isActive = true,
  targetingRules = List.empty,
  maxPlays = Some(100),
  dailyLimit = Some(10)
)
```

**Request:**
```
screenId = "screen123"
```

**Current State:**
- Total impressions: 100 (at limit)
- Daily impressions: 10 (at limit)

**Evaluation:**
1. ✅ Targeting: Pass
2. ❌ Budget:
   - Check 1: `totalImpressions < 100` → ❌ Fail (100 >= 100)
   - Check 2: `dailyImpressions < 10` → ❌ Fail (10 >= 10)
4. ❌ **REJECTED** (budget exhausted)

**Result:** ❌ Ad not delivered

---

### Example 5: Ad Rejected by Frequency Cap

**Ad:**
```scala
Ad(
  id = "ad5",
  isActive = true,
  targetingRules = List.empty,
  maxImpressionsPerDevice = Some(3),
  frequencyCapWindowHours = Some(24)
)
```

**Request:**
```
deviceId = "device123"
```

**Current State:**
- Device impressions in last 24h: 3 (at limit)

**Evaluation:**
1. ✅ Targeting: Pass
2. ✅ Budget: Pass
3. ❌ Frequency:
   - Check: `deviceImpressions < 3` → ❌ Fail (3 >= 3)
5. ❌ **REJECTED** (frequency cap exceeded)

**Result:** ❌ Ad not delivered

---

### Example 6: Weighted Selection with Screen Classification

**Eligible Ads:**
- Ad1: weight=1, screenClassification=5 → effectiveWeight=5
- Ad2: weight=3, screenClassification=5 → effectiveWeight=15
- Ad3: weight=2, screenClassification=5 → effectiveWeight=10

**Weighted Pool:**
```
[Ad1, Ad1, Ad1, Ad1, Ad1, Ad2, Ad2, Ad2, ..., Ad2 (15 times), Ad3, Ad3, ..., Ad3 (10 times)]
Total: 30 entries
```

**Probabilities:**
- Ad1: 5/30 = 16.7%
- Ad2: 15/30 = 50.0%
- Ad3: 10/30 = 33.3%

**Selection:** Random pick from pool (Ad2 most likely)

---

## Edge Cases

### Case 1: No Active Ads

**Scenario:** No ads in database with `isActive = true`

**Result:** Returns `None`, logs: "ERROR: NO ACTIVE ADS IN DATABASE!"

---

### Case 2: All Ads Filtered Out

**Scenario:** Ads exist but all fail requirements

**Result:** Returns `None`, logs summary:
```
ERROR: NO ELIGIBLE ADS FOUND AFTER ALL FILTERS
Summary:
  - Total active ads: 10
  - After targeting: 0
  - After budget: 0
  - After frequency cap: 0
```

---

---

### Case 4: Ad Without Targeting Rules

**Scenario:** Ad has empty `targetingRules` list

**Behavior:**
- Ad matches all requests (default: show everywhere)
- Logs: "Ad has no targeting rules (should match all)"

---

### Case 5: Missing Request Fields

**Scenario:** Targeting rule requires field not present in request

**Behavior:**
- Rule fails (field missing = rule fails)
- Ad rejected if any rule fails

---

## Performance Considerations

### Optimization Strategies

1. **Early Termination**: Stop evaluation as soon as any requirement fails
2. **Parallel Budget/Frequency Checks**: Run budget and frequency checks in parallel
3. **Caching**: Cache screen information and ad metadata
4. **Indexing**: Database indexes on:
   - `ads.is_active`
   - `delivery_events.ad_id`
   - `delivery_events.device_id`
   - `delivery_events.user_id`
   - `delivery_events.occurred_at`

### Monitoring

Key metrics to track:
- **Filter rates**: How many ads pass each filter stage
- **Selection distribution**: Actual vs expected probability distribution
- **Performance**: Average response time per request
- **Error rates**: Frequency of "no ads available" scenarios

---

## Conclusion

The ad selection algorithm ensures:
1. **Targeting Accuracy**: Ads match request context (location, platform, etc.)
2. **Budget Compliance**: Ads respect spending limits
3. **Frequency Control**: Ads respect impression limits per device/user
4. **Fair Selection**: Weighted random selection with screen classification boost

This creates a robust, scalable ad serving system that balances advertiser requirements with fair distribution.

