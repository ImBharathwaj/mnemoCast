# Creative Media Upload & Storage Plan

## Overview
Plan for implementing image/video upload functionality for creatives, with centralized storage and serving capabilities in the ad serving engine.

## Architecture Overview

```
Frontend (Dashboard)
    ↓ (multipart/form-data)
Backend API (File Upload Endpoint)
    ↓ (store file)
Storage Layer (Local Filesystem / Object Storage)
    ↓ (generate URL)
Creative Model (store URL in creativeUrl field)
    ↓ (serve files)
File Serving Endpoint
    ↓
Ad Players / Screens (fetch media)
```

## Phase 1: Storage Strategy Selection

### Option A: Local Filesystem (Quick Start)
**Pros:**
- Simple to implement
- No external dependencies
- Good for development/MVP
- Easy to test

**Cons:**
- Not scalable for production
- Single server limitation
- No redundancy
- Manual backup required

**Implementation:**
- Store files in `storage/uploads/creatives/` directory
- Use UUID for filenames to avoid conflicts
- Maintain directory structure: `storage/uploads/creatives/{creativeId}/original.{ext}`

### Option B: Object Storage (Recommended for Production)
**Options:**
1. **AWS S3** (Production-ready)
2. **MinIO** (Self-hosted S3-compatible, good for development/production)
3. **Azure Blob Storage**
4. **Google Cloud Storage**

**Pros:**
- Scalable
- Redundant storage
- Built-in CDN capabilities
- Access control
- Cost-effective at scale

**Cons:**
- Requires external service setup
- Additional configuration
- Potential costs

**Recommendation:** Start with **MinIO** for development (self-hosted, S3-compatible), migrate to AWS S3 for production.

### Option C: Hybrid Approach (Best of Both)
- Local filesystem for development
- Object storage for production
- Abstract storage behind a trait/interface

## Phase 2: Backend Implementation

### 2.1 Storage Abstraction Layer

```scala
trait MediaStorage {
  def upload(file: File, creativeId: String, contentType: String): Future[String] // Returns URL
  def delete(url: String): Future[Unit]
  def getUrl(path: String): String // Returns full URL for serving
  def exists(path: String): Future[Boolean]
}

// Implementations:
// - LocalFileStorage (filesystem)
// - S3Storage (AWS S3)
// - MinIOStorage (MinIO)
```

### 2.2 File Upload Endpoint

**New Route:** `POST /api/v1/creatives/upload`

**Request:**
- Method: POST
- Content-Type: multipart/form-data
- Fields:
  - `file`: File (image/video)
  - `creativeId` (optional): If provided, associates with existing creative
  - `campaignId`: Required for new creative creation

**Response:**
```json
{
  "url": "http://localhost:8080/api/v1/media/creatives/uuid-filename.mp4",
  "path": "creatives/uuid-filename.mp4",
  "contentType": "video/mp4",
  "size": 1234567,
  "duration": 30 // For videos, if extracted
}
```

**Implementation Steps:**
1. Accept multipart file upload
2. Validate file type (images: jpg, png, gif, webp; videos: mp4, webm)
3. Validate file size (configurable limits)
4. Generate unique filename (UUID + original extension)
5. Store file using MediaStorage
6. Return URL for use in creativeUrl field

### 2.3 File Serving Endpoint

**New Route:** `GET /api/v1/media/creatives/{filename}`

**Implementation:**
- Serve files from storage
- Set appropriate Content-Type headers
- Support range requests for video streaming
- Add caching headers (Cache-Control)
- Optional: Add authentication/authorization

### 2.4 File Validation Service

```scala
trait MediaValidator {
  def validateFile(file: File): Future[ValidationResult]
  def extractMetadata(file: File): Future[MediaMetadata]
}

case class ValidationResult(
  isValid: Boolean,
  errors: List[String],
  metadata: Option[MediaMetadata]
)

case class MediaMetadata(
  contentType: String,
  size: Long,
  width: Option[Int], // For images/videos
  height: Option[Int], // For images/videos
  duration: Option[Int] // For videos (seconds)
)
```

**Validations:**
- File type (MIME type check)
- File size limits:
  - Images: Max 10MB
  - Videos: Max 500MB (configurable)
