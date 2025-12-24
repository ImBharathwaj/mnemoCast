#  API Route Protection Strategy

## Overview

This document outlines which API endpoints should be public vs protected with authentication.

---

##  Public Routes (No Authentication Required)

These routes are called by screens/clients and monitoring systems:

### Health & Monitoring
- `GET /api/v1/health` - Health check
- `GET /api/v1/ready` - Readiness probe (Kubernetes)
- `GET /api/v1/live` - Liveness probe (Kubernetes)
- `GET /api/v1/metrics` - Metrics (optional, can be protected)

### Ad Delivery (Client-facing)
- `GET /api/v1/ads/deliver` - Ad delivery for screens/clients

### Event Tracking (Client-facing)
- `GET /api/v1/events/impression` - Impression tracking from screens/clients

### Playlist Generation (Client-facing)
- `GET /api/v1/screens/{screenId}/playlist` - Get playlist for a screen

### Screen Operations (Client-facing)
- `GET /api/v1/screens/{screenId}` - Get screen info (screens may need to check their own info)
- `PUT /api/v1/screens/{screenId}/heartbeat` - Screen heartbeat (screens report they're alive)

---

##  Protected Routes (Authentication Required)

These routes are admin/management functions and should require authentication:

### Campaign Management
- `POST /api/v1/campaigns` - Create campaign
- `GET /api/v1/campaigns` - List campaigns
- `GET /api/v1/campaigns/{id}` - Get campaign details
- `PUT /api/v1/campaigns/{id}` - Update campaign
- `DELETE /api/v1/campaigns/{id}` - Delete campaign

### Creative Management
- `POST /api/v1/creatives` - Create creative
- `GET /api/v1/creatives` - List creatives
- `GET /api/v1/creatives/{id}` - Get creative details
- `PUT /api/v1/creatives/{id}` - Update creative
- `DELETE /api/v1/creatives/{id}` - Delete creative
- `GET /api/v1/campaigns/{id}/creatives` - Get creatives for campaign

### Screen Management (Admin)
- `POST /api/v1/screens/register` - Register new screen (admin function)
- `GET /api/v1/screens` - List all screens (admin function)
- `PUT /api/v1/screens/{id}` - Update screen (admin function)
- `DELETE /api/v1/screens/{id}` - Delete screen (admin function)

### Analytics & Reporting
- `GET /api/v1/analytics/*` - All analytics endpoints

### Admin Ad Management
- `POST /api/v1/admin/ads` - Create ad
- `GET /api/v1/admin/ads` - List ads
- `PUT /api/v1/admin/ads/{id}` - Update ad
- `DELETE /api/v1/admin/ads/{id}` - Delete ad

### Media Management
- `POST /api/v1/media/upload` - Upload media files
- `GET /api/v1/media/*` - Serve media files (can be public or protected)

---

##  Implementation Strategy

1. **Create a route wrapper** that applies authentication conditionally
2. **Update each route class** to use authentication where needed
3. **Keep public routes** accessible without auth
4. **Add optional auth** for some routes (e.g., metrics can be public or protected)

---

##  Notes

- Screen registration could potentially be public if screens self-register, but typically this is an admin function
- Media serving (`/api/v1/media/*`) can be public for serving creative assets to screens
- Consider rate limiting on public endpoints to prevent abuse

