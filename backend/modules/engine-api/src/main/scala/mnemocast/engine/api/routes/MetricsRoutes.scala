package mnemocast.engine.api.routes

import scala.concurrent.ExecutionContext

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.AuthMiddleware
import mnemocast.engine.infra.services.{AuthService, MetricsService}

/**
  * Metrics routes.
  *
  * GET /api/v1/metrics - System metrics and performance data
  */
class MetricsRoutes(
  metricsService: MetricsService,
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

  val routes: Route =
    pathPrefix("api" / "v1") {
      path("metrics") {
        get {
          requireAuth {
            onSuccess(metricsService.getMetrics()) { metrics =>
              complete(metrics)
            }
          }
        }
      }
    }
}

