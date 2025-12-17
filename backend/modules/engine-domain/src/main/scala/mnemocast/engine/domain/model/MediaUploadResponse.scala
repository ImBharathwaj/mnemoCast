package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Response model for media file upload.
  */
final case class MediaUploadResponse(
  url: String,              // Full URL to access the uploaded file
  path: String,             // Storage path (relative)
  contentType: String,      // MIME type
  size: Long,               // File size in bytes
  duration: Option[Int] = None // Duration in seconds (for videos, if extracted)
)

object MediaUploadResponse {
  implicit val mediaUploadResponseCodec: Codec[MediaUploadResponse] = deriveCodec[MediaUploadResponse]
}

