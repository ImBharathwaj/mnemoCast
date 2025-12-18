package mnemocast.engine.infra.store

import scala.concurrent.Future

import mnemocast.engine.domain.model.Creative

/**
  * Abstraction over creative storage.
  * Implementations can be in-memory, Redis, Postgres, etc.
  */
trait CreativeStore {

  /** Insert or update a creative. */
  def upsert(creative: Creative): Future[Unit]

  /** Fetch a single creative by its ID. */
  def getById(id: String): Future[Option[Creative]]

  /** Fetch all creatives for a campaign. */
  def findByCampaignId(campaignId: String): Future[List[Creative]]

  /** Return all active creatives for active campaigns. */
  def listActive(): Future[List[Creative]]

  /** Return all creatives (regardless of status). */
  def listAll(): Future[List[Creative]]

  /** Delete a creative. */
  def delete(id: String): Future[Unit]
}

