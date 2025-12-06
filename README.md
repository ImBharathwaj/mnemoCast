# Mnemocast — Epic 1 Development Steps (Ordered)

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