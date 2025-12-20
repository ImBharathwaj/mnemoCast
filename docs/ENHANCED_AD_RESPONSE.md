# 📺 Screen Client Ad Response Format

## ✅ Screen Client Compatible Response

The ad delivery endpoints now return responses in the exact format expected by screen clients, including:
- Ad identification and metadata
- Creative download URL
- Display timing information
- Campaign and targeting details

---

## 📊 Response Structure

### Single Ad Delivery Response

**Endpoint:** `GET /api/v1/screens/{screenId}/ads/deliver`

**Response:** `ScreenClientAdResponse` (JSON format matching screen client expectations)

```json
{
  "id": "ad-summer-sale-001",
  "title": "Summer Sale 2025",
  "type": "image",
  "contentUrl": "http://localhost:9000/api/v1/media/creatives/summer-sale-banner.jpg",
  "duration": 30,
  "startTime": "2025-12-19T10:00:00Z",
  "endTime": "2025-12-19T10:00:30Z",
  "priority": 8,
  "metadata": {
    "campaignId": "campaign-001",
    "targetAudience": "all"
  }
}
```

### Batch Ad Delivery Response

**Endpoint:** `GET /api/v1/screens/{screenId}/ads/batch?count=10`

**Response:** `BatchAdResponse` (array of `ScreenClientAdResponse`)

```json
{
  "screenId": "screen-chennai-airport-1",
  "screenName": "Chennai Airport Terminal 1 - Gate A1",
  "ads": [
    {
      "id": "ad-summer-sale-001",
      "title": "Summer Sale 2025",
      "type": "image",
      "contentUrl": "http://localhost:9000/api/v1/media/creatives/summer-sale-banner.jpg",
      "duration": 30,
      "startTime": "2025-12-19T10:00:00Z",
      "endTime": "2025-12-19T10:00:30Z",
      "priority": 8,
      "metadata": {
        "campaignId": "campaign-001",
        "targetAudience": "all"
      }
    },
    {
      "id": "ad-coffee-001",
      "title": "Coffee Promotion",
      "type": "image",
      "contentUrl": "http://localhost:9000/api/v1/media/creatives/coffee-banner.jpg",
      "duration": 15,
      "startTime": "2025-12-19T10:00:30Z",
      "endTime": "2025-12-19T10:00:45Z",
      "priority": 7,
      "metadata": {
        "campaignId": "campaign-002",
        "targetAudience": "Chennai"
      }
    }
  ],
  "totalDurationSeconds": 45,
  "requestedCount": 10,
  "actualCount": 2,
  "requestedDurationMinutes": null
}
```

---

## 📋 Field Descriptions

### Ad Identification
- **`id`**: Unique ad identifier (e.g., `"ad-summer-sale-001"`)
- **`title`**: Human-readable ad title (auto-generated from ad ID or advertiser)

### Creative Information
- **`type`**: Creative type (`"image"` or `"video"`)
  - Automatically detected from file extension
  - Helps screen client choose appropriate player
- **`contentUrl`**: **Direct URL to download the creative file**
  - Use this URL to download the creative locally
  - Supports HTTP GET requests
  - File types: images (jpg, png, gif, webp) or videos (mp4, webm, mov, avi)

### Display Timing
- **`duration`**: Ad duration in seconds (optional)
  - For images: display duration
  - For videos: video length
- **`startTime`**: ISO 8601 timestamp when ad should start displaying
  - Format: `"2025-12-19T10:00:00Z"`
  - Set to current time when ad is delivered
- **`endTime`**: ISO 8601 timestamp when ad should stop displaying
  - Format: `"2025-12-19T10:00:30Z"`
  - Calculated as `startTime + duration`

### Ad Priority
- **`priority`**: Ad priority/weight (higher = more important)
  - Used for ad selection and scheduling
  - Range: typically 1-10

### Metadata
- **`metadata.campaignId`**: Campaign identifier (or `"unknown"` if not linked)
  - Links ad to its parent campaign
- **`metadata.targetAudience`**: Target audience description
  - `"all"` if no targeting rules
  - City names if location-based targeting (e.g., `"Chennai, Mumbai"`)
  - `"targeted"` if other targeting rules apply

---

## 🔄 Screen Client Workflow

### 1. Request Ad

```bash
GET /api/v1/screens/{screenId}/ads/deliver
Headers:
  X-Screen-Id: {screenId}
  X-Screen-Passkey: {passkey}
```

### 2. Receive Response

```json
{
  "id": "ad-123",
  "title": "Summer Sale 2025",
  "type": "video",
  "contentUrl": "http://localhost:9000/api/v1/media/creatives/video.mp4",
  "duration": 30,
  "startTime": "2025-12-19T10:00:00Z",
  "endTime": "2025-12-19T10:00:30Z",
  "priority": 8,
  "metadata": {
    "campaignId": "campaign-001",
    "targetAudience": "all"
  }
}
```

### 3. Download Creative

```bash
# Download creative using contentUrl
curl -o /local/storage/ad-123-video.mp4 \
  "http://localhost:9000/api/v1/media/creatives/video.mp4"
```

### 4. Display Ad

- Use `type` to choose player (`"image"` → image viewer, `"video"` → video player)
- Use `duration` to set display duration
- Use `startTime` and `endTime` for scheduling
- Use `priority` for playlist ordering

