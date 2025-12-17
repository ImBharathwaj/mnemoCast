package mnemocast.engine.domain.model

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto._

/**
  * Request model for screen registration.
  */
final case class CreateScreenRequest(
  id: Option[String] = None,              // Optional: auto-generated if not provided
  name: String,
  location: ScreenLocation,
  tags: List[String] = List.empty,
  metadata: Map[String, String] = Map.empty,
  classification: Int = 1                 // Screen classification (1-10, default 1). Higher = premium screen
)

object CreateScreenRequest {
  implicit val createScreenRequestDecoder: Decoder[CreateScreenRequest] = Decoder.instance { cursor =>
    for {
      id <- cursor.get[Option[String]]("id")
      name <- cursor.get[String]("name")
      location <- cursor.get[ScreenLocation]("location")
      tags <- cursor.getOrElse[List[String]]("tags")(List.empty)
      metadata <- cursor.getOrElse[Map[String, String]]("metadata")(Map.empty)
      classification <- cursor.getOrElse[Int]("classification")(1)
    } yield CreateScreenRequest(id, name, location, tags, metadata, classification)
  }
  
  implicit val createScreenRequestEncoder: Encoder[CreateScreenRequest] = deriveEncoder[CreateScreenRequest]
  
  implicit val createScreenRequestCodec: Codec[CreateScreenRequest] = 
    Codec.from(createScreenRequestDecoder, createScreenRequestEncoder)
}

