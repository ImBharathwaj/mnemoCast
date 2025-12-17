package mnemocast.engine.domain.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto._

/**
  * Request model for creating a new creative.
  */
final case class CreateCreativeRequest(
  id: Option[String] = None,             // Optional: auto-generated if not provided
  campaignId: String,
  name: String,
  creativeType: String = "video",
  creativeUrl: String,
  targetUrl: Option[String] = None,
  durationSeconds: Int,
  status: String = "active",
  shareOfVoice: Option[Double] = None,
  frequencyCapPerScreen: Option[Int] = None,
  metadata: Map[String, String] = Map.empty
)

object CreateCreativeRequest {
  implicit val createCreativeRequestDecoder: Decoder[CreateCreativeRequest] = Decoder.instance { cursor =>
    for {
      id <- cursor.get[Option[String]]("id")
      campaignId <- cursor.get[String]("campaignId")
      name <- cursor.get[String]("name")
      creativeType <- cursor.getOrElse[String]("creativeType")("video")
      creativeUrl <- cursor.get[String]("creativeUrl")
      targetUrl <- cursor.get[Option[String]]("targetUrl")
      durationSeconds <- cursor.get[Int]("durationSeconds")
      status <- cursor.getOrElse[String]("status")("active")
      shareOfVoice <- cursor.get[Option[Double]]("shareOfVoice")
      frequencyCapPerScreen <- cursor.get[Option[Int]]("frequencyCapPerScreen")
      metadata <- cursor.getOrElse[Map[String, String]]("metadata")(Map.empty)
    } yield CreateCreativeRequest(id, campaignId, name, creativeType, creativeUrl, targetUrl, durationSeconds, status, shareOfVoice, frequencyCapPerScreen, metadata)
  }
  
  implicit val createCreativeRequestEncoder: Encoder[CreateCreativeRequest] = deriveEncoder[CreateCreativeRequest]
  
  implicit val createCreativeRequestCodec: Codec[CreateCreativeRequest] =
    Codec.from(createCreativeRequestDecoder, createCreativeRequestEncoder)
}

