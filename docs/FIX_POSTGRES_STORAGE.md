# Fix: Data Not Stored in Postgres

## Problem

When you create an ad via `POST /admin/ads`, you get a JSON response but data is not stored in Postgres.

## Root Cause

The default storage strategy is **"redis"**, which means data only goes to Redis, not Postgres.

## Solution

### Option 1: Use Hybrid Mode (Recommended)

This stores data in both Redis (cache) and Postgres (persistent):

```bash
# Set environment variable before starting server
export STORAGE_STRATEGY=hybrid
export POSTGRES_HOST=localhost
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root

# Make sure Postgres is running and schema is created
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql

# Start server
cd backend
sbt run
```

### Option 2: Use Postgres-Only Mode

This stores data only in Postgres:

```bash
export STORAGE_STRATEGY=postgres
export POSTGRES_HOST=localhost
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root

cd backend
sbt run
```

### Option 3: Check Current Strategy

```bash
# Check what strategy is being used
echo $STORAGE_STRATEGY

# If not set, it defaults to "redis" (Redis-only)
```

---

## Verify Data is in Postgres

After setting the correct storage strategy and creating an ad:

```bash
# Check ads table
psql -h localhost -U postgres -d mnemocast -c "SELECT * FROM ads;"

# Check targeting rules
psql -h localhost -U postgres -d mnemocast -c "SELECT * FROM targeting_rules;"

# Count records
psql -h localhost -U postgres -d mnemocast -c "SELECT COUNT(*) FROM ads;"
```

---

## Troubleshooting

### Issue: Still not storing in Postgres

**Check 1: Storage Strategy**
```bash
# When server starts, look for this message:
# "  Postgres unavailable for hybrid mode, using Redis-only"
# This means Postgres connection failed

# Check Postgres is running
psql -h localhost -U postgres -c "SELECT 1;"
```

**Check 2: Database Exists**
```bash
psql -h localhost -U postgres -l | grep mnemocast
```

**Check 3: Tables Exist**
```bash
psql -h localhost -U postgres -d mnemocast -c "\dt"
# Should show: ads, targeting_rules, delivery_events, event_metadata
```

**Check 4: Connection Test**
```bash
export PGPASSWORD=root
psql -h localhost -U postgres -d mnemocast -c "SELECT 1;"
```

---

## Quick Test

```bash
# 1. Set hybrid mode
export STORAGE_STRATEGY=hybrid
export POSTGRES_PASSWORD=root

# 2. Create ad
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [],
    "isActive": true
  }'

# 3. Check Postgres
export PGPASSWORD=root
psql -h localhost -U postgres -d mnemocast -c "SELECT id, advertiser_id FROM ads;"
```

---

## Expected Behavior

### With STORAGE_STRATEGY=redis (Default)
-  Data stored in Redis
-  Data NOT stored in Postgres

### With STORAGE_STRATEGY=hybrid
-  Data stored in Redis (cache)
-  Data stored in Postgres (persistent)

### With STORAGE_STRATEGY=postgres
-  Data NOT stored in Redis
-  Data stored in Postgres

---

## Server Startup Messages

When server starts, you should see:

**Hybrid Mode (Working):**
```
Mnemocast Engine API running at http://0.0.0.0:8080/
```

**Hybrid Mode (Postgres Failed):**
```
  Warning: Failed to initialize Postgres client: ...
  Postgres unavailable for hybrid mode, using Redis-only
Mnemocast Engine API running at http://0.0.0.0:8080/
```

If you see the warning, Postgres connection failed and data will only go to Redis.

---

## Fix Postgres Connection

If you see warnings about Postgres:

1. **Check Postgres is running:**
   ```bash
   sudo systemctl status postgresql
   ```

2. **Check database exists:**
   ```bash
   psql -h localhost -U postgres -l | grep mnemocast
   ```

3. **Create database if needed:**
   ```bash
   export PGPASSWORD=root
   psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"
   ```

4. **Run schema:**
   ```bash
   psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
   ```

5. **Test connection:**
   ```bash
   psql -h localhost -U postgres -d mnemocast -c "SELECT 1;"
   ```

---

After setting `STORAGE_STRATEGY=hybrid` or `STORAGE_STRATEGY=postgres`, data will be stored in Postgres!

