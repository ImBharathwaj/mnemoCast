package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Creative represents a single playable asset (video, image, etc.)
  *
  * @param id Unique creative identifier
  * @param campaignId ID of the campaign this creative belongs to
  * @param name Creative name
  * @param creativeType Type of creative: "video", "image", "html"
  * @param creativeUrl URL to the creative asset
  * @param targetUrl Optional click-through URL
  * @param durationSeconds Duration of the creative in seconds
  * @param status Creative status: "active", "paused", "deleted"
  * @param shareOfVoice Share of voice (0.0 to 1.0) for SOV distribution (optional)
  * @param frequencyCapPerScreen Max plays per day per screen (optional)
  * @param metadata Additional metadata
  * @param createdAt Creation timestamp
  * @param updatedAt Last update timestamp
  */
final case class Creative(
  id: String,
  campaignId: String,                    // FK to Campaign
  name: String,
  creativeType: String = "video",        // video, image, html
  creativeUrl: String,
  targetUrl: Option[String] = None,
  durationSeconds: Int,
  status: String = "active",             // active, paused, deleted
  shareOfVoice: Option[Double] = None,   // 0.0 to 1.0 for SOV distribution
  frequencyCapPerScreen: Option[Int] = None,  // Plays per day per screen
  metadata: Map[String, String] = Map.empty,
  createdAt: Instant,
  updatedAt: Instant
)

object Creative {
  implicit val creativeCodec: Codec[Creative] = deriveCodec[Creative]
}

