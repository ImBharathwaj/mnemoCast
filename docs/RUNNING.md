# How to Run the Mnemocast Engine

Step-by-step guide to run the Mnemocast Ad Serving Engine.

---

## Prerequisites

1. **Java 11 or higher**
   ```bash
   java -version
   ```
   Should show Java 11 or later.

2. **sbt (Scala Build Tool)**
   ```bash
   sbt --version
   ```
   Install if missing:
   ```bash
   # On Ubuntu/Debian
   sudo apt-get install sbt
   
   # On macOS (using Homebrew)
   brew install sbt
   
   # Or download from: https://www.scala-sbt.org/download.html
   ```

3. **Redis Server** (required for data storage)
   ```bash
   redis-cli ping
   ```
   Should return `PONG`. If not installed:
   ```bash
   # On Ubuntu/Debian
   sudo apt-get install redis-server
   sudo systemctl start redis-server
   
   # On macOS (using Homebrew)
   brew install redis
   brew services start redis
   
   # Or download from: https://redis.io/download
   ```

---

## Quick Start

### 1. Start Redis

Ensure Redis is running on `localhost:6379` (default):

```bash
# Check if Redis is running
redis-cli ping

# If not running, start it:
redis-server

# Or if installed as a service:
sudo systemctl start redis-server  # Linux
brew services start redis          # macOS
```

### 2. Navigate to Backend Directory

```bash
cd backend
```

### 3. Run the Application

**Option A: Using the run script (recommended)**

```bash
# From project root
./scripts/dev-run.sh

# Or from backend directory
cd backend
sbt run
```

**Option B: Using sbt directly**

```bash
cd backend
sbt "project engineApi" run
```

**Option C: Compile and run manually**

```bash
cd backend
sbt compile
sbt "project engineApi" run
```

### 4. Verify the Application is Running

You should see:
```
✅ Mnemocast Engine API running at http://0.0.0.0:8080/
Press ENTER to stop...
```

### 5. Test the API

Open a new terminal and test:

```bash
# Health check (should return 204 or ad)
curl http://localhost:8080/ads/deliver

# List ads (should return empty array initially)
curl http://localhost:8080/admin/ads
```

---

## Running Options

### Run in Background

Using `nohup`:

```bash
cd backend
nohup sbt "project engineApi" run > engine.log 2>&1 &
```

Check logs:
```bash
tail -f engine.log
```

### Run with Custom Configuration

To change Redis host/port, edit:
- `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/HttpServer.scala`
- Change: `new RedisClient("localhost", 6379)` to your values

To change server port, edit the same file:
- Change: `private val port = 8080` to your desired port

### Stop the Application

- If running in foreground: Press `ENTER` in the terminal
- If running in background: Find process and kill:
  ```bash
  ps aux | grep sbt
  kill <PID>
  ```

---

## First-Time Setup

### 1. Download Dependencies

sbt will automatically download dependencies on first run:

```bash
cd backend
sbt update
```

This may take a few minutes on first run.

### 2. Compile the Project

```bash
cd backend
sbt compile
```

Expected output:
```
[success] Total time: X s
```

### 3. Create Sample Data (Optional)

After the server is running, you can create sample ads using the demo script:

See `docs/demo-script.md` for examples, or create an ad manually:

```bash
# Basic ad (no budget or frequency capping)
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [
      {"key": "country", "operator": "eq", "value": "IN"}
    ],
    "isActive": true
  }'

# Ad with budget management
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [],
    "isActive": true,
    "maxPlays": 1000,
    "dailyLimit": 100,
    "hourlyLimit": 10
  }'

# Ad with frequency capping
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test-advertiser",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [],
    "isActive": true,
    "maxImpressionsPerDevice": 5,
    "maxImpressionsPerUser": 3,
    "frequencyCapWindowHours": 24
  }'
```

---

## Troubleshooting

### Port 8080 Already in Use

**Error:** `Address already in use`

**Solution:**
```bash
# Find process using port 8080
lsof -i :8080

# Kill the process
kill -9 <PID>

# Or change port in HttpServer.scala
```

### Redis Connection Failed

**Error:** `Connection refused` or Redis-related errors

**Solution:**
1. Check if Redis is running:
   ```bash
   redis-cli ping
   ```
2. If not running, start Redis:
   ```bash
   redis-server
   ```
3. Verify Redis is on the correct host/port (default: localhost:6379)

### sbt Not Found

**Error:** `command not found: sbt`

**Solution:**
- Install sbt (see Prerequisites above)
- Or use the pre-installed sbt if available in your IDE

### Java Version Issues

**Error:** `UnsupportedClassVersionError` or Java version errors

**Solution:**
- Ensure Java 11+ is installed: `java -version`
- Set JAVA_HOME if needed:
  ```bash
  export JAVA_HOME=/path/to/java11
  ```

### Compilation Errors

**Error:** Build failures

**Solution:**
1. Clean and rebuild:
   ```bash
   cd backend
   sbt clean compile
   ```
2. Check for dependency issues:
   ```bash
   sbt update
   ```
3. Verify all required files are present

### No Ads Returned

**Expected:** Empty array or 204 No Content

**This is normal if no ads are created yet!**

1. Create an ad using POST /admin/ads
2. Verify it's active: `isActive: true`
3. Ensure targeting rules match your request

---

## Development Workflow

### 1. Make Code Changes

Edit files in `backend/modules/`

### 2. Recompile

```bash
cd backend
sbt compile
```

### 3. Restart Server

Stop the running server (ENTER) and start again:
```bash
sbt "project engineApi" run
```

### 4. Test Changes

Use curl or the demo script to test your changes.

---

## Production Considerations

For production deployment:

1. **Use environment variables** for configuration (Redis host, port, etc.)
2. **Use a process manager** like systemd, supervisor, or Docker
3. **Set up logging** to files instead of console
4. **Configure Redis persistence** (AOF or RDB)
5. **Set up monitoring** and health checks
6. **Use reverse proxy** (nginx, Apache) for SSL/TLS

---

## API Endpoints

Once running, the API is available at:

- **Base URL:** `http://localhost:8080`

### Ad Delivery
- **Ad Delivery:** `GET /ads/deliver`

### Admin
- **Create Ad:** `POST /admin/ads`
- **List Ads:** `GET /admin/ads`
- **Get Events:** `GET /admin/ads/{adId}/events`

### Event Tracking
- **Impression Tracking:** `GET /api/v1/events/impression?adId={adId}&requestId={requestId}`
- **Impression Tracking:** `GET /api/v1/events/impression?adId={adId}&requestId={requestId}`

### Analytics
- **Ad Performance:** `GET /api/v1/analytics/ads/{adId}`
- **Campaign Performance:** `GET /api/v1/analytics/campaigns`
- **Dashboard Metrics:** `GET /api/v1/analytics/dashboard`

See `docs/02-api-spec.md` for full API documentation.

---

## Next Steps

1. ✅ Application is running
2. 📖 Read `docs/demo-script.md` for demo walkthrough
3. 📚 Check `docs/02-api-spec.md` for API details
4. 🧪 Test endpoints using curl or Postman

**Happy coding!** 🚀

