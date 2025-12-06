package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

final case class DeliveryResponse(
  requestId: String,
  adId: String,
  creativeUrl: String,
  targetUrl: Option[String],
  impressionTrackingUrl: Option[String]
)

object DeliveryResponse {
  implicit val deliveryResponseCodec: Codec[DeliveryResponse] =
    deriveCodec[DeliveryResponse]
}
