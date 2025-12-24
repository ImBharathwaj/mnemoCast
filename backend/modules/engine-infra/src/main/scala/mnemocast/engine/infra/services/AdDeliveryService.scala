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
        val timestamp = java.time.LocalDateTime.now()
        println(s"[$timestamp] [AdDelivery] ========== AD DELIVERY REQUEST ==========")
        println(s"[$timestamp] [AdDelivery] Request ID: ${request.requestId}")
        println(s"[$timestamp] [AdDelivery] Screen ID: ${request.screenId.getOrElse("N/A")}")
        println(s"[$timestamp] [AdDelivery] Screen Classification: $screenClassification")
        println(s"[$timestamp] [AdDelivery] Request Context: country=${request.country.getOrElse("N/A")}, city=${request.city.getOrElse("N/A")}, area=${request.area.getOrElse("N/A")}, venueType=${request.venueType.getOrElse("N/A")}, tags=[${request.screenTags.mkString(", ")}]")
        println(s"[$timestamp] [AdDelivery] Total active ads in store: ${ads.length}")
        
        if (ads.isEmpty) {
          println(s"[$timestamp] [AdDelivery] ERROR: NO ACTIVE ADS IN DATABASE!")
          println(s"[$timestamp] [AdDelivery] ==========================================")
        } else {
          println(s"[$timestamp] [AdDelivery] Active ads: ${ads.map(_.id).mkString(", ")}")
        }
        
        // Step 1: Filter ads using targeting rules
        val targetingEligible = ads.filter { ad =>
          val matches = TargetingService.matches(ad, request)
          if (!matches && ads.length <= 10) {
            // Log why each ad was rejected (only if we have few ads to avoid spam)
            println(s"[$timestamp] [AdDelivery] REJECTED: Ad '${ad.id}' rejected by targeting:")
            if (ad.targetingRules.isEmpty) {
              println(s"[$timestamp] [AdDelivery]   -> Ad has no targeting rules (should match all)")
            } else {
              ad.targetingRules.foreach { rule =>
                val requestValue = TargetingService.extractRequestValue(rule.key, request)
                println(s"[$timestamp] [AdDelivery]   -> Rule: ${rule.key} ${rule.operator} ${rule.value}")
                println(s"[$timestamp] [AdDelivery]   -> Request value: ${requestValue.getOrElse("MISSING")}")
                val ruleMatches = TargetingService.evaluateRule(rule, request)
                println(s"[$timestamp] [AdDelivery]   -> Rule result: ${if (ruleMatches) "PASS" else "FAIL"}")
              }
            }
          }
          matches
        }
        println(s"[$timestamp] [AdDelivery] After targeting filter: ${targetingEligible.length}/${ads.length} ads eligible")
        
        if (targetingEligible.isEmpty && ads.nonEmpty) {
          println(s"[$timestamp] [AdDelivery] WARNING: ALL ADS REJECTED BY TARGETING RULES!")
          println(s"[$timestamp] [AdDelivery] Checking for ads with no targeting rules (fallback ads)...")
          val fallbackAds = ads.filter(_.targetingRules.isEmpty)
          if (fallbackAds.nonEmpty) {
            println(s"[$timestamp] [AdDelivery] Found ${fallbackAds.length} fallback ads (no targeting rules): ${fallbackAds.map(_.id).mkString(", ")}")
            println(s"[$timestamp] [AdDelivery] These should match all screens. Investigating why they didn't pass...")
          } else {
            println(s"[$timestamp] [AdDelivery] No fallback ads found. All ads have targeting rules.")
          }
        }
        
        // Step 2: Filter by budget constraints (async)
        Future.sequence(
          targetingEligible.map(ad => 
            budgetService.isWithinBudget(ad).map(withinBudget => (ad, withinBudget))
          )
        ).flatMap { budgetResults =>
          val budgetEligible = budgetResults.filter(_._2).map(_._1)
          val budgetRejected = budgetResults.filter(!_._2).map(_._1)
          println(s"[$timestamp] [AdDelivery] After budget filter: ${budgetEligible.length}/${targetingEligible.length} ads eligible")
          if (budgetRejected.nonEmpty) {
            println(s"[$timestamp] [AdDelivery] Budget rejected ads: ${budgetRejected.map(_.id).mkString(", ")}")
          }
          if (targetingEligible.nonEmpty && budgetEligible.isEmpty) {
            println(s"[$timestamp] [AdDelivery] WARNING: ALL ADS FILTERED OUT BY BUDGET CONSTRAINTS!")
          }
          
          // Step 3: Filter by frequency capping (async)
          Future.sequence(
            budgetEligible.map(ad =>
              frequencyCapService.canShow(ad, request).map(canShow => (ad, canShow))
            )
          ).flatMap { frequencyResults =>
            val eligible = frequencyResults.filter(_._2).map(_._1)
            val frequencyRejected = frequencyResults.filter(!_._2).map(_._1)
            println(s"[$timestamp] [AdDelivery] After frequency cap filter: ${eligible.length}/${budgetEligible.length} ads eligible")
            if (frequencyRejected.nonEmpty) {
              println(s"[$timestamp] [AdDelivery] Frequency cap rejected ads: ${frequencyRejected.map(_.id).mkString(", ")}")
            }
            if (budgetEligible.nonEmpty && eligible.isEmpty) {
              println(s"[$timestamp] [AdDelivery] WARNING: ALL ADS FILTERED OUT BY FREQUENCY CAPPING!")
            }
            
            println(s"[$timestamp] [AdDelivery] Final eligible ads: ${eligible.map(_.id).mkString(", ")}")
            
            pickAd(eligible, screenClassification) match {
            case Some(ad) =>
              println(s"[$timestamp] [AdDelivery] SUCCESS: Selected ad: ${ad.id} (weight: ${ad.weight}, screen classification: $screenClassification)")
              println(s"[$timestamp] [AdDelivery] ==========================================")
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
              println(s"[$timestamp] [AdDelivery] ERROR: NO ELIGIBLE ADS FOUND AFTER ALL FILTERS")
              println(s"[$timestamp] [AdDelivery] Summary:")
              println(s"[$timestamp] [AdDelivery]   - Total active ads: ${ads.length}")
              println(s"[$timestamp] [AdDelivery]   - After targeting: ${targetingEligible.length}")
              println(s"[$timestamp] [AdDelivery]   - After budget: ${budgetEligible.length}")
              println(s"[$timestamp] [AdDelivery]   - After frequency cap: ${eligible.length}")
              println(s"[$timestamp] [AdDelivery] ==========================================")
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
