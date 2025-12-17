# How to Run and Test the OOH Ad Serving Engine

This guide explains how to run the application and test all available endpoints.

## Prerequisites

1. **Java 11 or higher**
   ```bash
   java -version
   ```

2. **sbt (Scala Build Tool)**
   ```bash
   sbt --version
   ```
   Install if needed: https://www.scala-sbt.org/download.html

3. **Redis Server** (required for data storage)
   ```bash
   redis-cli ping
   ```
   Should return `PONG`. If not installed:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install redis-server
   sudo systemctl start redis-server
   
   # macOS
   brew install redis
   brew services start redis
   ```

4. **PostgreSQL** (optional, for persistent storage)
   ```bash
   psql --version
   ```
   Only needed if using `STORAGE_STRATEGY=postgres` or `STORAGE_STRATEGY=hybrid`

---

## Running the Application

### Option 1: Using the Development Script (Recommended)

```bash
# From project root
./scripts/dev-run.sh
```

This script will:
- Navigate to the backend directory
- Compile the project
- Start the server on `http://localhost:8080`

### Option 2: Using sbt Directly

```bash
cd backend
sbt "project engineApi" run
```

### Option 3: Run in Background

```bash
cd backend
nohup sbt "project engineApi" run > /tmp/mnemocast.log 2>&1 &
```

Check logs:
```bash
tail -f /tmp/mnemocast.log
```

---

## Storage Strategy

The application supports different storage strategies via the `STORAGE_STRATEGY` environment variable:

- **`redis`** (default): Fast in-memory storage, good for development
- **`postgres`**: Persistent database storage
- **`hybrid`**: Postgres as source of truth + Redis cache (recommended for production)

To use a specific strategy:

```bash
# Redis-only (default)
STORAGE_STRATEGY=redis sbt "project engineApi" run

# Postgres-only
STORAGE_STRATEGY=postgres sbt "project engineApi" run

# Hybrid (Postgres + Redis cache)
STORAGE_STRATEGY=hybrid sbt "project engineApi" run
```

### Setting Up Postgres (if using postgres/hybrid)

1. Start Postgres:
   ```bash
   # Using Docker
   docker run -d --name postgres-mnemocast \
     -e POSTGRES_PASSWORD=root \
     -e POSTGRES_DB=mnemocast \
     -p 5432:5432 \
     postgres:13
   ```

2. Run the initialization script:
   ```bash
   psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
   ```

3. Set environment variables (if different from defaults):
   ```bash
   export POSTGRES_HOST=localhost
   export POSTGRES_PORT=5432
   export POSTGRES_DB=mnemocast
   export POSTGRES_USER=postgres
   export POSTGRES_PASSWORD=root
   ```

---

## Testing Endpoints

### Automated Testing Script

Use the provided test script to test all endpoints:

```bash
# Make script executable
chmod +x scripts/test-ooh-endpoints.sh

# Run all tests
./scripts/test-ooh-endpoints.sh

# Or with custom base URL
BASE_URL=http://localhost:8080 ./scripts/test-ooh-endpoints.sh
```

The script will test:
1. Screen registration
2. Get screen by ID
3. List all screens
4. Screen heartbeat
5. Create ad with OOH features
6. Create ad with time-based targeting
7. List all ads
8. Playlist generation
9. Ad delivery (basic)
10. Ad delivery (with OOH parameters)

### Manual Testing with curl

#### 1. Screen Management

**Register a Screen:**
```bash
curl -X POST http://localhost:8080/api/v1/screens/register \
  -H "Content-Type: application/json" \
  -d '{
    "id": "screen-001",
    "name": "Phoenix Mall - Food Court Screen 1",
    "location": {
      "country": "IN",
      "city": "Chennai",
      "area": "Phoenix Mall",
      "venueType": "mall",
      "timezone": "Asia/Kolkata"
    },
    "tags": ["mall", "food-court", "premium"],
    "metadata": {}
  }'
```

**Get Screen:**
```bash
curl http://localhost:8080/api/v1/screens/screen-001
```

**List All Screens:**
```bash
curl http://localhost:8080/api/v1/screens
```

**Update Screen Heartbeat:**
```bash
curl -X PUT http://localhost:8080/api/v1/screens/screen-001/heartbeat
```

