# Postgres Database Setup Guide

Step-by-step guide to set up the Postgres database for Mnemocast Engine.

---

## Quick Setup (Automated)

```bash
# Set your Postgres password
export POSTGRES_PASSWORD=root

# Run the setup script
./scripts/setup-postgres.sh
```

---

## Manual Setup

### Step 1: Check Postgres is Running

```bash
# Linux
sudo systemctl status postgresql

# macOS
brew services list | grep postgres

# If not running, start it:
sudo systemctl start postgresql  # Linux
brew services start postgresql    # macOS
```

### Step 2: Create Database

**Option A: Using psql with password**
```bash
export PGPASSWORD=root
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"
```

**Option B: Using sudo (Linux)**
```bash
sudo -u postgres psql -c "CREATE DATABASE mnemocast;"
```

**Option C: Interactive psql**
```bash
psql -U postgres
# Then in psql prompt:
CREATE DATABASE mnemocast;
\q
```

### Step 3: Run Schema Migration

```bash
# Set password
export PGPASSWORD=root

# Run schema script
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

**Or using sudo:**
```bash
sudo -u postgres psql -d mnemocast -f infra/local-dev/postgres/init.sql
```

### Step 4: Verify Tables Created

```bash
# List all tables
psql -h localhost -U postgres -d mnemocast -c "\dt"

# Should show:
# - ads
# - targeting_rules
# - delivery_events
# - event_metadata
```

---

## Troubleshooting

### Issue: "Peer authentication failed"

**Solution:** Use password authentication instead:

```bash
export PGPASSWORD=root
psql -h localhost -U postgres -d mnemocast
```

### Issue: "Database does not exist"

**Solution:** Create the database first:

```bash
export PGPASSWORD=root
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"
```

### Issue: "Permission denied"

**Solution:** Use sudo or check user permissions:

```bash
# Linux - use postgres user
sudo -u postgres psql -d mnemocast -f infra/local-dev/postgres/init.sql

# Or grant permissions
sudo -u postgres psql -c "GRANT ALL PRIVILEGES ON DATABASE mnemocast TO postgres;"
```

### Issue: "relation does not exist"

**Meaning:** Tables haven't been created yet.

**Solution:** Run the schema migration:

```bash
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

---

## Verify Setup

### Check Database Exists

```bash
psql -h localhost -U postgres -l | grep mnemocast
```

### Check Tables Exist

```bash
psql -h localhost -U postgres -d mnemocast -c "\dt"
```

**Expected output:**
```
              List of relations
 Schema |      Name       | Type  |  Owner   
--------+-----------------+-------+----------
 public | ads              | table | postgres
 public | delivery_events  | table | postgres
 public | event_metadata   | table | postgres
 public | targeting_rules  | table | postgres
```

### Check Table Structure

```bash
# Check ads table
psql -h localhost -U postgres -d mnemocast -c "\d ads"

# Check delivery_events table
psql -h localhost -U postgres -d mnemocast -c "\d delivery_events"
```

---

## Complete Setup Example

```bash
# 1. Set password
export POSTGRES_PASSWORD=root
export PGPASSWORD=root

# 2. Create database
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"

# 3. Run schema
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql

# 4. Verify
psql -h localhost -U postgres -d mnemocast -c "\dt"

# 5. Test connection from application
export STORAGE_STRATEGY=hybrid
cd backend
sbt run
```

---

## Using Docker (Alternative)

If you prefer Docker:

```bash
# Start Postgres in Docker
docker run --name mnemocast-postgres \
  -e POSTGRES_PASSWORD=root \
  -e POSTGRES_DB=mnemocast \
  -p 5432:5432 \
  -d postgres:15-alpine

# Wait a few seconds for Postgres to start
sleep 5

# Run schema
export PGPASSWORD=root
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

---

## Reset Database (If Needed)

```bash
# Drop and recreate database
export PGPASSWORD=root
psql -h localhost -U postgres -c "DROP DATABASE IF EXISTS mnemocast;"
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

---

## Common Commands

```bash
# Connect to database
psql -h localhost -U postgres -d mnemocast

# List all databases
psql -h localhost -U postgres -l

# List tables
psql -h localhost -U postgres -d mnemocast -c "\dt"

# Count rows in ads table
psql -h localhost -U postgres -d mnemocast -c "SELECT COUNT(*) FROM ads;"

# View ads
psql -h localhost -U postgres -d mnemocast -c "SELECT id, advertiser_id, is_active FROM ads;"
```

---

For more details, see:
- `docs/STORAGE.md` - Storage configuration
- `docs/TROUBLESHOOTING.md` - Common issues

