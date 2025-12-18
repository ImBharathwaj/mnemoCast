package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.{HttpResponse, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.AuthMiddleware
import mnemocast.engine.domain.model.{Ad, CreateAdRequest}
import mnemocast.engine.infra.services.AuthService
import mnemocast.engine.infra.store.{AdStore, EventStore}

class AdminAdRoutes(
  adStore: AdStore,
  eventStore: EventStore,
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
    * Admin routes for ad management.
    *
    * POST /admin/ads - Create a new ad
    * GET /admin/ads?activeOnly=true - List ads
    * GET /admin/ads/{adId}/events?limit=50 - List events for an ad
    */
  val routes: Route =
    pathPrefix("admin") {
      requireAuth {
        pathPrefix("ads") {
          concat(
            post {
            entity(as[CreateAdRequest]) { request =>
              val now = Instant.now()
              
              // Generate UUID if id not provided
              val adId = request.id.getOrElse(UUID.randomUUID().toString)
              
              // Build Ad object from request
              val ad = Ad(
                id = adId,
                advertiserId = request.advertiserId,
                creativeUrl = request.creativeUrl,
                targetUrl = request.targetUrl,
                targetingRules = request.targetingRules,
                isActive = request.isActive,
                maxPlays = request.maxPlays,
                dailyLimit = request.dailyLimit,
                hourlyLimit = request.hourlyLimit,
                maxImpressionsPerDevice = request.maxImpressionsPerDevice,
                maxImpressionsPerUser = request.maxImpressionsPerUser,
                frequencyCapWindowHours = request.frequencyCapWindowHours,
                durationSeconds = request.durationSeconds,
                weight = request.weight,
                createdAt = now,
                updatedAt = now
              )
              
              // Store ad and return the created ad
              val futureAd = adStore.upsert(ad).map(_ => ad)
              onSuccess(futureAd) { createdAd =>
                complete(createdAd)
              }
            }
          },
          pathEnd {
            get {
              parameters("activeOnly".as[Boolean].?(true)) { activeOnly =>
                val futureAds = if (activeOnly) {
                  adStore.listActive()
                } else {
                  adStore.listAll()
                }
                
                onSuccess(futureAds) { ads =>
                  complete(ads)
                }
              }
            }
          },
          path(Segment / "events") { adId =>
            get {
              parameters("limit".as[Int].?(50)) { limit =>
                val futureEvents = eventStore.findByAdId(adId, limit)
                
                onSuccess(futureEvents) { events =>
                  complete(events)
                }
              }
            }
          }
        )
        }
      }
    }
}

