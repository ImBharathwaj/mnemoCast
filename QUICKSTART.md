# Quick Start Guide

Get the Mnemocast Engine running in 3 steps!

---

## Step 1: Start Redis

```bash
# Check if Redis is running
redis-cli ping

# If not, start Redis:
redis-server
# OR
sudo systemctl start redis-server  # Linux
brew services start redis          # macOS
```

---

## Step 2: Run the Engine

```bash
cd backend
sbt "project engineApi" run
```

Or use the run script:
```bash
./scripts/dev-run.sh
```

---

## Step 3: Test It

Open a new terminal:

```bash
# Should return 204 (no ads yet - this is normal!)
curl http://localhost:8080/ads/deliver

# List ads (empty array initially)
curl http://localhost:8080/admin/ads
```

---

## Create Your First Ad

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [],
    "isActive": true
  }'
```

Now try the delivery endpoint again - you should get an ad!

---

## Try New Features

### Create Ad with Budget Management

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [],
    "isActive": true,
    "maxPlays": 100,
    "dailyLimit": 10,
    "hourlyLimit": 2
  }'
```

### Create Ad with Frequency Capping

```bash
curl -X POST http://localhost:8080/admin/ads \
  -H "Content-Type: application/json" \
  -d '{
    "advertiserId": "test",
    "creativeUrl": "https://example.com/ad.jpg",
    "targetUrl": "https://example.com",
    "targetingRules": [],
    "isActive": true,
    "maxImpressionsPerDevice": 3,
    "maxImpressionsPerUser": 2,
    "frequencyCapWindowHours": 24
  }'
```

### View Analytics

```bash
# Get dashboard metrics
curl http://localhost:8080/api/v1/analytics/dashboard

# Get performance for a specific ad (replace ad-123 with actual ad ID)
curl http://localhost:8080/api/v1/analytics/ads/ad-123

# Get all campaign performance
curl http://localhost:8080/api/v1/analytics/campaigns
```

---

## Need More Details?

- **Full guide:** See `docs/RUNNING.md`
- **API docs:** See `docs/02-api-spec.md`
- **Features:** See `docs/FEATURES.md`
- **Demo walkthrough:** See `docs/demo-script.md`

---

## Prerequisites

- **Java 11+**: `java -version`
- **sbt**: `sbt --version`
- **Redis**: `redis-cli ping`

Install missing prerequisites:
- Java: https://adoptium.net/
- sbt: https://www.scala-sbt.org/download.html
- Redis: https://redis.io/download

minio server ~/minio/data --console-address ":9001" &