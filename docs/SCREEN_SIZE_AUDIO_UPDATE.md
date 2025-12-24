#  Screen Display Size and Audio Capability Update

**Update:** Added display size (width, height) and audio capability fields to Screen model and APIs.

---

##  Changes Made

### 1. Domain Model Updates

**Screen Model** (`backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Screen.scala`):
- Added `width: Option[Int]` - Display width in pixels
- Added `height: Option[Int]` - Display height in pixels  
- Added `isAudible: Boolean` - Whether the display supports audio playback

**CreateScreenRequest** (`backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/CreateScreenRequest.scala`):
- Added `width: Option[Int]`
- Added `height: Option[Int]`
- Added `isAudible: Boolean` (default: false)

### 2. Database Schema Updates

**PostgreSQL** (`infra/local-dev/postgres/init.sql`):
- Added `width INTEGER` column to `screens` table
- Added `height INTEGER` column to `screens` table
- Added `is_audible BOOLEAN NOT NULL DEFAULT false` column to `screens` table

**Migration Required:**
```sql
ALTER TABLE screens 
  ADD COLUMN IF NOT EXISTS width INTEGER,
  ADD COLUMN IF NOT EXISTS height INTEGER,
  ADD COLUMN IF NOT EXISTS is_audible BOOLEAN NOT NULL DEFAULT false;
```

### 3. API Updates

**ScreenRoutes** (`backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/ScreenRoutes.scala`):
- Updated `POST /api/v1/screens/register` to accept width, height, isAudible
- Updated `PUT /api/v1/screens/{screenId}` to accept width, height, isAudible

**PostgresScreenStore** (`backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/postgres/PostgresScreenStore.scala`):
- Updated SQL queries to include new columns
- Updated row mapping to handle nullable width/height

### 4. Dashboard Updates

**TypeScript Types** (`dashboard/src/types/index.ts`):
- Added `width?: number` to Screen interface
- Added `height?: number` to Screen interface
- Added `isAudible: boolean` to Screen interface

**CreateScreenModal** (`dashboard/src/components/CreateScreenModal.tsx`):
- Added width and height input fields
- Added audio capability checkbox

**EditScreenModal** (`dashboard/src/components/EditScreenModal.tsx`):
- Added width and height input fields
- Added audio capability checkbox

**ScreenCard** (`dashboard/src/pages/Screens.tsx`):
- Displays display size (width × height)
- Shows audio capability status

---

##  API Examples

### Register Screen with Display Size

```bash
curl -X POST http://localhost:8080/api/v1/screens/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mall Food Court Screen 1",
    "location": {
      "city": "Chennai",
      "area": "Velachery",
      "venueType": "mall"
    },
    "width": 1920,
    "height": 1080,
    "isAudible": true,
    "classification": 5
  }'
```

### Update Screen Display Size

```bash
curl -X PUT http://localhost:8080/api/v1/screens/screen-1 \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Mall Food Court Screen 1",
    "location": {
      "city": "Chennai",
      "area": "Velachery",
      "venueType": "mall"
    },
    "width": 3840,
    "height": 2160,
    "isAudible": true,
    "classification": 5
  }'
```

---

##  Use Cases

### 1. Ad Targeting by Display Size
- Serve high-resolution creatives to 4K displays (3840×2160)
- Serve standard creatives to HD displays (1920×1080)
- Optimize creative dimensions for specific screen sizes

### 2. Audio Capability Targeting
- Serve video ads with audio to audible displays
- Serve silent/visual-only ads to non-audible displays
- Match creative type (video with sound vs. silent video) to screen capability

### 3. Creative Optimization
- Pre-generate creatives in multiple resolutions
- Select appropriate creative based on screen dimensions
- Ensure aspect ratio compatibility

---

##  Migration Steps

### For Existing Databases

1. **Run Migration SQL:**
```sql
ALTER TABLE screens 
  ADD COLUMN IF NOT EXISTS width INTEGER,
  ADD COLUMN IF NOT EXISTS height INTEGER,
  ADD COLUMN IF NOT EXISTS is_audible BOOLEAN NOT NULL DEFAULT false;
```

2. **Update Existing Screens (Optional):**
```sql
-- Set default values for existing screens if needed
UPDATE screens 
SET width = 1920, height = 1080, is_audible = false
WHERE width IS NULL;
```

### For Redis Storage

Redis storage uses JSON serialization, so existing screens will work with default values (width=None, height=None, isAudible=false). New fields will be automatically included when screens are updated.

---

##  Field Details

### width (Optional Integer)
- **Purpose:** Display width in pixels
- **Example Values:** 1920, 3840, 1280
- **Use Case:** Match creative resolution to screen size

### height (Optional Integer)
- **Purpose:** Display height in pixels
- **Example Values:** 1080, 2160, 720
- **Use Case:** Match creative resolution to screen size

### isAudible (Boolean)
- **Purpose:** Whether the display supports audio playback
- **Default:** false
- **Use Case:** 
  - true: Can serve video ads with sound
  - false: Should serve silent/visual-only ads

---

##  Testing

### Test Screen Registration
```bash
# Register screen with display size
curl -X POST http://localhost:8080/api/v1/screens/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Test Screen",
    "location": {"city": "Test"},
    "width": 1920,
    "height": 1080,
    "isAudible": true
  }'
```

### Test Screen Retrieval
```bash
# Get screen details
curl http://localhost:8080/api/v1/screens/screen-id

# Response should include:
# {
#   "id": "screen-id",
#   "name": "Test Screen",
#   "width": 1920,
#   "height": 1080,
#   "isAudible": true,
#   ...
# }
```

---

##  Checklist

- [x] Update Screen domain model
- [x] Update CreateScreenRequest model
- [x] Update database schema
- [x] Update PostgresScreenStore
- [x] Update ScreenRoutes API
- [x] Update dashboard TypeScript types
- [x] Update CreateScreenModal
- [x] Update EditScreenModal
- [x] Update ScreenCard display
- [ ] Run database migration
- [ ] Test API endpoints
- [ ] Test dashboard UI

---

**Last Updated:** December 2024

