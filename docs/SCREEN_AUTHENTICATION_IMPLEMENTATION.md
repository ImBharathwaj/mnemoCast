# 🔐 Screen Authentication Implementation

## ✅ Implementation Complete

Screen authentication with unique ID and passkey has been successfully implemented. All screen endpoints now require authentication using the passkey generated during registration.

---

## 🎯 What Was Implemented

### 1. **Passkey Generation** ✅

- **Service:** `PasskeyService`
- **Location:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/PasskeyService.scala`
- **Features:**
  - Generates cryptographically secure 32-byte (256-bit) passkeys
  - Base64 encoded for easy storage and transmission
  - Suitable for use as API keys/device credentials

**Code:**
```scala
object PasskeyService {
  def generatePasskey(): String = {
    val bytes = new Array[Byte](32)
    secureRandom.nextBytes(bytes)
    Base64.getEncoder.encodeToString(bytes)
  }
}
```

### 2. **Screen Model Update** ✅

- **File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/Screen.scala`
- **Changes:**
  - Added `passkey: String` field to `Screen` case class
  - Passkey is generated automatically on registration
  - Passkey is stored securely and never returned in API responses (except during registration)

### 3. **Database Schema Update** ✅

- **File:** `infra/local-dev/postgres/init.sql`
- **Changes:**
  - Added `passkey TEXT NOT NULL` column to `screens` table
  - Added index on `passkey` for fast lookups

**Migration SQL:**
```sql
ALTER TABLE screens ADD COLUMN passkey TEXT NOT NULL;
CREATE INDEX IF NOT EXISTS idx_screens_passkey ON screens(passkey);
```

### 4. **Screen Authentication Middleware** ✅

