package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Screen represents a physical OOH display device/player.
  *
  * @param id Unique identifier for the screen (player/device ID)
  * @param name Human-readable name for the screen
  * @param location Location information
  * @param tags Tags for targeting (e.g., "mall", "airport", "transit", "food-court")
  * @param metadata Additional metadata (JSON-like structure)
  * @param classification Screen classification/tier (1-10, higher = premium). Used for pay-per-attention model where higher classified screens favor higher weight ads.
  * @param isOnline Whether the screen is currently online
  * @param lastSeen Timestamp of last heartbeat/contact
  * @param createdAt When the screen was registered
  * @param updatedAt Last update timestamp
  */
final case class Screen(
  id: String,                        // Player/device identifier
  name: String,
  location: ScreenLocation,
  tags: List[String] = List.empty,   // e.g., ["mall", "food-court", "premium"]
  metadata: Map[String, String] = Map.empty,
  classification: Int = 1,           // Screen classification (1-10, default 1). Higher = premium screen
  isOnline: Boolean = false,
  lastSeen: Option[Instant] = None,
  createdAt: Instant,
  updatedAt: Instant
)

object Screen {
  implicit val screenCodec: Codec[Screen] = deriveCodec[Screen]
}

/**
  * Location information for a screen.
  *
  * @param country Country code (e.g., "IN", "US")
  * @param city City name (e.g., "Chennai", "Mumbai")
  * @param area Area/neighborhood (e.g., "Phoenix Mall", "Airport Terminal 1")
  * @param venueType Venue type (e.g., "mall", "airport", "metro", "roadside")
  * @param timezone IANA timezone identifier (e.g., "Asia/Kolkata")
  */
final case class ScreenLocation(
  country: Option[String] = None,
  city: Option[String] = None,
  area: Option[String] = None,
  venueType: Option[String] = None,  // e.g., "mall", "airport", "metro", "roadside"
  timezone: Option[String] = None    // IANA timezone, e.g., "Asia/Kolkata"
)

object ScreenLocation {
  implicit val screenLocationCodec: Codec[ScreenLocation] = deriveCodec[ScreenLocation]
}

