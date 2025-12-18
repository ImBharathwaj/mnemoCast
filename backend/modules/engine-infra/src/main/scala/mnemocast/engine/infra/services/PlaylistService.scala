package mnemocast.engine.infra.services

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import mnemocast.engine.domain.model.{Ad, DeliveryRequest, PlaylistItem, PlaylistResponse}
import mnemocast.engine.domain.services.TargetingService
import mnemocast.engine.infra.store.{AdStore, ScreenStore}

/**
  * Service for generating playlists of ads for OOH screens.
  *
  * - Fetches all active ads
  * - Filters by targeting rules (including time-based)
  * - Filters by budget and frequency capping
  * - Selects ads to fill the requested duration
  * - Returns a playlist with multiple ads
  */
class PlaylistService(
  adStore: AdStore,
  budgetService: BudgetService,
  frequencyCapService: FrequencyCapService,
  screenStore: Option[ScreenStore] = None, // Optional: for screen classification-based weighting
  baseUrl: String = "http://localhost:8080"
)(implicit ec: ExecutionContext) {

  /**
    * Generates a playlist for a screen with the requested duration.
    *
    * @param request Delivery request (should include screenId)
    * @param durationMinutes Desired playlist duration in minutes
    * @return PlaylistResponse with ads
    */
  def generatePlaylist(
    request: DeliveryRequest,
    durationMinutes: Int
  ): Future[Option[PlaylistResponse]] = {
    val targetDurationSeconds = durationMinutes * 60

    // Fetch screen classification if screenId is present and ScreenStore is available
    val screenClassificationFut = (request.screenId, screenStore) match {
      case (Some(screenId), Some(store)) =>
        store.getById(screenId).map(_.map(_.classification).getOrElse(1))
      case _ =>
        Future.successful(1) // Default classification if screen not found or store not available
    }

    screenClassificationFut.flatMap { screenClassification =>
      // Fetch eligible ads
      adStore.listActive().flatMap { allAds =>
        // Step 1: Filter by targeting (including time-based)
        val targetingEligible = allAds.filter(ad => TargetingService.matches(ad, request))

        // Step 2: Filter by budget (async)
        filterByBudget(targetingEligible).flatMap { budgetEligible =>
          // Step 3: Filter by frequency capping (async)
          filterByFrequencyCap(budgetEligible, request).flatMap { eligibleAds =>
            // Step 4: Generate playlist from eligible ads with screen classification
            val playlist = buildPlaylist(eligibleAds, targetDurationSeconds, request, screenClassification)

            playlist match {
              case Some(playlistResponse) =>
                Future.successful(Some(playlistResponse))
              case None =>
                Future.successful(None)
            }
          }
        }
      }
    }
  }

  /**
    * Filters ads by budget constraints.
    */
  private def filterByBudget(ads: List[Ad]): Future[List[Ad]] = {
    Future.sequence(
      ads.map(ad =>
        budgetService.isWithinBudget(ad).map(withinBudget => (ad, withinBudget))
      )
    ).map(_.filter(_._2).map(_._1))
  }

  /**
    * Filters ads by frequency capping.
    */
  private def filterByFrequencyCap(
    ads: List[Ad],
    request: DeliveryRequest
  ): Future[List[Ad]] = {
    Future.sequence(
      ads.map(ad =>
        frequencyCapService.canShow(ad, request).map(canShow => (ad, canShow))
      )
    ).map(_.filter(_._2).map(_._1))
  }

    /**
    * Builds a playlist from eligible ads to fill the target duration.
    *
    * Uses weighted selection based on ad weight boosted by screen classification:
    * - Higher classified screens boost ad weights proportionally
    * - Creates a pay-per-attention model where premium screens favor premium (high-weight) ads
    * - Repetition allowed (ads can appear multiple times in the playlist if needed)
    */
  private def buildPlaylist(
    eligibleAds: List[Ad],
    targetDurationSeconds: Int,
    request: DeliveryRequest,
    screenClassification: Int = 1
  ): Option[PlaylistResponse] = {
    if (eligibleAds.isEmpty) {
      return None
    }

    // Filter ads that have duration specified (required for playlist)
    val adsWithDuration = eligibleAds.filter(_.durationSeconds.isDefined)
    if (adsWithDuration.isEmpty) {
      return None
    }

    var currentDuration = 0
    val items = scala.collection.mutable.ListBuffer[PlaylistItem]()
    var position = 0

    // Build playlist by adding ads until we reach or exceed target duration
    // Limit iterations to prevent infinite loops
    var iterations = 0
    val maxIterations = (targetDurationSeconds / 5) + 10 // Reasonable upper bound

    // Build weighted pool for ad selection with screen classification boost
    // Higher classified screens boost ad weights proportionally (pay-per-attention model)
    val weightedPool = adsWithDuration.flatMap { ad =>
      val baseWeight = math.max(1, ad.weight) // Ensure weight is at least 1
      val effectiveWeight = baseWeight * math.max(1, screenClassification) // Boost by screen classification
      List.fill(effectiveWeight)(ad)
    }

    while (currentDuration < targetDurationSeconds && iterations < maxIterations) {
      // Pick an ad using weighted selection
      val selectedAd = weightedPool(Random.nextInt(weightedPool.length))
      val adDuration = selectedAd.durationSeconds.getOrElse(30) // Default to 30s if missing

      // Add to playlist
      val item = PlaylistItem(
        adId = selectedAd.id,
        creativeUrl = selectedAd.creativeUrl,
        targetUrl = selectedAd.targetUrl,
        durationSeconds = adDuration,
        impressionTrackingUrl = Some(
          s"$baseUrl/api/v1/events/impression?adId=${selectedAd.id}&requestId=${request.requestId}&position=$position"
        ),
        position = position
      )

      items.append(item)
      currentDuration += adDuration
      position += 1
      iterations += 1
    }

    if (items.nonEmpty) {
      Some(
        PlaylistResponse(
          requestId = request.requestId,
          screenId = request.screenId,
          items = items.toList,
          validForSeconds = targetDurationSeconds, // Valid for the full playlist duration
          totalDurationSeconds = currentDuration
        )
      )
    } else {
      None
    }
  }
}

