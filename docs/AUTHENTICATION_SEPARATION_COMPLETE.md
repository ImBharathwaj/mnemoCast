#  Authentication Separation - Complete Guide

##  Authentication Properly Separated

Admin/dashboard endpoints and screen endpoints now use **completely separate authentication mechanisms**.

---

##  Endpoint Classification

###  Admin/Dashboard Endpoints (User Authentication - JWT)

**Authentication:** `Authorization: Bearer <JWT_TOKEN>`
**Middleware:** `AuthMiddleware.authenticate(authService)`

#### Screen Management (Admin Only)
-  `POST /api/v1/screens/register` - Register new screen
-  `GET /api/v1/screens` - List all screens
-  `PUT /api/v1/screens/{screenId}` - Update screen (admin)
-  `DELETE /api/v1/screens/{screenId}` - Delete screen (admin)

#### Campaign Management
-  `POST /api/v1/campaigns` - Create campaign
-  `GET /api/v1/campaigns` - List campaigns
-  `GET /api/v1/campaigns/{campaignId}` - Get campaign
-  `PUT /api/v1/campaigns/{campaignId}` - Update campaign
-  `DELETE /api/v1/campaigns/{campaignId}` - Delete campaign

#### Creative Management
-  `POST /api/v1/creatives` - Create creative
-  `GET /api/v1/creatives` - List creatives
-  `GET /api/v1/creatives/{creativeId}` - Get creative
-  `PUT /api/v1/creatives/{creativeId}` - Update creative
-  `DELETE /api/v1/creatives/{creativeId}` - Delete creative

#### Media Upload
-  `POST /api/v1/creatives/upload` - Upload creative media

#### Analytics & Metrics
-  `GET /api/v1/analytics/*` - All analytics endpoints
-  `GET /api/v1/metrics` - System metrics

#### Admin Ad Management
-  `POST /api/v1/admin/ads` - Create ad
-  `GET /api/v1/admin/ads` - List ads
-  `PUT /api/v1/admin/ads/{adId}` - Update ad
-  `DELETE /api/v1/admin/ads/{adId}` - Delete ad

---

###  Screen Endpoints (Screen Authentication - Passkey)

**Authentication:** 
- `X-Screen-Id: <screen-id>`
- `X-Screen-Passkey: <passkey>`
**Middleware:** `ScreenAuthMiddleware.authenticate(screenStore)`

#### Screen Operations
-  `GET /api/v1/screens/{screenId}` - Get own screen details
-  `PUT /api/v1/screens/{screenId}/heartbeat` - Send heartbeat
-  `GET /api/v1/screens/{screenId}/status` - Get own screen status

#### Screen Ad Delivery
-  `GET /api/v1/screens/{screenId}/ads/deliver` - Request single ad
-  `GET /api/v1/screens/{screenId}/ads/batch` - Request batch of ads
-  `GET /api/v1/screens/{screenId}/ads/preferences` - Get ad preferences
-  `GET /api/v1/screens/{screenId}/ads/history` - Get ad history

---

###  Public Endpoints (No Authentication)

-  `GET /api/v1/health` - Health check
-  `POST /api/v1/auth/register` - User registration
-  `POST /api/v1/auth/login` - User login
-  `GET /api/v1/media/creatives/*` - Media file serving

---

##  Route Structure

### ScreenRoutes.scala

Routes are organized with **clear separation**:

```scala
pathPrefix("api" / "v1" / "screens") {
  concat(
    // ============================================
    // ADMIN ENDPOINTS (User Authentication - JWT)
    // ============================================
    path("register") {
      post { requireAuth { ... } }  // User auth
    },
    pathEnd {
      get { requireAuth { ... } }  // User auth - List all
    },
    
    // ============================================
    // SCREEN ENDPOINTS (Screen Authentication - Passkey)
    // ============================================
    // More specific routes come first
    path(Segment / "heartbeat") {
      put { ScreenAuthMiddleware.authenticate(...) { ... } }  // Screen auth
    },
    path(Segment / "status") {
      get { ScreenAuthMiddleware.authenticate(...) { ... } }  // Screen auth
    },
    path(Segment) { screenId =>
      concat(
        get { ScreenAuthMiddleware.authenticate(...) { ... } },  // Screen auth - Get own
        put { requireAuth { ... } },  // User auth - Admin update
        delete { requireAuth { ... } }  // User auth - Admin delete
      )
    }
  )
}
```

