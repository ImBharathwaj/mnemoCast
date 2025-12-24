#  Digital Display Client Application Plan

**Version:** 1.0.0  
**Last Updated:** December 2024  
**Status:** Planning Phase

---

##  Executive Summary

This document outlines the comprehensive plan for building the **Digital Display Client Application** - the client-side application that runs on physical or virtual display screens to fetch and play advertisements from the Mnemocast backend engine.

### Purpose

The Digital Display Client is responsible for:
- Registering displays with the backend
- Fetching dynamic playlists based on screen context
- Rendering and playing creatives (images/videos)
- Tracking playback events and sending analytics
- Maintaining connectivity and health status

---

##  Architecture Overview

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│              Digital Display Client Application              │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │   Player     │  │   API        │  │   Event      │     │
│  │   Engine     │◄─┤   Client     │◄─┤   Tracker   │     │
│  │              │  │              │  │              │     │
│  └──────────────┘  └──────────────┘  └──────────────┘     │
│         │                 │                  │              │
│         └─────────────────┼──────────────────┘             │
│                           │                                  │
│                  ┌────────▼────────┐                        │
│                  │  Configuration  │                        │
│                  │   & State Mgmt  │                        │
│                  └─────────────────┘                        │
└───────────────────────────┬──────────────────────────────────┘
                            │
                            │ HTTP/REST API
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              Mnemocast Backend Engine                         │
│         (http://localhost:8080/api/v1/...)                   │
└──────────────────────────────────────────────────────────────┘
```

### Component Breakdown

1. **Player Engine** - Media playback (images/videos)
2. **API Client** - Backend communication
3. **Event Tracker** - Analytics and event logging
4. **Configuration Manager** - Settings and state management
5. **Scheduler** - Playlist refresh and heartbeat

---

##  Technology Stack Options

### Option 1: Web-Based (Recommended for MVP)
**Tech Stack:**
- **Frontend Framework:** React or Vue.js
- **Media Player:** HTML5 Video/Image elements, Video.js, or Plyr
- **HTTP Client:** Axios or Fetch API
- **State Management:** React Context/Redux or Vuex
- **Build Tool:** Vite or Create React App
- **Deployment:** Electron (for desktop) or Web App (for kiosk mode)

**Pros:**
-  Fast development
-  Cross-platform (runs on any device with browser)
-  Easy to update (just refresh)
-  Rich ecosystem
-  Can run on Raspberry Pi, tablets, smart TVs

**Cons:**
-  Requires browser runtime
-  Less control over low-level hardware

**Best For:** MVP, rapid prototyping, flexible deployment

---

### Option 2: Native Desktop Application
**Tech Stack:**
- **Framework:** Electron (JavaScript/TypeScript) or Tauri (Rust)
- **Media Player:** Native video/image rendering
- **HTTP Client:** Built-in fetch or axios
- **Language:** TypeScript/JavaScript or Rust

**Pros:**
-  More control over system resources
-  Better performance
-  Can run without browser UI
-  Better for kiosk mode

**Cons:**
-  Larger application size
-  Platform-specific builds needed

**Best For:** Production deployments, kiosk installations

---

### Option 3: Python Application
**Tech Stack:**
- **Framework:** PyQt5/PyQt6 or Tkinter
- **Media Player:** VLC Python bindings or PyGame
- **HTTP Client:** Requests or httpx
- **Async:** asyncio

**Pros:**
-  Simple deployment
-  Good for Raspberry Pi
-  Easy to script and automate

**Cons:**
-  Less modern UI capabilities
-  Performance limitations for complex UIs

**Best For:** Embedded systems, Raspberry Pi, simple displays

---

### Option 4: React Native / Flutter
**Tech Stack:**
- **Framework:** React Native or Flutter
- **Media Player:** Native video players
- **HTTP Client:** Built-in fetch or http package

**Pros:**
-  Cross-platform mobile/tablet
-  Native performance
-  Good for Android/iOS tablets

**Cons:**
-  More complex setup
-  Platform-specific considerations

**Best For:** Tablet-based displays, mobile installations

---

##  Recommended Approach: Web-Based MVP

**Recommended Stack:**
- **Framework:** React + TypeScript
- **Build Tool:** Vite
- **Media Player:** Video.js for videos, native img for images
- **HTTP Client:** Axios
- **State Management:** React Context + Zustand
- **Styling:** Tailwind CSS
- **Deployment:** Electron wrapper (optional) or Kiosk mode browser

**Rationale:**
- Fastest to develop and iterate
- Works on any platform (Windows, Linux, macOS, Raspberry Pi)
- Easy to update remotely
- Can be packaged as Electron app for kiosk mode
- Can run in fullscreen browser mode

---

##  Core Features & Functionality

### Phase 1: MVP Core Features

#### 1.1 Screen Registration
- **Purpose:** Register display with backend on first launch
- **Implementation:**
  - Read screen configuration (ID, location, tags) from config file or environment
  - Call `POST /api/v1/screens/register`
  - Store registration confirmation
  - Handle registration failures gracefully

**Configuration Format:**
```json
{
  "screenId": "screen-1",
  "name": "Chennai Airport Screen 1",
  "location": {
    "city": "Chennai",
    "area": "Airport",
    "venueType": "airport"
  },
  "classification": 1,
  "backendUrl": "http://localhost:8080"
}
```

#### 1.2 Playlist Fetching
- **Purpose:** Get dynamic playlist from backend
- **Implementation:**
  - Call `GET /api/v1/screens/{screenId}/playlist?durationMinutes=3`
  - Cache playlist locally
  - Refresh playlist every N minutes (configurable, default: 3 minutes)
  - Handle network failures with retry logic
  - Fallback to cached playlist if refresh fails

**Refresh Strategy:**
- Fetch new playlist every 3 minutes (or when current playlist ends)
- Use exponential backoff for retries
- Show "Loading..." or cached content during refresh

#### 1.3 Media Playback
- **Purpose:** Display creatives from playlist
- **Implementation:**
  - Support image and video types
  - Play items in order from playlist
  - Respect `durationSeconds` for each item
  - Smooth transitions between items
  - Handle playback errors gracefully

**Playback Flow:**
```
1. Load playlist item
2. Preload media (image/video)
3. Display media
4. Start timer for durationSeconds
5. Send impression event
6. When duration expires:
   - Send play event
   - Move to next item
7. Repeat until playlist ends
8. Fetch new playlist
```

#### 1.4 Event Tracking
- **Purpose:** Send analytics events to backend
- **Implementation:**
  - Send `POST /api/v1/events/impression` when creative starts displaying
  - Send `POST /api/v1/events/play` when creative finishes playing
  - Queue events if offline, send when online
  - Batch events for efficiency (optional)

**Event Payload:**
```json
{
  "screenId": "screen-1",
  "creativeId": "creative-1",
  "campaignId": "campaign-1",
  "timestamp": "2024-12-18T15:00:00Z"
}
```

#### 1.5 Heartbeat Mechanism
- **Purpose:** Keep backend informed of screen status
- **Implementation:**
  - Send `POST /api/v1/screens/{screenId}/heartbeat` every 30 seconds
  - Update `lastHeartbeat` timestamp in backend
  - Handle heartbeat failures (log but don't block playback)

#### 1.6 Error Handling & Resilience
- **Purpose:** Handle network failures and errors gracefully
- **Implementation:**
  - Retry failed API calls with exponential backoff
  - Cache last successful playlist
  - Show error message or fallback content
  - Log errors for debugging
  - Continue playback even if events fail to send

---

### Phase 2: Enhanced Features

#### 2.1 Offline Mode
- Cache playlists locally (IndexedDB or localStorage)
- Queue events when offline, sync when online
- Show cached content when backend unavailable

#### 2.2 Playback Analytics
- Track actual playback duration vs intended duration
- Detect skipped items
- Monitor playback quality (buffering, errors)

#### 2.3 Configuration UI
- Admin interface for screen settings
- Update configuration without restart
- Test connection to backend

#### 2.4 Health Monitoring
- Display connection status
- Show last successful playlist fetch time
- Display error counts
- System resource monitoring

#### 2.5 Multi-Screen Support
- Support multiple screens on same device
- Different playlists per screen
- Independent event tracking

---

### Phase 3: Advanced Features

#### 3.1 Scheduled Content
- Support time-based content switching
- Different playlists for different times of day

#### 3.2 Interactive Elements
- Touch/click interactions (if supported)
- QR code display
- Call-to-action buttons

#### 3.3 Content Preloading
- Preload next playlist items
- Smooth transitions without loading delays

#### 3.4 Remote Control
- Remote pause/play/resume
- Force playlist refresh
- Emergency message display

---

##  API Integration Points

### Required Endpoints

#### 1. Screen Registration
```
POST /api/v1/screens/register
Request: {
  "id": "screen-1",
  "name": "Chennai Airport Screen 1",
  "location": { ... },
  "classification": 1
}
Response: Screen object with isOnline: true
```

#### 2. Playlist Fetching
```
GET /api/v1/screens/{screenId}/playlist?durationMinutes=3
Response: {
  "screenId": "screen-1",
  "generatedAt": "2024-12-18T15:00:00Z",
  "durationMinutes": 3,
  "items": [
    {
      "creativeId": "creative-1",
      "campaignId": "campaign-1",
      "url": "https://storage.example.com/creatives/banner.jpg",
      "durationSeconds": 10,
      "order": 1,
      "type": "image"
    }
  ]
}
```

#### 3. Heartbeat
```
POST /api/v1/screens/{screenId}/heartbeat
Response: { "message": "Heartbeat recorded" }
```

#### 4. Event Tracking
```
POST /api/v1/events/impression
POST /api/v1/events/play
Request: {
  "screenId": "screen-1",
  "creativeId": "creative-1",
  "campaignId": "campaign-1",
  "timestamp": "2024-12-18T15:00:00Z"
}
```

---

##  Project Structure

```
digital-display-client/
├── src/
│   ├── components/
│   │   ├── Player/
│   │   │   ├── MediaPlayer.tsx
│   │   │   ├── ImagePlayer.tsx
│   │   │   ├── VideoPlayer.tsx
│   │   │   └── PlaylistController.tsx
│   │   ├── Status/
│   │   │   ├── ConnectionStatus.tsx
│   │   │   └── HealthIndicator.tsx
│   │   └── Error/
│   │       └── ErrorBoundary.tsx
│   ├── services/
│   │   ├── api/
│   │   │   ├── client.ts
│   │   │   ├── screen.ts
│   │   │   ├── playlist.ts
│   │   │   └── events.ts
│   │   ├── storage/
│   │   │   ├── cache.ts
│   │   │   └── config.ts
│   │   └── scheduler/
│   │       ├── playlistRefresh.ts
│   │       └── heartbeat.ts
│   ├── hooks/
│   │   ├── usePlaylist.ts
│   │   ├── usePlayer.ts
│   │   └── useConnection.ts
│   ├── types/
│   │   ├── playlist.ts
│   │   ├── screen.ts
│   │   └── events.ts
│   ├── utils/
│   │   ├── retry.ts
│   │   └── logger.ts
│   ├── config/
│   │   └── default.ts
│   └── App.tsx
├── public/
│   ├── config.json (screen configuration)
│   └── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
└── README.md
```

---

##  Implementation Phases

### Phase 1: Foundation (Week 1-2)
**Goal:** Basic working client that can fetch and play playlists

**Tasks:**
1.  Set up project structure (React + TypeScript + Vite)
2.  Create API client service
3.  Implement screen registration
4.  Implement playlist fetching
5.  Basic media player (images only first)
6.  Simple playlist playback loop
7.  Basic error handling

**Deliverables:**
- Working client that displays images from playlist
- Can register with backend
- Can fetch and play playlists

---

### Phase 2: Core Features (Week 3-4)
**Goal:** Complete MVP with all core features

**Tasks:**
1.  Add video playback support
2.  Implement event tracking (impression + play)
3.  Implement heartbeat mechanism
4.  Add retry logic and error recovery
5.  Add playlist caching
6.  Improve UI/UX (fullscreen, transitions)
7.  Add configuration management

**Deliverables:**
- Full-featured MVP client
- All events tracked
- Heartbeat working
- Error handling robust

---

### Phase 3: Polish & Testing (Week 5-6)
**Goal:** Production-ready client

**Tasks:**
1.  Offline mode support
2.  Event queue for offline scenarios
3.  Health monitoring UI
4.  Configuration UI
5.  Comprehensive error handling
6.  Performance optimization
7.  Testing (unit + integration)
8.  Documentation

**Deliverables:**
- Production-ready client
- Complete documentation
- Deployment guide

---

##  Technical Requirements

### Runtime Requirements
- **Browser:** Chrome/Edge 90+, Firefox 88+, Safari 14+ (for web version)
- **Node.js:** 18+ (for Electron version)
- **Network:** Internet connection (with offline fallback)
- **Display:** Any resolution (responsive design)

### Performance Targets
- **Playlist Fetch:** < 500ms
- **Media Load:** < 2s for images, < 5s for videos
- **Event Send:** < 100ms (async, non-blocking)
- **Memory Usage:** < 200MB
- **CPU Usage:** < 10% idle, < 30% during playback

### Security Considerations
- Validate all API responses
- Sanitize media URLs
- Secure configuration storage
- HTTPS for production API calls
- Content Security Policy (CSP)

---

##  Deployment Options

### Option 1: Web Application (Kiosk Mode)
- Deploy as static files
- Run in fullscreen browser (kiosk mode)
- Auto-start on boot
- **Best for:** Quick deployment, easy updates

### Option 2: Electron Application
- Package as desktop application
- No browser UI visible
- Auto-start on boot
- **Best for:** Production installations, better control

### Option 3: Docker Container
- Containerized application
- Easy deployment on any platform
- **Best for:** Cloud deployments, consistent environments

### Option 4: Raspberry Pi Image
- Custom OS image with pre-installed client
- Auto-start on boot
- **Best for:** Hardware deployments

---

##  Testing Strategy

### Unit Tests
- API client functions
- Playlist parsing
- Event tracking logic
- Configuration management

### Integration Tests
- End-to-end playlist fetch and play
- Event tracking flow
- Heartbeat mechanism
- Error recovery

### Manual Testing
- Different screen configurations
- Network failure scenarios
- Long-running playback
- Multiple screen instances

---

##  Monitoring & Logging

### Logging
- API call logs (request/response)
- Playback events
- Error logs
- Performance metrics

### Monitoring
- Connection status
- Last successful playlist fetch
- Event send success rate
- Playback errors

### Debug Mode
- Verbose logging
- API response inspection
- Playlist visualization
- Event queue inspection

---

##  Update Strategy

### Over-the-Air Updates
- Check for updates on startup
- Download new version if available
- Graceful update (finish current playlist)
- Rollback on failure

### Configuration Updates
- Hot-reload configuration
- No restart required
- Validate before applying

---

##  Configuration Management

### Configuration Sources (Priority Order)
1. Environment variables
2. `config.json` file
3. Default values

### Configuration Schema
```typescript
interface ScreenConfig {
  screenId: string;
  name: string;
  location: {
    city: string;
    area: string;
    venueType: string;
  };
  classification: number;
  backendUrl: string;
  playlistRefreshInterval: number; // minutes
  heartbeatInterval: number; // seconds
  offlineMode: boolean;
  debugMode: boolean;
}
```

---

##  UI/UX Considerations

### Display Modes
- **Fullscreen:** No UI elements, just content
- **Kiosk Mode:** Fullscreen + no browser controls
- **Debug Mode:** Show status overlay, connection info

### Transitions
- Fade between items
- Smooth video transitions
- Loading indicators

### Error States
- Network error: Show cached content + error indicator
- Playback error: Skip to next item
- Configuration error: Show setup screen

---

##  Security & Privacy

### Security Measures
- Validate all API responses
- Sanitize media URLs (whitelist domains)
- Secure storage of configuration
- HTTPS only in production
- Content Security Policy

### Privacy
- No user tracking
- Only screen-level analytics
- No personal data collection

---

##  Success Metrics

### Key Performance Indicators (KPIs)
- **Uptime:** > 99% screen availability
- **Playback Success Rate:** > 95%
- **Event Delivery Rate:** > 98%
- **Playlist Refresh Success:** > 99%
- **Average Playlist Fetch Time:** < 500ms

### Monitoring Dashboard
- Real-time screen status
- Playback statistics
- Error rates
- Connection health

---

##  Next Steps

### Immediate Actions
1. **Choose Technology Stack** - Finalize web-based vs native
2. **Set Up Project** - Initialize React/TypeScript project
3. **Create API Client** - Implement backend communication
4. **Build MVP Player** - Basic image/video playback
5. **Test Integration** - Connect to running backend

### Week 1 Deliverables
-  Project setup complete
-  API client implemented
-  Screen registration working
-  Playlist fetching working
-  Basic player displaying images

---

##  Related Documentation

- **API Documentation:** `docs/API_DOCUMENTATION.md`
- **Domain Model:** `docs/01-domain-model.md`
- **Architecture:** `docs/ARCHITECTURE.md`
- **Backend Setup:** `QUICKSTART.md`

---

##  Open Questions

1. **Deployment Target:** What devices will run the client? (Raspberry Pi, tablets, desktop PCs?)
2. **Update Mechanism:** How will clients receive updates? (Manual, OTA, scheduled?)
3. **Offline Duration:** How long should client work offline? (Hours, days?)
4. **Multi-Screen:** Will one device run multiple screens?
5. **Remote Control:** Need remote control capabilities?

---

##  Support & Contact

For questions or clarifications:
- Review API documentation: `docs/API_DOCUMENTATION.md`
- Check backend health: `http://localhost:8080/api/v1/health`
- Review domain model: `docs/01-domain-model.md`

---

**Document Status:** Ready for Implementation  
**Next Review:** After Phase 1 completion

