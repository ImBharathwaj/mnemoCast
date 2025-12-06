package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

final case class Ad(
  id: String,
  advertiserId: String,
  creativeUrl: String,
  targetUrl: Option[String],
  targetingRules: List[TargetingRule],
  isActive: Boolean,
  createdAt: Instant,
  updatedAt: Instant
)

object Ad {
  implicit val adCodec: Codec[Ad] = deriveCodec[Ad]
}
