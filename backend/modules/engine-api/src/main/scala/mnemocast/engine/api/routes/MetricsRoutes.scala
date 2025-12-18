package mnemocast.engine.api.routes

import scala.concurrent.ExecutionContext

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.infra.services.MetricsService

/**
  * Metrics routes.
  *
  * GET /api/v1/metrics - System metrics and performance data
  */
class MetricsRoutes(
  metricsService: MetricsService
)(implicit ec: ExecutionContext)
    extends JsonSupport {

  val routes: Route =
    pathPrefix("api" / "v1") {
      path("metrics") {
        get {
          onSuccess(metricsService.getMetrics()) { metrics =>
            complete(metrics)
          }
        }
      }
    }
}

