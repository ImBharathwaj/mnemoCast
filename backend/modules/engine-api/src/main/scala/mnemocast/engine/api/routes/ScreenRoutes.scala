package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{RejectionHandler, Route}
import org.apache.pekko.http.scaladsl.model.HttpRequest

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
            extractRequest { httpRequest =>
              val timestamp = java.time.LocalDateTime.now()
              println(s"[$timestamp] [POST /api/v1/screens/register] Request received from ${httpRequest.uri}")
              println(s"[$timestamp] [POST /api/v1/screens/register] Content-Type: ${httpRequest.entity.contentType}")
              
              handleRejections(RejectionHandler.newBuilder()
                .handleAll[org.apache.pekko.http.scaladsl.server.Rejection] { rejections =>
                  val rejectTimestamp = java.time.LocalDateTime.now()
                  println(s"[$rejectTimestamp] [POST /api/v1/screens/register] REJECTIONS: ${rejections.size} rejection(s)")
                  rejections.foreach { rejection =>
                    println(s"[$rejectTimestamp] [POST /api/v1/screens/register] REJECTION: ${rejection.getClass.getSimpleName} - $rejection")
                  }
                  respondWithHeaders(
                    org.apache.pekko.http.scaladsl.model.headers.`Access-Control-Allow-Origin`.*,
                    org.apache.pekko.http.scaladsl.model.headers.`Access-Control-Allow-Methods`(org.apache.pekko.http.scaladsl.model.HttpMethods.GET, org.apache.pekko.http.scaladsl.model.HttpMethods.POST, org.apache.pekko.http.scaladsl.model.HttpMethods.PUT, org.apache.pekko.http.scaladsl.model.HttpMethods.DELETE, org.apache.pekko.http.scaladsl.model.HttpMethods.OPTIONS),
                    org.apache.pekko.http.scaladsl.model.headers.`Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
                  ) {
                    complete(StatusCodes.BadRequest, s"Request rejected: ${rejections.mkString(", ")}")
                  }
                }
                .result()) {
                entity(as[CreateScreenRequest]) { request =>
                println(s"[$timestamp] [POST /api/v1/screens/register] Request parsed successfully: id=${request.id}, name=${request.name}, location=${request.location}, tags=${request.tags}")
                try {
                  val now = Instant.now()
                  val screenId = request.id.getOrElse(UUID.randomUUID().toString)
                  println(s"[$timestamp] [POST /api/v1/screens/register] Generated screenId: $screenId")
                  val screen = Screen(
                    id = screenId,
                    name = request.name,
                    location = request.location,
                    tags = request.tags,
                    metadata = request.metadata,
                    isOnline = true,
                    lastSeen = Some(now),
                    createdAt = now,
                    updatedAt = now
                  )

                  println(s"[$timestamp] [POST /api/v1/screens/register] About to save screen to store...")
                  val futureScreen = screenStore.upsert(screen).map(_ => {
                    println(s"[$timestamp] [POST /api/v1/screens/register] Screen saved to store")
                    screen
                  })
                  onComplete(futureScreen) {
                    case scala.util.Success(savedScreen) =>
                      println(s"[$timestamp] [POST /api/v1/screens/register] Success: Screen registered with id=${savedScreen.id}")
                      complete(savedScreen)
                    case scala.util.Failure(ex) =>
                      println(s"[$timestamp] [POST /api/v1/screens/register] ERROR in store: ${ex.getMessage}")
                      ex.printStackTrace()
                      complete(StatusCodes.InternalServerError, s"Failed to register screen: ${ex.getMessage}")
                  }
                } catch {
                  case ex: Exception =>
                    println(s"[$timestamp] [POST /api/v1/screens/register] EXCEPTION: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.BadRequest, s"Invalid request: ${ex.getMessage}")
                }
                }
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

