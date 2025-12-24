#  Authentication Separation - Admin vs Screen Endpoints

##  Current Authentication Strategy

###  Admin/Dashboard Endpoints (User Authentication - JWT)
These endpoints require **user authentication** via JWT token in `Authorization: Bearer <token>` header:

#### Screen Management (Admin)
- `POST /api/v1/screens/register` - Register new screen (admin only)
- `GET /api/v1/screens` - List all screens (admin only)
- `PUT /api/v1/screens/{screenId}` - Update screen (admin only)
- `DELETE /api/v1/screens/{screenId}` - Delete screen (admin only)

#### Campaign Management
- `POST /api/v1/campaigns` - Create campaign
- `GET /api/v1/campaigns` - List campaigns
- `GET /api/v1/campaigns/{campaignId}` - Get campaign
- `PUT /api/v1/campaigns/{campaignId}` - Update campaign
- `DELETE /api/v1/campaigns/{campaignId}` - Delete campaign

#### Creative Management
- `POST /api/v1/creatives` - Create creative
- `GET /api/v1/creatives` - List creatives
- `GET /api/v1/creatives/{creativeId}` - Get creative
- `PUT /api/v1/creatives/{creativeId}` - Update creative
- `DELETE /api/v1/creatives/{creativeId}` - Delete creative

#### Media Upload
- `POST /api/v1/creatives/upload` - Upload creative media

#### Analytics & Metrics
- `GET /api/v1/analytics/*` - All analytics endpoints
- `GET /api/v1/metrics` - System metrics

#### Admin Ad Management
- `POST /api/v1/admin/ads` - Create ad
- `GET /api/v1/admin/ads` - List ads
- `PUT /api/v1/admin/ads/{adId}` - Update ad
- `DELETE /api/v1/admin/ads/{adId}` - Delete ad

---

###  Screen Endpoints (Screen Authentication - Passkey)
These endpoints require **screen authentication** via `X-Screen-Id` and `X-Screen-Passkey` headers:

#### Screen Operations
- `GET /api/v1/screens/{screenId}` - Get own screen details
- `PUT /api/v1/screens/{screenId}/heartbeat` - Send heartbeat
- `GET /api/v1/screens/{screenId}/status` - Get own screen status

#### Screen Ad Delivery
- `GET /api/v1/screens/{screenId}/ads/deliver` - Request single ad
- `GET /api/v1/screens/{screenId}/ads/batch` - Request batch of ads
- `GET /api/v1/screens/{screenId}/ads/preferences` - Get ad preferences
- `GET /api/v1/screens/{screenId}/ads/history` - Get ad history

---

###  Public Endpoints (No Authentication)
- `GET /api/v1/health` - Health check
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `GET /api/v1/media/creatives/*` - Media file serving (public)

---

##  Implementation Details

### User Authentication (Admin/Dashboard)
**Middleware:** `AuthMiddleware.authenticate(authService)`
**Header:** `Authorization: Bearer <JWT_TOKEN>`
**Used in:** Admin routes, dashboard routes, management endpoints

### Screen Authentication (Screen Clients)
**Middleware:** `ScreenAuthMiddleware.authenticate(screenStore)`
**Headers:** 
- `X-Screen-Id: <screen-id>`
- `X-Screen-Passkey: <passkey>`
**Used in:** Screen client endpoints, ad delivery endpoints

---

##  Route Organization

### ScreenRoutes.scala
```scala
pathPrefix("api" / "v1" / "screens") {
  concat(
    // Admin endpoints (user auth)
    path("register") { post { requireAuth { ... } } },
    pathEnd { get { requireAuth { ... } } },  // List all
    path(Segment) { screenId =>
      concat(
        put { requireAuth { ... } },  // Update
        delete { requireAuth { ... } }  // Delete
      )
    },
    
    // Screen endpoints (screen auth)
    path(Segment / "heartbeat") { screenId =>
      put { ScreenAuthMiddleware.authenticate(...) { ... } }
    },
    path(Segment / "status") { screenId =>
      get { ScreenAuthMiddleware.authenticate(...) { ... } }
    },
    path(Segment) { screenId =>
      get { ScreenAuthMiddleware.authenticate(...) { ... } }  // Get own screen
    }
  )
}
```

### ScreenAdRoutes.scala
All endpoints use `ScreenAuthMiddleware.authenticate(screenStore)`:
- `GET /api/v1/screens/{screenId}/ads/deliver`
- `GET /api/v1/screens/{screenId}/ads/batch`
- `GET /api/v1/screens/{screenId}/ads/preferences`
- `GET /api/v1/screens/{screenId}/ads/history`

---

##  Verification Checklist

- [x] Screen registration requires user auth (admin only)
- [x] Screen listing requires user auth (admin only)
- [x] Screen update requires user auth (admin only)
- [x] Screen delete requires user auth (admin only)
- [x] Screen heartbeat requires screen auth
- [x] Screen status requires screen auth
- [x] Get own screen requires screen auth
- [x] All ad delivery endpoints require screen auth
- [x] Campaign endpoints require user auth
- [x] Creative endpoints require user auth
- [x] Analytics endpoints require user auth
- [x] Media upload requires user auth
- [x] Media serving is public

---

##  Summary

**Admin/Dashboard Endpoints:**
- Use `requireAuth` → `AuthMiddleware.authenticate(authService)`
- Require JWT token in `Authorization` header
- For managing the system (CRUD operations)

**Screen Client Endpoints:**
- Use `ScreenAuthMiddleware.authenticate(screenStore)`
- Require `X-Screen-Id` and `X-Screen-Passkey` headers
- For screen operations (heartbeat, ad delivery, status)

**Authentication is properly separated!** 

---

**Status:**  Verified  
**Date:** 2025-12-19

