package mnemocast.engine.infra.store

import scala.concurrent.Future

import mnemocast.engine.domain.model.Screen

/**
  * Abstraction over screen storage.
  * Implementations can be in-memory, Redis, Postgres, etc.
  */
trait ScreenStore {

  /** Insert or update a screen. */
  def upsert(screen: Screen): Future[Unit]

  /** Fetch a single screen by its ID. */
  def getById(id: String): Future[Option[Screen]]

  /** Return all screens. */
  def listAll(): Future[List[Screen]]

  /** Delete a screen. */
  def delete(id: String): Future[Unit]

  /** Update last seen timestamp for a screen (heartbeat). */
  def updateLastSeen(id: String): Future[Unit]
}

