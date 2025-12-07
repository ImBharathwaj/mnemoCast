# Mnemocast Engine Backend

Core ad serving engine for Out-of-Home (OOH) advertising.

## Module Structure

- **engine-domain**: Pure domain models and business logic (no HTTP/DB dependencies)
- **engine-infra**: Persistence stores, configuration, and wiring
- **engine-api**: Pekko HTTP server, routes, and JSON formats
- **shared-kernel**: Shared types and utilities (future)

## Running the Application

### Prerequisites

1. **Java 11+** - `java -version`
2. **sbt** - `sbt --version`
3. **Redis** - Running on `localhost:6379`

### Quick Start

1. **Start Redis:**
   ```bash
   redis-server
   # OR if installed as service
   sudo systemctl start redis-server
   ```

2. **Run the application:**
   ```bash
   # From backend directory
   sbt "project engineApi" run
   
   # OR use the run script from project root
   ./scripts/dev-run.sh
   ```

3. **Verify it's running:**
   - You should see: `✅ Mnemocast Engine API running at http://0.0.0.0:8080/`
   - Test: `curl http://localhost:8080/ads/deliver`

### Alternative Run Commands

```bash
# Run from backend directory
cd backend
sbt "project engineApi" run

# Run using script (from project root)
./scripts/dev-run.sh

# Run in sbt shell
cd backend
sbt
> project engineApi
> run
```

## API Endpoints

- **Ad Delivery:** `GET /ads/deliver`
- **Admin - Create Ad:** `POST /admin/ads`
- **Admin - List Ads:** `GET /admin/ads`
- **Admin - Get Events:** `GET /admin/ads/{adId}/events`

See `../docs/02-api-spec.md` for full API documentation.

## Documentation

- **Running Guide:** `../docs/RUNNING.md`
- **Quick Start:** `../QUICKSTART.md`
- **API Spec:** `../docs/02-api-spec.md`
- **Demo Script:** `../docs/demo-script.md`

## Troubleshooting

- **"No main class detected"**: Use `sbt "project engineApi" run` instead of `sbt run`
- **Redis connection errors**: Ensure Redis is running on `localhost:6379`
- **Port 8080 in use**: Kill the process using port 8080 or change port in `HttpServer.scala`

## Getting Started

See the main [README.md](../README.md) and [docs/](../docs/) for project documentation.
