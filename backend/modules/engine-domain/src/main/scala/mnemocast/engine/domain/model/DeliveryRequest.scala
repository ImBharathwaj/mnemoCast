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
  // OOH-specific fields
  screenId: Option[String] = None,      // Screen/player identifier
  city: Option[String] = None,          // City name
  area: Option[String] = None,          // Area/neighborhood
  venueType: Option[String] = None,     // Venue type (e.g., "mall", "airport")
  screenTags: List[String] = List.empty, // Screen tags for tag-based targeting
  timezone: Option[String] = None,      // IANA timezone identifier
  timestamp: Instant
)

object DeliveryRequest {
  implicit val deliveryRequestCodec: Codec[DeliveryRequest] =
    deriveCodec[DeliveryRequest]
}
