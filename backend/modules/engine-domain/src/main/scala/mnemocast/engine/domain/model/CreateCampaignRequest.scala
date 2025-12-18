package mnemocast.engine.domain.model

import java.time.Instant

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto._

/**
  * Request model for creating a new campaign.
  */
final case class CreateCampaignRequest(
  id: Option[String] = None,             // Optional: auto-generated if not provided
  name: String,
  advertiserId: String,
  status: String = "active",
  startDate: Instant,
  endDate: Instant,
  totalBudget: Option[Long] = None,
  targetPlayouts: Option[Long] = None,
  targetingRules: List[TargetingRule] = List.empty,
  priority: Int = 1
)

object CreateCampaignRequest {
  implicit val createCampaignRequestDecoder: Decoder[CreateCampaignRequest] = Decoder.instance { cursor =>
    for {
      id <- cursor.get[Option[String]]("id")
      name <- cursor.get[String]("name")
      advertiserId <- cursor.get[String]("advertiserId")
      status <- cursor.getOrElse[String]("status")("active")
      startDate <- cursor.get[Instant]("startDate")
      endDate <- cursor.get[Instant]("endDate")
      totalBudget <- cursor.get[Option[Long]]("totalBudget")
      targetPlayouts <- cursor.get[Option[Long]]("targetPlayouts")
      targetingRules <- cursor.getOrElse[List[TargetingRule]]("targetingRules")(List.empty)
      priority <- cursor.getOrElse[Int]("priority")(1)
    } yield CreateCampaignRequest(id, name, advertiserId, status, startDate, endDate, totalBudget, targetPlayouts, targetingRules, priority)
  }
  
  implicit val createCampaignRequestEncoder: Encoder[CreateCampaignRequest] = deriveEncoder[CreateCampaignRequest]
  
  implicit val createCampaignRequestCodec: Codec[CreateCampaignRequest] =
    Codec.from(createCampaignRequestDecoder, createCampaignRequestEncoder)
}

