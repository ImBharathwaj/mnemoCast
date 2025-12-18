package mnemocast.engine.infra.services

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.{Ad, AdPerformance, CampaignComparison, CampaignPerformance, CreativePerformance, DashboardMetrics, DeliveryEvent, GeographicPerformance, ROIMetrics, ScreenPerformance, TimeSeriesData, TimeSeriesPoint}
import mnemocast.engine.infra.store.{AdStore, CampaignStore, CreativeStore, EventStore, ScreenStore}

/**
  * Analytics service for calculating performance metrics.
  */
class AnalyticsService(
  adStore: AdStore,
  eventStore: EventStore,
  campaignStore: Option[CampaignStore] = None,
  creativeStore: Option[CreativeStore] = None,
  screenStore: Option[ScreenStore] = None
)(implicit ec: ExecutionContext) {

  /**
    * Calculates performance metrics for a specific ad.
    *
    * @param adId      The ad ID
    * @param startTime Optional start time for filtering (defaults to all time)
    * @param endTime   Optional end time for filtering (defaults to now)
    * @return AdPerformance with impressions
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

      Some(AdPerformance(
        adId = adId,
        impressions = impressions,
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
                ads = List(perf)
              )
            case None =>
              CampaignPerformance(
                campaignId = ad.id,
                totalImpressions = 0,
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
      
      // Get top performing ads (by impressions)
      topPerforming = adPerformances
        .filter(_.impressions > 0) // Only ads with impressions
        .sortBy(perf => -perf.impressions) // Sort by impressions desc
        .take(topN)
      
      // Get recent events (last 50 across all ads)
      recentEvents <- getRecentEvents(50)
    } yield {
      DashboardMetrics(
        totalAds = allAds.size,
        activeAds = activeAds.size,
        totalImpressions = totalImpressions,
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

  /**
    * Compares performance across multiple campaigns.
    */
  def compareCampaigns(
    campaignIds: List[String],
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[List[CampaignComparison]] = {
    campaignStore match {
      case Some(cs) =>
        val end = endTime.getOrElse(Instant.now())
        cs.listAll().flatMap { allCampaigns =>
          val campaignsToCompare = allCampaigns.filter(c => campaignIds.contains(c.id))
          
          Future.sequence(
            campaignsToCompare.map { campaign =>
              // For now, we'll use ad-based events. In future, we can track by campaignId
              // For MVP, we'll aggregate by getting all creatives for the campaign
              creativeStore match {
                case Some(creStore) =>
                  creStore.listAll().flatMap { allCreatives =>
                    val campaignCreatives = allCreatives.filter(_.campaignId == campaign.id)
                    // Get events for creatives (using creativeId as adId for now)
                    Future.sequence(
                      campaignCreatives.map(creative => eventStore.findByAdId(creative.id, Int.MaxValue))
                    ).map { eventLists =>
                      val allEvents = eventLists.flatten
                      val filteredEvents = startTime match {
                        case Some(start) =>
                          allEvents.filter(e => !e.occurredAt.isBefore(start) && !e.occurredAt.isAfter(end))
                        case None =>
                          allEvents.filter(!_.occurredAt.isAfter(end))
                      }
                      val impressions = filteredEvents.count(_.eventType == "impression")
                      
                      CampaignComparison(
                        campaignId = campaign.id,
                        campaignName = campaign.name,
                        impressions = impressions,
                        startTime = startTime,
                        endTime = Some(end)
                      )
                    }
                  }
                case None =>
                  // Fallback: return zero impressions if no creative store
                  Future.successful(
                    CampaignComparison(
                      campaignId = campaign.id,
                      campaignName = campaign.name,
                      impressions = 0L,
                      startTime = startTime,
                      endTime = Some(end)
                    )
                  )
              }
            }
          )
        }
      case None =>
        // Fallback: return empty list if no campaign store
        Future.successful(Nil)
    }
  }

  /**
    * Gets time-series data for impressions over time.
    */
  def getTimeSeries(
    adId: Option[String] = None,
    startTime: Instant,
    endTime: Instant,
    intervalHours: Int = 1
  ): Future[TimeSeriesData] = {
    val adsFuture = adId match {
      case Some(id) => adStore.getById(id).map(_.toList)
      case None => adStore.listAll()
    }
    
    adsFuture.flatMap { ads =>
      Future.sequence(
        ads.map(ad => eventStore.findByAdId(ad.id, Int.MaxValue))
      ).map { eventLists =>
        val allEvents = eventLists.flatten
        val filteredEvents = allEvents.filter(e => 
          !e.occurredAt.isBefore(startTime) && !e.occurredAt.isAfter(endTime) && e.eventType == "impression"
        )
        
        // Group by time interval
        val intervalMillis = intervalHours * 3600 * 1000L
        val grouped = filteredEvents.groupBy { event =>
          val timestamp = event.occurredAt.toEpochMilli
          val intervalStart = (timestamp / intervalMillis) * intervalMillis
          Instant.ofEpochMilli(intervalStart)
        }
        
        val points = grouped.map { case (timestamp, events) =>
          TimeSeriesPoint(timestamp, events.size.toLong)
        }.toList.sortBy(_.timestamp)
        
        TimeSeriesData(
          metric = "impressions",
          points = points
        )
      }
    }
  }

  /**
    * Gets ROI metrics for campaigns (if budget data exists).
    */
  def getROIMetrics(
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[List[ROIMetrics]] = {
    campaignStore match {
      case Some(cs) =>
        val end = endTime.getOrElse(Instant.now())
        cs.listAll().flatMap { campaigns =>
          Future.sequence(
            campaigns.map { campaign =>
              creativeStore match {
                case Some(creStore) =>
                  creStore.listAll().flatMap { allCreatives =>
                    val campaignCreatives = allCreatives.filter(_.campaignId == campaign.id)
                    Future.sequence(
                      campaignCreatives.map(creative => eventStore.findByAdId(creative.id, Int.MaxValue))
                    ).map { eventLists =>
                      val allEvents = eventLists.flatten
                      val filteredEvents = startTime match {
                        case Some(start) =>
                          allEvents.filter(e => !e.occurredAt.isBefore(start) && !e.occurredAt.isAfter(end))
                        case None =>
                          allEvents.filter(!_.occurredAt.isAfter(end))
                      }
                      val impressions = filteredEvents.count(_.eventType == "impression")
                      val budgetSpent = impressions.toLong
                      val budgetAllocated = campaign.totalBudget
                      val budgetUtilization = budgetAllocated match {
                        case Some(budget) if budget > 0 => (budgetSpent.toDouble / budget) * 100.0
                        case _ => 0.0
                      }
                      
                      ROIMetrics(
                        campaignId = campaign.id,
                        campaignName = campaign.name,
                        impressions = impressions,
                        budgetAllocated = budgetAllocated,
                        budgetSpent = budgetSpent,
                        budgetUtilization = budgetUtilization
                      )
                    }
                  }
                case None =>
                  Future.successful(
                    ROIMetrics(
                      campaignId = campaign.id,
                      campaignName = campaign.name,
                      impressions = 0L,
                      budgetAllocated = campaign.totalBudget
                    )
                  )
              }
            }
          )
        }
      case None =>
        Future.successful(Nil)
    }
  }

  /**
    * Gets screen-level performance analytics.
    */
  def getScreenPerformance(
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[List[ScreenPerformance]] = {
    screenStore match {
      case Some(ss) =>
        val end = endTime.getOrElse(Instant.now())
        ss.listAll().flatMap { screens =>
          // Get all events and filter by screenId from metadata
          adStore.listAll().flatMap { ads =>
            Future.sequence(
              ads.map(ad => eventStore.findByAdId(ad.id, Int.MaxValue))
            ).map { eventLists =>
              val allEvents = eventLists.flatten
              val filteredEvents = startTime match {
                case Some(start) =>
                  allEvents.filter(e => !e.occurredAt.isBefore(start) && !e.occurredAt.isAfter(end))
                case None =>
                  allEvents.filter(!_.occurredAt.isAfter(end))
              }
              
              // Group events by screenId from metadata
              val eventsByScreen = filteredEvents
                .filter(_.eventType == "impression")
                .groupBy(_.metadata.get("screenId"))
                .filterKeys(_.isDefined)
              
              screens.map { screen =>
                val screenEvents = eventsByScreen.get(Some(screen.id)).getOrElse(Nil)
                ScreenPerformance(
                  screenId = screen.id,
                  screenName = screen.name,
                  impressions = screenEvents.size.toLong,
                  city = screen.location.city,
                  area = screen.location.area,
                  classification = screen.classification
                )
              }.filter(_.impressions > 0).sortBy(_.impressions)(Ordering[Long].reverse)
            }
          }
        }
      case None =>
        Future.successful(Nil)
    }
  }

  /**
    * Gets creative performance analytics.
    */
  def getCreativePerformance(
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[List[CreativePerformance]] = {
    creativeStore match {
      case Some(creStore) =>
        val end = endTime.getOrElse(Instant.now())
        creStore.listAll().flatMap { creatives =>
          campaignStore match {
            case Some(cs) =>
              cs.listAll().flatMap { campaigns =>
                val campaignMap = campaigns.map(c => c.id -> c).toMap
                
                Future.sequence(
                  creatives.map { creative =>
                    eventStore.findByAdId(creative.id, Int.MaxValue).map { allEvents =>
                      val filteredEvents = startTime match {
                        case Some(start) =>
                          allEvents.filter(e => !e.occurredAt.isBefore(start) && !e.occurredAt.isAfter(end))
                        case None =>
                          allEvents.filter(!_.occurredAt.isAfter(end))
                      }
                      val impressions = filteredEvents.count(_.eventType == "impression")
                      val campaign = campaignMap.get(creative.campaignId)
                      
                      CreativePerformance(
                        creativeId = creative.id,
                        creativeName = creative.name,
                        campaignId = creative.campaignId,
                        campaignName = campaign.map(_.name).getOrElse("Unknown"),
                        impressions = impressions,
                        playCount = impressions // For now, playCount = impressions
                      )
                    }
                  }
                ).map(_.filter(_.impressions > 0).sortBy(_.impressions)(Ordering[Long].reverse))
              }
            case None =>
              Future.successful(Nil)
          }
        }
      case None =>
        Future.successful(Nil)
    }
  }

  /**
    * Gets geographic performance analytics (by city/area).
    */
  def getGeographicPerformance(
    startTime: Option[Instant] = None,
    endTime: Option[Instant] = None
  ): Future[List[GeographicPerformance]] = {
    screenStore match {
      case Some(ss) =>
        val end = endTime.getOrElse(Instant.now())
        ss.listAll().flatMap { screens =>
          adStore.listAll().flatMap { ads =>
            Future.sequence(
              ads.map(ad => eventStore.findByAdId(ad.id, Int.MaxValue))
            ).map { eventLists =>
              val allEvents = eventLists.flatten
              val filteredEvents = startTime match {
                case Some(start) =>
                  allEvents.filter(e => !e.occurredAt.isBefore(start) && !e.occurredAt.isAfter(end))
                case None =>
                  allEvents.filter(!_.occurredAt.isAfter(end))
              }
              
              // Group by city/area from screen metadata
              val eventsByLocation = filteredEvents
                .filter(_.eventType == "impression")
                .groupBy { event =>
                  val screenId = event.metadata.get("screenId")
                  val screen = screens.find(_.id == screenId.getOrElse(""))
                  (screen.flatMap(_.location.city), screen.flatMap(_.location.area))
                }
              
              eventsByLocation.map { case ((city, area), events) =>
                val screenIds = events.flatMap(e => e.metadata.get("screenId")).distinct
                GeographicPerformance(
                  city = city,
                  area = area,
                  impressions = events.size.toLong,
                  screenCount = screenIds.size
                )
              }.toList.filter(_.impressions > 0).sortBy(_.impressions)(Ordering[Long].reverse)
            }
          }
        }
      case None =>
        Future.successful(Nil)
    }
  }
}

