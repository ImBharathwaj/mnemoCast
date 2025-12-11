package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Ad performance metrics.
  */
final case class AdPerformance(
  adId: String,
  impressions: Long,
  startTime: Option[Instant] = None,
  endTime: Option[Instant] = None
)

object AdPerformance {
  implicit val adPerformanceCodec: Codec[AdPerformance] =
    deriveCodec[AdPerformance]
}

/**
  * Campaign performance summary (aggregated across all ads in a campaign).
  * For MVP, we treat each ad as its own "campaign" since we don't have campaign grouping yet.
  */
final case class CampaignPerformance(
  campaignId: String,  // For MVP, this is the adId
  totalImpressions: Long,
  ads: List[AdPerformance] = Nil
)

object CampaignPerformance {
  implicit val campaignPerformanceCodec: Codec[CampaignPerformance] =
    deriveCodec[CampaignPerformance]
}

/**
  * Dashboard summary with key metrics.
  */
final case class DashboardMetrics(
  totalAds: Int,
  activeAds: Int,
  totalImpressions: Long,
  topPerformingAds: List[AdPerformance] = Nil,
  recentActivity: List[DeliveryEvent] = Nil
)

object DashboardMetrics {
  implicit val dashboardMetricsCodec: Codec[DashboardMetrics] =
    deriveCodec[DashboardMetrics]
}

