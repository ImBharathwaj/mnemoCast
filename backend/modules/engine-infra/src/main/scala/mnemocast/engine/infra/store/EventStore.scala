package mnemocast.engine.infra.store

import scala.concurrent.Future

import mnemocast.engine.domain.model.DeliveryEvent

/**
  * Abstraction for logging and querying delivery events.
  */
trait EventStore {

  /** Persist a single delivery-related event (impression, click, etc.). */
  def append(event: DeliveryEvent): Future[Unit]

  /** Get recent events for a given ad (handy for debugging & analytics later). */
  def findByAdId(adId: String, limit: Int): Future[List[DeliveryEvent]]
}
