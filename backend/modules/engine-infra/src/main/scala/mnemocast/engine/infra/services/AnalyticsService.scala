package mnemocast.engine.infra.services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.{Ad, AdPerformance, CampaignPerformance, DashboardMetrics, DeliveryEvent}
import mnemocast.engine.infra.store.{AdStore, EventStore}

/**
  * Analytics service for calculating performance metrics.
  */
class AnalyticsService(
  adStore: AdStore,
  eventStore: EventStore
)(implicit ec: ExecutionContext) {

  /**
    * Calculates performance metrics for a specific ad.
    *
    * @param adId      The ad ID
    * @param startTime Optional start time for filtering (defaults to all time)
    * @param endTime   Optional end time for filtering (defaults to now)
    * @return AdPerformance with impressions, clicks, and CTR
    */
  def getAdPerformance(
    adId: String,
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[Option[AdPerformance]] = {
    val end = endTime.getOrElse(Instant.now())
    
    // Get all events for this ad
    eventStore.findByAdId(adId, Int.MaxValue).map { allEvents =>
      // Filter by time range if provided
      val filteredEvents = startTime match {
        case Some(start) =>
          allEvents.filter(e => !e.occurredAt.isBefore(start) && !e.occurredAt.isAfter(end))
        case None =>
          allEvents.filter(!_.occurredAt.isAfter(end))
      }

      val impressions = filteredEvents.count(_.eventType == "impression")
      val clicks = filteredEvents.count(_.eventType == "click")
      val ctr = if (impressions > 0) (clicks.toDouble / impressions.toDouble) * 100.0 else 0.0

      Some(AdPerformance(
        adId = adId,
        impressions = impressions,
        clicks = clicks,
        ctr = ctr,
        startTime = startTime,
        endTime = Some(end)
      ))
    }
  }

  /**
    * Calculates performance metrics for all ads (campaign performance).
    * For MVP, each ad is treated as its own campaign.
    *
    * @param startTime Optional start time for filtering
    * @param endTime   Optional end time for filtering
    * @return List of CampaignPerformance (one per ad)
    */
  def getCampaignPerformance(
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[List[CampaignPerformance]] = {
    adStore.listAll().flatMap { ads =>
      Future.sequence(
        ads.map { ad =>
          getAdPerformance(ad.id, startTime, endTime).map {
            case Some(perf) =>
              CampaignPerformance(
                campaignId = ad.id,
                totalImpressions = perf.impressions,
                totalClicks = perf.clicks,
                overallCTR = perf.ctr,
                ads = List(perf)
              )
            case None =>
              CampaignPerformance(
                campaignId = ad.id,
                totalImpressions = 0,
                totalClicks = 0,
                overallCTR = 0.0,
                ads = Nil
              )
          }
        }
      )
    }
  }

  /**
    * Gets dashboard metrics with key summary statistics.
    *
    * @param topN Number of top performing ads to include (default: 10)
    * @return DashboardMetrics with summary
    */
  def getDashboardMetrics(topN: Int = 10): Future[DashboardMetrics] = {
    for {
      allAds <- adStore.listAll()
      activeAds <- adStore.listActive()
      
      // Get performance for all ads
      adPerformances <- Future.sequence(
        allAds.map(ad => getAdPerformance(ad.id))
      ).map(_.flatten)
      
      // Calculate totals
      totalImpressions = adPerformances.map(_.impressions).sum
      totalClicks = adPerformances.map(_.clicks).sum
      overallCTR = if (totalImpressions > 0) (totalClicks.toDouble / totalImpressions.toDouble) * 100.0 else 0.0
      
      // Get top performing ads (by CTR, then by impressions)
      topPerforming = adPerformances
        .filter(_.impressions > 0) // Only ads with impressions
        .sortBy(perf => (-perf.ctr, -perf.impressions)) // Sort by CTR desc, then impressions desc
        .take(topN)
      
      // Get recent events (last 50 across all ads)
      recentEvents <- getRecentEvents(50)
    } yield {
      DashboardMetrics(
        totalAds = allAds.size,
        activeAds = activeAds.size,
        totalImpressions = totalImpressions,
        totalClicks = totalClicks,
        overallCTR = overallCTR,
        topPerformingAds = topPerforming,
        recentActivity = recentEvents
      )
    }
  }

  /**
    * Gets recent events across all ads.
    */
  private def getRecentEvents(limit: Int): Future[List[DeliveryEvent]] = {
    adStore.listAll().flatMap { ads =>
      // Get recent events from all ads and merge
      Future.sequence(
        ads.map(ad => eventStore.findByAdId(ad.id, limit))
      ).map { eventLists =>
        eventLists
          .flatten
          .sortBy(_.occurredAt)(Ordering[Instant].reverse)
          .take(limit)
      }
    }
  }
}

