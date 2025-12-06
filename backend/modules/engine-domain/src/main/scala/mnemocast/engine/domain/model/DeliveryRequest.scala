package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

final case class DeliveryRequest(
  requestId: String,
  deviceId: Option[String],
  userId: Option[String],
  appId: Option[String],
  ip: Option[String],
  country: Option[String],
  platform: Option[String], // e.g. "android", "ios", "web"
  timestamp: Instant
)

object DeliveryRequest {
  implicit val deliveryRequestCodec: Codec[DeliveryRequest] =
    deriveCodec[DeliveryRequest]
}