### 5. Track Impression (Optional)

The response doesn't include impression tracking URL in the new format, but you can still track impressions by calling:
```bash
GET http://localhost:8080/api/v1/events/impression?adId={id}&requestId={requestId}
```

---

## 📥 Creative Download

### Download URL Format

**MinIO Storage:**
```
http://{server}:9000/mnemocast-creatives/creatives/{filename}
```

**Local Storage:**
```
http://{server}:8080/api/v1/media/creatives/{filename}
```

### Download Example

```python
import requests
from datetime import datetime

# Get ad
response = requests.get(
    "http://localhost:8080/api/v1/screens/screen-123/ads/deliver",
    headers={
        "X-Screen-Id": "screen-123",
        "X-Screen-Passkey": "passkey"
    }
)
ad = response.json()

# Download creative using contentUrl
creative_response = requests.get(ad["contentUrl"])
extension = "mp4" if ad["type"] == "video" else "jpg"
with open(f"/local/ads/{ad['id']}.{extension}", "wb") as f:
    f.write(creative_response.content)

# Parse timing
start_time = datetime.fromisoformat(ad["startTime"].replace("Z", "+00:00"))
end_time = datetime.fromisoformat(ad["endTime"].replace("Z", "+00:00"))
duration = ad["duration"] or 30

# Display ad
display_ad(
    ad_type=ad["type"],
    file_path=f"/local/ads/{ad['id']}.{extension}",
    duration=duration,
    start_time=start_time,
    end_time=end_time,
    priority=ad["priority"]
)
```

---

## 🎯 Creative Type Detection

The system automatically detects creative type from URL:

- **Video:** `.mp4`, `.webm`, `.mov`, `.avi` → `"video"`
- **Image:** `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp` → `"image"`
- **Unknown:** Other file types → `"unknown"`

Screen clients can use `type` field to:
- Choose appropriate player
- Validate file before download
- Set correct MIME type

---

## 📊 Metadata Usage

### Campaign Information
- **`campaignId`**: Links ad to its parent campaign
  - Use for analytics and reporting
  - `"unknown"` if ad is not linked to a campaign

### Target Audience
- **`targetAudience`**: Describes who the ad targets
  - `"all"`: No targeting restrictions
  - City names: Location-based targeting (e.g., `"Chennai, Mumbai"`)
  - `"targeted"`: Other targeting rules apply

### Ad Priority
- **`priority`**: Higher priority ads are more likely to be selected
  - Used for ad selection algorithm
  - Useful for understanding ad importance

---

## ✅ Benefits

1. **Screen Client Compatible**
   - Exact JSON format expected by screen clients
   - No transformation needed on client side
   - Standardized response structure

2. **Complete Information**
   - All ad metadata included
   - Campaign and targeting information
   - No need for additional API calls

3. **Easy Download**
   - Direct `contentUrl` for download
   - Automatic type detection
   - Clear download instructions

4. **Better Scheduling**
   - `startTime` and `endTime` for precise scheduling
   - `duration` for playback control
   - `priority` for playlist ordering

---

## 🔍 Example Usage

### Single Ad Request

```bash
curl -X GET "http://localhost:8080/api/v1/screens/screen-123/ads/deliver" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: passkey"
```

**Response:**
```json
{
  "id": "ad-123",
  "title": "Summer Sale 2025",
  "type": "video",
  "contentUrl": "http://localhost:9000/api/v1/media/creatives/video.mp4",
  "duration": 30,
  "startTime": "2025-12-19T10:00:00Z",
  "endTime": "2025-12-19T10:00:30Z",
  "priority": 8,
  "metadata": {
    "campaignId": "campaign-001",
    "targetAudience": "all"
  }
}
```

### Batch Ad Request

```bash
curl -X GET "http://localhost:8080/api/v1/screens/screen-123/ads/batch?count=5" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: passkey"
```

**Response:**
```json
{
  "screenId": "screen-123",
  "screenName": "Chennai Airport Terminal 1",
  "ads": [
    {
      "id": "ad-123",
      "title": "Summer Sale 2025",
      "type": "video",
      "contentUrl": "http://localhost:9000/api/v1/media/creatives/video.mp4",
      "duration": 30,
      "startTime": "2025-12-19T10:00:00Z",
      "endTime": "2025-12-19T10:00:30Z",
      "priority": 8,
      "metadata": {
        "campaignId": "campaign-001",
        "targetAudience": "all"
      }
    },
    ...
  ],
  "totalDurationSeconds": 150,
  "requestedCount": 5,
  "actualCount": 5
}
```

---

## 📝 Summary

✅ **Screen Client Response Format:**
- Ad identification (`id`, `title`)
- Creative information (`type`, `contentUrl`)
- Display timing (`duration`, `startTime`, `endTime`)
- Priority (`priority`)
- Metadata (`campaignId`, `targetAudience`)

✅ **Screen Client Can:**
- Download creatives using `contentUrl`
- Choose appropriate player using `type`
- Schedule playback using `startTime`, `endTime`, and `duration`
- Order playlist using `priority`
- Link to campaigns using `metadata.campaignId`

**The response format now exactly matches what screen clients expect!** 🎉

---

**Status:** ✅ Complete  
**Date:** 2025-12-19  
**Version:** 4.3.0

