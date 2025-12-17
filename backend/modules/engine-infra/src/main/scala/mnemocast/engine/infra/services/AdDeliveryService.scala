package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import mnemocast.engine.domain.model.{Ad, DeliveryEvent, DeliveryRequest, DeliveryResponse}
import mnemocast.engine.domain.services.TargetingService
import mnemocast.engine.infra.store.{AdStore, EventStore, ScreenStore}

/**
  * Core ad delivery logic (v3 with targeting, budget, and frequency capping).
  *
  * - Fetch all active ads from AdStore
  * - Filter ads using targeting rules (TargetingService)
  * - Filter ads using budget constraints (BudgetService)
  * - Filter ads using frequency capping (FrequencyCapService)
  * - Pick one using weighted selection from eligible ads (ads with higher weights are more likely to be selected)
  * - Build DeliveryResponse with impression tracking URL
  * - Log an "impression" DeliveryEvent when an ad is served
  */
class AdDeliveryService(
  adStore: AdStore,
  eventStore: EventStore,
  budgetService: BudgetService,
  frequencyCapService: FrequencyCapService,
  screenStore: Option[ScreenStore] = None, // Optional: for screen classification-based weighting
  baseUrl: String = "http://localhost:8080" // Base URL for tracking endpoints
)(implicit ec: ExecutionContext) {

  /** Main entry point: given a request, decide which ad (if any) to serve. */
  def deliver(request: DeliveryRequest): Future[Option[DeliveryResponse]] = {
    // Fetch screen classification if screenId is present and ScreenStore is available
    val screenClassificationFut = (request.screenId, screenStore) match {
      case (Some(screenId), Some(store)) =>
        store.getById(screenId).map(_.map(_.classification).getOrElse(1))
      case _ =>
        Future.successful(1) // Default classification if screen not found or store not available
    }

    screenClassificationFut.flatMap { screenClassification =>
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
            
            pickAd(eligible, screenClassification) match {
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
    }
  }

  /**
    * Weighted selection among eligible ads with screen classification boost.
    * 
    * Higher classified screens boost ad weights proportionally:
    * - Screen classification 5 + Ad weight 3 = effective weight 15
    * - This creates a pay-per-attention model where premium screens favor premium (high-weight) ads
    * 
    * @param ads List of eligible ads
    * @param screenClassification Screen classification (1-10, default 1). Higher = premium screen
    */
  private def pickAd(ads: List[Ad], screenClassification: Int = 1): Option[Ad] = {
    if (ads.isEmpty) return None

    // Build weighted pool: each ad appears in the pool (weight * screenClassification) times
    // This ensures higher classified screens favor higher weight ads
    val weightedPool = ads.flatMap { ad =>
      val baseWeight = math.max(1, ad.weight) // Ensure weight is at least 1
      val effectiveWeight = baseWeight * math.max(1, screenClassification) // Boost by screen classification
      List.fill(effectiveWeight)(ad)
    }

    // Select randomly from the weighted pool
    Some(weightedPool(Random.nextInt(weightedPool.length)))
  }

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
