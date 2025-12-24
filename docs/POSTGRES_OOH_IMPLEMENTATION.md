# Postgres Storage Implementation for OOH Features

This document describes the Postgres storage implementation for all OOH features.

## Overview

All OOH features now support Postgres storage with the following storage strategies:

1. **Redis-only**: Fast in-memory storage (default, good for development)
2. **Postgres-only**: Persistent database storage (production-ready)
3. **Hybrid**: Postgres as source of truth + Redis as cache (recommended for production)

The storage strategy is controlled by the `STORAGE_STRATEGY` environment variable.

## Database Schema Changes

### 1. Added `duration_seconds` Column to `ads` Table

```sql
ALTER TABLE ads ADD COLUMN duration_seconds INTEGER;
```

This column stores the duration of ad creatives in seconds, required for playlist generation.

### 2. New Tables for Screens

#### `screens` Table
Stores OOH screen/device information with location data:
- `id` (TEXT, PK) - Screen/device identifier
- `name` (TEXT) - Human-readable name
- `country`, `city`, `area`, `venue_type`, `timezone` (TEXT) - Location information
- `is_online` (BOOLEAN) - Online status
- `last_seen` (TIMESTAMPTZ) - Last heartbeat timestamp
- `created_at`, `updated_at` (TIMESTAMPTZ) - Timestamps

#### `screen_tags` Table
Stores tags associated with screens (for targeting):
- `id` (UUID, PK)
- `screen_id` (TEXT, FK → screens.id)
- `tag` (TEXT) - Tag value (e.g., "mall", "food-court")
- `created_at` (TIMESTAMPTZ)

#### `screen_metadata` Table
Stores arbitrary key-value metadata for screens:
- `id` (UUID, PK)
- `screen_id` (TEXT, FK → screens.id)
- `metadata_key` (TEXT)
- `metadata_value` (TEXT)
- `created_at` (TIMESTAMPTZ)

## Storage Implementations

### PostgresScreenStore

Location: `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/postgres/PostgresScreenStore.scala`

Features:
- Full CRUD operations for screens
- Handles tags and metadata in separate tables
- Supports `updateLastSeen` for heartbeat tracking
- Transactional operations for data consistency

### HybridScreenStore

Location: `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/HybridScreenStore.scala`

Features:
- Writes to both Postgres (persistent) and Redis (cache)
- Reads from Redis first, falls back to Postgres if cache miss
- Caches results from Postgres in Redis for subsequent reads
- Graceful degradation if Postgres is unavailable

### Updated PostgresAdStore

The `PostgresAdStore` has been updated to:
- Store and retrieve `duration_seconds` column
- Support all OOH-related ad fields

## Configuration

### Environment Variables

```bash
# Storage strategy: "redis", "postgres", or "hybrid"
STORAGE_STRATEGY=hybrid

# Postgres connection (required if strategy is "postgres" or "hybrid")
POSTGRES_HOST=localhost
POSTGRES_PORT=5432
POSTGRES_DB=mnemocast
POSTGRES_USER=postgres
POSTGRES_PASSWORD=root
```

### HttpServer Configuration

The `HttpServer` automatically configures storage based on `STORAGE_STRATEGY`:

- **redis**: Uses only Redis stores
- **postgres**: Uses only Postgres stores (falls back to Redis if Postgres unavailable)
- **hybrid**: Uses hybrid stores (Postgres + Redis cache)

## Database Setup

### For New Databases

Run the complete `init.sql` script:
```bash
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

### For Existing Databases

The `init.sql` script is idempotent and safe to run on existing databases:
- Uses `CREATE TABLE IF NOT EXISTS` for tables
- Uses `CREATE INDEX IF NOT EXISTS` for indexes
- Uses `DO $$ ... END $$` blocks for safe trigger creation
- All OOH tables (screens, screen_tags, screen_metadata) will be created if they don't exist
- The `duration_seconds` column is included in the ads table schema

Simply run:
```bash
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

## What's Stored in Postgres

### Already Implemented (Before OOH)
-  **Ads** - Complete ad data with targeting rules
-  **Targeting Rules** - Ad targeting configuration
-  **Delivery Events** - Impression and event tracking
-  **Event Metadata** - Additional event information

### Newly Added (OOH Features)
-  **Screens** - OOH screen/device registration
-  **Screen Tags** - Tags for screen targeting
-  **Screen Metadata** - Custom screen metadata
-  **Ad duration_seconds** - Duration field for playlist generation

## Storage Strategy Recommendations

### Development
- Use `STORAGE_STRATEGY=redis` for fast iteration
- No Postgres setup required

### Production
- Use `STORAGE_STRATEGY=hybrid` for best performance and reliability
- Postgres as source of truth ensures data persistence
- Redis cache provides fast access for hot data

### Analytics/Reporting
- Use `STORAGE_STRATEGY=postgres` for accurate historical data
- All data stored in Postgres for complex queries
- Better for reporting and analytics workloads

## Testing

To test Postgres storage:

1. Start Postgres:
```bash
docker run -d --name postgres-mnemocast \
  -e POSTGRES_PASSWORD=root \
  -e POSTGRES_DB=mnemocast \
  -p 5432:5432 \
  postgres:13
```

2. Run init script:
```bash
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

3. Start server with hybrid strategy:
```bash
STORAGE_STRATEGY=hybrid sbt run
```

4. Test screen registration:
```bash
curl -X POST http://localhost:8080/api/v1/screens/register \
  -H "Content-Type: application/json" \
  -d '{
    "id": "screen-001",
    "name": "Test Screen",
    "location": {
      "city": "Chennai",
      "venueType": "mall"
    },
    "tags": ["test"]
  }'
```

5. Verify in Postgres:
```sql
SELECT * FROM screens;
SELECT * FROM screen_tags;
```

## Summary

All OOH features now support Postgres storage:
-  Screens stored in Postgres
-  Screen tags and metadata stored in Postgres
-  Ad duration_seconds stored in Postgres
-  Hybrid storage strategy available (Postgres + Redis cache)
-  Migration script provided for existing databases
-  Full backward compatibility with Redis-only mode

