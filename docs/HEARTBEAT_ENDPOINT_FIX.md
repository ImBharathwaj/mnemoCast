# 🔧 Heartbeat Endpoint Fix

## ✅ Issue Resolved

Fixed the heartbeat endpoint (`PUT /api/v1/screens/{screenId}/heartbeat`) to:
1. Return updated screen data in JSON format
2. Handle CORS preflight requests (OPTIONS)
3. Return status 200 with screen data
4. Include screen authentication headers in CORS

---

## 🐛 Problem

**Error:** `405: HTTP method not allowed, supported methods: OPTIONS`

**Root Causes:**
1. CORS preflight (OPTIONS) requests were not handled for the heartbeat endpoint
2. CORS headers didn't include `X-Screen-Id` and `X-Screen-Passkey` custom headers
3. Endpoint returned plain text instead of JSON screen data

---

## ✅ Solution

### 1. Updated Heartbeat Endpoint

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/ScreenRoutes.scala`

**Changes:**
- Added explicit `options` handler for CORS preflight
- Returns updated screen data in JSON format (not plain text)
- Fetches updated screen after heartbeat to return latest data
- Returns status 200 with screen JSON

**Before:**
```scala
path(Segment / "heartbeat") { screenId =>
  put {
    // ... update last seen ...
    complete(StatusCodes.OK, "Heartbeat recorded")  // Plain text
  }
}
```

**After:**
```scala
path(Segment / "heartbeat") { screenId =>
  concat(
    put {
      ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
        // Update last seen
        val futureUpdate = screenStore.updateLastSeen(screenId).flatMap { _ =>
          // Fetch updated screen
          screenStore.getById(screenId).map {
            case Some(updatedScreen) =>
              updatedScreen.copy(passkey = "***REDACTED***")
            case None =>
              screen.copy(passkey = "***REDACTED***")
          }
        }
        onComplete(futureUpdate) {
          case scala.util.Success(screenResponse) =>
            complete(StatusCodes.OK, screenResponse)  // JSON screen data
          case scala.util.Failure(ex) =>
            complete(StatusCodes.InternalServerError, Map("error" -> ...))
        }
      }
    },
    options {
      // Handle CORS preflight
      complete(StatusCodes.OK)
    }
  )
}
```

### 2. Updated CORS Headers

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/HttpServer.scala`

**Changes:**
- Added `X-Screen-Id` and `X-Screen-Passkey` to allowed CORS headers
- Updated all CORS header configurations

**Before:**
```scala
`Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
```

**After:**
```scala
`Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With", "X-Screen-Id", "X-Screen-Passkey")
```

---

## 📝 API Usage

### Request

```bash
PUT /api/v1/screens/{screenId}/heartbeat
Headers:
  X-Screen-Id: screen-123
  X-Screen-Passkey: base64-passkey
```

### Response (200 OK)

```json
{
  "id": "screen-123",
  "name": "Mumbai Mall Screen 1",
  "location": {
    "country": "IN",
    "city": "Mumbai",
    "area": "Phoenix Mall",
    "venueType": "mall",
    "timezone": "Asia/Kolkata"
  },
  "tags": ["mall", "premium"],
  "metadata": {},
  "classification": 9,
  "width": 2560,
  "height": 1440,
  "isAudible": true,
  "isOnline": true,
  "lastSeen": "2025-12-19T00:18:00Z",
  "passkey": "***REDACTED***",
  "createdAt": "2025-11-30T00:00:00Z",
  "updatedAt": "2025-12-19T00:18:00Z"
}
```

### CORS Preflight (OPTIONS)

```bash
OPTIONS /api/v1/screens/{screenId}/heartbeat
```

**Response (200 OK):**
- Empty body
- CORS headers included

---

## 🔒 Security

- **Passkey Redaction:** Screen passkey is redacted (`***REDACTED***`) in responses
- **Authentication Required:** Endpoint requires valid `X-Screen-Id` and `X-Screen-Passkey` headers
- **Screen ID Verification:** Path parameter `screenId` must match authenticated screen

---

## ✅ Testing

### Test with curl

```bash
# Heartbeat request
curl -X PUT "http://localhost:8080/api/v1/screens/screen-123/heartbeat" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: your-passkey-here" \
  -H "Content-Type: application/json"

# Expected: 200 OK with screen JSON
```

### Test CORS preflight

```bash
curl -X OPTIONS "http://localhost:8080/api/v1/screens/screen-123/heartbeat" \
  -H "Origin: http://localhost:3000" \
  -H "Access-Control-Request-Method: PUT" \
  -H "Access-Control-Request-Headers: X-Screen-Id, X-Screen-Passkey" \
  -v

# Expected: 200 OK with CORS headers
```

---

## 🎉 Summary

✅ **Heartbeat endpoint now:**
- Returns updated screen data in JSON format
- Handles CORS preflight requests
- Returns status 200
- Includes screen authentication headers in CORS
- Redacts passkey in response

✅ **CORS headers now include:**
- `X-Screen-Id`
- `X-Screen-Passkey`

The endpoint is now fully functional for screen clients!

---

**Status:** ✅ Fixed  
**Date:** 2025-12-19  
**Version:** 4.1.0

