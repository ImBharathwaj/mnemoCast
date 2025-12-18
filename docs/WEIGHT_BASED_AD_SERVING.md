# Weight-Based Ad Serving System

## Overview

The Mnemocast Engine uses a **weight-based selection algorithm** with **screen classification** to implement a "pay-per-attention" model. Higher-weight ads are more likely to be selected, and premium screens (higher classification) amplify this effect.

---

## Core Concepts

### 1. **Ad Weight**
- Every `Ad` has a `weight` field (default: 1)
- **Higher weight = More likely to be selected**
- Weight is an integer (typically 1-10, but can be higher)
- Example:
  - Ad A: `weight = 1` (standard ad)
  - Ad B: `weight = 5` (premium ad)
  - Ad C: `weight = 10` (ultra-premium ad)

### 2. **Screen Classification**
- Every `Screen` has a `classification` field (1-10, default: 1)
- **Higher classification = Premium screen location**
- Example:
  - Screen 1: `classification = 1` (standard location)
  - Screen 2: `classification = 5` (premium location)
  - Screen 3: `classification = 10` (ultra-premium location)

### 3. **Effective Weight (Ad Weight × Screen Classification)**
- The system multiplies `ad.weight × screen.classification` to get **effective weight**
- This creates a **pay-per-attention model**: Premium screens favor premium ads

---

## How It Works

### Step 1: Build Weighted Pool

For each eligible ad, the system creates multiple entries in a pool:

```scala
effectiveWeight = ad.weight × screen.classification
```

Then it adds the ad to the pool `effectiveWeight` times.

**Example:**
- Ad A: weight = 2, Screen: classification = 3
  - Effective weight = 2 × 3 = 6
  - Ad A appears **6 times** in the pool

- Ad B: weight = 5, Screen: classification = 3
  - Effective weight = 5 × 3 = 15
  - Ad B appears **15 times** in the pool

### Step 2: Random Selection from Pool

The system randomly selects from the weighted pool. Since higher-weight ads appear more times, they have a higher probability of being selected.

**Example Pool:**
```
[A, A, A, A, A, A, B, B, B, B, B, B, B, B, B, B, B, B, B, B, B]
 (Ad A appears 6 times, Ad B appears 15 times)

Random selection probability:
- Ad A: 6/21 = ~28.6%
- Ad B: 15/21 = ~71.4%
```

---

## Real-World Examples

### Example 1: Equal Weight Ads, Different Screen Classifications

**Scenario:**
- Ad X: weight = 2 (on Screen A: classification = 1)
- Ad Y: weight = 2 (on Screen B: classification = 5)

**Effective Weights:**
- Ad X on Screen A: 2 × 1 = 2
- Ad Y on Screen B: 2 × 5 = 10

**Result:** Ad Y has **5x higher probability** of being selected on the premium screen.

### Example 2: Different Weight Ads, Same Screen

**Scenario:**
- Ad A: weight = 1
- Ad B: weight = 3
- Ad C: weight = 5
- Screen: classification = 2

**Effective Weights:**
- Ad A: 1 × 2 = 2
- Ad B: 3 × 2 = 6
- Ad C: 5 × 2 = 10

**Selection Probabilities:**
- Ad A: 2/18 = ~11.1%
- Ad B: 6/18 = ~33.3%
- Ad C: 10/18 = ~55.6%

### Example 3: Pay-Per-Attention Model

**Scenario:**
- Standard Ad: weight = 1
- Premium Ad: weight = 5
- Standard Screen: classification = 1
- Premium Screen: classification = 5

**On Standard Screen:**
- Standard Ad: 1 × 1 = 1
- Premium Ad: 5 × 1 = 5
- Premium Ad has 5x higher probability

**On Premium Screen:**
- Standard Ad: 1 × 5 = 5
- Premium Ad: 5 × 5 = 25
- Premium Ad has 25x higher probability (5x more than on standard screen)

**Business Value:** Advertisers pay more for higher weights, and premium screens amplify this effect, creating a true pay-per-attention model.

---

## Implementation Details

### Single Ad Delivery (`AdDeliveryService`)

When a single ad is requested:

```scala
private def pickAd(ads: List[Ad], screenClassification: Int = 1): Option[Ad] = {
  val weightedPool = ads.flatMap { ad =>
    val effectiveWeight = ad.weight × screenClassification
    List.fill(effectiveWeight)(ad)  // Add ad to pool 'effectiveWeight' times
  }
  weightedPool(Random.nextInt(weightedPool.length))  // Random selection
}
```

### Playlist Generation (`PlaylistService`)

When generating a playlist for a screen:

```scala
val weightedPool = adsWithDuration.flatMap { ad =>
  val effectiveWeight = ad.weight × screenClassification
  List.fill(effectiveWeight)(ad)
}

// Select ads repeatedly from the weighted pool to fill target duration
while (currentDuration < targetDurationSeconds) {
  val selectedAd = weightedPool(Random.nextInt(weightedPool.length))
  // Add to playlist...
}
```

### Campaign-Based Selection (`CampaignPlaylistService`)

For campaign-based playlists, the system uses **campaign priority**:

```scala
private def buildWeightedPool(
  campaigns: List[Campaign],
  campaignCreatives: Map[String, List[Creative]]
): List[(Creative, String)] = {
  campaigns.flatMap { campaign =>
    val creatives = campaignCreatives.getOrElse(campaign.id, List.empty)
    val weight = math.max(1, campaign.priority)  // Campaign priority as weight
    activeCreatives.flatMap(creative => 
      List.fill(weight)((creative, campaign.id))  // Each creative appears 'weight' times
    )
  }
}
```

---

## Configuration

### Setting Ad Weight

When creating an ad via the API:
```json
{
  "weight": 5,  // Higher weight = more likely to be selected
  ...
}
```

Default weight is **1** (equal probability with other weight-1 ads).

### Setting Screen Classification

When registering a screen:
```json
{
  "classification": 5,  // 1-10, higher = premium screen
  ...
}
```

Default classification is **1** (standard screen).

---

## Benefits

1. **Fair Distribution**: Lower-weight ads still have a chance to be selected
2. **Pay-Per-Attention**: Premium advertisers (high weight) get more exposure
3. **Screen Premium**: Premium screens amplify ad weights, increasing value
4. **Dynamic**: Works automatically without manual intervention
5. **Scalable**: Handles any number of ads with different weights

---

## Use Cases

### Use Case 1: Tiered Pricing Model
- Standard tier: weight = 1
- Premium tier: weight = 3
- Platinum tier: weight = 5

### Use Case 2: Campaign Priority
- High-priority campaign: weight = 10
- Normal campaign: weight = 3
- Low-priority campaign: weight = 1

### Use Case 3: Location-Based Premium
- Airport screens: classification = 8
- Mall screens: classification = 5
- Transit screens: classification = 2
- Roadside screens: classification = 1

---

## Technical Notes

- **Minimum Weight**: System ensures weight is at least 1 (if weight is 0 or negative, it's treated as 1)
- **Minimum Classification**: System ensures classification is at least 1
- **Random Selection**: Uses `scala.util.Random` for fair random selection
- **Repetition Allowed**: In playlists, the same ad can appear multiple times based on its weight

---

## Summary

The weight-based system creates a **probabilistic selection mechanism** where:
- **Higher ad weight** = More entries in the pool = Higher selection probability
- **Higher screen classification** = Multiplier effect = Amplifies ad weights
- **Combined effect** = Pay-per-attention model where premium screens favor premium ads

This creates a dynamic, fair, and monetizable ad serving system that automatically balances exposure based on advertiser investment and screen premium.

