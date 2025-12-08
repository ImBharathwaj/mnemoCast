package mnemocast.engine.infra.services

import java.time.{Instant, ZoneId}
import java.time.temporal.ChronoUnit

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.{Ad, DeliveryRequest}
import mnemocast.engine.infra.store.EventStore

/**
  * Frequency capping service.
  *
  * Validates whether an ad can be shown to a specific device/user based on frequency limits:
  * - maxImpressionsPerDevice: Maximum impressions per device within a time window
  * - maxImpressionsPerUser: Maximum impressions per user within a time window
  * - frequencyCapWindowHours: Time window for frequency cap (e.g., 24 hours)
  */
class FrequencyCapService(
  eventStore: EventStore
)(implicit ec: ExecutionContext) {

  /**
    * Checks if an ad can be shown to the given request based on frequency capping rules.
    *
    * @param ad      The ad to check
    * @param request The delivery request (contains deviceId, userId)
    * @return Future[Boolean] - true if ad can be shown, false if frequency cap exceeded
    */
  def canShow(ad: Ad, request: DeliveryRequest): Future[Boolean] = {
    // If no frequency capping is configured, allow the ad
    if (ad.maxImpressionsPerDevice.isEmpty && ad.maxImpressionsPerUser.isEmpty) {
      return Future.successful(true)
    }

    val windowStart = getWindowStart(ad.frequencyCapWindowHours)

    // Check device-level frequency cap
    val deviceCheck = request.deviceId match {
      case Some(deviceId) if ad.maxImpressionsPerDevice.isDefined =>
        eventStore.countImpressionsByAdIdAndDevice(ad.id, deviceId, windowStart)
          .map(count => count < ad.maxImpressionsPerDevice.get)
      case _ => Future.successful(true) // No deviceId or no device cap configured
    }

    // Check user-level frequency cap
    val userCheck = request.userId match {
      case Some(userId) if ad.maxImpressionsPerUser.isDefined =>
        eventStore.countImpressionsByAdIdAndUser(ad.id, userId, windowStart)
          .map(count => count < ad.maxImpressionsPerUser.get)
      case _ => Future.successful(true) // No userId or no user cap configured
    }

    // Both checks must pass (AND logic)
    for {
      deviceOk <- deviceCheck
      userOk   <- userCheck
    } yield deviceOk && userOk
  }

  /**
    * Gets the start of the frequency cap window.
    *
    * @param windowHours Optional window size in hours (defaults to 24 if not specified)
    * @return Instant representing the start of the window
    */
  private def getWindowStart(windowHours: Option[Int]): Instant = {
    val hours = windowHours.getOrElse(24)
    Instant.now().minus(hours, ChronoUnit.HOURS)
  }
}

