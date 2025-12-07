# Troubleshooting Guide

Common issues and solutions for the Mnemocast Engine.

---

## Internal Server Error (500)

### Issue
```
curl -X GET "http://localhost:8080/ads/deliver"
# Returns: "There was an internal server error."
```

### Common Causes & Solutions

#### 1. Postgres Connection Failed (Most Common)

**Symptoms:**
- Error in logs: `FATAL: password authentication failed`
- Error: `Failed to initialize pool`
- Server starts but requests fail

**Solution A: Use Redis-Only Mode (Quick Fix)**
```bash
export STORAGE_STRATEGY=redis
cd backend
sbt run
```

**Solution B: Fix Postgres Connection**
```bash
# Check Postgres is running
sudo systemctl status postgresql  # Linux
# or
brew services list | grep postgres  # macOS

# Check credentials
psql -U postgres -d mnemocast -c "SELECT 1;"
# If this fails, fix password or create database

# Set correct environment variables
export STORAGE_STRATEGY=hybrid
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root  # Your actual password

# Create database if it doesn't exist
sudo -u postgres psql -c "CREATE DATABASE mnemocast;"

# Run schema
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

**Solution C: Disable Postgres (Use Redis Only)**
```bash
# Set environment variable before starting
export STORAGE_STRATEGY=redis

# Or edit HttpServer.scala to change default
# Change: val storageStrategy = sys.env.getOrElse("STORAGE_STRATEGY", "redis")
```

---

#### 2. Redis Connection Failed

**Symptoms:**
- Error: `Connection refused`
- Error: `Unable to connect to Redis`

**Solution:**
```bash
# Check Redis is running
redis-cli ping
# Should return: PONG

# If not running, start Redis
redis-server

# Or as service
sudo systemctl start redis-server  # Linux
brew services start redis          # macOS
```

---

#### 3. Database Schema Not Created

**Symptoms:**
- Error: `relation "ads" does not exist`
- Error: `table does not exist`

**Solution:**
```bash
# Create database
sudo -u postgres psql -c "CREATE DATABASE mnemocast;"

# Run schema migration
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql

# Verify tables exist
psql -U postgres -d mnemocast -c "\dt"
```

---

#### 4. No Ads Available (204 Response)

**Symptoms:**
- Request returns 204 No Content (not an error, but no ads)

**Solution:**
```bash
# Create a test ad
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [],
    "isActive": true
  }'

# Verify ad was created
curl -X GET "http://localhost:8080/admin/ads"
```

---

## Connection Issues

### Redis Connection Refused

```bash
# Check if Redis is running
redis-cli ping

# Start Redis
redis-server

# Check port
netstat -tuln | grep 6379
# or
lsof -i :6379
```

### Postgres Connection Refused

```bash
# Check if Postgres is running
sudo systemctl status postgresql  # Linux
ps aux | grep postgres

# Start Postgres
sudo systemctl start postgresql  # Linux
brew services start postgresql   # macOS

# Check port
netstat -tuln | grep 5432
# or
lsof -i :5432
```

### Port 8080 Already in Use

```bash
# Find process using port 8080
lsof -i :8080
# or
netstat -tuln | grep 8080

# Kill the process
kill -9 <PID>

# Or change port in HttpServer.scala
```

---

## Storage Strategy Issues

### Default Strategy Changed

The default storage strategy is now **"redis"** (was "hybrid"). This means:
- ✅ Works without Postgres
- ✅ Only requires Redis
- ✅ Faster startup

### To Use Hybrid Mode

```bash
# Ensure Postgres is set up
export STORAGE_STRATEGY=hybrid
export POSTGRES_HOST=localhost
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root

# Create database and schema
sudo -u postgres psql -c "CREATE DATABASE mnemocast;"
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql

