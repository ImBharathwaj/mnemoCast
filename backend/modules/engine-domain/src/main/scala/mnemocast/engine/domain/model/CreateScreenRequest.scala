package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Request model for screen registration.
  */
final case class CreateScreenRequest(
  id: String,                              // Player/device identifier
  name: String,
  location: ScreenLocation,
  tags: List[String] = List.empty,
  metadata: Map[String, String] = Map.empty
)

object CreateScreenRequest {
  implicit val createScreenRequestCodec: Codec[CreateScreenRequest] = deriveCodec[CreateScreenRequest]
}

