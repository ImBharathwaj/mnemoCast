package mnemocast.engine.infra.store

import java.time.Instant

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

  /** Count impressions for an ad within a time window (for budget tracking). */
  def countImpressionsByAdId(adId: String, since: Instant): Future[Int]

  /** Count impressions for an ad by device within a time window (for frequency capping). */
  def countImpressionsByAdIdAndDevice(adId: String, deviceId: String, since: Instant): Future[Int]

  /** Count impressions for an ad by user within a time window (for frequency capping). */
  def countImpressionsByAdIdAndUser(adId: String, userId: String, since: Instant): Future[Int]

  /** Get total impression count for an ad (for maxPlays budget). */
  def countTotalImpressionsByAdId(adId: String): Future[Int]
}
