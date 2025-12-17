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
  // Budget fields
  maxPlays: Option[Int] = None,        // Total maximum plays (global)
  dailyLimit: Option[Int] = None,       // Maximum plays per day
  hourlyLimit: Option[Int] = None,      // Maximum plays per hour
  // Frequency capping fields
  maxImpressionsPerDevice: Option[Int] = None,  // Max impressions per device
  maxImpressionsPerUser: Option[Int] = None,    // Max impressions per user
  frequencyCapWindowHours: Option[Int] = None,  // Time window for frequency cap (e.g., 24 hours)
  // OOH-specific fields
  durationSeconds: Option[Int] = None,  // Duration of the ad creative in seconds (for playlist generation)
  // Weight/priority for ad serving (higher weight = more likely to be selected)
  weight: Int = 1,  // Default weight of 1 (equal probability)
  createdAt: Instant,
  updatedAt: Instant
)

object Ad {
  implicit val adCodec: Codec[Ad] = deriveCodec[Ad]
}
