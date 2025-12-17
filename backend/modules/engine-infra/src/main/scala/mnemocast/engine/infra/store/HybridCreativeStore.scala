package mnemocast.engine.infra.store

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.Creative

/**
  * Hybrid storage strategy for creatives:
  * - Postgres: Persistent storage (source of truth)
  * - Redis: Cache for hot data (active creatives) for fast access
  */
class HybridCreativeStore(
  postgresStore: CreativeStore,  // Postgres implementation
  redisStore: CreativeStore      // Redis implementation (cache)
)(implicit ec: ExecutionContext) extends CreativeStore {

  override def upsert(creative: Creative): Future[Unit] = {
    postgresStore.upsert(creative).recover { case ex: Exception =>
      println(s"⚠️  Warning: Failed to write creative to Postgres: ${ex.getMessage}")
    }.flatMap { _ =>
      redisStore.upsert(creative)
    }
  }

  override def getById(id: String): Future[Option[Creative]] = {
    redisStore.getById(id).flatMap {
      case Some(creative) => Future.successful(Some(creative))
      case None =>
        postgresStore.getById(id).flatMap {
          case Some(creative) =>
            redisStore.upsert(creative).map(_ => Some(creative))
          case None => Future.successful(None)
        }
    }
  }

  override def findByCampaignId(campaignId: String): Future[List[Creative]] = {
    // Use Postgres for accurate campaign-to-creative relationships
    postgresStore.findByCampaignId(campaignId)
  }

  override def listActive(): Future[List[Creative]] = {
    postgresStore.listActive()
  }

  override def listAll(): Future[List[Creative]] = {
    postgresStore.listAll()
  }

  override def delete(id: String): Future[Unit] = {
    for {
      _ <- postgresStore.delete(id)
      _ <- redisStore.delete(id)
    } yield ()
  }
}

