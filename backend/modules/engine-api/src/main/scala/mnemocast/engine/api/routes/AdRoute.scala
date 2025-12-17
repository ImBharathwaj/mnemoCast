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
            "platform".?,
            "screenId".?,
            "city".?,
            "area".?,
            "venueType".?,
            "timezone".?
          ) { (deviceIdOpt, userIdOpt, appIdOpt, countryOpt, platformOpt, screenIdOpt, cityOpt, areaOpt, venueTypeOpt, timezoneOpt) =>
            extractClientIP { ip =>
              val request = DeliveryRequest(
                requestId = UUID.randomUUID().toString,
                deviceId = deviceIdOpt,
                userId = userIdOpt,
                appId = appIdOpt,
                ip = Some(ip.toOption.map(_.getHostAddress).getOrElse(ip.toString())),
                country = countryOpt,
                platform = platformOpt,
                screenId = screenIdOpt,
                city = cityOpt,
                area = areaOpt,
                venueType = venueTypeOpt,
                timezone = timezoneOpt,
                timestamp = Instant.now()
              )

              onComplete(adDeliveryService.deliver(request)) {
                case scala.util.Success(Some(response)) =>
                  complete(response) // JSON (via JsonSupport + Circe)
                case scala.util.Success(None) =>
                  complete(StatusCodes.NoContent)
                case scala.util.Failure(ex) =>
                  println(s"Error in ad delivery: ${ex.getMessage}")
                  ex.printStackTrace()
                  complete(StatusCodes.InternalServerError, s"Internal server error: ${ex.getMessage}")
              }
            }
          }
        }
      }
    }
}