- **File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/middleware/ScreenAuthMiddleware.scala`
- **Features:**
  - `authenticate(screenStore)` - Requires valid screen ID and passkey
  - `optionalAuth(screenStore)` - Optionally extracts screen if headers are present
  - Validates `X-Screen-Id` and `X-Screen-Passkey` headers
  - Rejects requests with invalid or missing credentials

**Authentication Headers:**
- `X-Screen-Id`: The screen's unique identifier
- `X-Screen-Passkey`: The authentication passkey

### 5. **Screen Registration Update** ✅

- **File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/ScreenRoutes.scala`
- **Changes:**
  - Generates passkey automatically on registration
  - Returns passkey in registration response (only time it's shown)
  - Returns `ScreenRegistrationResponse` with screen and passkey

**Registration Response:**
```json
{
  "screen": {
    "id": "screen-123",
    "name": "Mumbai Mall Screen 1",
    ...
  },
  "passkey": "base64-encoded-passkey-here",
  "message": "Screen registered successfully. Save this passkey securely - it will not be shown again."
}
```

### 6. **Protected Endpoints** ✅

All screen endpoints now require authentication:

#### Screen Management Endpoints
- ✅ `GET /api/v1/screens/{screenId}` - Get screen details
- ✅ `PUT /api/v1/screens/{screenId}/heartbeat` - Send heartbeat
- ✅ `GET /api/v1/screens/{screenId}/status` - Get screen status

#### Screen Ad Delivery Endpoints
- ✅ `GET /api/v1/screens/{screenId}/ads/deliver` - Single ad delivery
- ✅ `GET /api/v1/screens/{screenId}/ads/batch` - Batch ad delivery
- ✅ `GET /api/v1/screens/{screenId}/ads/preferences` - Ad preferences
- ✅ `GET /api/v1/screens/{screenId}/ads/history` - Ad history

**Note:** `POST /api/v1/screens/register` remains public (no authentication required for registration).

---

## 🔒 Security Features

### Authentication Flow

1. **Registration:**
   ```
   POST /api/v1/screens/register
   → Returns: screen + passkey
   → Client saves passkey securely
   ```

2. **Authenticated Requests:**
   ```
   GET /api/v1/screens/{screenId}/ads/deliver
   Headers:
     X-Screen-Id: screen-123
     X-Screen-Passkey: base64-passkey
   → Validates passkey matches screen
   → Returns ad or rejects with 401/403
   ```

### Security Measures

1. **Passkey Generation:**
   - Cryptographically secure random generation
   - 256 bits of entropy
   - Base64 encoded for transmission

2. **Passkey Storage:**
   - Stored securely in database
   - Never returned in API responses (except registration)
   - Indexed for fast validation

3. **Authentication Validation:**
   - Validates screen ID exists
   - Validates passkey matches screen
   - Rejects mismatched screen IDs in path vs headers
   - Returns 401/403 for invalid credentials

4. **Screen ID Verification:**
   - Verifies path parameter `screenId` matches authenticated screen
   - Prevents screen A from accessing screen B's endpoints

---

## 📝 Usage Examples

### 1. Register a Screen

```bash
curl -X POST http://localhost:8080/api/v1/screens/register \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <admin-token>" \
  -d '{
    "name": "Mumbai Mall Screen 1",
    "location": {
      "country": "IN",
      "city": "Mumbai",
      "area": "Bandra",
      "venueType": "mall",
      "timezone": "Asia/Kolkata"
    },
    "tags": ["mall", "premium"],
    "width": 1920,
    "height": 1080,
    "isAudible": true
  }'
```

**Response:**
```json
{
  "screen": {
    "id": "screen-123",
    "name": "Mumbai Mall Screen 1",
    ...
  },
  "passkey": "aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890+/=",
  "message": "Screen registered successfully. Save this passkey securely - it will not be shown again."
}
```

### 2. Request Ad (Authenticated)

```bash
curl -X GET "http://localhost:8080/api/v1/screens/screen-123/ads/deliver?durationSeconds=30" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890+/="
```

**Response:**
```json
{
  "requestId": "req-456",
  "adId": "ad-789",
  "creativeUrl": "http://localhost:9000/api/v1/media/creatives/...",
  "targetUrl": "https://example.com",
  "impressionTrackingUrl": "http://localhost:8080/api/v1/events/impression?adId=ad-789&requestId=req-456",
  "screenId": "screen-123",
  "screenName": "Mumbai Mall Screen 1",
  ...
}
```

### 3. Send Heartbeat (Authenticated)

```bash
curl -X PUT http://localhost:8080/api/v1/screens/screen-123/heartbeat \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890+/="
```

**Response:**
```
Heartbeat recorded
```

### 4. Get Screen Status (Authenticated)

```bash
curl -X GET http://localhost:8080/api/v1/screens/screen-123/status \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890+/="
```

**Response:**
```json
{
  "screenId": "screen-123",
  "screenName": "Mumbai Mall Screen 1",
  "isOnline": true,
  "lastSeen": "2025-12-18T23:30:00Z",
  "lastSeenAgoSeconds": 60,
  ...
}
```

---

## 🚨 Error Responses

### Invalid Credentials

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/screens/screen-123/ads/deliver \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: invalid-passkey"
```

**Response:**
```
HTTP 401 Unauthorized
```

### Missing Headers

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/screens/screen-123/ads/deliver
```

**Response:**
```
HTTP 401 Unauthorized
```

### Screen ID Mismatch

**Request:**
```bash
curl -X GET http://localhost:8080/api/v1/screens/screen-456/ads/deliver \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: valid-passkey-for-screen-123"
```

**Response:**
```json
HTTP 403 Forbidden
{
  "error": "Screen ID mismatch"
}
```

---

## 🔄 Migration Guide

### For Existing Screens

If you have existing screens without passkeys, you'll need to:

1. **Add passkey column** (if not already added):
   ```sql
   ALTER TABLE screens ADD COLUMN passkey TEXT;
   ```

2. **Generate passkeys for existing screens:**
   ```sql
   -- Generate passkeys for existing screens
   UPDATE screens SET passkey = encode(gen_random_bytes(32), 'base64') WHERE passkey IS NULL;
   ```

3. **Make passkey NOT NULL:**
   ```sql
   ALTER TABLE screens ALTER COLUMN passkey SET NOT NULL;
   ```

4. **Re-register screens** to get passkeys (recommended):
   - Delete existing screens
   - Re-register them to get new passkeys
   - Configure passkeys on screen clients

---

## 📋 Client Configuration

### Screen Client Setup

1. **Register Screen:**
   - Call `POST /api/v1/screens/register`
   - Save the returned `passkey` securely

2. **Configure Client:**
   ```javascript
   const SCREEN_ID = "screen-123";
   const SCREEN_PASSKEY = "aBcDeFgHiJkLmNoPqRsTuVwXyZ1234567890+/=";
   ```

3. **Make Authenticated Requests:**
   ```javascript
   fetch(`http://localhost:8080/api/v1/screens/${SCREEN_ID}/ads/deliver`, {
     headers: {
       "X-Screen-Id": SCREEN_ID,
       "X-Screen-Passkey": SCREEN_PASSKEY
     }
   });
   ```

4. **Send Heartbeat:**
   ```javascript
   setInterval(() => {
     fetch(`http://localhost:8080/api/v1/screens/${SCREEN_ID}/heartbeat`, {
       method: "PUT",
       headers: {
         "X-Screen-Id": SCREEN_ID,
         "X-Screen-Passkey": SCREEN_PASSKEY
       }
     });
   }, 30000); // Every 30 seconds
   ```

---

## ✅ Testing Checklist

- [x] Passkey generation works
- [x] Passkey stored in database
- [x] Registration returns passkey
- [x] Authenticated requests work
- [x] Invalid passkey rejected
- [x] Missing headers rejected
- [x] Screen ID mismatch rejected
- [x] All screen endpoints protected
- [x] Heartbeat requires authentication
- [x] Status endpoint requires authentication

---

## 🎉 Summary

Screen authentication is now fully implemented:

✅ **Unique Screen ID** - Generated on registration  
✅ **Secure Passkey** - Cryptographically secure, 256-bit entropy  
✅ **Authentication Middleware** - Validates all screen requests  
✅ **Protected Endpoints** - All screen endpoints require authentication  
✅ **Secure Storage** - Passkeys stored securely, never exposed  
✅ **Client Configuration** - Simple header-based authentication  

All screen transactions (including heartbeat) now require valid screen ID and passkey authentication!

---

**Status:** ✅ Complete  
**Date:** 2025-12-18  
**Version:** 4.0.0

