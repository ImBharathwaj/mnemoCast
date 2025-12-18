package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.AuthMiddleware
import mnemocast.engine.domain.model.{CreateCreativeRequest, Creative}
import mnemocast.engine.infra.services.AuthService
import mnemocast.engine.infra.store.{CampaignStore, CreativeStore}

class CreativeRoutes(
  creativeStore: CreativeStore,
  campaignStore: CampaignStore,
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
    * Creative management routes.
    *
    * POST /api/v1/campaigns/{campaignId}/creatives - Create a new creative
    * GET /api/v1/campaigns/{campaignId}/creatives - List creatives for a campaign
    * GET /api/v1/creatives/{creativeId} - Get creative by ID
    */
  val routes: Route =
    concat(
      pathPrefix("api" / "v1" / "campaigns" / Segment / "creatives") { campaignId =>
        concat(
          pathEnd {
            post {
              requireAuth {
                extractRequest { httpRequest =>
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Request received")
                
                entity(as[CreateCreativeRequest]) { request =>
                  println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Request body: name=${request.name}, creativeType=${request.creativeType}, creativeUrl=${request.creativeUrl}")
                  
                  // Verify campaign exists
                  val futureResult = campaignStore.getById(campaignId).flatMap {
                    case Some(campaign) =>
                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Campaign found: ${campaign.name}")
                      val now = Instant.now()
                      val creativeId = request.id.getOrElse(UUID.randomUUID().toString)
                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Generated creative ID: $creativeId")
                      
                      val creative = Creative(
                        id = creativeId,
                        campaignId = campaignId, // Use path parameter, ignore request.campaignId
                        name = request.name,
                        creativeType = request.creativeType,
                        creativeUrl = request.creativeUrl,
                        targetUrl = request.targetUrl,
                        durationSeconds = request.durationSeconds,
                        status = request.status,
                        shareOfVoice = request.shareOfVoice,
                        frequencyCapPerScreen = request.frequencyCapPerScreen,
                        metadata = request.metadata,
                        createdAt = now,
                        updatedAt = now
                      )

                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] About to upsert creative to store...")
                      creativeStore.upsert(creative).map { _ =>
                        println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Creative upserted successfully")
                        creative
                      }.recoverWith { case ex =>
                        println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] ERROR during upsert: ${ex.getClass.getName}: ${ex.getMessage}")
                        ex.printStackTrace()
                        Future.failed(ex)
                      }

                    case None =>
                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Campaign not found: $campaignId")
                      Future.failed(new IllegalArgumentException(s"Campaign not found: $campaignId"))
                  }

                  onComplete(futureResult) {
                    case scala.util.Success(creative) =>
                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Success: Creative created with id=${creative.id}")
                      complete(creative)
                    case scala.util.Failure(ex: IllegalArgumentException) =>
                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] Not found: ${ex.getMessage}")
                      complete(StatusCodes.NotFound, ex.getMessage)
                    case scala.util.Failure(ex) =>
                      println(s"[$timestamp] [POST /api/v1/campaigns/$campaignId/creatives] ERROR: ${ex.getClass.getName}: ${ex.getMessage}")
                      ex.printStackTrace()
                      complete(StatusCodes.InternalServerError, s"Failed to create creative: ${ex.getMessage}")
                  }
                }
              }
              }
            } ~
            get {
              requireAuth {
                extractRequest { httpRequest =>
                  val timestamp = java.time.LocalDateTime.now()
                  println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId/creatives] Request received")
                  val futureCreatives = creativeStore.findByCampaignId(campaignId)
                  onComplete(futureCreatives) {
                    case scala.util.Success(creatives) =>
                      println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId/creatives] Success: Found ${creatives.length} creatives")
                      complete(creatives)
                    case scala.util.Failure(ex) =>
                      println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId/creatives] ERROR: ${ex.getMessage}")
                      ex.printStackTrace()
                      complete(StatusCodes.InternalServerError, s"Error listing creatives: ${ex.getMessage}")
                  }
                }
              }
            }
          }
        )
      },
      pathPrefix("api" / "v1" / "creatives") {
        concat(
          pathEnd {
            get {
              extractRequest { httpRequest =>
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [GET /api/v1/creatives] Request received")
                val futureCreatives = creativeStore.listAll()
                onComplete(futureCreatives) {
                  case scala.util.Success(creatives) =>
                    println(s"[$timestamp] [GET /api/v1/creatives] Success: Found ${creatives.length} creatives")
                    complete(creatives)
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [GET /api/v1/creatives] ERROR: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, s"Error listing creatives: ${ex.getMessage}")
                }
              }
            }
          },
          path(Segment) { creativeId =>
            concat(
              get {
                requireAuth {
                  extractRequest { httpRequest =>
                  val timestamp = java.time.LocalDateTime.now()
                  println(s"[$timestamp] [GET /api/v1/creatives/$creativeId] Request received")
                  val futureCreative = creativeStore.getById(creativeId)
                  onComplete(futureCreative) {
                    case scala.util.Success(Some(creative)) =>
                      println(s"[$timestamp] [GET /api/v1/creatives/$creativeId] Success: Creative found")
                      complete(creative)
                    case scala.util.Success(None) =>
                      println(s"[$timestamp] [GET /api/v1/creatives/$creativeId] Not found")
                      complete(StatusCodes.NotFound, s"Creative not found: $creativeId")
                    case scala.util.Failure(ex) =>
                      println(s"[$timestamp] [GET /api/v1/creatives/$creativeId] ERROR: ${ex.getMessage}")
                      ex.printStackTrace()
                      complete(StatusCodes.InternalServerError, s"Error retrieving creative: ${ex.getMessage}")
                  }
                }
                }
              },
              put {
                requireAuth {
                  extractRequest { httpRequest =>
                  val timestamp = java.time.LocalDateTime.now()
                  println(s"[$timestamp] [PUT /api/v1/creatives/$creativeId] Request received")
                  entity(as[CreateCreativeRequest]) { request =>
                    println(s"[$timestamp] [PUT /api/v1/creatives/$creativeId] Request body: name=${request.name}")
                    
                    val futureCreative = creativeStore.getById(creativeId).flatMap {
                      case Some(existingCreative) =>
                        val updatedCreative = existingCreative.copy(
                          name = request.name,
                          creativeType = request.creativeType,
                          creativeUrl = request.creativeUrl,
                          targetUrl = request.targetUrl,
                          durationSeconds = request.durationSeconds,
                          status = request.status,
                          shareOfVoice = request.shareOfVoice,
                          frequencyCapPerScreen = request.frequencyCapPerScreen,
                          metadata = request.metadata,
                          updatedAt = Instant.now()
                        )
                        creativeStore.upsert(updatedCreative).map(_ => updatedCreative)
                      case None =>
                        Future.failed(new IllegalArgumentException(s"Creative not found: $creativeId"))
                    }
                    
                    onComplete(futureCreative) {
                      case scala.util.Success(creative) =>
                        println(s"[$timestamp] [PUT /api/v1/creatives/$creativeId] Success: Creative updated")
                        complete(creative)
                      case scala.util.Failure(ex: IllegalArgumentException) =>
                        println(s"[$timestamp] [PUT /api/v1/creatives/$creativeId] Not found: ${ex.getMessage}")
                        complete(StatusCodes.NotFound, ex.getMessage)
                      case scala.util.Failure(ex) =>
                        println(s"[$timestamp] [PUT /api/v1/creatives/$creativeId] ERROR: ${ex.getMessage}")
                        ex.printStackTrace()
                        complete(StatusCodes.InternalServerError, s"Failed to update creative: ${ex.getMessage}")
                    }
                  }
                }
                }
              },
              delete {
                requireAuth {
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [DELETE /api/v1/creatives/$creativeId] Request received")
                val futureDelete = creativeStore.delete(creativeId)
                onComplete(futureDelete) {
                  case scala.util.Success(_) =>
                    println(s"[$timestamp] [DELETE /api/v1/creatives/$creativeId] Success: Creative deleted")
                    complete(StatusCodes.OK, "Creative deleted successfully")
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [DELETE /api/v1/creatives/$creativeId] ERROR: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, s"Failed to delete creative: ${ex.getMessage}")
                }
                }
              }
            )
          }
        )
      }
    )
}

