package mnemocast.engine.api.routes

import java.time.{Instant, LocalDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.infra.services.AnalyticsService

/**
  * Analytics and reporting routes.
  *
  * GET /api/v1/analytics/ads/{adId} - Get performance metrics for a specific ad
  * GET /api/v1/analytics/campaigns - Get performance metrics for all campaigns
  * GET /api/v1/analytics/dashboard - Get dashboard summary metrics
  */
class AnalyticsRoutes(
  analyticsService: AnalyticsService
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  private val isoDateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

  val routes: Route =
    pathPrefix("api" / "v1" / "analytics") {
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
            "endTime".?
          ) { (startTimeOpt, endTimeOpt) =>
            val startTime = startTimeOpt.flatMap(parseInstant)
            val endTime = endTimeOpt.flatMap(parseInstant)

            onSuccess(analyticsService.getCampaignPerformance(startTime, endTime)) { campaigns =>
              complete(campaigns)
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
}

