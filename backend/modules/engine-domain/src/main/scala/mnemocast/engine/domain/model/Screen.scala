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
  * @param width Display width in pixels (e.g., 1920)
  * @param height Display height in pixels (e.g., 1080)
  * @param isAudible Whether the display supports audio playback
  * @param isOnline Whether the screen is currently online
  * @param lastSeen Timestamp of last heartbeat/contact
  * @param passkey Authentication passkey for screen client (generated on registration)
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
  width: Option[Int] = None,         // Display width in pixels
  height: Option[Int] = None,       // Display height in pixels
  isAudible: Boolean = false,        // Whether the display supports audio
  isOnline: Boolean = false,
  lastSeen: Option[Instant] = None,
  passkey: String,                   // Authentication passkey (generated on registration)
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

