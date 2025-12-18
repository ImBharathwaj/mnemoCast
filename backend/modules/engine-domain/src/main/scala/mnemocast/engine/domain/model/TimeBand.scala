package mnemocast.engine.domain.model

import java.time.DayOfWeek

import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto._

/**
  * TimeBand represents a time window for daypart targeting.
  *
  * @param startTime Start time in HH:mm format (e.g., "09:00")
  * @param endTime End time in HH:mm format (e.g., "17:00")
  * @param daysOfWeek List of days when this time band applies (empty = all days)
  */
final case class TimeBand(
  startTime: String,                     // HH:mm format
  endTime: String,                       // HH:mm format
  daysOfWeek: List[DayOfWeek] = List.empty  // Empty = all days
)

object TimeBand {
  // Custom codec for DayOfWeek (serialize as string name)
  implicit val dayOfWeekEncoder: Encoder[DayOfWeek] = Encoder.encodeString.contramap(_.toString)
  implicit val dayOfWeekDecoder: Decoder[DayOfWeek] = Decoder.decodeString.emap { str =>
    try {
      Right(DayOfWeek.valueOf(str.toUpperCase))
    } catch {
      case _: IllegalArgumentException => Left(s"Invalid day of week: $str")
    }
  }

  implicit val timeBandCodec: Codec[TimeBand] = deriveCodec[TimeBand]
}

