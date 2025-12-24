#  Mnemocast Deployment Guide

**Version:** 1.0.0  
**Last Updated:** December 2024

---

##  Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Environment Setup](#environment-setup)
4. [Docker Deployment](#docker-deployment)
5. [Kubernetes Deployment](#kubernetes-deployment)
6. [Configuration](#configuration)
7. [Health Checks](#health-checks)
8. [Monitoring](#monitoring)
9. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### **Required Services**
- **Redis** (v6.0+): For caching and fast data access
- **PostgreSQL** (v12+): For persistent data storage (optional, can use Redis-only)
- **MinIO** (v8.0+): For media storage (S3-compatible object storage)

### **System Requirements**
- **CPU:** 2+ cores recommended
- **Memory:** 4GB+ RAM recommended
- **Disk:** 20GB+ for media storage
- **Network:** Port 8080 (API), 6379 (Redis), 5432 (PostgreSQL), 9000 (MinIO)

### **Software Requirements**
- **Java 11+** (for Scala/Pekko HTTP backend)
- **Node.js 16+** (for React frontend)
- **sbt 1.8+** (Scala build tool)

---

## Quick Start

### **1. Clone Repository**
```bash
git clone <repository-url>
cd mnemoCast
```

### **2. Start Dependencies**
```bash
# Using Docker Compose (recommended)
docker-compose up -d redis postgres minio

# Or start individually:
# Redis
docker run -d -p 6379:6379 redis:7-alpine

# PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=mnemocast \
  postgres:14-alpine

# MinIO
docker run -d -p 9000:9000 -p 9001:9001 \
  -e MINIO_ROOT_USER=minioadmin \
  -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

### **3. Configure Environment**
```bash
# Backend
cd backend
export STORAGE_STRATEGY=hybrid  # or "redis" or "postgres"
export REDIS_HOST=localhost
export REDIS_PORT=6379
export POSTGRES_URL=jdbc:postgresql://localhost:5432/mnemocast
export POSTGRES_USER=postgres
export POSTGRES_PASSWORD=postgres
export MINIO_ENDPOINT=localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin
```

### **4. Start Backend**
```bash
cd backend
sbt run
# Server starts on http://localhost:8080
```

### **5. Start Frontend**
```bash
cd dashboard
npm install
npm start
# Dashboard starts on http://localhost:3000
```

### **6. Seed Demo Data**
```bash
# Make sure backend is running
chmod +x scripts/seed-comprehensive-demo.sh
./scripts/seed-comprehensive-demo.sh
```

---

## Environment Setup

### **Backend Environment Variables**

| Variable | Default | Description |
|----------|---------|-------------|
| `STORAGE_STRATEGY` | `redis` | Storage strategy: `redis`, `postgres`, or `hybrid` |
| `REDIS_HOST` | `localhost` | Redis server host |
| `REDIS_PORT` | `6379` | Redis server port |
| `POSTGRES_URL` | - | PostgreSQL JDBC URL |
| `POSTGRES_USER` | `postgres` | PostgreSQL username |
| `POSTGRES_PASSWORD` | - | PostgreSQL password |
| `MINIO_ENDPOINT` | `localhost:9000` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | - | MinIO access key |
| `MINIO_SECRET_KEY` | - | MinIO secret key |
| `MINIO_BUCKET` | `creatives` | MinIO bucket name |
| `MAX_IMAGE_SIZE_MB` | `10` | Maximum image size in MB |
| `MAX_VIDEO_SIZE_MB` | `500` | Maximum video size in MB |

### **Frontend Environment Variables**

| Variable | Default | Description |
|----------|---------|-------------|
| `REACT_APP_API_URL` | `http://localhost:8080` | Backend API URL |

Create `.env` file in `dashboard/`:
```bash
REACT_APP_API_URL=http://localhost:8080
```

---

## Docker Deployment

### **Backend Dockerfile**
```dockerfile
FROM openjdk:11-jre-slim

WORKDIR /app

# Copy built JAR
COPY backend/target/scala-2.13/mnemocast-engine_2.13-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **Build and Run**
```bash
# Build backend
cd backend
sbt assembly
docker build -t mnemocast-engine:latest .

# Run backend
docker run -d \
  -p 8080:8080 \
  -e STORAGE_STRATEGY=hybrid \
  -e REDIS_HOST=redis \
  -e POSTGRES_URL=jdbc:postgresql://postgres:5432/mnemocast \
  --name mnemocast-engine \
  mnemocast-engine:latest
```

### **Docker Compose**
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
    image: postgres:14-alpine
    environment:
      POSTGRES_DB: mnemocast
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data

  minio:
    image: minio/minio:latest
    command: server /data --console-address ":9001"
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    ports:
      - "8080:8080"
    environment:
      STORAGE_STRATEGY: hybrid
      REDIS_HOST: redis
      REDIS_PORT: 6379
      POSTGRES_URL: jdbc:postgresql://postgres:5432/mnemocast
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      MINIO_ENDPOINT: minio:9000
      MINIO_ACCESS_KEY: minioadmin
      MINIO_SECRET_KEY: minioadmin
    depends_on:
      - redis
      - postgres
      - minio

  frontend:
    build:
      context: ./dashboard
      dockerfile: Dockerfile
    ports:
      - "3000:80"
    environment:
      REACT_APP_API_URL: http://localhost:8080
    depends_on:
      - backend

volumes:
  redis-data:
  postgres-data:
  minio-data:
```

---

## Kubernetes Deployment

### **Deployment YAML**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: mnemocast-engine
spec:
  replicas: 3
  selector:
    matchLabels:
      app: mnemocast-engine
  template:
    metadata:
      labels:
        app: mnemocast-engine
    spec:
      containers:
      - name: engine
        image: mnemocast-engine:latest
        ports:
        - containerPort: 8080
        env:
        - name: STORAGE_STRATEGY
          value: "hybrid"
        - name: REDIS_HOST
          value: "redis-service"
        livenessProbe:
          httpGet:
            path: /api/v1/live
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /api/v1/ready
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1Gi"
            cpu: "1000m"
---
apiVersion: v1
kind: Service
metadata:
  name: mnemocast-engine-service
spec:
  selector:
    app: mnemocast-engine
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
```

---

## Configuration

### **Storage Strategy**

#### **Redis-Only (Fastest, No Persistence)**
```bash
export STORAGE_STRATEGY=redis
```
-  Fastest performance
-  Simple setup
-  No data persistence
-  Data lost on restart

#### **PostgreSQL-Only (Persistent)**
```bash
export STORAGE_STRATEGY=postgres
```
-  Data persistence
-  ACID compliance
-  Slower than Redis
-  More complex setup

#### **Hybrid (Recommended)**
```bash
export STORAGE_STRATEGY=hybrid
```
-  Best of both worlds
-  Fast reads (Redis cache)
-  Persistent writes (PostgreSQL)
-  Data durability

---

## Health Checks

### **Health Endpoints**

| Endpoint | Purpose | Kubernetes Probe |
|----------|---------|------------------|
| `/api/v1/health` | Full system health | - |
| `/api/v1/ready` | Readiness check | Readiness Probe |
| `/api/v1/live` | Liveness check | Liveness Probe |

### **Health Check Response**
```json
{
  "status": "UP",
  "timestamp": "2024-12-18T15:00:00Z",
  "uptimeSeconds": 3600,
  "components": [
    {
      "name": "Redis",
      "status": "healthy",
      "responseTimeMs": 2
    },
    {
      "name": "PostgreSQL",
      "status": "healthy",
      "responseTimeMs": 15
    },
    {
      "name": "MediaStorage",
      "status": "healthy",
      "responseTimeMs": 5
    }
  ],
  "version": "1.0.0"
}
```

---

## Monitoring

### **Metrics Endpoint**
```bash
GET /api/v1/metrics
```

**Response:**
```json
{
  "totalRequests": 10000,
  "averageResponseTimeMs": 25.5,
  "p95ResponseTimeMs": 85.0,
  "p99ResponseTimeMs": 150.0,
  "errorRate": 0.001,
  "activeCampaigns": 5,
  "activeCreatives": 17,
  "activeScreens": 9
}
```

### **Monitoring Dashboard**
Access the monitoring dashboard at:
- **URL:** `http://localhost:3000/monitoring`
- **Features:**
  - Real-time system health
  - Component status
  - Request metrics
  - Error rates

---

## Troubleshooting

### **Common Issues**

#### **1. Backend Won't Start**
- Check Redis is running: `redis-cli ping`
- Check PostgreSQL is accessible
- Verify environment variables are set

#### **2. Frontend Can't Connect to Backend**
- Verify `REACT_APP_API_URL` is correct
- Check CORS settings
- Ensure backend is running on correct port

#### **3. Health Check Failing**
- Check component connectivity (Redis, Postgres, MinIO)
- Review logs for specific error messages
- Verify network connectivity

#### **4. Performance Issues**
- Check Redis connection pool
- Monitor database query performance
- Review metrics endpoint for bottlenecks

### **Logs**
```bash
# Backend logs
tail -f backend/logs/application.log

# Docker logs
docker logs mnemocast-engine

# Kubernetes logs
kubectl logs -f deployment/mnemocast-engine
```

---

## Production Checklist

- [ ] Set strong passwords for all services
- [ ] Enable SSL/TLS for API
- [ ] Configure rate limiting
- [ ] Set up monitoring and alerting
- [ ] Configure backup strategy
- [ ] Set resource limits
- [ ] Enable logging aggregation
- [ ] Configure auto-scaling
- [ ] Set up CI/CD pipeline
- [ ] Perform load testing

---

## Support

For issues or questions:
- **Documentation:** See `docs/` directory
- **API Docs:** `http://localhost:8080/api/docs`
- **Health Check:** `http://localhost:8080/api/v1/health`

---

**Last Updated:** December 2024

