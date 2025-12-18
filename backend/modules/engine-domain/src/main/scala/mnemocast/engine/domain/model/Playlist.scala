package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * PlaylistResponse represents a playlist of ads for OOH screens.
  *
  * @param requestId Original request ID
  * @param screenId Screen identifier
  * @param items List of playlist items (ads) in sequence
  * @param validForSeconds How long this playlist is valid (TTL)
  * @param totalDurationSeconds Total duration of the playlist in seconds
  */
final case class PlaylistResponse(
  requestId: String,
  screenId: Option[String],
  items: List[PlaylistItem],
  validForSeconds: Int = 300,  // Default 5 minutes
  totalDurationSeconds: Int
)

object PlaylistResponse {
  implicit val playlistResponseCodec: Codec[PlaylistResponse] = deriveCodec[PlaylistResponse]
}

/**
  * PlaylistItem represents a single ad in a playlist.
  *
  * @param adId Ad identifier
  * @param creativeUrl URL to the creative asset
  * @param targetUrl Optional click-through URL
  * @param durationSeconds Duration of this ad in seconds
  * @param impressionTrackingUrl Optional impression tracking URL
  * @param position Position index in the playlist (0-based)
  */
final case class PlaylistItem(
  adId: String,
  creativeUrl: String,
  targetUrl: Option[String],
  durationSeconds: Int,
  impressionTrackingUrl: Option[String] = None,
  position: Int = 0
)

object PlaylistItem {
  implicit val playlistItemCodec: Codec[PlaylistItem] = deriveCodec[PlaylistItem]
}

