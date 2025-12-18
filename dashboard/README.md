# Mnemocast Dashboard

A modern React dashboard for managing OOH ad serving campaigns, creatives, screens, and analytics.

## Features

- 📊 **Dashboard Overview** - Real-time stats and metrics
- 📢 **Campaign Management** - Create, view, and manage campaigns
- 🎨 **Creative Management** - Manage creatives linked to campaigns
- 📺 **Screen Management** - Register and monitor screens
- 📈 **Analytics** - Performance metrics and charts
- 🎵 **Playlist Generator** - Generate playlists for screens

## Quick Start

### Prerequisites

- Node.js 14+ and npm
- Backend API running on `http://localhost:8080`

### Installation

```bash
cd dashboard
npm install
```

### Development

```bash
npm start
```

The dashboard will open at `http://localhost:3000`

### Build for Production

```bash
npm run build
```

The production build will be in the `build` directory.

## Configuration

The dashboard connects to the backend API. By default, it expects the API at `http://localhost:8080`.

To change the API URL, create a `.env` file:

```
REACT_APP_API_URL=http://localhost:8080
```

## Usage

1. **Start the backend API** (see backend README)
2. **Start the dashboard**: `npm start`
3. **Access the dashboard**: Open `http://localhost:3000` in your browser

### Demo Workflow

1. **Register Screens** - Go to Screens page and register OOH screens with location and tags
2. **Create Campaigns** - Go to Campaigns page and create campaigns with targeting rules
3. **Add Creatives** - Go to Creatives page and add creatives to campaigns
4. **Generate Playlists** - Go to Playlist page, select a screen, and generate a playlist
5. **View Analytics** - Go to Analytics page to see performance metrics

## Technology Stack

- **React** - UI framework
- **TypeScript** - Type safety
- **Tailwind CSS** - Styling
- **React Router** - Navigation
- **Chart.js** - Data visualization
- **Axios** - HTTP client

## Project Structure

```
dashboard/
├── src/
│   ├── components/      # Reusable components
│   │   ├── Layout.tsx
│   │   ├── CreateCampaignModal.tsx
│   │   ├── CreateCreativeModal.tsx
│   │   └── CreateScreenModal.tsx
│   ├── pages/          # Page components
│   │   ├── Dashboard.tsx
│   │   ├── Campaigns.tsx
│   │   ├── Creatives.tsx
│   │   ├── Screens.tsx
│   │   ├── Analytics.tsx
│   │   └── Playlist.tsx
│   ├── services/       # API services
│   │   └── api.ts
│   ├── config/         # Configuration
│   │   └── api.ts
│   ├── types/          # TypeScript types
│   │   └── index.ts
│   └── App.tsx         # Main app component
└── public/
```

## API Integration

The dashboard uses the following API endpoints:

- `GET /api/v1/campaigns` - List campaigns
- `POST /api/v1/campaigns` - Create campaign
- `GET /api/v1/creatives` - List creatives
- `POST /api/v1/campaigns/{id}/creatives` - Create creative
- `GET /api/v1/screens` - List screens
- `POST /api/v1/screens/register` - Register screen
- `GET /api/v1/screens/{id}/playlist` - Generate playlist
- `GET /api/v1/analytics/dashboard` - Get analytics

See `docs/02-api-spec.md` for complete API documentation.

## Troubleshooting

### CORS Errors

If you see CORS errors in the browser console, ensure the backend has CORS enabled (should be enabled by default in recent versions).

### API Connection Issues

- Verify the backend is running: `curl http://localhost:8080/api/v1/campaigns`
- Check the API URL in `.env` file
- Check browser console for error messages

### Build Errors

- Clear node_modules and reinstall: `rm -rf node_modules && npm install`
- Clear cache: `npm start -- --reset-cache`

## Development Notes

- The dashboard is a separate frontend application that communicates with the backend via REST APIs
- All API calls are centralized in `src/services/api.ts`
- Types are defined in `src/types/index.ts` matching the backend domain models
- The UI uses Tailwind CSS for styling - see `tailwind.config.js` for configuration
