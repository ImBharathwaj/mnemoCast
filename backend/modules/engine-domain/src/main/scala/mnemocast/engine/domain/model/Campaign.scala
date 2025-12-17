package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Campaign represents an advertising campaign that groups multiple creatives.
  *
  * @param id Unique campaign identifier
  * @param name Campaign name
  * @param advertiserId Advertiser identifier
  * @param status Campaign status: "active", "paused", "completed"
  * @param startDate Campaign start date/time
  * @param endDate Campaign end date/time
  * @param totalBudget Total budget (in plays/impressions, optional)
  * @param targetPlayouts Target number of playouts (optional)
  * @param targetingRules Campaign-level targeting rules (creatives inherit these)
  * @param priority Priority weight for selection (higher = more likely to be selected)
  * @param createdAt Creation timestamp
  * @param updatedAt Last update timestamp
  */
final case class Campaign(
  id: String,
  name: String,
  advertiserId: String,
  status: String = "active",  // active, paused, completed
  startDate: Instant,
  endDate: Instant,
  totalBudget: Option[Long] = None,      // Total budget in plays
  targetPlayouts: Option[Long] = None,   // Target playouts
  targetingRules: List[TargetingRule] = List.empty,
  priority: Int = 1,                     // Priority weight (1-10, higher = more frequent)
  createdAt: Instant,
  updatedAt: Instant
)

object Campaign {
  implicit val campaignCodec: Codec[Campaign] = deriveCodec[Campaign]
}

