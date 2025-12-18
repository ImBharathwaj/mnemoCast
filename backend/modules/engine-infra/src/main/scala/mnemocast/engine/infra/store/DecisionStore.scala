package mnemocast.engine.infra.store

import scala.concurrent.Future

import mnemocast.engine.domain.model.Decision

/**
  * Abstraction over decision storage.
  * Decisions are logged for audit trail and analytics.
  */
trait DecisionStore {

  /** Insert a decision. */
  def append(decision: Decision): Future[Unit]

  /** Get recent decisions for a screen. */
  def findByScreenId(screenId: String, limit: Int): Future[List[Decision]]

  /** Get a decision by its ID. */
  def getById(decisionId: String): Future[Option[Decision]]

  /** Get recent decisions (across all screens). */
  def listRecent(limit: Int): Future[List[Decision]]
}

