package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.{CreateScreenRequest, Screen}
import mnemocast.engine.infra.store.ScreenStore

class ScreenRoutes(
  screenStore: ScreenStore
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  /**
    * Screen management routes.
    *
    * POST /api/v1/screens/register - Register a new screen
    * GET /api/v1/screens/{screenId} - Get screen by ID
    * GET /api/v1/screens - List all screens
    * PUT /api/v1/screens/{screenId}/heartbeat - Update last seen timestamp
    */
  val routes: Route =
    pathPrefix("api" / "v1" / "screens") {
      concat(
        path("register") {
          post {
            entity(as[CreateScreenRequest]) { request =>
              val now = Instant.now()
              val screen = Screen(
                id = request.id,
                name = request.name,
                location = request.location,
                tags = request.tags,
                metadata = request.metadata,
                isOnline = true,
                lastSeen = Some(now),
                createdAt = now,
                updatedAt = now
              )

              val futureScreen = screenStore.upsert(screen).map(_ => screen)
              onComplete(futureScreen) {
                case scala.util.Success(savedScreen) =>
                  complete(savedScreen)
                case scala.util.Failure(ex) =>
                  complete(StatusCodes.InternalServerError, s"Failed to register screen: ${ex.getMessage}")
              }
            }
          }
        },
        path(Segment / "heartbeat") { screenId =>
          put {
            val futureUpdate = screenStore.updateLastSeen(screenId)
            onComplete(futureUpdate) {
              case scala.util.Success(_) =>
                complete(StatusCodes.OK, "Heartbeat recorded")
              case scala.util.Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Failed to record heartbeat: ${ex.getMessage}")
            }
          }
        },
        path(Segment) { screenId =>
          get {
            val futureScreen = screenStore.getById(screenId)
            onComplete(futureScreen) {
              case scala.util.Success(Some(screen)) =>
                complete(screen)
              case scala.util.Success(None) =>
                complete(StatusCodes.NotFound, s"Screen not found: $screenId")
              case scala.util.Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Error retrieving screen: ${ex.getMessage}")
            }
          }
        },
        pathEnd {
          get {
            val futureScreens = screenStore.listAll()
            onComplete(futureScreens) {
              case scala.util.Success(screens) =>
                complete(screens)
              case scala.util.Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Error listing screens: ${ex.getMessage}")
            }
          }
        }
      )
    }
}

