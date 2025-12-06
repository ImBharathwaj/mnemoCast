package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Simple key-operator-value style targeting.
  * Examples:
  *   key = "country", operator = "eq", value = "IN"
  *   key = "platform", operator = "in", value = "android,ios"
  */
final case class TargetingRule(
  key: String,
  operator: String,
  value: String
)

object TargetingRule {
  implicit val targetingRuleCodec: Codec[TargetingRule] =
    deriveCodec[TargetingRule]
}
