package mnemocast.engine.infra.store

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.DeliveryEvent

/**
  * Hybrid event storage strategy:
  * - Postgres: Persistent storage for historical events and analytics
  * - Redis: Cache for recent events (fast queries)
  */
class HybridEventStore(
  postgresStore: EventStore,  // Postgres implementation (persistent)
  redisStore: EventStore      // Redis implementation (cache for recent events)
)(implicit ec: ExecutionContext) extends EventStore {

  override def append(event: DeliveryEvent): Future[Unit] = {
    // Write to both: Postgres (persistent) and Redis (cache)
    for {
      _ <- postgresStore.append(event)
      _ <- redisStore.append(event)
    } yield ()
  }

  override def findByAdId(adId: String, limit: Int): Future[List[DeliveryEvent]] = {
    // For recent events, try Redis first, then Postgres
    // Redis is good for recent events (last N events)
    if (limit <= 100) {
      // Small limit = likely recent events, try Redis first
      redisStore.findByAdId(adId, limit).flatMap {
        case events if events.nonEmpty => Future.successful(events)
        case _ => postgresStore.findByAdId(adId, limit)
      }
    } else {
      // Large limit = historical query, use Postgres
      postgresStore.findByAdId(adId, limit)
    }
  }

  override def countImpressionsByAdId(adId: String, since: Instant): Future[Int] = {
    // Use Postgres for accurate counts (source of truth)
    postgresStore.countImpressionsByAdId(adId, since)
  }

  override def countImpressionsByAdIdAndDevice(adId: String, deviceId: String, since: Instant): Future[Int] = {
    // Use Postgres for accurate counts
    postgresStore.countImpressionsByAdIdAndDevice(adId, deviceId, since)
  }

  override def countImpressionsByAdIdAndUser(adId: String, userId: String, since: Instant): Future[Int] = {
    // Use Postgres for accurate counts
    postgresStore.countImpressionsByAdIdAndUser(adId, userId, since)
  }

  override def countTotalImpressionsByAdId(adId: String): Future[Int] = {
    // Use Postgres for accurate counts
    postgresStore.countTotalImpressionsByAdId(adId)
  }
}

