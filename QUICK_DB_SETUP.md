# Quick Postgres Database Setup

## Why No Tables?

The database exists but **tables haven't been created yet**. You need to run the schema migration script.

---

## Quick Fix (3 Steps)

### Step 1: Set Postgres Password

```bash
export PGPASSWORD=root
```

### Step 2: Create Database (if needed)

```bash
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"
```

### Step 3: Run Schema Migration

```bash
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
```

---

## Verify Tables Were Created

```bash
psql -h localhost -U postgres -d mnemocast -c "\dt"
```

**You should see:**
```
              List of relations
 Schema |      Name       | Type  |  Owner   
--------+-----------------+-------+----------
 public | ads              | table | postgres
 public | delivery_events  | table | postgres
 public | event_metadata   | table | postgres
 public | targeting_rules  | table | postgres
```

---

## One-Line Command

```bash
export PGPASSWORD=root && \
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;" 2>&1 | grep -v "already exists" && \
psql -h localhost -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql && \
psql -h localhost -U postgres -d mnemocast -c "\dt"
```

---

## Using the Setup Script

```bash
# From project root
export POSTGRES_PASSWORD=root
./scripts/setup-postgres.sh
```

---

## Alternative: Using sudo (Linux)

If password authentication doesn't work:

```bash
# Create database
sudo -u postgres psql -c "CREATE DATABASE mnemocast;"

# Run schema
sudo -u postgres psql -d mnemocast -f infra/local-dev/postgres/init.sql

# Verify
sudo -u postgres psql -d mnemocast -c "\dt"
```

---

## What the Schema Creates

The `init.sql` script creates:

1. **ads** - Stores ad information
2. **targeting_rules** - Stores targeting rules for ads
3. **delivery_events** - Stores impression and click events
4. **event_metadata** - Stores metadata for events (deviceId, userId, etc.)

Plus:
- Indexes for performance
- Views for analytics
- Triggers for auto-updating timestamps

---

## Troubleshooting

### "Peer authentication failed"

Use password authentication:
```bash
export PGPASSWORD=root
psql -h localhost -U postgres ...
```

### "Database does not exist"

Create it first:
```bash
psql -h localhost -U postgres -c "CREATE DATABASE mnemocast;"
```

### "Permission denied"

Use sudo (Linux):
```bash
sudo -u postgres psql ...
```

---

After running the schema, your tables will be created and the application will work!

