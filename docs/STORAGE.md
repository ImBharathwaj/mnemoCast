# Storage Strategy - Redis + Postgres Hybrid

The Mnemocast Engine supports multiple storage strategies for optimal performance and persistence.

---

## Storage Strategies

### 1. Redis Only (Fast, In-Memory)
- **Use Case**: Development, testing, high-speed requirements
- **Pros**: Very fast, simple setup
- **Cons**: Data lost on restart, limited analytics capabilities
- **Configuration**: `STORAGE_STRATEGY=redis`

### 2. Postgres Only (Persistent, Relational)
- **Use Case**: Production with persistence requirements
- **Pros**: ACID guarantees, complex queries, data persistence
- **Cons**: Slower than Redis for hot data
- **Configuration**: `STORAGE_STRATEGY=postgres`

### 3. Hybrid (Recommended)
- **Use Case**: Production (best of both worlds)
- **Pros**: Fast access + persistence, optimal performance
- **Cons**: More complex setup
- **Configuration**: `STORAGE_STRATEGY=hybrid` (default)

---

## Hybrid Strategy Details

### How It Works

**Ads Storage:**
- **Postgres**: Source of truth (persistent storage)
- **Redis**: Cache for active ads (fast access)
- **Write**: Both stores updated
- **Read**: Redis first (cache), fallback to Postgres

**Events Storage:**
- **Postgres**: All events (historical, analytics)
- **Redis**: Recent events cache (last 100 events)
- **Write**: Both stores updated
- **Read**: Redis for recent, Postgres for historical

### Benefits

1. **Performance**: Redis provides sub-millisecond access for hot data
2. **Persistence**: Postgres ensures data durability
3. **Analytics**: Postgres enables complex queries and reporting
4. **Scalability**: Can scale Redis and Postgres independently

---

## Setup Instructions

### Prerequisites

1. **Redis** (required for all strategies)
   ```bash
   redis-server
   ```

2. **Postgres** (required for postgres/hybrid strategies)
   ```bash
   # Install Postgres
   sudo apt-get install postgresql postgresql-contrib  # Ubuntu/Debian
   brew install postgresql                            # macOS

   # Start Postgres
   sudo systemctl start postgresql  # Linux
   brew services start postgresql    # macOS
   ```

### Database Setup

1. **Create Database**
   ```bash
   sudo -u postgres psql
   CREATE DATABASE mnemocast;
   \q
   ```

2. **Run Schema Migration**
   ```bash
   psql -U postgres -d mnemocast -f infra/local-dev/postgres/init.sql
   ```

   Or manually:
   ```bash
   psql -U postgres -d mnemocast
   # Then paste the contents of infra/local-dev/postgres/init.sql
   ```

### Configuration

Set environment variables before running:

```bash
# Storage strategy
export STORAGE_STRATEGY=hybrid  # or "redis" or "postgres"

# Postgres connection (if using postgres/hybrid)
export POSTGRES_HOST=localhost
export POSTGRES_PORT=5432
export POSTGRES_DB=mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=root

# Run the application
cd backend
sbt run
```

### Docker Compose (Recommended)

Create `docker-compose.yml`:

```yaml
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

  postgres:
    image: postgres:15-alpine
    environment:
      POSTGRES_DB: mnemocast
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: root
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
      - ./infra/local-dev/postgres/init.sql:/docker-entrypoint-initdb.d/init.sql

volumes:
  redis-data:
  postgres-data:
```

Run:
```bash
docker-compose up -d
```

---

## What Goes Where

### Redis (Appropriate For)
- ✅ Active ads cache (fast delivery)
- ✅ Recent events cache (last 100 events)
- ✅ Hot data (frequently accessed)
- ✅ Real-time counters
- ✅ Session data

### Postgres (Suitable For)
- ✅ Ad metadata (persistent storage)
- ✅ Historical events (analytics)
- ✅ Complex queries (JOINs, aggregations)
- ✅ ACID transactions
- ✅ Relationships (targeting rules)
- ✅ Audit trails
- ✅ Budget tracking (accurate counts)

---

## Migration Guide

### From Redis-Only to Hybrid

1. **Setup Postgres** (see above)
2. **Run schema migration**
3. **Set environment variable**: `STORAGE_STRATEGY=hybrid`
4. **Restart application**

The hybrid store will:
- Read existing data from Redis
- Write new data to both Redis and Postgres
- Gradually migrate data to Postgres

### From Hybrid to Postgres-Only

1. **Ensure all data is in Postgres** (let hybrid run for a while)
2. **Set environment variable**: `STORAGE_STRATEGY=postgres`
3. **Restart application**

---

## Performance Considerations

### Redis
- **Read**: < 1ms
- **Write**: < 1ms
- **Best for**: Hot data, recent events

### Postgres
- **Read**: 5-50ms (depends on query complexity)
- **Write**: 10-100ms (with indexes)
- **Best for**: Historical data, analytics, complex queries

### Hybrid
- **Hot data read**: < 1ms (from Redis)
- **Cold data read**: 5-50ms (from Postgres)
- **Write**: ~10ms (both stores)

---

## Monitoring

### Redis
```bash
redis-cli info stats
redis-cli dbsize
```

### Postgres
```sql
-- Check table sizes
SELECT 
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;

-- Check event counts
SELECT event_type, COUNT(*) FROM delivery_events GROUP BY event_type;
```

---

## Troubleshooting

### Postgres Connection Failed
```
Error: Connection refused
```
**Solution**: Check Postgres is running and credentials are correct

### Schema Not Found
```
Error: relation "ads" does not exist
```
**Solution**: Run the init.sql script to create tables

### Redis Connection Failed
```
Error: Connection refused
```
**Solution**: Check Redis is running on localhost:6379

### Hybrid Mode Not Working
**Check**: Environment variable `STORAGE_STRATEGY=hybrid` is set

---

## Production Recommendations

1. **Use Hybrid Strategy**: Best performance + persistence
2. **Postgres Replication**: Set up read replicas for analytics
3. **Redis Persistence**: Enable AOF or RDB for Redis backup
4. **Connection Pooling**: Already configured (HikariCP)
5. **Monitoring**: Monitor both Redis and Postgres metrics
6. **Backups**: Regular Postgres backups, Redis snapshots

---

For more details, see:
- `docs/05-data-modeling.md` - Database schema design
- `infra/local-dev/postgres/init.sql` - Schema definition

