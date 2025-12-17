# Dashboard Setup Guide

## Overview

A complete React-based UI dashboard has been created for the Mnemocast Ad Serving Engine. The dashboard provides a user-friendly interface for managing campaigns, creatives, screens, and viewing analytics.

## Quick Start

### 1. Install Dependencies

```bash
cd dashboard
npm install
```

### 2. Start Backend API

```bash
cd backend
sbt run
```

The API will run on `http://localhost:8080`

### 3. Start Dashboard

```bash
cd dashboard
npm start
```

The dashboard will open at `http://localhost:3000`

## Features

### ✅ Implemented Features

1. **Dashboard Overview**
   - Total campaigns, active campaigns
   - Total screens, online screens
   - Total impressions
   - Top performing ads

2. **Campaign Management**
   - List all campaigns (with active filter)
   - Create new campaigns with:
     - Basic info (name, advertiser, status, dates)
     - Budget settings (total budget, target playouts)
     - Priority settings
     - Targeting rules (location, time, tags)
   - View campaign details

3. **Creative Management**
   - List creatives (filtered by campaign)
   - Create creatives with:
     - Campaign association
     - Creative type (video, image, HTML)
     - Duration, URLs
     - Share of voice settings
   - View creative details

4. **Screen Management**
   - List all registered screens
   - Register new screens with:
     - Location details (city, area, venue type)
     - Tags
     - Timezone
   - Update screen heartbeat
   - View screen online/offline status

5. **Analytics Dashboard**
   - Performance metrics (impressions, active ads)
   - Top performing ads chart
   - Recent activity feed
   - Date range filtering

6. **Playlist Generator**
   - Select screen
   - Set duration
   - Generate and preview playlists
   - View playlist items with tracking URLs

## Technology Stack

- **React 18** - UI framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Modern styling
- **React Router** - Navigation
- **Chart.js** - Data visualization
- **Axios** - HTTP client

## Project Structure

```
dashboard/
├── src/
│   ├── components/         # Reusable components
│   │   ├── Layout.tsx      # Main layout with sidebar
│   │   ├── CreateCampaignModal.tsx
│   │   ├── CreateCreativeModal.tsx
│   │   └── CreateScreenModal.tsx
│   ├── pages/              # Page components
│   │   ├── Dashboard.tsx   # Overview page
│   │   ├── Campaigns.tsx   # Campaign management
│   │   ├── Creatives.tsx   # Creative management
│   │   ├── Screens.tsx     # Screen management
│   │   ├── Analytics.tsx   # Analytics dashboard
│   │   └── Playlist.tsx    # Playlist generator
│   ├── services/           # API integration
│   │   └── api.ts          # API service functions
│   ├── config/             # Configuration
│   │   └── api.ts          # API endpoints
│   ├── types/              # TypeScript types
│   │   └── index.ts        # Type definitions
│   └── App.tsx             # Main app component
├── public/                 # Static assets
└── package.json            # Dependencies
```

## API Integration

The dashboard communicates with the backend via REST APIs:

- **Campaigns**: `/api/v1/campaigns`
- **Creatives**: `/api/v1/creatives`, `/api/v1/campaigns/{id}/creatives`
- **Screens**: `/api/v1/screens`, `/api/v1/screens/register`
- **Playlists**: `/api/v1/screens/{id}/playlist`
- **Analytics**: `/api/v1/analytics/dashboard`

CORS is enabled in the backend to allow cross-origin requests from the dashboard.

## Configuration

### Environment Variables

Create a `.env` file in the `dashboard` directory:

```
REACT_APP_API_URL=http://localhost:8080
```

Default API URL is `http://localhost:8080` if not specified.

## Usage Guide

### Creating Your First Campaign

1. Go to **Campaigns** page
2. Click **"Create Campaign"**
3. Fill in:
   - Campaign name
   - Advertiser ID
   - Start and end dates
   - Budget (optional)
   - Priority (1-10)
   - Targeting rules (optional)
4. Click **"Create Campaign"**

### Adding Creatives to Campaigns

1. Go to **Creatives** page
2. Click **"Create Creative"**
3. Select a campaign
4. Fill in creative details:
   - Name, type, URL
   - Duration in seconds
   - Target URL (optional)
5. Click **"Create Creative"**

### Registering Screens

1. Go to **Screens** page
2. Click **"Register Screen"**
3. Fill in:
   - Screen name
   - Location (city, area, venue type)
   - Tags (comma-separated)
   - Timezone
4. Click **"Register Screen"**

### Generating Playlists

1. Go to **Playlist** page
2. Select a screen from dropdown
3. Set duration (minutes)
4. Click **"Generate Playlist"**
5. View the generated playlist with items

### Viewing Analytics

1. Go to **Analytics** page
2. Optionally set date range filters
3. View:
   - Summary statistics
   - Top performing ads chart
   - Recent activity

## Troubleshooting

### CORS Errors

If you see CORS errors:
- Ensure backend is running
- Check that CORS is enabled in backend (should be by default)
- Verify API URL in `.env` file

### API Connection Issues

- Check backend is running: `curl http://localhost:8080/api/v1/campaigns`
- Check browser console for error messages
- Verify API endpoints match backend version

### Build Errors

- Clear and reinstall: `rm -rf node_modules && npm install`
- Clear cache: `npm start -- --reset-cache`

## Next Steps

The dashboard is fully functional for pitching and demos. Future enhancements could include:

- Campaign editing
- Creative editing
- Screen editing
- More advanced analytics visualizations
- Export functionality
- User authentication (if needed)
- Real-time updates (WebSocket)

## Demo Workflow for Pitch

1. **Start Backend**: `cd backend && sbt run`
2. **Start Dashboard**: `cd dashboard && npm start`
3. **Register Screens**: Create 2-3 screens with different locations/tags
4. **Create Campaign**: Create a campaign with targeting rules
5. **Add Creatives**: Add creatives to the campaign
6. **Generate Playlist**: Show playlist generation for different screens
7. **View Analytics**: Show performance metrics

This demonstrates the complete workflow from screen registration to playlist generation with full visual feedback.

