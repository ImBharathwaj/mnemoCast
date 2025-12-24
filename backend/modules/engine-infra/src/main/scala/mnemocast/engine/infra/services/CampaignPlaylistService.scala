package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Random

import mnemocast.engine.domain.model.{Campaign, Creative, Decision, DeliveryRequest, PlaylistItem, PlaylistResponse, SelectedCreative}
import mnemocast.engine.domain.services.TargetingService
import mnemocast.engine.infra.store.{CampaignStore, CreativeStore, DecisionStore}

/**
  * Service for generating playlists from campaigns and creatives.
  *
  * - Fetches all active campaigns
  * - Filters by campaign-level targeting rules (including time-based)
  * - Filters by campaign budget constraints
  * - Loads creatives for eligible campaigns
  * - Selects creatives to fill the requested duration
  * - Returns a playlist with multiple creatives
  */
class CampaignPlaylistService(
  campaignStore: CampaignStore,
  creativeStore: CreativeStore,
  campaignBudgetService: CampaignBudgetService,
  decisionStore: DecisionStore,
  baseUrl: String = "http://localhost:8080"
)(implicit ec: ExecutionContext) {

  /**
    * Generates a playlist for a screen with the requested duration using campaigns/creatives.
    *
    * @param request Delivery request (should include screenId)
    * @param durationMinutes Desired playlist duration in minutes
    * @return PlaylistResponse with creatives
    */
  def generatePlaylist(
    request: DeliveryRequest,
    durationMinutes: Int
  ): Future[Option[PlaylistResponse]] = {
    val targetDurationSeconds = durationMinutes * 60

    // Step 1: Fetch active campaigns (already filtered by date range in store)
    campaignStore.listActive().flatMap { allCampaigns =>
      // Step 2: Filter by campaign-level targeting
      val targetingEligible = allCampaigns.filter(campaign => TargetingService.matches(campaign, request))

      // Step 3: Filter by campaign budget constraints
      filterCampaignsByBudget(targetingEligible).flatMap { eligibleCampaigns =>
        val eligibleCampaignIds = eligibleCampaigns.map(_.id)
        
        // Step 4: Load creatives for eligible campaigns
        loadCreativesForCampaigns(eligibleCampaigns).flatMap { campaignCreatives =>
          // Step 5: Filter active creatives
          val activeCreatives = campaignCreatives.values.flatten.filter(_.status == "active").toList

          // Step 6: Generate playlist from eligible creatives
          buildPlaylist(eligibleCampaigns, campaignCreatives, activeCreatives, targetDurationSeconds, request).map { playlistResultOpt =>
            // Step 7: Log decision (fire and forget)
            playlistResultOpt.foreach { case (playlistResponse, selectedCreativesWithCampaigns) =>
              val selectedCreatives = selectedCreativesWithCampaigns.zipWithIndex.map { case ((creative, campaignId), idx) =>
                SelectedCreative(
                  creativeId = creative.id,
                  campaignId = campaignId,
                  position = idx,
                  durationSeconds = creative.durationSeconds
                )
              }
              
              val decision = Decision(
                decisionId = UUID.randomUUID().toString,
                requestId = request.requestId,
                screenId = request.screenId.getOrElse("unknown"),
                eligibleCampaignIds = eligibleCampaignIds,
                selectedCreatives = selectedCreatives,
                totalDurationSeconds = playlistResponse.totalDurationSeconds,
                timestamp = Instant.now()
              )
              
              decisionStore.append(decision).recover { case ex =>
                println(s"WARNING: Failed to log decision: ${ex.getMessage}")
              }
            }

            playlistResultOpt.map(_._1)
          }
        }
      }
    }
  }

  /**
    * Filters campaigns by budget constraints.
    */
  private def filterCampaignsByBudget(campaigns: List[Campaign]): Future[List[Campaign]] = {
    Future.sequence(
      campaigns.map(campaign =>
        campaignBudgetService.isWithinBudget(campaign).map(withinBudget => (campaign, withinBudget))
      )
    ).map(_.filter(_._2).map(_._1))
  }

  /**
    * Loads creatives for multiple campaigns.
    */
  private def loadCreativesForCampaigns(campaigns: List[Campaign]): Future[Map[String, List[Creative]]] = {
    Future.sequence(
      campaigns.map { campaign =>
        creativeStore.findByCampaignId(campaign.id).map(creatives => campaign.id -> creatives)
      }
    ).map(_.toMap)
  }

  /**
    * Builds a playlist from eligible campaigns and creatives.
    *
    * Uses weighted random selection based on campaign priority.
    * Returns both the playlist and a list of selected creatives with their campaign IDs.
    */
  private def buildPlaylist(
    eligibleCampaigns: List[Campaign],
    campaignCreatives: Map[String, List[Creative]],
    allCreatives: List[Creative],
    targetDurationSeconds: Int,
    request: DeliveryRequest
  ): Future[Option[(PlaylistResponse, List[(Creative, String)])]] = {
    if (allCreatives.isEmpty) {
      return Future.successful(None)
    }

    // Filter creatives that have duration specified
    val creativesWithDuration = allCreatives.filter(_.durationSeconds > 0)
    if (creativesWithDuration.isEmpty) {
      return Future.successful(None)
    }

    // Build weighted pool based on campaign priority
    val weightedPool = buildWeightedPool(eligibleCampaigns, campaignCreatives)

    var currentDuration = 0
    val items = scala.collection.mutable.ListBuffer[PlaylistItem]()
    val selectedCreativesWithCampaigns = scala.collection.mutable.ListBuffer[(Creative, String)]()
    var position = 0

    // Limit iterations to prevent infinite loops
    var iterations = 0
    val maxIterations = (targetDurationSeconds / 5) + 10

    while (currentDuration < targetDurationSeconds && iterations < maxIterations) {
      // Select from weighted pool
      val selectedCreative = selectWeightedCreative(weightedPool)

      selectedCreative.foreach { case (creative, campaignId) =>
        val item = PlaylistItem(
          adId = creative.id, // Using adId field for backward compatibility
          creativeUrl = creative.creativeUrl,
          targetUrl = creative.targetUrl,
          durationSeconds = creative.durationSeconds,
          impressionTrackingUrl = Some(
            s"$baseUrl/api/v1/events/impression?adId=${creative.id}&campaignId=$campaignId&requestId=${request.requestId}&position=$position"
          ),
          position = position
        )

        items.append(item)
        selectedCreativesWithCampaigns.append((creative, campaignId))
        currentDuration += creative.durationSeconds
        position += 1
      }

      iterations += 1
    }

    if (items.nonEmpty) {
      Future.successful(Some((
        PlaylistResponse(
          requestId = request.requestId,
          screenId = request.screenId,
          items = items.toList,
          validForSeconds = targetDurationSeconds,
          totalDurationSeconds = currentDuration
        ),
        selectedCreativesWithCampaigns.toList
      )))
    } else {
      Future.successful(None)
    }
  }

  /**
    * Builds a weighted pool of creatives based on campaign priority.
    * Higher priority campaigns have more entries in the pool.
    */
  private def buildWeightedPool(
    campaigns: List[Campaign],
    campaignCreatives: Map[String, List[Creative]]
  ): List[(Creative, String)] = {
    campaigns.flatMap { campaign =>
      val creatives = campaignCreatives.getOrElse(campaign.id, List.empty)
      val activeCreatives = creatives.filter(c => c.status == "active" && c.durationSeconds > 0)
      
      // Weight by priority (priority 1 = 1 entry, priority 5 = 5 entries, etc.)
      val weight = math.max(1, campaign.priority)
      activeCreatives.flatMap(creative => List.fill(weight)((creative, campaign.id)))
    }
  }

  /**
    * Selects a creative from the weighted pool using random selection.
    */
  private def selectWeightedCreative(pool: List[(Creative, String)]): Option[(Creative, String)] = {
    if (pool.isEmpty) None
    else Some(pool(Random.nextInt(pool.length)))
  }

}

