package mnemocast.engine.infra.store

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.Ad

/**
  * Hybrid storage strategy:
  * - Postgres: Persistent storage (source of truth)
  * - Redis: Cache for hot data (active ads) for fast access
  */
class HybridAdStore(
  postgresStore: AdStore,  // Postgres implementation
  redisStore: AdStore      // Redis implementation (cache)
)(implicit ec: ExecutionContext) extends AdStore {

  override def upsert(ad: Ad): Future[Unit] = {
    // Write to both: Postgres (persistent) and Redis (cache)
    for {
      _ <- postgresStore.upsert(ad)
      _ <- redisStore.upsert(ad)
    } yield ()
  }

  override def getById(id: String): Future[Option[Ad]] = {
    // Try Redis first (cache), fallback to Postgres
    redisStore.getById(id).flatMap {
      case Some(ad) => Future.successful(Some(ad))
      case None =>
        postgresStore.getById(id).flatMap {
          case Some(ad) =>
            // Cache in Redis for next time
            redisStore.upsert(ad).map(_ => Some(ad))
          case None => Future.successful(None)
        }
    }
  }

  override def listActive(): Future[List[Ad]] = {
    // For list operations, use Postgres (source of truth)
    // Could optimize by caching active ads list in Redis with TTL
    postgresStore.listActive()
  }

  override def listAll(): Future[List[Ad]] = {
    // Use Postgres for complete list
    postgresStore.listAll()
  }

  override def delete(id: String): Future[Unit] = {
    // Delete from both
    for {
      _ <- postgresStore.delete(id)
      _ <- redisStore.delete(id)
    } yield ()
  }
}

