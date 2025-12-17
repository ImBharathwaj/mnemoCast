package mnemocast.engine.domain.services

import java.time.{DayOfWeek, Instant, LocalDateTime, ZoneId}

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import mnemocast.engine.domain.model.{DeliveryRequest, TimeBand}

class TimeTargetingServiceSpec extends AnyFlatSpec with Matchers {

  "TimeTargetingService" should "match time bands when current time is within range" in {
    val now = LocalDateTime.of(2024, 1, 15, 14, 30) // 14:30 (2:30 PM)
    val instant = now.atZone(ZoneId.of("UTC")).toInstant

    val timeBands = List(
      TimeBand("09:00", "17:00", List.empty) // 9 AM to 5 PM
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = None,
      platform = None,
      screenId = Some("screen1"),
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = Some("UTC"),
      timestamp = instant
    )

    TimeTargetingService.matches(timeBands, request) should be(true)
  }

  it should "not match time bands when current time is outside range" in {
    val now = LocalDateTime.of(2024, 1, 15, 20, 0) // 8 PM
    val instant = now.atZone(ZoneId.of("UTC")).toInstant

    val timeBands = List(
      TimeBand("09:00", "17:00", List.empty) // 9 AM to 5 PM
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = None,
      platform = None,
      screenId = Some("screen1"),
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = Some("UTC"),
      timestamp = instant
    )

    TimeTargetingService.matches(timeBands, request) should be(false)
  }

  it should "match time bands with specific day of week restriction" in {
    val monday = LocalDateTime.of(2024, 1, 15, 14, 0) // Monday, 2 PM
    val instant = monday.atZone(ZoneId.of("UTC")).toInstant

    val timeBands = List(
      TimeBand("09:00", "17:00", List(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = None,
      platform = None,
      screenId = Some("screen1"),
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = Some("UTC"),
      timestamp = instant
    )

    TimeTargetingService.matches(timeBands, request) should be(true)
  }

  it should "not match time bands when day of week doesn't match" in {
    val tuesday = LocalDateTime.of(2024, 1, 16, 14, 0) // Tuesday, 2 PM
    val instant = tuesday.atZone(ZoneId.of("UTC")).toInstant

    val timeBands = List(
      TimeBand("09:00", "17:00", List(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = None,
      platform = None,
      screenId = Some("screen1"),
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = Some("UTC"),
      timestamp = instant
    )

    TimeTargetingService.matches(timeBands, request) should be(false)
  }

  it should "match time bands that wrap around midnight" in {
    val lateNight = LocalDateTime.of(2024, 1, 15, 23, 30) // 11:30 PM
    val instant = lateNight.atZone(ZoneId.of("UTC")).toInstant

    val timeBands = List(
      TimeBand("22:00", "06:00", List.empty) // 10 PM to 6 AM (wraps midnight)
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = None,
      platform = None,
      screenId = Some("screen1"),
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = Some("UTC"),
      timestamp = instant
    )

    TimeTargetingService.matches(timeBands, request) should be(true)
  }

  it should "parse time band from targeting rule string format" in {
    val ruleValue = "09:00-17:00"
    val timeBand = TimeTargetingService.parseTimeBandFromRule(ruleValue)

    timeBand should be(Some(TimeBand("09:00", "17:00", List.empty)))
  }

  it should "parse time band with day of week restrictions" in {
    val ruleValue = "09:00-17:00,monday,friday"
    val timeBand = TimeTargetingService.parseTimeBandFromRule(ruleValue)

    timeBand should be(Some(TimeBand("09:00", "17:00", List(DayOfWeek.MONDAY, DayOfWeek.FRIDAY))))
  }

  it should "return None for invalid time band format" in {
    val ruleValue = "invalid-format"
    val timeBand = TimeTargetingService.parseTimeBandFromRule(ruleValue)

    timeBand should be(None)
  }
}