**Key Points:**
1. **Specific routes first:** `/heartbeat` and `/status` come before general `path(Segment)`
2. **Method-based separation:** Same path, different HTTP methods use different auth
   - `GET /api/v1/screens/{screenId}` → Screen auth (screen gets own data)
   - `PUT /api/v1/screens/{screenId}` → User auth (admin updates)
   - `DELETE /api/v1/screens/{screenId}` → User auth (admin deletes)

### ScreenAdRoutes.scala

All endpoints use **screen authentication only**:
```scala
pathPrefix("api" / "v1" / "screens") {
  concat(
    path(Segment / "ads" / "deliver") {
      get { ScreenAuthMiddleware.authenticate(...) { ... } }
    },
    path(Segment / "ads" / "batch") {
      get { ScreenAuthMiddleware.authenticate(...) { ... } }
    },
    // ... other screen ad endpoints
  )
}
```

---

##  Authentication Details

### User Authentication (Admin/Dashboard)

**How it works:**
1. User logs in via `POST /api/v1/auth/login`
2. Receives JWT token
3. Includes token in requests: `Authorization: Bearer <token>`
4. `AuthMiddleware.authenticate(authService)` validates token
5. Extracts user information from token

**Used for:**
- Dashboard access
- Admin operations
- Management endpoints (CRUD)

### Screen Authentication (Screen Clients)

**How it works:**
1. Screen registers via `POST /api/v1/screens/register` (admin only)
2. Receives screen ID and passkey
3. Includes headers in requests:
   - `X-Screen-Id: <screen-id>`
   - `X-Screen-Passkey: <passkey>`
4. `ScreenAuthMiddleware.authenticate(screenStore)` validates credentials
5. Extracts screen information from store

**Used for:**
- Screen operations (heartbeat, status)
- Ad delivery requests
- Screen-specific data access

---

##  Usage Examples

### Admin Endpoint (User Auth)

```bash
# List all screens (admin)
curl -X GET "http://localhost:8080/api/v1/screens" \
  -H "Authorization: Bearer <jwt-token>"

# Update screen (admin)
curl -X PUT "http://localhost:8080/api/v1/screens/screen-123" \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Updated Name", ...}'
```

### Screen Endpoint (Screen Auth)

```bash
# Get own screen (screen client)
curl -X GET "http://localhost:8080/api/v1/screens/screen-123" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: <passkey>"

# Send heartbeat (screen client)
curl -X PUT "http://localhost:8080/api/v1/screens/screen-123/heartbeat" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: <passkey>"

# Request ad (screen client)
curl -X GET "http://localhost:8080/api/v1/screens/screen-123/ads/deliver" \
  -H "X-Screen-Id: screen-123" \
  -H "X-Screen-Passkey: <passkey>"
```

---

##  Verification

### Screen Endpoints
-  `GET /api/v1/screens/{screenId}` - Uses `ScreenAuthMiddleware` (screen auth)
-  `PUT /api/v1/screens/{screenId}/heartbeat` - Uses `ScreenAuthMiddleware` (screen auth)
-  `GET /api/v1/screens/{screenId}/status` - Uses `ScreenAuthMiddleware` (screen auth)
-  All `/api/v1/screens/{screenId}/ads/*` - Use `ScreenAuthMiddleware` (screen auth)

### Admin Endpoints
-  `POST /api/v1/screens/register` - Uses `requireAuth` (user auth)
-  `GET /api/v1/screens` - Uses `requireAuth` (user auth)
-  `PUT /api/v1/screens/{screenId}` - Uses `requireAuth` (user auth)
-  `DELETE /api/v1/screens/{screenId}` - Uses `requireAuth` (user auth)
-  All campaign/creative/analytics endpoints - Use `requireAuth` (user auth)

---

##  Summary

 **Complete Separation Achieved:**

1. **Admin/Dashboard Endpoints:**
   - Use JWT token authentication
   - Require `Authorization: Bearer <token>` header
   - Protected by `AuthMiddleware.authenticate(authService)`
   - For system management and dashboard access

2. **Screen Client Endpoints:**
   - Use passkey authentication
   - Require `X-Screen-Id` and `X-Screen-Passkey` headers
   - Protected by `ScreenAuthMiddleware.authenticate(screenStore)`
   - For screen operations and ad delivery

3. **Route Organization:**
   - Specific routes (`/heartbeat`, `/status`) come before general routes
   - Same path with different HTTP methods can use different auth
   - Clear separation with comments

**Screen endpoints no longer require user authentication!** 

---

**Status:**  Complete  
**Date:** 2025-12-19  
**Version:** 4.2.0

