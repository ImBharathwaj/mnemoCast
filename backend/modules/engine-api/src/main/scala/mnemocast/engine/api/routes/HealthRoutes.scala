package mnemocast.engine.api.routes

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.infra.services.HealthService

/**
  * Health check routes.
  *
  * GET /api/v1/health - Comprehensive health check
  * GET /api/v1/ready - Readiness probe (for Kubernetes)
  * GET /api/v1/live - Liveness probe (for Kubernetes)
  */
class HealthRoutes(
  healthService: HealthService
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  val routes: Route =
    pathPrefix("api" / "v1") {
      // Comprehensive health check
      path("health") {
        get {
          onSuccess(healthService.checkHealth()) { health =>
            val statusCode = health.status match {
              case mnemocast.engine.domain.model.Up => StatusCodes.OK
              case mnemocast.engine.domain.model.Degraded => StatusCodes.OK // Still OK but degraded
              case mnemocast.engine.domain.model.Down => StatusCodes.ServiceUnavailable
            }
            complete(statusCode, health)
          }
        }
      } ~
      // Readiness probe
      path("ready") {
        get {
          onSuccess(healthService.checkReady()) { ready =>
            if (ready) {
              complete(StatusCodes.OK, """{"status":"ready"}""")
            } else {
              complete(StatusCodes.ServiceUnavailable, """{"status":"not ready"}""")
            }
          }
        }
      } ~
      // Liveness probe
      path("live") {
        get {
          onSuccess(healthService.checkLive()) { live =>
            if (live) {
              complete(StatusCodes.OK, """{"status":"alive"}""")
            } else {
              complete(StatusCodes.ServiceUnavailable, """{"status":"not alive"}""")
            }
          }
        }
      }
    }
}

