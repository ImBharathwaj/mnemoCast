# Mnemocast — Ad Serving Engine

A smart ad serving engine built with Scala and Pekko HTTP.

## Features

- **Ad Delivery**: Smart ad selection with targeting rules
- **Budget Management**: Daily/hourly/total budget limits
- **Frequency Capping**: Limit impressions per device/user
- **Analytics**: Performance metrics, dashboard reports

## Quick Start

1. **Start Redis**: `redis-server`
2. **Run the engine**: `cd backend && sbt run`
3. **Test the API**: `curl http://localhost:8080/ads/deliver`

See `docs/RUNNING.md` for detailed setup instructions.

## Documentation

- **cURL Testing Guide**: `docs/CURL_TESTING.md` - Test all capabilities with curl commands
- **Capabilities**: `docs/CAPABILITIES.md` - What the system can do
- **API Specification**: `docs/02-api-spec.md` - Complete API reference
- **Features Overview**: `docs/FEATURES.md` - Feature details
- **Running Guide**: `docs/RUNNING.md` - Setup and deployment
- **Domain Model**: `docs/01-domain-model.md` - Data models

## API Endpoints

- `GET /ads/deliver` - Request an ad
- `POST /admin/ads` - Create an ad
- `GET /api/v1/analytics/dashboard` - Dashboard metrics
- `GET /api/v1/events/impression` - Impression tracking

See `docs/02-api-spec.md` for complete API documentation.

---

# Development Steps (Epic 1)

1. **Implement Core Data Models**  
   - Create case classes + JSON codecs  
   - Files:
     - Ad.scala
     - TargetingRule.scala
     - DeliveryRequest.scala
     - DeliveryResponse.scala
     - DeliveryEvent.scala

2. **Define Storage Interfaces**  
   - Create minimal CRUD traits
   - Files:
     - AdStore.scala
     - EventStore.scala

3. **Implement Redis-Based Storage**  
   - Setup Redis client + JSON storage
   - Files:
     - RedisClient.scala
     - RedisAdStore.scala
     - RedisEventStore.scala (optional)

4. **Implement Ad Delivery Service**  
   - Simple random ad selection logic first
   - File:
     - AdDeliveryService.scala

5. **Create HTTP API Endpoint**  
   - Expose GET /ads/deliver
   - Files:
     - AdDeliveryController.scala
     - Routes.scala
     - Main.scala

6. **Add Event Logging**  
   - Log `DeliveryEvent` after serving an ad

7. **End-to-End Testing**
   - Run API → verify ad response + Redis logs
   - Command:
     ```
     curl http://localhost:8080/ads/deliver
     ```