# Start server
cd backend
sbt run
```

---

## Error Messages Explained

### "Postgres not configured but hybrid strategy selected"

**Meaning:** You set `STORAGE_STRATEGY=hybrid` but Postgres connection failed.

**Fix:**
1. Set `STORAGE_STRATEGY=redis` (quick fix)
2. Or fix Postgres connection (see above)

### "Failed to initialize pool"

**Meaning:** HikariCP (Postgres connection pool) couldn't connect.

**Fix:**
- Check Postgres is running
- Check credentials are correct
- Check database exists
- Check network connectivity

### "relation does not exist"

**Meaning:** Database tables haven't been created.

**Fix:**
```bash
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

---

## Debugging Steps

### 1. Check Server Logs

Look at the console output when starting the server:
```bash
cd backend
sbt run
```

Look for:
- ✅ "Mnemocast Engine API running at..."
- ⚠️  Warnings about Postgres
- ❌ Error messages

### 2. Test Individual Components

```bash
# Test Redis
redis-cli ping
# Should return: PONG

# Test Postgres
psql -U postgres -d mnemocast -c "SELECT 1;"
# Should return: 1

# Test HTTP endpoint
curl -v "http://localhost:8080/ads/deliver"
# -v shows verbose output including headers
```

### 3. Check Environment Variables

```bash
# Print current storage strategy
echo $STORAGE_STRATEGY

# Print Postgres settings
echo $POSTGRES_HOST
echo $POSTGRES_DB
echo $POSTGRES_USER
echo $POSTGRES_PASSWORD
```

### 4. Verify Database Schema

```bash
# List all tables
psql -U postgres -d mnemocast -c "\dt"

# Check ads table structure
psql -U postgres -d mnemocast -c "\d ads"

# Count ads
psql -U postgres -d mnemocast -c "SELECT COUNT(*) FROM ads;"
```

---

## Quick Fixes

### Fix 1: Use Redis Only (No Postgres Needed)

```bash
export STORAGE_STRATEGY=redis
cd backend
sbt run
```

### Fix 2: Reset to Defaults

```bash
# Unset all Postgres variables
unset STORAGE_STRATEGY
unset POSTGRES_HOST
unset POSTGRES_DB
unset POSTGRES_USER
unset POSTGRES_PASSWORD

# Defaults to Redis-only
cd backend
sbt run
```

### Fix 3: Create Test Ad

```bash
# If getting 204 No Content
curl -X POST "http://localhost:8080/admin/ads" \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetingRules": [],
    "isActive": true
  }'
```

---

## Common Error Patterns

### Pattern 1: Server Won't Start

**Check:**
1. Port 8080 available?
2. Java version correct? (`java -version` should show 11+)
3. Dependencies downloaded? (`sbt update`)

### Pattern 2: Server Starts But Requests Fail

**Check:**
1. Redis running? (`redis-cli ping`)
2. Postgres running? (if using hybrid/postgres)
3. Database exists?
4. Schema created?

### Pattern 3: 204 No Content (Not an Error)

**This is normal if:**
- No ads created yet
- All ads are inactive
- All ads filtered out by targeting/budget/frequency

**Fix:** Create an active ad with no restrictions

---

## Getting Help

1. **Check logs**: Look at server console output
2. **Test components**: Redis, Postgres individually
3. **Use Redis-only**: Simplest setup, no Postgres needed
4. **Check documentation**: `docs/RUNNING.md`, `docs/STORAGE.md`

---

## Prevention

### Recommended Setup for Development

```bash
# Use Redis-only (simplest)
export STORAGE_STRATEGY=redis

# Start Redis
redis-server

# Start server
cd backend
sbt run
```

### Recommended Setup for Production

```bash
# Use hybrid mode
export STORAGE_STRATEGY=hybrid
export POSTGRES_HOST=localhost
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=your-secure-password

# Ensure both Redis and Postgres are running
# Run schema migrations
# Start server
```

---

For more details, see:
- `docs/RUNNING.md` - Setup instructions
- `docs/STORAGE.md` - Storage configuration
- `docs/STORAGE_SETUP.md` - Quick setup guide

