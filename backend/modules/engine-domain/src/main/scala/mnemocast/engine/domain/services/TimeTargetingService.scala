package mnemocast.engine.domain.services

import java.time.{DayOfWeek, LocalTime}
import java.util.Locale

import mnemocast.engine.domain.model.{DeliveryRequest, TimeBand}

/**
  * Service for time-based targeting (dayparts).
  */
object TimeTargetingService {

  /**
    * Checks if the current time matches the time bands.
    *
    * @param timeBands List of time bands to check
    * @param request Delivery request containing timestamp and timezone
    * @return true if current time matches any of the time bands
    */
  def matches(timeBands: List[TimeBand], request: DeliveryRequest): Boolean = {
    if (timeBands.isEmpty) {
      return true // No time bands = match all times
    }

    val requestTime = request.timestamp
    val timezone = request.timezone.getOrElse("UTC")
    val localDateTime = try {
      requestTime.atZone(java.time.ZoneId.of(timezone)).toLocalDateTime
    } catch {
      case _: Exception => requestTime.atZone(java.time.ZoneId.of("UTC")).toLocalDateTime
    }

    val currentDayOfWeek = localDateTime.getDayOfWeek
    val currentTime = localDateTime.toLocalTime

    timeBands.exists { band =>
      matchesTimeBand(band, currentDayOfWeek, currentTime)
    }
  }

  /**
    * Checks if a time band matches the given day and time.
    */
  private def matchesTimeBand(
    band: TimeBand,
    dayOfWeek: DayOfWeek,
    currentTime: LocalTime
  ): Boolean = {
    // Check day of week if specified
    if (band.daysOfWeek.nonEmpty && !band.daysOfWeek.contains(dayOfWeek)) {
      return false
    }

    // Parse time range
    val startTime = parseTime(band.startTime)
    val endTime = parseTime(band.endTime)

    (startTime, endTime) match {
      case (Some(start), Some(end)) =>
        if (start.isBefore(end) || start.equals(end)) {
          // Normal case: 09:00 to 17:00 (start inclusive, end exclusive)
          (currentTime.equals(start) || currentTime.isAfter(start)) && currentTime.isBefore(end)
        } else {
          // Wraps around midnight: 22:00 to 06:00
          currentTime.equals(start) || currentTime.isAfter(start) || currentTime.isBefore(end)
        }
      case _ => false
    }
  }

  /**
    * Parses a time string in HH:mm format.
    */
  private def parseTime(timeStr: String): Option[LocalTime] = {
    try {
      Some(LocalTime.parse(timeStr.trim))
    } catch {
      case _: Exception => None
    }
  }

  /**
    * Parses a time band from a targeting rule value.
    * Expected format: "HH:mm-HH:mm" or "HH:mm-HH:mm,day1,day2" (days optional)
    * Example: "09:00-17:00" or "09:00-17:00,monday,friday"
    */
  def parseTimeBandFromRule(ruleValue: String): Option[TimeBand] = {
    val parts = ruleValue.split(",").map(_.trim)
    if (parts.isEmpty) return None

    val timeRange = parts(0)
    val timeParts = timeRange.split("-")
    if (timeParts.length != 2) return None

    val startTime = timeParts(0).trim
    val endTime = timeParts(1).trim

    // Validate that times are in correct format (HH:mm)
    if (!isValidTimeFormat(startTime) || !isValidTimeFormat(endTime)) {
      return None
    }

    val daysOfWeek = if (parts.length > 1) {
      parts.tail.flatMap(parseDayOfWeek).toList
    } else {
      List.empty
    }

    Some(TimeBand(startTime = startTime, endTime = endTime, daysOfWeek = daysOfWeek))
  }

  /**
    * Validates that a string is in HH:mm format.
    */
  private def isValidTimeFormat(timeStr: String): Boolean = {
    timeStr.matches("^\\d{2}:\\d{2}$") && {
      val parts = timeStr.split(":")
      val hours = parts(0).toInt
      val minutes = parts(1).toInt
      hours >= 0 && hours < 24 && minutes >= 0 && minutes < 60
    }
  }

  private def parseDayOfWeek(dayStr: String): Option[DayOfWeek] = {
    val normalized = dayStr.toLowerCase(Locale.ROOT)
    normalized match {
      case "monday" | "mon" | "1" => Some(DayOfWeek.MONDAY)
      case "tuesday" | "tue" | "2" => Some(DayOfWeek.TUESDAY)
      case "wednesday" | "wed" | "3" => Some(DayOfWeek.WEDNESDAY)
      case "thursday" | "thu" | "4" => Some(DayOfWeek.THURSDAY)
      case "friday" | "fri" | "5" => Some(DayOfWeek.FRIDAY)
      case "saturday" | "sat" | "6" => Some(DayOfWeek.SATURDAY)
      case "sunday" | "sun" | "7" => Some(DayOfWeek.SUNDAY)
      case _ => None
    }
  }
}