- Content validation (verify it's actually an image/video)
- Duration extraction for videos (to populate durationSeconds automatically)

### 2.5 Video Processing (Optional - Phase 3)

For videos, consider:
- Thumbnail generation
- Multiple quality versions (transcoding)
- Format standardization (all videos → mp4)
- Duration extraction

**Tools:**
- FFmpeg for video processing
- OpenCV for thumbnail generation

## Phase 3: Database Changes

### 3.1 Optional: Media Metadata Table

```sql
CREATE TABLE IF NOT EXISTS creative_media (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    creative_id TEXT NOT NULL REFERENCES creatives(id) ON DELETE CASCADE,
    original_filename TEXT NOT NULL,
    stored_path TEXT NOT NULL UNIQUE,
    content_type TEXT NOT NULL,
    file_size BIGINT NOT NULL,
    width INTEGER,
    height INTEGER,
    duration_seconds INTEGER, -- For videos
    thumbnail_path TEXT, -- For videos
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_creative_media_creative_id ON creative_media(creative_id);
```

**Benefits:**
- Track file metadata separately
- Enable file versioning
- Track file sizes for billing/analytics
- Support multiple formats per creative

**Alternative:** Store metadata in `creatives.metadata` JSON field (simpler, no schema changes)

## Phase 4: Frontend Implementation

### 4.1 Update CreateCreativeModal

**Changes:**
1. Add file upload input
2. Show file preview (for images)
3. Show upload progress
4. Display selected file name/size
5. Validate file before submission
6. Two options:
   - **Option A:** Upload first, then create creative with returned URL
   - **Option B:** Create creative with temporary URL, upload in background

**Recommended:** Option A (Upload first)

**UI Flow:**
1. User selects file → Preview shown
2. User clicks "Upload" → File uploaded, URL returned
3. User fills other creative fields
4. User clicks "Create Creative" → Creative created with uploaded URL

### 4.2 File Upload Component

**New Component:** `FileUpload.tsx`

**Features:**
- Drag & drop support
- File type validation
- File size validation
- Upload progress bar
- Image preview
- Video preview (thumbnail + play button)
- Error handling

### 4.3 API Integration

**New API Method:**
```typescript
// services/api.ts
export const mediaApi = {
  upload: (file: File, campaignId: string, creativeId?: string) => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('campaignId', campaignId);
    if (creativeId) formData.append('creativeId', creativeId);
    
    return api.post(API_ENDPOINTS.mediaUpload, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: (progressEvent) => {
        // Handle progress
      }
    }).then(res => res.data);
  }
};
```

## Phase 5: Configuration

### 5.1 Application Configuration

**Environment Variables:**
```bash
# Storage configuration
STORAGE_TYPE=local|s3|minio  # Default: local
STORAGE_BASE_PATH=/path/to/storage  # For local
STORAGE_BASE_URL=http://localhost:8080/api/v1/media  # Base URL for serving

# File size limits
MAX_IMAGE_SIZE_MB=10
MAX_VIDEO_SIZE_MB=500

# Allowed file types
ALLOWED_IMAGE_TYPES=image/jpeg,image/png,image/gif,image/webp
ALLOWED_VIDEO_TYPES=video/mp4,video/webm

# S3 Configuration (if using S3)
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
AWS_REGION=us-east-1
S3_BUCKET_NAME=mnemocast-creatives

# MinIO Configuration (if using MinIO)
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=minioadmin
MINIO_SECRET_KEY=minioadmin
MINIO_BUCKET_NAME=creatives
```

## Phase 6: Security Considerations

### 6.1 File Upload Security

1. **File Type Validation:**
   - Check MIME type (not just extension)
   - Validate file content (magic bytes)
   - Whitelist allowed types only

2. **File Size Limits:**
   - Enforce limits server-side
   - Reject oversized files immediately

3. **Filename Sanitization:**
   - Use UUID for filenames (prevents path traversal)
   - Strip special characters
   - Validate extension

4. **Rate Limiting:**
   - Limit uploads per user/IP
   - Prevent abuse

5. **Access Control:**
   - Authenticate upload requests
   - Authorize based on user role
   - Optional: Add signed URLs for file access

### 6.2 File Serving Security

1. **Access Control:**
   - Public files: No auth required
   - Private files: Require authentication/token
   - Signed URLs with expiration

2. **CORS:**
   - Configure appropriate CORS headers
   - Allow access from ad players

3. **Content Security:**
   - Serve files with appropriate headers
   - Prevent XSS (Content-Type headers)
   - Validate range requests

## Phase 7: Implementation Steps (Recommended Order)

### Step 1: Storage Abstraction ✅ COMPLETED
1. ✅ Create `MediaStorage` trait - `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/storage/MediaStorage.scala`
2. ✅ Implement `LocalFileStorage` - `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/storage/LocalFileStorage.scala`
3. ✅ Add configuration support via environment variables

### Step 2: Backend Upload Endpoint ✅ COMPLETED
1. ✅ Create file upload route - `POST /api/v1/creatives/upload`
2. ✅ Add file validation - `MediaValidator` service
3. ✅ Integrate with `LocalFileStorage`
4. ✅ Add file serving endpoint - `GET /api/v1/media/creatives/{filename}`
5. ✅ Wire routes in `HttpServer`

### Step 3: Frontend Upload UI ✅ COMPLETED
1. ✅ Create `FileUpload` component - `dashboard/src/components/FileUpload.tsx`
2. ✅ Update `CreateCreativeModal` - Integrated FileUpload component
3. ✅ Add file preview - Image and video preview support
4. ✅ Add progress indication - Upload progress bar
5. ✅ Add file validation - Client-side file type and size validation

### Step 4: Testing & Validation (PENDING)
1. ⏳ Test image uploads
2. ⏳ Test video uploads
3. ⏳ Test file serving
4. ⏳ Validate file types/sizes

### Step 5: Production Readiness (FUTURE)
1. ⏳ Implement S3/MinIO storage
2. ⏳ Add video processing (if needed)
3. ⏳ Add CDN integration (if needed)
4. ⏳ Performance optimization
5. ⏳ Load testing

## Phase 8: Future Enhancements (Post-MVP)

1. **Video Processing:**
   - Automatic transcoding
   - Multiple quality versions
   - Thumbnail generation

2. **CDN Integration:**
   - CloudFront/Cloudflare integration
   - Edge caching
   - Global distribution

3. **Analytics:**
   - File size tracking
   - Storage usage metrics
   - Upload/download statistics

4. **Batch Upload:**
   - Support multiple file uploads
   - Bulk creative creation

5. **File Management:**
   - File replacement
   - File versioning
   - File deletion with cleanup

## Recommended Approach for MVP

**Quick Start (Development):**
1. Implement local filesystem storage
2. Simple file upload endpoint
3. Basic file serving
4. Frontend file upload UI
5. File validation

**Production Migration:**
1. Switch to MinIO or S3
2. Add CDN (if needed)
3. Add authentication/authorization
4. Optimize for scale

## File Structure

```
backend/
  modules/
    engine-infra/
      src/main/scala/mnemocast/engine/infra/
        storage/
          MediaStorage.scala (trait)
          LocalFileStorage.scala
          S3Storage.scala (future)
          MinIOStorage.scala (future)
        services/
          MediaValidator.scala
          MediaMetadataExtractor.scala (optional)
    engine-api/
      src/main/scala/mnemocast/engine/api/
        routes/
          MediaRoutes.scala (upload & serve)
          
storage/
  uploads/
    creatives/
      {creativeId}/
        original.{ext}
        thumbnail.jpg (if video)
        
dashboard/
  src/
    components/
      FileUpload.tsx
    services/
      api.ts (add mediaApi)
```

## Dependencies to Add

**Backend:**
- Pekko HTTP (already have) - for multipart handling
- Apache Commons IO (for file operations) - optional
- AWS SDK (for S3) - if using S3
- MinIO Java Client - if using MinIO
- ImageIO / JavaCV (for image processing) - optional
- FFmpeg wrapper (for video processing) - optional

**Frontend:**
- No new dependencies needed (use native File API + FormData)

## Success Criteria

✅ Users can upload images/videos through the dashboard
✅ Files are stored in a centralized location
✅ Files are served via URLs that work in ad players
✅ File validation prevents invalid uploads
✅ File size limits are enforced
✅ System handles concurrent uploads
✅ Files persist across server restarts

## Implementation Summary

### Completed Features (MVP)

**Backend:**
1. ✅ `MediaStorage` trait abstraction for storage layer
2. ✅ `LocalFileStorage` implementation using local filesystem
3. ✅ `MediaValidator` service for file validation (type, size)
4. ✅ `POST /api/v1/creatives/upload` endpoint for file uploads
5. ✅ `GET /api/v1/media/creatives/{filename}` endpoint for serving files
6. ✅ Multipart form data handling with Pekko HTTP
7. ✅ File validation (MIME types, file sizes)
8. ✅ UUID-based filename generation to prevent conflicts
9. ✅ Support for nested file paths (creativeId-organized storage)

**Frontend:**
1. ✅ `FileUpload` React component with drag & drop support
2. ✅ File preview for images and videos
3. ✅ Upload progress indicator
4. ✅ Client-side file validation
5. ✅ Integration with `CreateCreativeModal`
6. ✅ Automatic URL population after upload
7. ✅ Error handling and user feedback

**Configuration:**
- Storage base path: `STORAGE_BASE_PATH` (default: `storage/uploads`)
- Storage base URL: `STORAGE_BASE_URL` (default: `http://localhost:8080/api/v1/media`)
- Max image size: `MAX_IMAGE_SIZE_MB` (default: 10 MB)
- Max video size: `MAX_VIDEO_SIZE_MB` (default: 500 MB)

### Files Created/Modified

**Backend:**
- `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/storage/MediaStorage.scala`
- `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/storage/LocalFileStorage.scala`
- `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/MediaValidator.scala`
- `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/MediaUploadResponse.scala`
- `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/MediaRoutes.scala`
- `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/HttpServer.scala` (modified)

**Frontend:**
- `dashboard/src/components/FileUpload.tsx`
- `dashboard/src/components/CreateCreativeModal.tsx` (modified)
- `dashboard/src/config/api.ts` (modified)
- `dashboard/src/services/api.ts` (modified)

**Infrastructure:**
- `storage/uploads/creatives/` directory created

### Next Steps (Future Enhancements)

1. **S3/MinIO Storage** - Migrate to object storage for production
2. **Video Processing** - Duration extraction, thumbnail generation
3. **CDN Integration** - CloudFront/Cloudflare for global distribution
4. **Authentication** - Add auth/authorization for uploads
5. **File Management** - File replacement, deletion, versioning
6. **Analytics** - Track storage usage, upload/download statistics

