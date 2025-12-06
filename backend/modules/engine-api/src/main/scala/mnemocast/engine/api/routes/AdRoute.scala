package mnemocast.engine.api.routes

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.DeliveryRequest
import mnemocast.engine.infra.services.AdDeliveryService

class AdRoutes(
  adDeliveryService: AdDeliveryService
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  /**
    * All routes for the ad engine.
    *
    * GET /ads/deliver?deviceId=...&appId=...&country=IN&platform=android
    */
  val routes: Route =
    pathPrefix("ads") {
      path("deliver") {
        get {
          parameters(
            "deviceId".?,
            "userId".?,
            "appId".?,
            "country".?,
            "platform".?
          ) { (deviceIdOpt, userIdOpt, appIdOpt, countryOpt, platformOpt) =>
            extractClientIP { ip =>
              val request = DeliveryRequest(
                requestId = UUID.randomUUID().toString,
                deviceId = deviceIdOpt,
                userId = userIdOpt,
                appId = appIdOpt,
                ip = Some(ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())),
                country = countryOpt,
                platform = platformOpt,
                timestamp = Instant.now()
              )

              onSuccess(adDeliveryService.deliver(request)) {
                case Some(response) =>
                  complete(response) // JSON (via JsonSupport + Circe)
                case None =>
                  complete(StatusCodes.NoContent)
              }
            }
          }
        }
      }
    }
}
