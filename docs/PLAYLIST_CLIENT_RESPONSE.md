# Playlist Client Response Format

> This document describes the exact response format that client systems receive when requesting playlists from the MnemoCast ad serving engine.

---

## Table of Contents

1. [API Endpoint](#api-endpoint)
2. [Response Structure](#response-structure)
3. [Response Fields](#response-fields)
4. [Example Responses](#example-responses)
5. [Error Responses](#error-responses)
6. [Client Usage Guide](#client-usage-guide)

---

## API Endpoint

### Request

```
GET /api/v1/screens/{screenId}/playlist?durationMinutes={minutes}
```

**Path Parameters:**
- `screenId` (required): Screen identifier requesting the playlist

**Query Parameters:**
- `durationMinutes` (optional, default: 3): Desired playlist duration in minutes

**Example:**
```
GET /api/v1/screens/screen-123/playlist?durationMinutes=5
```

---

## Response Structure

### Success Response (200 OK)

The client receives a JSON object with the following structure:

```json
{
  "requestId": "string",
  "screenId": "string | null",
  "items": [
    {
      "adId": "string",
      "creativeUrl": "string",
      "targetUrl": "string | null",
      "durationSeconds": number,
      "impressionTrackingUrl": "string | null",
      "position": number
    }
  ],
  "validForSeconds": number,
  "totalDurationSeconds": number
}
```

### HTTP Status Codes

| Status Code | Meaning | Response Body |
|-------------|---------|---------------|
| `200 OK` | Playlist generated successfully | `PlaylistResponse` JSON |
| `204 No Content` | No playlist available (no eligible ads) | Empty body or error message |
| `404 Not Found` | Screen not found | Error message |
| `500 Internal Server Error` | Server error | Error message |

---

## Response Fields

### PlaylistResponse (Root Object)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `requestId` | String | Yes | Unique identifier for this playlist request. Used for tracking and debugging. |
| `screenId` | String \| null | Yes | Screen identifier for which playlist was generated. Matches the `screenId` from the request path. |
| `items` | Array[PlaylistItem] | Yes | Ordered list of ads to play. Items are in playback order (position 0 plays first). |
| `validForSeconds` | Integer | Yes | Time-to-live (TTL) for this playlist in seconds. Client should request a new playlist after this time expires. Typically equals `durationMinutes * 60`. |
| `totalDurationSeconds` | Integer | Yes | Total duration of the playlist in seconds. Sum of all item durations. May slightly exceed requested duration. |

### PlaylistItem (Array Element)

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `adId` | String | Yes | Unique identifier of the ad. Used for tracking and analytics. |
| `creativeUrl` | String | Yes | **Primary field for client**: URL to the creative asset (video/image) to be displayed. Client should fetch and play this media. |
| `targetUrl` | String \| null | Optional | Click-through URL. If provided, client should navigate here when ad is clicked/tapped. |
| `durationSeconds` | Integer | Yes | **Duration for playback**: How long (in seconds) this ad should be displayed. Client must respect this duration. |
| `impressionTrackingUrl` | String \| null | Optional | **Tracking URL**: Client should call this URL when the ad starts playing (impression event). Used for analytics and budget tracking. |
| `position` | Integer | Yes | **Playback order**: Zero-based index indicating position in playlist. Position 0 plays first, then 1, 2, etc. |

---

## Example Responses

### Example 1: Basic 5-Minute Playlist

**Request:**
```
GET /api/v1/screens/screen-123/playlist?durationMinutes=5
```

**Response (200 OK):**
```json
{
  "requestId": "req-550e8400-e29b-41d4-a716-446655440000",
  "screenId": "screen-123",
  "items": [
    {
      "adId": "ad-forum-mall-food-court-001",
      "creativeUrl": "https://example.com/creatives/forum-mall-food-ad.mp4",
      "targetUrl": "https://example.com/landing/forum-mall-promo",
      "durationSeconds": 30,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-forum-mall-food-court-001&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=0",
      "position": 0
    },
    {
      "adId": "ad-coffee-morning-002",
      "creativeUrl": "https://example.com/creatives/coffee-morning-ad.mp4",
      "targetUrl": null,
      "durationSeconds": 60,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-coffee-morning-002&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=1",
      "position": 1
    },
    {
      "adId": "ad-forum-mall-food-court-001",
      "creativeUrl": "https://example.com/creatives/forum-mall-food-ad.mp4",
      "targetUrl": "https://example.com/landing/forum-mall-promo",
      "durationSeconds": 30,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-forum-mall-food-court-001&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=2",
      "position": 2
    },
    {
      "adId": "ad-premium-brand-003",
      "creativeUrl": "https://example.com/creatives/premium-brand-ad.mp4",
      "targetUrl": "https://example.com/brand-landing",
      "durationSeconds": 45,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-premium-brand-003&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=3",
      "position": 3
    },
    {
      "adId": "ad-coffee-morning-002",
      "creativeUrl": "https://example.com/creatives/coffee-morning-ad.mp4",
      "targetUrl": null,
      "durationSeconds": 60,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-coffee-morning-002&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=4",
      "position": 4
    },
    {
      "adId": "ad-forum-mall-food-court-001",
      "creativeUrl": "https://example.com/creatives/forum-mall-food-ad.mp4",
      "targetUrl": "https://example.com/landing/forum-mall-promo",
      "durationSeconds": 30,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-forum-mall-food-court-001&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=5",
      "position": 5
    },
    {
      "adId": "ad-premium-brand-003",
      "creativeUrl": "https://example.com/creatives/premium-brand-ad.mp4",
      "targetUrl": "https://example.com/brand-landing",
      "durationSeconds": 45,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-premium-brand-003&requestId=req-550e8400-e29b-41d4-a716-446655440000&position=6",
      "position": 6
    }
  ],
  "validForSeconds": 300,
  "totalDurationSeconds": 300
}
```

**Client Actions:**
1. Parse JSON response
2. Play items in order (position 0 → 1 → 2 → ...)
3. For each item:
   - Fetch media from `creativeUrl`
   - Display for `durationSeconds`
   - Call `impressionTrackingUrl` when ad starts
   - Navigate to `targetUrl` if clicked (if provided)
4. After `validForSeconds` expires, request new playlist

---

### Example 2: Short Playlist (1 Minute)

**Request:**
```
GET /api/v1/screens/screen-456/playlist?durationMinutes=1
```

**Response (200 OK):**
```json
{
  "requestId": "req-660e8400-e29b-41d4-a716-446655440001",
  "screenId": "screen-456",
  "items": [
    {
      "adId": "ad-quick-promo-004",
      "creativeUrl": "https://example.com/creatives/quick-promo.mp4",
      "targetUrl": null,
      "durationSeconds": 30,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-quick-promo-004&requestId=req-660e8400-e29b-41d4-a716-446655440001&position=0",
      "position": 0
    },
    {
      "adId": "ad-quick-promo-004",
      "creativeUrl": "https://example.com/creatives/quick-promo.mp4",
      "targetUrl": null,
      "durationSeconds": 30,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-quick-promo-004&requestId=req-660e8400-e29b-41d4-a716-446655440001&position=1",
      "position": 1
    }
  ],
  "validForSeconds": 60,
  "totalDurationSeconds": 60
}
```

**Note:** Same ad repeated twice to fill duration (repetition is allowed)

---

### Example 3: No Playlist Available

**Request:**
```
GET /api/v1/screens/screen-789/playlist?durationMinutes=5
```

**Response (204 No Content):**
```
No playlist available
```

**Or Empty Body:**
```
(empty response body)
```

**Client Actions:**
- Handle 204 status code
- Display default content or retry after delay
- Log that no ads are available

---

### Example 4: Screen Not Found

**Request:**
```
GET /api/v1/screens/invalid-screen-id/playlist?durationMinutes=5
```

**Response (404 Not Found):**
```
Screen not found
```

---

### Example 5: Long Playlist (10 Minutes)

**Request:**
```
GET /api/v1/screens/screen-999/playlist?durationMinutes=10
```

**Response (200 OK):**
```json
{
  "requestId": "req-770e8400-e29b-41d4-a716-446655440002",
  "screenId": "screen-999",
  "items": [
    {
      "adId": "ad-long-format-005",
      "creativeUrl": "https://example.com/creatives/long-format-ad.mp4",
      "targetUrl": "https://example.com/long-landing",
      "durationSeconds": 60,
      "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-long-format-005&requestId=req-770e8400-e29b-41d4-a716-446655440002&position=0",
      "position": 0
    }
    // ... (many more items)
  ],
  "validForSeconds": 600,
  "totalDurationSeconds": 600
}
```

---

## Error Responses

### 204 No Content

**When:** No eligible ads available for the screen

**Response:**
```
Status: 204 No Content
Body: "No playlist available" (or empty)
```

**Possible Reasons:**
- No active ads in database
- All ads filtered out by targeting rules
- All ads filtered out by budget constraints
- All ads filtered out by frequency capping
- No ads have duration specified

**Client Handling:**
- Display default/fallback content
- Retry after a delay (e.g., 30 seconds)
- Log the event for monitoring

---

### 404 Not Found

**When:** Screen ID not found in database

**Response:**
```
Status: 404 Not Found
Body: "Screen not found"
```

**Client Handling:**
- Verify screen ID is correct
- Register screen if not already registered
- Retry with correct screen ID

---

### 500 Internal Server Error

**When:** Server-side error occurred

**Response:**
```
Status: 500 Internal Server Error
Body: "Internal server error: {error message}"
```

**Client Handling:**
- Log error for debugging
- Retry with exponential backoff
- Fall back to cached playlist if available

---

## Client Usage Guide

### Step-by-Step Client Implementation

#### 1. Request Playlist

```javascript
// Example: JavaScript/TypeScript
async function fetchPlaylist(screenId, durationMinutes = 5) {
  const url = `http://localhost:8080/api/v1/screens/${screenId}/playlist?durationMinutes=${durationMinutes}`;
  
  const response = await fetch(url);
  
  if (response.status === 200) {
    const playlist = await response.json();
    return playlist;
  } else if (response.status === 204) {
    // No playlist available
    return null;
  } else {
    throw new Error(`Failed to fetch playlist: ${response.status}`);
  }
}
```

#### 2. Parse Response

```javascript
const playlist = await fetchPlaylist('screen-123', 5);

if (playlist) {
  console.log(`Playlist ID: ${playlist.requestId}`);
  console.log(`Screen ID: ${playlist.screenId}`);
  console.log(`Total duration: ${playlist.totalDurationSeconds}s`);
  console.log(`Valid for: ${playlist.validForSeconds}s`);
  console.log(`Number of items: ${playlist.items.length}`);
}
```

#### 3. Play Items Sequentially

```javascript
async function playPlaylist(playlist) {
  for (const item of playlist.items) {
    // 1. Track impression
    if (item.impressionTrackingUrl) {
      await fetch(item.impressionTrackingUrl, { method: 'GET' });
    }
    
    // 2. Load and display creative
    await loadAndDisplayCreative(item.creativeUrl, item.durationSeconds);
    
    // 3. Handle click (if targetUrl provided)
    if (item.targetUrl) {
      setupClickHandler(item.targetUrl);
    }
    
    // 4. Wait for duration
    await sleep(item.durationSeconds * 1000);
  }
}
```

#### 4. Handle Playlist Expiry

```javascript
function schedulePlaylistRefresh(playlist) {
  const refreshDelay = playlist.validForSeconds * 1000; // Convert to milliseconds
  
  setTimeout(async () => {
    console.log('Playlist expired, fetching new playlist...');
    const newPlaylist = await fetchPlaylist(playlist.screenId, 5);
    if (newPlaylist) {
      playPlaylist(newPlaylist);
      schedulePlaylistRefresh(newPlaylist);
    }
  }, refreshDelay);
}
```

---

### Client Responsibilities

#### Required Actions

1. **Play Items in Order**
   - Items must be played sequentially by `position` (0, 1, 2, ...)
   - Do not skip or reorder items

2. **Respect Duration**
   - Display each ad for exactly `durationSeconds`
   - Do not truncate or extend duration

3. **Track Impressions**
   - Call `impressionTrackingUrl` when ad starts playing
   - Use GET request (or POST if required)
   - Handle tracking failures gracefully (don't block playback)

4. **Handle Click-Through**
   - If `targetUrl` is provided, navigate when ad is clicked/tapped
   - Open in new window/tab if appropriate

5. **Refresh Playlist**
   - Request new playlist after `validForSeconds` expires
   - Handle 204 (no content) gracefully

#### Optional Actions

1. **Preload Next Item**
   - Preload `creativeUrl` of next item while current item plays
   - Improves playback smoothness

2. **Cache Playlist**
   - Cache playlist response for offline playback
   - Refresh when `validForSeconds` expires

3. **Error Handling**
   - Retry failed creative loads
   - Fallback to next item if creative fails to load
   - Log errors for debugging

4. **Analytics**
   - Track which ads were actually played
   - Monitor playlist refresh frequency
   - Report playback errors

---

### Example: Complete Client Implementation

```javascript
class PlaylistPlayer {
  constructor(screenId, baseUrl = 'http://localhost:8080') {
    this.screenId = screenId;
    this.baseUrl = baseUrl;
    this.currentPlaylist = null;
    this.refreshTimer = null;
  }
  
  async start(durationMinutes = 5) {
    await this.loadAndPlay(durationMinutes);
  }
  
  async loadAndPlay(durationMinutes) {
    try {
      const playlist = await this.fetchPlaylist(durationMinutes);
      
      if (!playlist) {
        console.warn('No playlist available, retrying in 30s...');
        setTimeout(() => this.loadAndPlay(durationMinutes), 30000);
        return;
      }
      
      this.currentPlaylist = playlist;
      this.scheduleRefresh(playlist.validForSeconds);
      await this.playItems(playlist.items);
      
    } catch (error) {
      console.error('Error loading playlist:', error);
      // Retry after delay
      setTimeout(() => this.loadAndPlay(durationMinutes), 60000);
    }
  }
  
  async fetchPlaylist(durationMinutes) {
    const url = `${this.baseUrl}/api/v1/screens/${this.screenId}/playlist?durationMinutes=${durationMinutes}`;
    const response = await fetch(url);
    
    if (response.status === 200) {
      return await response.json();
    } else if (response.status === 204) {
      return null;
    } else {
      throw new Error(`HTTP ${response.status}: ${await response.text()}`);
    }
  }
  
  async playItems(items) {
    for (const item of items) {
      // Track impression
      if (item.impressionTrackingUrl) {
        this.trackImpression(item.impressionTrackingUrl);
      }
      
      // Play creative
      await this.playCreative(item);
    }
    
    // Playlist finished, load new one
    await this.loadAndPlay(5);
  }
  
  async playCreative(item) {
    return new Promise((resolve) => {
      // Load creative media
      const media = this.loadMedia(item.creativeUrl);
      
      // Setup click handler
      if (item.targetUrl) {
        media.onclick = () => {
          window.open(item.targetUrl, '_blank');
        };
      }
      
      // Display media
      this.displayMedia(media);
      
      // Wait for duration
      setTimeout(() => {
        this.hideMedia(media);
        resolve();
      }, item.durationSeconds * 1000);
    });
  }
  
  trackImpression(url) {
    // Fire-and-forget tracking
    fetch(url).catch(err => console.error('Tracking failed:', err));
  }
  
  scheduleRefresh(validForSeconds) {
    if (this.refreshTimer) {
      clearTimeout(this.refreshTimer);
    }
    
    this.refreshTimer = setTimeout(() => {
      console.log('Playlist expired, refreshing...');
      this.loadAndPlay(5);
    }, validForSeconds * 1000);
  }
}

// Usage
const player = new PlaylistPlayer('screen-123');
player.start(5); // Start with 5-minute playlist
```

---

## Response Field Details

### requestId

- **Purpose**: Unique identifier for tracking and debugging
- **Format**: UUID string (e.g., `"550e8400-e29b-41d4-a716-446655440000"`)
- **Usage**: Include in tracking URLs and error logs
- **Example**: `"req-550e8400-e29b-41d4-a716-446655440000"`

### screenId

- **Purpose**: Identifies the screen this playlist is for
- **Format**: String (matches request path parameter)
- **Can be null**: Yes (if screen not found, but playlist still generated)
- **Example**: `"screen-123"` or `null`

### items

- **Purpose**: Ordered list of ads to play
- **Type**: Array of PlaylistItem objects
- **Order**: Must be played sequentially by `position` field
- **Can be empty**: No (if empty, response would be 204)
- **Repetition**: Yes, same ad can appear multiple times

### validForSeconds

- **Purpose**: TTL (time-to-live) for playlist validity
- **Unit**: Seconds
- **Typical value**: `durationMinutes * 60`
- **Usage**: Client should request new playlist after this time
- **Example**: `300` (5 minutes)

### totalDurationSeconds

- **Purpose**: Total playback duration of all items
- **Unit**: Seconds
- **Calculation**: Sum of all `item.durationSeconds`
- **May exceed**: Yes, may slightly exceed requested duration
- **Example**: `300` (exactly 5 minutes) or `315` (slightly over)

### adId

- **Purpose**: Unique identifier for the ad
- **Format**: String
- **Usage**: Used in tracking URLs and analytics
- **Example**: `"ad-forum-mall-food-court-001"`

### creativeUrl

- **Purpose**: **Primary field** - URL to media asset to display
- **Format**: HTTP/HTTPS URL string
- **Media types**: Video (MP4, WebM) or Image (JPG, PNG, WebP)
- **Required**: Yes - client must fetch and display this
- **Example**: `"https://example.com/creatives/ad.mp4"`

### targetUrl

- **Purpose**: Click-through URL for ad interaction
- **Format**: HTTP/HTTPS URL string or `null`
- **Required**: No - only present if ad has click-through
- **Usage**: Navigate when ad is clicked/tapped
- **Example**: `"https://example.com/landing"` or `null`

### durationSeconds

- **Purpose**: **Required** - How long to display the ad
- **Unit**: Seconds
- **Required**: Yes - client must respect this duration
- **Typical values**: 15, 30, 45, 60 seconds
- **Example**: `30`

### impressionTrackingUrl

- **Purpose**: URL to call when ad starts playing (impression event)
- **Format**: HTTP/HTTPS URL string or `null`
- **Method**: GET request (or POST if required)
- **Required**: No, but should be called if provided
- **Query params**: Contains `adId`, `requestId`, `position`
- **Example**: `"http://localhost:8080/api/v1/events/impression?adId=ad-001&requestId=req-123&position=0"`

### position

- **Purpose**: Zero-based index indicating playback order
- **Type**: Integer
- **Range**: 0 to `items.length - 1`
- **Usage**: Play items in order (0 → 1 → 2 → ...)
- **Example**: `0` (first item), `1` (second item)

---

## Client Best Practices

### 1. Error Handling

```javascript
try {
  const playlist = await fetchPlaylist(screenId, 5);
  if (playlist) {
    await playPlaylist(playlist);
  } else {
    // Handle no playlist
    showDefaultContent();
  }
} catch (error) {
  // Handle network/server errors
  console.error('Playlist error:', error);
  retryWithBackoff();
}
```

### 2. Impression Tracking

```javascript
async function trackImpression(url) {
  try {
    // Fire-and-forget (don't block playback)
    fetch(url, { method: 'GET' }).catch(() => {
      // Silently fail - don't interrupt playback
    });
  } catch (error) {
    // Ignore tracking errors
  }
}
```

### 3. Preloading

```javascript
function preloadNextItem(currentIndex, items) {
  if (currentIndex + 1 < items.length) {
    const nextItem = items[currentIndex + 1];
    // Preload creative while current ad plays
    const link = document.createElement('link');
    link.rel = 'prefetch';
    link.href = nextItem.creativeUrl;
    document.head.appendChild(link);
  }
}
```

### 4. Playlist Refresh

```javascript
function scheduleRefresh(validForSeconds) {
  // Request new playlist slightly before expiry
  const refreshDelay = (validForSeconds - 30) * 1000; // 30s buffer
  
  setTimeout(async () => {
    const newPlaylist = await fetchPlaylist(screenId, 5);
    if (newPlaylist) {
      // Seamlessly transition to new playlist
      transitionToNewPlaylist(newPlaylist);
    }
  }, refreshDelay);
}
```

### 5. Offline Support

```javascript
// Cache playlist for offline playback
function cachePlaylist(playlist) {
  localStorage.setItem('cachedPlaylist', JSON.stringify(playlist));
  localStorage.setItem('playlistExpiry', Date.now() + playlist.validForSeconds * 1000);
}

function getCachedPlaylist() {
  const cached = localStorage.getItem('cachedPlaylist');
  const expiry = localStorage.getItem('playlistExpiry');
  
  if (cached && expiry && Date.now() < expiry) {
    return JSON.parse(cached);
  }
  
  return null;
}
```

---

## Testing Playlist Responses

### Using cURL

```bash
# Request 5-minute playlist
curl "http://localhost:8080/api/v1/screens/screen-123/playlist?durationMinutes=5"

# Request 10-minute playlist
curl "http://localhost:8080/api/v1/screens/screen-123/playlist?durationMinutes=10"

# Pretty print JSON response
curl "http://localhost:8080/api/v1/screens/screen-123/playlist?durationMinutes=5" | jq
```

### Using Postman

1. **Method**: GET
2. **URL**: `http://localhost:8080/api/v1/screens/{screenId}/playlist`
3. **Query Params**:
   - `durationMinutes`: 5
4. **Headers**: None required
5. **Response**: JSON `PlaylistResponse`

---

## Summary

When a client system requests a playlist, it receives:

1. **JSON Response** with playlist metadata and ordered list of ads
2. **Ordered Items** - Array of ads to play sequentially
3. **Media URLs** - `creativeUrl` for each ad to fetch and display
4. **Duration Info** - How long to display each ad
5. **Tracking URLs** - URLs to call for impression tracking
6. **Validity Period** - How long the playlist is valid before refresh

The client's primary responsibilities are:
- ✅ Play items in order (by `position`)
- ✅ Display each ad for `durationSeconds`
- ✅ Call `impressionTrackingUrl` when ad starts
- ✅ Navigate to `targetUrl` if ad is clicked
- ✅ Refresh playlist after `validForSeconds` expires

This format enables client systems to seamlessly integrate with the MnemoCast ad serving engine and display dynamic, context-aware playlists.

