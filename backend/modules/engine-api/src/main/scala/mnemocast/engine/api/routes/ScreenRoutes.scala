package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{RejectionHandler, Route}
import org.apache.pekko.http.scaladsl.model.HttpRequest

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.{AuthMiddleware, ScreenAuthMiddleware}
import mnemocast.engine.domain.model.{CreateScreenRequest, Screen}
import mnemocast.engine.infra.services.{AuthService, PasskeyService}
import mnemocast.engine.infra.store.ScreenStore

class ScreenRoutes(
  screenStore: ScreenStore,
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
        // ============================================
        // ADMIN ENDPOINTS (User Authentication - JWT)
        // ============================================
        path("register") {
          post {
            requireAuth {
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
                      val passkey = PasskeyService.generatePasskey()
                      println(s"[$timestamp] [POST /api/v1/screens/register] Generated screenId: $screenId")
                      println(s"[$timestamp] [POST /api/v1/screens/register] Generated passkey: $passkey")
                      val screen = Screen(
                        id = screenId,
                        name = request.name,
                        location = request.location,
                        tags = request.tags,
                        metadata = request.metadata,
                        classification = request.classification,
                        width = request.width,
                        height = request.height,
                        isAudible = request.isAudible,
                        isOnline = true,
                        lastSeen = Some(now),
                        passkey = passkey,
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
                          // Return registration response with passkey (only time passkey is returned)
                          val registrationResponse = ScreenRegistrationResponse(
                            screen = savedScreen,
                            passkey = passkey,
                            message = "Screen registered successfully. Save this passkey securely - it will not be shown again."
                          )
                          complete(registrationResponse)
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
          }
        },
        pathEnd {
          get {
            requireAuth {
              val futureScreens = screenStore.listAll()
              onComplete(futureScreens) {
                case scala.util.Success(screens) =>
                  complete(screens)
                case scala.util.Failure(ex) =>
                  complete(StatusCodes.InternalServerError, s"Error listing screens: ${ex.getMessage}")
              }
            }
          }
        },
        // ============================================
        // SCREEN ENDPOINTS (Screen Authentication - Passkey)
        // ============================================
        // More specific routes must come before general path(Segment)
        path(Segment / "heartbeat") { screenId =>
          concat(
            put {
              extractRequest { request =>
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [PUT /api/v1/screens/$screenId/heartbeat] Route matched - checking authentication")
                println(s"[$timestamp] [PUT /api/v1/screens/$screenId/heartbeat] Request headers: ${request.headers.map(h => s"${h.name()}=${h.value()}").mkString(", ")}")
                ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
                  // Verify the screenId in path matches authenticated screen
                  if (screen.id != screenId) {
                    complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
                  } else {
                    println(s"[$timestamp] [PUT /api/v1/screens/$screenId/heartbeat] Request received from authenticated screen: ${screen.name}")
                    
                    // Update last seen timestamp
                    val futureUpdate = screenStore.updateLastSeen(screenId).flatMap { _ =>
                      // Fetch updated screen data
                      screenStore.getById(screenId).map {
                        case Some(updatedScreen) =>
                          // Return screen without passkey for security
                          updatedScreen.copy(passkey = "***REDACTED***")
                        case None =>
                          // Fallback to authenticated screen if fetch fails
                          screen.copy(passkey = "***REDACTED***")
                      }
                    }
                    
                    onComplete(futureUpdate) {
                      case scala.util.Success(screenResponse) =>
                        println(s"[$timestamp] [PUT /api/v1/screens/$screenId/heartbeat] Success: Heartbeat recorded")
                        complete(StatusCodes.OK, screenResponse)
                      case scala.util.Failure(ex) =>
                        println(s"[$timestamp] [PUT /api/v1/screens/$screenId/heartbeat] Error: ${ex.getMessage}")
                        ex.printStackTrace()
                        complete(StatusCodes.InternalServerError, Map("error" -> s"Failed to record heartbeat: ${ex.getMessage}"))
                    }
                  }
                }
              }
            },
            options {
              // Handle CORS preflight for heartbeat endpoint
              complete(StatusCodes.OK)
            }
          )
        },
        // Screen status endpoint (requires screen authentication)
        path(Segment / "status") { screenId =>
          get {
            ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
              // Verify the screenId in path matches authenticated screen
              if (screen.id != screenId) {
                complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
              } else {
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [GET /api/v1/screens/$screenId/status] Request received from authenticated screen: ${screen.name}")
                
                // Use authenticated screen directly
                val now = java.time.Instant.now()
                val lastSeenAgoSeconds = screen.lastSeen match {
                  case Some(lastSeen) => java.time.Duration.between(lastSeen, now).getSeconds.toInt
                  case None => -1 // Never seen
                }
                
                val status = ScreenStatusResponse(
                  screenId = screen.id,
                  screenName = screen.name,
                  isOnline = screen.isOnline,
                  lastSeen = screen.lastSeen,
                  lastSeenAgoSeconds = if (lastSeenAgoSeconds >= 0) Some(lastSeenAgoSeconds) else None,
                  location = screen.location,
                  classification = screen.classification,
                  width = screen.width,
                  height = screen.height,
                  isAudible = screen.isAudible,
                  tags = screen.tags,
                  createdAt = screen.createdAt,
                  updatedAt = screen.updatedAt
                )
                println(s"[$timestamp] [GET /api/v1/screens/$screenId/status] Success")
                complete(status)
              }
            }
          }
        },
        // Screen get own details (screen authentication)
        path(Segment) { screenId =>
          concat(
            // Screen endpoint: GET own screen (screen authentication)
            get {
              ScreenAuthMiddleware.authenticate(screenStore).apply { screen =>
                // Verify the screenId in path matches authenticated screen
                if (screen.id != screenId) {
                  complete(StatusCodes.Forbidden, Map("error" -> "Screen ID mismatch"))
                } else {
                  val timestamp = java.time.LocalDateTime.now()
                  println(s"[$timestamp] [GET /api/v1/screens/$screenId] Request received from authenticated screen: ${screen.name}")
                  // Return screen without passkey for security
                  val screenResponse = screen.copy(passkey = "***REDACTED***")
                  println(s"[$timestamp] [GET /api/v1/screens/$screenId] Success: Screen found")
                  complete(screenResponse)
                }
              }
            },
            // Admin endpoints: PUT and DELETE (user authentication)
            put {
              requireAuth {
                extractRequest { httpRequest =>
                  val timestamp = java.time.LocalDateTime.now()
                  println(s"[$timestamp] [PUT /api/v1/screens/$screenId] Request received")
                  entity(as[CreateScreenRequest]) { request =>
                    println(s"[$timestamp] [PUT /api/v1/screens/$screenId] Request body: name=${request.name}")
                    
                    val futureScreen = screenStore.getById(screenId).flatMap {
                      case Some(existingScreen) =>
                        val now = Instant.now()
                        val updatedScreen = existingScreen.copy(
                          name = request.name,
                          location = request.location,
                          tags = request.tags,
                          metadata = request.metadata,
                          classification = request.classification,
                          width = request.width,
                          height = request.height,
                          isAudible = request.isAudible,
                          updatedAt = now
                        )
                        screenStore.upsert(updatedScreen).map(_ => updatedScreen)
                      case None =>
                        Future.failed(new IllegalArgumentException(s"Screen not found: $screenId"))
                    }
                    
                    onComplete(futureScreen) {
                      case scala.util.Success(screen) =>
                        println(s"[$timestamp] [PUT /api/v1/screens/$screenId] Success: Screen updated")
                        complete(screen)
                      case scala.util.Failure(ex: IllegalArgumentException) =>
                        println(s"[$timestamp] [PUT /api/v1/screens/$screenId] Not found: ${ex.getMessage}")
                        complete(StatusCodes.NotFound, ex.getMessage)
                      case scala.util.Failure(ex) =>
                        println(s"[$timestamp] [PUT /api/v1/screens/$screenId] ERROR: ${ex.getMessage}")
                        ex.printStackTrace()
                        complete(StatusCodes.InternalServerError, s"Failed to update screen: ${ex.getMessage}")
                    }
                  }
                }
              }
            },
            delete {
              requireAuth {
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [DELETE /api/v1/screens/$screenId] Request received")
                val futureDelete = screenStore.delete(screenId)
                onComplete(futureDelete) {
                  case scala.util.Success(_) =>
                    println(s"[$timestamp] [DELETE /api/v1/screens/$screenId] Success: Screen deleted")
                    complete(StatusCodes.OK, "Screen deleted successfully")
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [DELETE /api/v1/screens/$screenId] ERROR: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, s"Failed to delete screen: ${ex.getMessage}")
                }
              }
            }
          )
        }
      )
    }
  
  /**
    * Screen status response model.
    */
  case class ScreenStatusResponse(
    screenId: String,
    screenName: String,
    isOnline: Boolean,
    lastSeen: Option[java.time.Instant],
    lastSeenAgoSeconds: Option[Int],
    location: mnemocast.engine.domain.model.ScreenLocation,
    classification: Int,
    width: Option[Int],
    height: Option[Int],
    isAudible: Boolean,
    tags: List[String],
    createdAt: java.time.Instant,
    updatedAt: java.time.Instant
  )
  
  implicit val screenStatusResponseCodec: io.circe.Codec[ScreenStatusResponse] =
    io.circe.generic.semiauto.deriveCodec[ScreenStatusResponse]
  
  /**
    * Screen registration response model.
    * Includes the passkey only during registration (never shown again).
    */
  case class ScreenRegistrationResponse(
    screen: Screen,
    passkey: String,
    message: String
  )
  
  implicit val screenRegistrationResponseCodec: io.circe.Codec[ScreenRegistrationResponse] =
    io.circe.generic.semiauto.deriveCodec[ScreenRegistrationResponse]
}

