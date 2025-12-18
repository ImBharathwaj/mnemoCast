package mnemocast.engine.infra.store

import scala.concurrent.Future

import mnemocast.engine.domain.model.Campaign

/**
  * Abstraction over campaign storage.
  * Implementations can be in-memory, Redis, Postgres, etc.
  */
trait CampaignStore {

  /** Insert or update a campaign. */
  def upsert(campaign: Campaign): Future[Unit]

  /** Fetch a single campaign by its ID. */
  def getById(id: String): Future[Option[Campaign]]

  /** Return all active campaigns (status = "active" and within date range). */
  def listActive(): Future[List[Campaign]]

  /** Return all campaigns (regardless of status). */
  def listAll(): Future[List[Campaign]]

  /** Delete a campaign. */
  def delete(id: String): Future[Unit]
}

