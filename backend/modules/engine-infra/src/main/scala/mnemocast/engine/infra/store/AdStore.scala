package mnemocast.engine.infra.store

import scala.concurrent.Future

import mnemocast.engine.domain.model.Ad

/**
  * Abstraction over ad storage.
  * Implementations can be in-memory, Redis, SQL, etc.
  */
trait AdStore {

  /** Insert or overwrite an ad. */
  def upsert(ad: Ad): Future[Unit]

  /** Fetch a single ad by its ID. */
  def getById(id: String): Future[Option[Ad]]

  /** Return all active ads (for simple v1 delivery). */
  def listActive(): Future[List[Ad]]

  /** Return all ads (both active and inactive). */
  def listAll(): Future[List[Ad]]

  /** Delete an ad (optional for v1, useful later). */
  def delete(id: String): Future[Unit]
}
