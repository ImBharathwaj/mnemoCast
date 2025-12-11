package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.DeliveryEvent
import mnemocast.engine.infra.store.EventStore

/**
  * Routes for tracking events (impressions).
  *
  * GET /api/v1/events/impression?adId=...&requestId=... -> logs impression (for client-side tracking)
  */
class EventRoutes(
  eventStore: EventStore
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  val routes: Route =
    pathPrefix("api" / "v1" / "events") {
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

