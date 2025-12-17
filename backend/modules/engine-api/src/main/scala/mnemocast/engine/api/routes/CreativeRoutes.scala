package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.{CreateCreativeRequest, Creative}
import mnemocast.engine.infra.store.{CampaignStore, CreativeStore}

class CreativeRoutes(
  creativeStore: CreativeStore,
  campaignStore: CampaignStore
)(implicit ec: ExecutionContext)
    extends JsonSupport {

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
              entity(as[CreateCreativeRequest]) { request =>
                // Verify campaign exists
                val futureResult = campaignStore.getById(campaignId).flatMap {
                  case Some(_) =>
                    val now = Instant.now()
                    val creativeId = request.id.getOrElse(UUID.randomUUID().toString)
                    
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

                    creativeStore.upsert(creative).map(_ => creative)

                  case None =>
                    Future.failed(new IllegalArgumentException(s"Campaign not found: $campaignId"))
                }

                onComplete(futureResult) {
                  case scala.util.Success(creative) =>
                    complete(creative)
                  case scala.util.Failure(ex: IllegalArgumentException) =>
                    complete(StatusCodes.NotFound, ex.getMessage)
                  case scala.util.Failure(ex) =>
                    complete(StatusCodes.InternalServerError, s"Failed to create creative: ${ex.getMessage}")
                }
              }
            } ~
            get {
              val futureCreatives = creativeStore.findByCampaignId(campaignId)
              onComplete(futureCreatives) {
                case scala.util.Success(creatives) =>
                  complete(creatives)
                case scala.util.Failure(ex) =>
                  complete(StatusCodes.InternalServerError, s"Error listing creatives: ${ex.getMessage}")
              }
            }
          }
        )
      },
      pathPrefix("api" / "v1" / "creatives") {
        path(Segment) { creativeId =>
          get {
            val futureCreative = creativeStore.getById(creativeId)
            onComplete(futureCreative) {
              case scala.util.Success(Some(creative)) =>
                complete(creative)
              case scala.util.Success(None) =>
                complete(StatusCodes.NotFound, s"Creative not found: $creativeId")
              case scala.util.Failure(ex) =>
                complete(StatusCodes.InternalServerError, s"Error retrieving creative: ${ex.getMessage}")
            }
          }
        }
      }
    )
}

