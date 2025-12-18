package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Endpoint-level metrics.
  */
final case class EndpointMetrics(
  endpoint: String,
  method: String,
  requestCount: Long,
  errorCount: Long,
  avgResponseTimeMs: Double,
  p95ResponseTimeMs: Option[Long] = None,
  p99ResponseTimeMs: Option[Long] = None
)

object EndpointMetrics {
  implicit val endpointMetricsCodec: Codec[EndpointMetrics] =
    deriveCodec[EndpointMetrics]
}

/**
  * System-wide metrics.
  */
final case class SystemMetrics(
  timestamp: String,
  uptimeSeconds: Long,
  totalRequests: Long,
  totalErrors: Long,
  errorRate: Double,
  avgResponseTimeMs: Double,
  p95ResponseTimeMs: Option[Long] = None,
  p99ResponseTimeMs: Option[Long] = None,
  endpoints: List[EndpointMetrics] = Nil,
  activeCampaigns: Int = 0,
  activeCreatives: Int = 0,
  activeScreens: Int = 0,
  memoryUsageMB: Option[Long] = None
)

object SystemMetrics {
  implicit val systemMetricsCodec: Codec[SystemMetrics] =
    deriveCodec[SystemMetrics]
}

