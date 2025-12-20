package mnemocast.engine.api.routes

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.{ContentTypes, HttpEntity, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import io.circe.syntax._
import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.AuthMiddleware
import mnemocast.engine.infra.services.{AnalyticsService, AuthService}

/**
  * Analytics and reporting routes.
  *
  * GET /api/v1/analytics/ads/{adId} - Get performance metrics for a specific ad
  * GET /api/v1/analytics/campaigns - Get performance metrics for all campaigns
  * GET /api/v1/analytics/campaigns/compare - Compare multiple campaigns
  * GET /api/v1/analytics/campaigns/roi - Get ROI metrics for campaigns
  * GET /api/v1/analytics/screens - Get screen-level performance analytics
  * GET /api/v1/analytics/creatives - Get creative performance analytics
  * GET /api/v1/analytics/geographic - Get geographic performance analytics
  * GET /api/v1/analytics/timeseries - Get time-series data
  * GET /api/v1/analytics/export - Export analytics data (CSV/JSON)
  * GET /api/v1/analytics/dashboard - Get dashboard summary metrics
  */
class AnalyticsRoutes(
  analyticsService: AnalyticsService,
  authServiceOpt: Option[AuthService] = None
)(implicit ec: ExecutionContext)
    extends JsonSupport {
  
  // Helper to protect routes with authentication
  private def requireAuth(route: Route): Route = {
    authServiceOpt match {
      case Some(authService) => AuthMiddleware.authenticate(authService).apply { _ => route }
      case None => route // If auth is not available, allow access (backward compatibility)
    }
  }
  
  // Wrap all analytics routes with authentication
  private def protectedRoute(route: Route): Route = requireAuth(route)

  private val isoDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  val routes: Route =
    pathPrefix("api" / "v1" / "analytics") {
      // All analytics routes require authentication
      requireAuth {
        // Ad performance endpoint
        path("ads" / Segment) { adId =>
        get {
          parameters(
            "startTime".?,
            "endTime".?,
            "format".?(false) // format parameter for future CSV export
          ) { (startTimeOpt, endTimeOpt, format) =>
            val startTime = startTimeOpt.flatMap(parseInstant)
            val endTime = endTimeOpt.flatMap(parseInstant)

            onSuccess(analyticsService.getAdPerformance(adId, startTime, endTime)) {
              case Some(performance) =>
                complete(performance)
              case None =>
                complete(StatusCodes.NotFound, s"Ad not found: $adId")
            }
          }
        }
      } ~
      // Campaign performance endpoint
      path("campaigns") {
        get {
          parameters(
            "startTime".?,
            "endTime".?,
            "format".?
          ) { (startTimeOpt, endTimeOpt, formatOpt) =>
            val startTime = startTimeOpt.flatMap(parseInstant)
            val endTime = endTimeOpt.flatMap(parseInstant)

            onSuccess(analyticsService.getCampaignPerformance(startTime, endTime)) { campaigns =>
              formatOpt match {
                case Some("csv") =>
                  complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, toCsv(campaigns)))
                case _ =>
                  complete(campaigns)
              }
            }
          }
        }
      } ~
      // Campaign comparison endpoint
      path("campaigns" / "compare") {
        get {
          parameters(
            "campaignIds",
            "startTime".?,
            "endTime".?,
            "format".?
          ) { (campaignIdsStr, startTimeOpt, endTimeOpt, formatOpt) =>
            val campaignIds = campaignIdsStr.split(",").toList.map(_.trim)
            val startTime = startTimeOpt.flatMap(parseInstant)
            val endTime = endTimeOpt.flatMap(parseInstant)

            onSuccess(analyticsService.compareCampaigns(campaignIds, startTime, endTime)) { comparisons =>
              formatOpt match {
                case Some("csv") =>
                  complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, comparisonsToCsv(comparisons)))
                case _ =>
                  complete(comparisons)
              }
            }
          }
        }
      } ~
      // Time-series endpoint
      path("timeseries") {
        get {
          parameters(
            "adId".?,
            "startTime",
            "endTime",
            "intervalHours".as[Int].?(1),
            "format".?
          ) { (adIdOpt, startTimeStr, endTimeStr, intervalHours, formatOpt) =>
            (parseInstant(startTimeStr), parseInstant(endTimeStr)) match {
              case (Some(startTime), Some(endTime)) =>
                onSuccess(analyticsService.getTimeSeries(adIdOpt, startTime, endTime, intervalHours)) { timeSeries =>
                  formatOpt match {
                    case Some("csv") =>
                      complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, timeSeriesToCsv(timeSeries)))
                    case _ =>
                      complete(timeSeries)
                  }
                }
              case _ =>
                complete(StatusCodes.BadRequest, "Invalid startTime or endTime format")
            }
          }
        }
      } ~
      // Export endpoint (general export)
      path("export") {
        get {
          parameters(
            "type".?,
            "startTime".?,
            "endTime".?,
            "format".?
          ) { (exportTypeOpt, startTimeOpt, endTimeOpt, formatOpt) =>
            val format = formatOpt.getOrElse("json")
            exportTypeOpt match {
              case Some("campaigns") =>
                val startTime = startTimeOpt.flatMap(parseInstant)
                val endTime = endTimeOpt.flatMap(parseInstant)
                onSuccess(analyticsService.getCampaignPerformance(startTime, endTime)) { campaigns =>
                  if (format == "csv") {
                    complete(HttpEntity(ContentTypes.`text/csv(UTF-8)`, toCsv(campaigns)))
                  } else {
                    complete(HttpEntity(ContentTypes.`application/json`, campaigns.asJson.noSpaces))
                  }
                }
              case _ =>
                complete(StatusCodes.BadRequest, "Invalid export type. Supported: campaigns")
            }
          }
        }
      } ~
      // Dashboard metrics endpoint
      path("dashboard") {
        get {
          parameters("topN".as[Int].?(10)) { topN =>
            onSuccess(analyticsService.getDashboardMetrics(topN)) { metrics =>
              complete(metrics)
            }
          }
        }
      }
      }
    }

  /**
    * Parses an ISO 8601 datetime string to Instant.
    * Supports formats like: "2024-01-15T10:30:00Z" or "2024-01-15T10:30:00"
    */
  private def parseInstant(timeStr: String): Option[Instant] = {
    try {
      // Try ISO format first
      val instant = Instant.parse(timeStr)
      Some(instant)
    } catch {
      case _: Exception =>
        try {
          // Try parsing as local datetime and convert to UTC
          val localDateTime = LocalDateTime.parse(timeStr, isoDateTimeFormatter)
          Some(localDateTime.atZone(ZoneId.of("UTC")).toInstant)
        } catch {
          case _: Exception => None
        }
    }
  }

  /**
    * Converts campaign performance to CSV format.
    */
  private def toCsv(campaigns: List[mnemocast.engine.domain.model.CampaignPerformance]): String = {
    val header = "Campaign ID,Total Impressions,Ad Count\n"
    val rows = campaigns.map { c =>
      s"${c.campaignId},${c.totalImpressions},${c.ads.size}\n"
    }.mkString
    header + rows
  }

  /**
    * Converts campaign comparisons to CSV format.
    */
  private def comparisonsToCsv(comparisons: List[mnemocast.engine.domain.model.CampaignComparison]): String = {
    val header = "Campaign ID,Campaign Name,Impressions,Start Time,End Time\n"
    val rows = comparisons.map { c =>
      val startTime = c.startTime.map(_.toString).getOrElse("")
      val endTime = c.endTime.map(_.toString).getOrElse("")
      s"${c.campaignId},${c.campaignName},${c.impressions},$startTime,$endTime\n"
    }.mkString
    header + rows
  }

  /**
    * Converts time-series data to CSV format.
    */
  private def timeSeriesToCsv(timeSeries: mnemocast.engine.domain.model.TimeSeriesData): String = {
    val header = s"Timestamp,${timeSeries.metric}\n"
    val rows = timeSeries.points.map { p =>
      s"${p.timestamp.toString},${p.value}\n"
    }.mkString
    header + rows
  }

  /**
    * Converts ROI metrics to CSV format.
    */
  private def roiMetricsToCsv(roiMetrics: List[mnemocast.engine.domain.model.ROIMetrics]): String = {
    val header = "Campaign ID,Campaign Name,Impressions,Budget Allocated,Budget Spent,Budget Utilization %\n"
    val rows = roiMetrics.map { r =>
      val budgetAlloc = r.budgetAllocated.map(_.toString).getOrElse("")
      s"${r.campaignId},${r.campaignName},${r.impressions},$budgetAlloc,${r.budgetSpent},${r.budgetUtilization}\n"
    }.mkString
    header + rows
  }

  /**
    * Converts screen performance to CSV format.
    */
  private def screenPerfToCsv(screenPerf: List[mnemocast.engine.domain.model.ScreenPerformance]): String = {
    val header = "Screen ID,Screen Name,Impressions,City,Area,Classification\n"
    val rows = screenPerf.map { s =>
      val city = s.city.getOrElse("")
      val area = s.area.getOrElse("")
      s"${s.screenId},${s.screenName},${s.impressions},$city,$area,${s.classification}\n"
    }.mkString
    header + rows
  }

  /**
    * Converts creative performance to CSV format.
    */
  private def creativePerfToCsv(creativePerf: List[mnemocast.engine.domain.model.CreativePerformance]): String = {
    val header = "Creative ID,Creative Name,Campaign ID,Campaign Name,Impressions,Play Count\n"
    val rows = creativePerf.map { c =>
      s"${c.creativeId},${c.creativeName},${c.campaignId},${c.campaignName},${c.impressions},${c.playCount}\n"
    }.mkString
    header + rows
  }

  /**
    * Converts geographic performance to CSV format.
    */
  private def geographicPerfToCsv(geoPerf: List[mnemocast.engine.domain.model.GeographicPerformance]): String = {
    val header = "City,Area,Impressions,Screen Count\n"
    val rows = geoPerf.map { g =>
      val city = g.city.getOrElse("")
      val area = g.area.getOrElse("")
      s"$city,$area,${g.impressions},${g.screenCount}\n"
    }.mkString
    header + rows
  }
}

