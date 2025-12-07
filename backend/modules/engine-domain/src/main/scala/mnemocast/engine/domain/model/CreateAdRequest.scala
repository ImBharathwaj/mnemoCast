package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Request model for creating a new ad.
  * All fields except id are required.
  */
final case class CreateAdRequest(
  id: Option[String],              // Optional: auto-generated if not provided
  advertiserId: String,
  creativeUrl: String,
  targetUrl: Option[String],
  targetingRules: List[TargetingRule],
  isActive: Boolean
)

object CreateAdRequest {
  implicit val createAdRequestCodec: Codec[CreateAdRequest] =
    deriveCodec[CreateAdRequest]
}

