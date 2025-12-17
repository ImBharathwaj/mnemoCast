package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.{DeliveryRequest, Screen}
import mnemocast.engine.infra.services.PlaylistService
import mnemocast.engine.infra.store.ScreenStore

class PlaylistRoutes(
  playlistService: PlaylistService,
  screenStore: ScreenStore
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  /**
    * Playlist generation routes for OOH screens.
    *
    * GET /api/v1/screens/{screenId}/playlist?durationMinutes=X - Generate playlist for a screen
    */
  val routes: Route =
    pathPrefix("api" / "v1" / "screens") {
      path(Segment / "playlist") { screenId =>
        get {
          parameters("durationMinutes".as[Int].?(3)) { durationMinutes =>
            extractClientIP { ip =>
              // Fetch screen to get location information
              val futurePlaylist = screenStore.getById(screenId).flatMap {
                case Some(screen) =>
                  // Build delivery request with screen context
                  val request = DeliveryRequest(
                    requestId = UUID.randomUUID().toString,
                    deviceId = Some(screenId),
                    userId = None,
                    appId = None,
                    ip = Some(ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())),
                    country = screen.location.country,
                    platform = None,
                    screenId = Some(screenId),
                    city = screen.location.city,
                    area = screen.location.area,
                    venueType = screen.location.venueType,
                    timezone = screen.location.timezone,
                    timestamp = Instant.now()
                  )

                  playlistService.generatePlaylist(request, durationMinutes)

                case None =>
                  Future.successful(None)
              }

              onComplete(futurePlaylist) {
                case scala.util.Success(Some(playlist)) =>
                  complete(playlist)
                case scala.util.Success(None) =>
                  complete(StatusCodes.NoContent, "No playlist available")
                case scala.util.Failure(ex) =>
                  println(s"Error generating playlist: ${ex.getMessage}")
                  ex.printStackTrace()
                  complete(StatusCodes.InternalServerError, s"Internal server error: ${ex.getMessage}")
              }
            }
          }
        }
      }
    }
}

