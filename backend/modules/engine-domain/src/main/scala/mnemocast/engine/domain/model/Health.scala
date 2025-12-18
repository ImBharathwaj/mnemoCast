package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Component health status.
  */
sealed trait ComponentStatus
case object Healthy extends ComponentStatus
case object Unhealthy extends ComponentStatus
case object Unknown extends ComponentStatus

object ComponentStatus {
  implicit val componentStatusCodec: Codec[ComponentStatus] = Codec.from(
    io.circe.Decoder.decodeString.emap {
      case "healthy" => Right(Healthy)
      case "unhealthy" => Right(Unhealthy)
      case "unknown" => Right(Unknown)
      case other => Left(s"Unknown component status: $other")
    },
    io.circe.Encoder.encodeString.contramap {
      case Healthy => "healthy"
      case Unhealthy => "unhealthy"
      case Unknown => "unknown"
    }
  )
}

/**
  * Individual component health check result.
  */
final case class ComponentHealth(
  name: String,
  status: ComponentStatus,
  message: Option[String] = None,
  responseTimeMs: Option[Long] = None
)

object ComponentHealth {
  implicit val componentHealthCodec: Codec[ComponentHealth] =
    deriveCodec[ComponentHealth]
}

/**
  * Overall system health status.
  */
sealed trait SystemStatus
case object Up extends SystemStatus
case object Down extends SystemStatus
case object Degraded extends SystemStatus

object SystemStatus {
  implicit val systemStatusCodec: Codec[SystemStatus] = Codec.from(
    io.circe.Decoder.decodeString.emap {
      case "UP" => Right(Up)
      case "DOWN" => Right(Down)
      case "DEGRADED" => Right(Degraded)
      case other => Left(s"Unknown system status: $other")
    },
    io.circe.Encoder.encodeString.contramap {
      case Up => "UP"
      case Down => "DOWN"
      case Degraded => "DEGRADED"
    }
  )
}

/**
  * Complete health check response.
  */
final case class HealthResponse(
  status: SystemStatus,
  timestamp: String,
  uptimeSeconds: Long,
  components: List[ComponentHealth] = Nil,
  version: String = "1.0.0"
)

object HealthResponse {
  implicit val healthResponseCodec: Codec[HealthResponse] =
    deriveCodec[HealthResponse]
}

