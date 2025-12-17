package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Decision represents a playlist generation decision.
  *
  * This is logged for audit trail and analytics purposes:
  * - What campaigns/creatives were eligible
  * - What was selected for the playlist
  * - What was actually played (linked via PlayEvent)
  *
  * @param decisionId Unique decision identifier
  * @param requestId Request ID that triggered the decision
  * @param screenId Screen for which the playlist was generated
  * @param eligibleCampaignIds List of campaign IDs that were eligible after targeting/budget filtering
  * @param selectedCreatives List of creatives selected for the playlist
  * @param totalDurationSeconds Total duration of the generated playlist
  * @param timestamp When the decision was made
  */
final case class Decision(
  decisionId: String,
  requestId: String,
  screenId: String,
  eligibleCampaignIds: List[String] = List.empty,
  selectedCreatives: List[SelectedCreative] = List.empty,
  totalDurationSeconds: Int,
  timestamp: Instant
)

/**
  * Represents a creative that was selected for a playlist.
  */
final case class SelectedCreative(
  creativeId: String,
  campaignId: String,
  position: Int,
  durationSeconds: Int
)

object Decision {
  implicit val decisionCodec: Codec[Decision] = deriveCodec[Decision]
  implicit val selectedCreativeCodec: Codec[SelectedCreative] = deriveCodec[SelectedCreative]
}

