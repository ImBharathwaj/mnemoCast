package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import mnemocast.engine.domain.model.{Ad, DeliveryEvent, DeliveryRequest, DeliveryResponse}
import mnemocast.engine.domain.services.TargetingService
import mnemocast.engine.infra.store.{AdStore, EventStore}

/**
  * Core ad delivery logic (v3 with targeting, budget, and frequency capping).
  *
  * - Fetch all active ads from AdStore
  * - Filter ads using targeting rules (TargetingService)
  * - Filter ads using budget constraints (BudgetService)
  * - Filter ads using frequency capping (FrequencyCapService)
  * - Pick one at random from eligible ads
  * - Build DeliveryResponse with impression tracking URL
  * - Log an "impression" DeliveryEvent when an ad is served
  */
class AdDeliveryService(
  adStore: AdStore,
  eventStore: EventStore,
  budgetService: BudgetService,
  frequencyCapService: FrequencyCapService,
  baseUrl: String = "http://localhost:8080" // Base URL for tracking endpoints
)(implicit ec: ExecutionContext) {

  /** Main entry point: given a request, decide which ad (if any) to serve. */
  def deliver(request: DeliveryRequest): Future[Option[DeliveryResponse]] =
    adStore.listActive().flatMap { ads =>
      // Step 1: Filter ads using targeting rules
      val targetingEligible = ads.filter(ad => TargetingService.matches(ad, request))
      
      // Step 2: Filter by budget constraints (async)
      Future.sequence(
        targetingEligible.map(ad => 
          budgetService.isWithinBudget(ad).map(withinBudget => (ad, withinBudget))
        )
      ).flatMap { budgetResults =>
        val budgetEligible = budgetResults.filter(_._2).map(_._1)
        
        // Step 3: Filter by frequency capping (async)
        Future.sequence(
          budgetEligible.map(ad =>
            frequencyCapService.canShow(ad, request).map(canShow => (ad, canShow))
          )
        ).flatMap { frequencyResults =>
          val eligible = frequencyResults.filter(_._2).map(_._1)
          
          pickAd(eligible) match {
            case Some(ad) =>
              val response = DeliveryResponse(
                requestId = request.requestId,
                adId = ad.id,
                creativeUrl = ad.creativeUrl,
                targetUrl = ad.targetUrl,
                impressionTrackingUrl = Some(s"$baseUrl/api/v1/events/impression?adId=${ad.id}&requestId=${request.requestId}")
              )

              val event = buildImpressionEvent(request, ad)

              // Log event, then return the response
              eventStore.append(event).map(_ => Some(response))

            case None =>
              Future.successful(None)
          }
        }
      }
    }

  /** Random selection among eligible ads. */
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
