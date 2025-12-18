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

/**
  * Campaign comparison result.
  */
final case class CampaignComparison(
  campaignId: String,
  campaignName: String,
  impressions: Long,
  startTime: Option[Instant] = None,
  endTime: Option[Instant] = None
)

object CampaignComparison {
  implicit val campaignComparisonCodec: Codec[CampaignComparison] =
    deriveCodec[CampaignComparison]
}

/**
  * Time-series data point for analytics.
  */
final case class TimeSeriesPoint(
  timestamp: Instant,
  value: Long
)

object TimeSeriesPoint {
  implicit val timeSeriesPointCodec: Codec[TimeSeriesPoint] =
    deriveCodec[TimeSeriesPoint]
}

/**
  * Time-series analytics data.
  */
final case class TimeSeriesData(
  metric: String,  // e.g., "impressions"
  points: List[TimeSeriesPoint] = Nil
)

object TimeSeriesData {
  implicit val timeSeriesDataCodec: Codec[TimeSeriesData] =
    deriveCodec[TimeSeriesData]
}

/**
  * ROI metrics for campaigns.
  */
final case class ROIMetrics(
  campaignId: String,
  campaignName: String,
  impressions: Long,
  budgetAllocated: Option[Long] = None,  // Budget in plays/impressions
  budgetSpent: Long = 0L,               // Actual impressions delivered
  costPerImpression: Option[Double] = None,  // If budget cost exists
  impressionsPerDollar: Option[Double] = None, // If budget cost exists
  budgetUtilization: Double = 0.0        // Percentage of budget used
)

object ROIMetrics {
  implicit val roiMetricsCodec: Codec[ROIMetrics] =
    deriveCodec[ROIMetrics]
}

/**
  * Screen-level performance metrics.
  */
final case class ScreenPerformance(
  screenId: String,
  screenName: String,
  impressions: Long,
  city: Option[String] = None,
  area: Option[String] = None,
  classification: Int = 1
)

object ScreenPerformance {
  implicit val screenPerformanceCodec: Codec[ScreenPerformance] =
    deriveCodec[ScreenPerformance]
}

/**
  * Creative performance metrics.
  */
final case class CreativePerformance(
  creativeId: String,
  creativeName: String,
  campaignId: String,
  campaignName: String,
  impressions: Long,
  playCount: Long = 0L
)

object CreativePerformance {
  implicit val creativePerformanceCodec: Codec[CreativePerformance] =
    deriveCodec[CreativePerformance]
}

/**
  * Geographic performance metrics.
  */
final case class GeographicPerformance(
  city: Option[String],
  area: Option[String],
  impressions: Long,
  screenCount: Int = 0
)

object GeographicPerformance {
  implicit val geographicPerformanceCodec: Codec[GeographicPerformance] =
    deriveCodec[GeographicPerformance]
}

