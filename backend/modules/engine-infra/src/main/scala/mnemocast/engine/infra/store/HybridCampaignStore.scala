package mnemocast.engine.infra.store

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.Campaign

/**
  * Hybrid storage strategy for campaigns:
  * - Postgres: Persistent storage (source of truth)
  * - Redis: Cache for hot data (active campaigns) for fast access
  */
class HybridCampaignStore(
  postgresStore: CampaignStore,  // Postgres implementation
  redisStore: CampaignStore      // Redis implementation (cache)
)(implicit ec: ExecutionContext) extends CampaignStore {

  override def upsert(campaign: Campaign): Future[Unit] = {
    postgresStore.upsert(campaign).recover { case ex: Exception =>
      println(s"⚠️  Warning: Failed to write campaign to Postgres: ${ex.getMessage}")
    }.flatMap { _ =>
      redisStore.upsert(campaign)
    }
  }

  override def getById(id: String): Future[Option[Campaign]] = {
    redisStore.getById(id).flatMap {
      case Some(campaign) => Future.successful(Some(campaign))
      case None =>
        postgresStore.getById(id).flatMap {
          case Some(campaign) =>
            redisStore.upsert(campaign).map(_ => Some(campaign))
          case None => Future.successful(None)
        }
    }
  }

  override def listActive(): Future[List[Campaign]] = {
    // Use Postgres for accurate active campaigns (date range checking)
    postgresStore.listActive()
  }

  override def listAll(): Future[List[Campaign]] = {
    postgresStore.listAll()
  }

  override def delete(id: String): Future[Unit] = {
    for {
      _ <- postgresStore.delete(id)
      _ <- redisStore.delete(id)
    } yield ()
  }
}

