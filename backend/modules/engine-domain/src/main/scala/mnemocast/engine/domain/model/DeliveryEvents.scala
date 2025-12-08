package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

final case class DeliveryEvent(
  eventId: String,
  requestId: String,
  adId: String,
  eventType: String,              // e.g. "impression", "click"
  occurredAt: Instant,
  metadata: Map[String, String] = Map.empty
)

object DeliveryEvent {
  implicit val deliveryEventCodec: Codec[DeliveryEvent] =
    deriveCodec[DeliveryEvent]
}
