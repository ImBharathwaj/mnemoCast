package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.{StatusCodes, Uri}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.DeliveryEvent
import mnemocast.engine.infra.store.{AdStore, EventStore}

/**
  * Routes for tracking events (impressions, clicks).
  *
  * GET /api/v1/events/click?adId=...&requestId=... -> redirects to target URL after logging click
  * GET /api/v1/events/impression?adId=...&requestId=... -> logs impression (for client-side tracking)
  */
class EventRoutes(
  adStore: AdStore,
  eventStore: EventStore
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  val routes: Route =
    pathPrefix("api" / "v1" / "events") {
      // Click tracking endpoint - redirects to target URL after logging
      path("click") {
        get {
          parameters("adId", "requestId".?) { (adId, requestIdOpt) =>
            extractClientIP { ip =>
              // Log the click event
              val clickEvent = DeliveryEvent(
                eventId = UUID.randomUUID().toString,
                requestId = requestIdOpt.getOrElse(UUID.randomUUID().toString),
                adId = adId,
                eventType = "click",
                occurredAt = Instant.now(),
                metadata = Map(
                  "ip" -> ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())
                )
              )

              // Get the ad to find the target URL
              onSuccess(eventStore.append(clickEvent).flatMap(_ => adStore.getById(adId))) {
                case Some(ad) =>
                  ad.targetUrl match {
                    case Some(targetUrl) =>
                      // Redirect to the target URL
                      redirect(Uri(targetUrl), StatusCodes.Found)
                    case None =>
                      // No target URL, return 204 No Content
                      complete(StatusCodes.NoContent)
                  }
                case None =>
                  // Ad not found
                  complete(StatusCodes.NotFound, "Ad not found")
              }
            }
          }
        }
      } ~
      // Impression tracking endpoint (for client-side tracking)
      path("impression") {
        get {
          parameters("adId", "requestId".?) { (adId, requestIdOpt) =>
            extractClientIP { ip =>
              val impressionEvent = DeliveryEvent(
                eventId = UUID.randomUUID().toString,
                requestId = requestIdOpt.getOrElse(UUID.randomUUID().toString),
                adId = adId,
                eventType = "impression",
                occurredAt = Instant.now(),
                metadata = Map(
                  "ip" -> ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())
                )
              )

              // Log event and return 204 No Content
              val future = eventStore.append(impressionEvent).map(_ => StatusCodes.NoContent)
              complete(future)
            }
          }
        }
      }
    }
}