#### 2. Ad Management

**Create Ad with OOH Features:**
```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "brand-x",
    "creativeUrl": "https://cdn.example.com/ad.mp4",
    "targetUrl": "https://example.com",
    "durationSeconds": 30,
    "targetingRules": [
      {"key": "city", "operator": "eq", "value": "Chennai"},
      {"key": "venueType", "operator": "eq", "value": "mall"}
    ],
    "isActive": true,
    "maxPlays": 1000
  }'
```

**Create Ad with Time-Based Targeting:**
```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "brand-y",
    "creativeUrl": "https://cdn.example.com/ad2.mp4",
    "durationSeconds": 15,
    "targetingRules": [
      {"key": "city", "operator": "eq", "value": "Chennai"},
      {"key": "time", "operator": "daypart", "value": "09:00-17:00,monday,friday"}
    ],
    "isActive": true
  }'
```

**List All Ads:**
```bash
curl http://localhost:8080/admin/ads
```

#### 3. Playlist Generation

**Generate Playlist for Screen:**
```bash
curl "http://localhost:8080/api/v1/screens/screen-001/playlist?durationMinutes=3"
```

The response will include:
- `requestId`: Unique request identifier
- `screenId`: Screen ID
- `items`: Array of playlist items (ads)
- `validForSeconds`: How long the playlist is valid
- `totalDurationSeconds`: Total duration of the playlist

#### 4. Ad Delivery

**Basic Ad Delivery:**
```bash
curl http://localhost:8080/ads/deliver
```

**Ad Delivery with OOH Parameters:**
```bash
curl "http://localhost:8080/ads/deliver?screenId=screen-001&city=Chennai&venueType=mall&timezone=Asia/Kolkata"
```

---

## API Endpoints Summary

### Screen Management (OOH)
- `POST /api/v1/screens/register` - Register a new screen
- `GET /api/v1/screens/{screenId}` - Get screen by ID
- `GET /api/v1/screens` - List all screens
- `PUT /api/v1/screens/{screenId}/heartbeat` - Update screen heartbeat

### Playlist Generation (OOH)
- `GET /api/v1/screens/{screenId}/playlist?durationMinutes=X` - Generate playlist

### Ad Management
- `POST /admin/ads` - Create a new ad
- `GET /admin/ads` - List all ads
- `GET /admin/ads/{adId}/events` - Get events for an ad

### Ad Delivery
- `GET /ads/deliver` - Request an ad (supports OOH parameters)
  - Query params: `screenId`, `city`, `area`, `venueType`, `timezone`, `deviceId`, `userId`, `country`, `platform`

### Event Tracking
- `GET /api/v1/events/impression?adId={adId}&requestId={requestId}` - Track impression

### Analytics
- `GET /api/v1/analytics/dashboard` - Dashboard metrics
- `GET /api/v1/analytics/ads/{adId}` - Ad performance

---

## Verification

Once the server is running, you should see:
```
📦 Storage Strategy: redis
Mnemocast Engine API running at http://0.0.0.0:8080/
Press ENTER to stop...
```

Test that it's working:
```bash
curl http://localhost:8080/ads/deliver
```

This should return either:
- `204 No Content` (if no ads are available)
- A JSON response with ad data (if ads exist)

---

## Troubleshooting

### Port 8080 Already in Use
```bash
# Find process using port 8080
lsof -i :8080
# or
ss -tlnp | grep 8080

# Kill the process
kill -9 <PID>
```

### Redis Connection Failed
```bash
# Check if Redis is running
redis-cli ping

# Start Redis if needed
redis-server
# or
sudo systemctl start redis-server
```

### Server Not Starting
1. Check Java version: `java -version` (should be 11+)
2. Clean and rebuild: `cd backend && sbt clean compile`
3. Check for compilation errors in the logs

### No Ads Returned
This is normal if no ads are created yet. Create an ad using `POST /admin/ads` first.

---

## Next Steps

1. ✅ Application is running
2. ✅ Test endpoints using the test script or curl commands
3. 📖 Read `docs/OOH_READINESS_GAP_ANALYSIS.md` for feature status
4. 📚 Check `docs/02-api-spec.md` for detailed API documentation
5. 🧪 Experiment with different targeting rules and playlist durations

Happy testing! 🚀

