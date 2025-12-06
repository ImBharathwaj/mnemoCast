package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import mnemocast.engine.domain.model.{Ad, DeliveryEvent, DeliveryRequest, DeliveryResponse}
import mnemocast.engine.infra.store.{AdStore, EventStore}

/**
  * Core ad delivery logic (v1).
  *
  * - Fetch all active ads from AdStore
  * - Pick one at random
  * - Build DeliveryResponse
  * - Log an "impression" DeliveryEvent when an ad is served
  */
class AdDeliveryService(
  adStore: AdStore,
  eventStore: EventStore
)(implicit ec: ExecutionContext) {

  /** Main entry point: given a request, decide which ad (if any) to serve. */
  def deliver(request: DeliveryRequest): Future[Option[DeliveryResponse]] =
    adStore.listActive().flatMap { ads =>
      pickAd(ads) match {
        case Some(ad) =>
          val response = DeliveryResponse(
            requestId = request.requestId,
            adId = ad.id,
            creativeUrl = ad.creativeUrl,
            targetUrl = ad.targetUrl,
            impressionTrackingUrl = None // will wire tracking endpoint later
          )

          val event = buildImpressionEvent(request, ad)

          // Log event, then return the response
          eventStore.append(event).map(_ => Some(response))

        case None =>
          Future.successful(None)
      }
    }

  /** v1 logic: random selection among active ads. */
  private def pickAd(ads: List[Ad]): Option[Ad] =
    if (ads.isEmpty) None
    else Some(ads(Random.nextInt(ads.length)))

  private def buildImpressionEvent(request: DeliveryRequest, ad: Ad): DeliveryEvent = {
    val baseMeta = Map(
      "deviceId" -> request.deviceId.getOrElse(""),
      "userId"   -> request.userId.getOrElse(""),
      "appId"    -> request.appId.getOrElse(""),
      "ip"       -> request.ip.getOrElse(""),
      "country"  -> request.country.getOrElse(""),
      "platform" -> request.platform.getOrElse("")
    )

    val cleanedMeta = baseMeta.filter { case (_, v) => v.nonEmpty }

    DeliveryEvent(
      eventId    = UUID.randomUUID().toString,
      requestId  = request.requestId,
      adId       = ad.id,
      eventType  = "impression",
      occurredAt = Instant.now(),
      metadata   = cleanedMeta
    )
  }
}
