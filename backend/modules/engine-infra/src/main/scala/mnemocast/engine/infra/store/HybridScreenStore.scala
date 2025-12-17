package mnemocast.engine.infra.store

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.Screen

/**
  * Hybrid storage strategy for screens:
  * - Postgres: Persistent storage (source of truth)
  * - Redis: Cache for hot data (active screens) for fast access
  */
class HybridScreenStore(
  postgresStore: ScreenStore,  // Postgres implementation
  redisStore: ScreenStore      // Redis implementation (cache)
)(implicit ec: ExecutionContext) extends ScreenStore {

  override def upsert(screen: Screen): Future[Unit] = {
    // Write to both: Postgres (persistent) and Redis (cache)
    // If Postgres fails, still write to Redis (graceful degradation)
    postgresStore.upsert(screen).recover { case ex: Exception =>
      println(s"⚠️  Warning: Failed to write screen to Postgres: ${ex.getMessage}")
      println(s"⚠️  Continuing with Redis-only write for screen ${screen.id}")
    }.flatMap { _ =>
      redisStore.upsert(screen)
    }
  }

  override def getById(id: String): Future[Option[Screen]] = {
    // Try Redis first (cache), fallback to Postgres
    redisStore.getById(id).flatMap {
      case Some(screen) => Future.successful(Some(screen))
      case None =>
        postgresStore.getById(id).flatMap {
          case Some(screen) =>
            // Cache in Redis for next time
            redisStore.upsert(screen).map(_ => Some(screen))
          case None => Future.successful(None)
        }
    }
  }

  override def listAll(): Future[List[Screen]] = {
    // For list operations, use Postgres (source of truth)
    // Could optimize by caching screen list in Redis with TTL
    postgresStore.listAll()
  }

  override def delete(id: String): Future[Unit] = {
    // Delete from both
    for {
      _ <- postgresStore.delete(id)
      _ <- redisStore.delete(id)
    } yield ()
  }

  override def updateLastSeen(id: String): Future[Unit] = {
    // Update both stores
    // For last_seen, we prioritize speed (Redis) but also persist to Postgres
    postgresStore.updateLastSeen(id).recover { case ex: Exception =>
      println(s"⚠️  Warning: Failed to update last_seen in Postgres: ${ex.getMessage}")
    }.flatMap { _ =>
      redisStore.updateLastSeen(id)
    }
  }
}

