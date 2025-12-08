# Quick Setup: Redis + Postgres Hybrid Storage

## Quick Start (5 minutes)

### 1. Start Services

```bash
# Start Redis
redis-server

# Start Postgres (if not running)
sudo systemctl start postgresql  # Linux
# or
brew services start postgresql      # macOS
```

### 2. Create Database

```bash
# Create database
sudo -u postgres psql -c "CREATE DATABASE mnemocast;"

# Run schema
psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

### 3. Set Environment Variables

```bash
export STORAGE_STRATEGY=hybrid
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root
```

### 4. Run Application

```bash
cd backend
sbt run
```

---

## Storage Strategy Options

### Redis Only (Fast, Development)
```bash
export STORAGE_STRATEGY=redis
# No Postgres needed
```

### Postgres Only (Persistent)
```bash
export STORAGE_STRATEGY=postgres
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root
```

### Hybrid (Recommended - Best of Both)
```bash
export STORAGE_STRATEGY=hybrid
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root
```

---

## What's Stored Where

### Redis (Hot Data)
- Active ads cache
- Recent events (last 100)
- Fast access (< 1ms)

### Postgres (Persistent)
- All ads (source of truth)
- All events (historical)
- Targeting rules
- Analytics data
- Complex queries

---

## Verify Setup

```bash
# Check Redis
redis-cli ping
# Should return: PONG

# Check Postgres
psql -U postgres -d mnemocast -c "SELECT COUNT(*) FROM ads;"
# Should return: 0 (or number of ads)
```

---

For detailed documentation, see `docs/STORAGE.md`

