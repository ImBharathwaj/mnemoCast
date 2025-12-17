package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.{Campaign, CreateCampaignRequest}
import mnemocast.engine.infra.store.CampaignStore

class CampaignRoutes(
  campaignStore: CampaignStore
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  /**
    * Campaign management routes.
    *
    * POST /api/v1/campaigns - Create a new campaign
    * GET /api/v1/campaigns/{campaignId} - Get campaign by ID
    * GET /api/v1/campaigns - List all campaigns
    */
  val routes: Route =
    pathPrefix("api" / "v1" / "campaigns") {
      concat(
        pathEnd {
          post {
            extractRequest { httpRequest =>
              val timestamp = java.time.LocalDateTime.now()
              println(s"[$timestamp] [POST /api/v1/campaigns] Request received")
              entity(as[CreateCampaignRequest]) { request =>
                println(s"[$timestamp] [POST /api/v1/campaigns] Request body: id=${request.id}, name=${request.name}, advertiserId=${request.advertiserId}, status=${request.status}")
                val now = Instant.now()
                val campaignId = request.id.getOrElse(UUID.randomUUID().toString)
              
                val campaign = Campaign(
                  id = campaignId,
                  name = request.name,
                  advertiserId = request.advertiserId,
                  status = request.status,
                  startDate = request.startDate,
                  endDate = request.endDate,
                  totalBudget = request.totalBudget,
                  targetPlayouts = request.targetPlayouts,
                  targetingRules = request.targetingRules,
                  priority = request.priority,
                  createdAt = now,
                  updatedAt = now
                )

                val futureCampaign = campaignStore.upsert(campaign).map(_ => campaign)
                onComplete(futureCampaign) {
                  case scala.util.Success(savedCampaign) =>
                    println(s"[$timestamp] [POST /api/v1/campaigns] Success: Campaign created with id=${savedCampaign.id}")
                    complete(savedCampaign)
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [POST /api/v1/campaigns] ERROR: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, s"Failed to create campaign: ${ex.getMessage}")
                }
              }
            }
          } ~
          get {
            extractRequest { httpRequest =>
              val timestamp = java.time.LocalDateTime.now()
              parameters("activeOnly".as[Boolean].?(false)) { activeOnly =>
                println(s"[$timestamp] [GET /api/v1/campaigns?activeOnly=$activeOnly] Request received")
                val futureCampaigns = if (activeOnly) {
                campaignStore.listActive()
              } else {
                campaignStore.listAll()
              }
              
                onComplete(futureCampaigns) {
                  case scala.util.Success(campaigns) =>
                    println(s"[$timestamp] [GET /api/v1/campaigns] Success: Found ${campaigns.length} campaigns")
                    complete(campaigns)
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [GET /api/v1/campaigns] ERROR: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, s"Error listing campaigns: ${ex.getMessage}")
                }
              }
            }
          }
        },
        path(Segment) { campaignId =>
          concat(
            get {
              val timestamp = java.time.LocalDateTime.now()
              println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId] Request received")
              val futureCampaign = campaignStore.getById(campaignId)
              onComplete(futureCampaign) {
                case scala.util.Success(Some(campaign)) =>
                  println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId] Success: Campaign found")
                  complete(campaign)
                case scala.util.Success(None) =>
                  println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId] Not found")
                  complete(StatusCodes.NotFound, s"Campaign not found: $campaignId")
                case scala.util.Failure(ex) =>
                  println(s"[$timestamp] [GET /api/v1/campaigns/$campaignId] ERROR: ${ex.getMessage}")
                  ex.printStackTrace()
                  complete(StatusCodes.InternalServerError, s"Error retrieving campaign: ${ex.getMessage}")
              }
            },
            put {
              extractRequest { httpRequest =>
                val timestamp = java.time.LocalDateTime.now()
                println(s"[$timestamp] [PUT /api/v1/campaigns/$campaignId] Request received")
                entity(as[CreateCampaignRequest]) { request =>
                  println(s"[$timestamp] [PUT /api/v1/campaigns/$campaignId] Request body: name=${request.name}, status=${request.status}")
                  
                  val futureCampaign = campaignStore.getById(campaignId).flatMap {
                    case Some(existingCampaign) =>
                      val updatedCampaign = existingCampaign.copy(
                        name = request.name,
                        advertiserId = request.advertiserId,
                        status = request.status,
                        startDate = request.startDate,
                        endDate = request.endDate,
                        totalBudget = request.totalBudget,
                        targetPlayouts = request.targetPlayouts,
                        targetingRules = request.targetingRules,
                        priority = request.priority,
                        updatedAt = Instant.now()
                      )
                      campaignStore.upsert(updatedCampaign).map(_ => updatedCampaign)
                    case None =>
                      Future.failed(new IllegalArgumentException(s"Campaign not found: $campaignId"))
                  }
                  
                  onComplete(futureCampaign) {
                    case scala.util.Success(campaign) =>
                      println(s"[$timestamp] [PUT /api/v1/campaigns/$campaignId] Success: Campaign updated")
                      complete(campaign)
                    case scala.util.Failure(ex: IllegalArgumentException) =>
                      println(s"[$timestamp] [PUT /api/v1/campaigns/$campaignId] Not found: ${ex.getMessage}")
                      complete(StatusCodes.NotFound, ex.getMessage)
                    case scala.util.Failure(ex) =>
                      println(s"[$timestamp] [PUT /api/v1/campaigns/$campaignId] ERROR: ${ex.getMessage}")
                      ex.printStackTrace()
                      complete(StatusCodes.InternalServerError, s"Failed to update campaign: ${ex.getMessage}")
                  }
                }
              }
            },
            delete {
              val timestamp = java.time.LocalDateTime.now()
              println(s"[$timestamp] [DELETE /api/v1/campaigns/$campaignId] Request received")
              val futureDelete = campaignStore.delete(campaignId)
              onComplete(futureDelete) {
                case scala.util.Success(_) =>
                  println(s"[$timestamp] [DELETE /api/v1/campaigns/$campaignId] Success: Campaign deleted")
                  complete(StatusCodes.OK, "Campaign deleted successfully")
                case scala.util.Failure(ex) =>
                  println(s"[$timestamp] [DELETE /api/v1/campaigns/$campaignId] ERROR: ${ex.getMessage}")
                  ex.printStackTrace()
                  complete(StatusCodes.InternalServerError, s"Failed to delete campaign: ${ex.getMessage}")
              }
            }
          )
        }
      )
    }
}

