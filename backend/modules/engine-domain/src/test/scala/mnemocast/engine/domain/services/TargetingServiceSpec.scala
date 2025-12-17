package mnemocast.engine.domain.services

import java.time.Instant

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import mnemocast.engine.domain.model.{Ad, Campaign, DeliveryRequest, TargetingRule}

class TargetingServiceSpec extends AnyFlatSpec with Matchers {

  "TargetingService" should "match ads with no targeting rules" in {
    val ad = Ad(
      id = "ad1",
      advertiserId = "adv1",
      creativeUrl = "http://example.com/ad1.jpg",
      targetUrl = None,
      targetingRules = List.empty,
      isActive = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = Some("US"),
      platform = Some("android"),
      screenId = None,
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(ad, request) should be(true)
  }

  it should "match ads with matching country targeting" in {
    val ad = Ad(
      id = "ad1",
      advertiserId = "adv1",
      creativeUrl = "http://example.com/ad1.jpg",
      targetUrl = None,
      targetingRules = List(
        TargetingRule("country", "eq", "US")
      ),
      isActive = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = Some("US"),
      platform = None,
      screenId = None,
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(ad, request) should be(true)
  }

  it should "not match ads with non-matching country targeting" in {
    val ad = Ad(
      id = "ad1",
      advertiserId = "adv1",
      creativeUrl = "http://example.com/ad1.jpg",
      targetUrl = None,
      targetingRules = List(
        TargetingRule("country", "eq", "US")
      ),
      isActive = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val request = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = Some("IN"),
      platform = None,
      screenId = None,
      city = None,
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(ad, request) should be(false)
  }

  it should "match ads with screen tag targeting using 'in' operator" in {
    val ad = Ad(
      id = "ad1",
      advertiserId = "adv1",
      creativeUrl = "http://example.com/ad1.jpg",
      targetUrl = None,
      targetingRules = List(
        TargetingRule("screenTag", "in", "mall,food_court")
      ),
      isActive = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
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
      screenTags = List("mall", "gym"),  // "mall" matches
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(ad, request) should be(true)
  }

  it should "not match ads with screen tag targeting when no tags match" in {
    val ad = Ad(
      id = "ad1",
      advertiserId = "adv1",
      creativeUrl = "http://example.com/ad1.jpg",
      targetUrl = None,
      targetingRules = List(
        TargetingRule("screenTag", "in", "mall,food_court")
      ),
      isActive = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
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
      screenTags = List("office", "gym"),  // No match
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(ad, request) should be(false)
  }

  it should "match campaigns with matching city targeting" in {
    val campaign = Campaign(
      id = "camp1",
      name = "Test Campaign",
      advertiserId = "adv1",
      status = "active",
      startDate = Instant.now().minusSeconds(3600),
      endDate = Instant.now().plusSeconds(3600),
      totalBudget = None,
      targetPlayouts = None,
      targetingRules = List(
        TargetingRule("city", "eq", "Chennai")
      ),
      priority = 1,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
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
      city = Some("Chennai"),
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(campaign, request) should be(true)
  }

  it should "match campaigns with matching venueType targeting" in {
    val campaign = Campaign(
      id = "camp1",
      name = "Test Campaign",
      advertiserId = "adv1",
      status = "active",
      startDate = Instant.now().minusSeconds(3600),
      endDate = Instant.now().plusSeconds(3600),
      totalBudget = None,
      targetPlayouts = None,
      targetingRules = List(
        TargetingRule("venueType", "in", "mall,airport")
      ),
      priority = 1,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
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
      venueType = Some("mall"),
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(campaign, request) should be(true)
  }

  it should "require all targeting rules to pass (AND logic)" in {
    val ad = Ad(
      id = "ad1",
      advertiserId = "adv1",
      creativeUrl = "http://example.com/ad1.jpg",
      targetUrl = None,
      targetingRules = List(
        TargetingRule("country", "eq", "US"),
        TargetingRule("city", "eq", "New York")
      ),
      isActive = true,
      createdAt = Instant.now(),
      updatedAt = Instant.now()
    )

    val requestMatchingBoth = DeliveryRequest(
      requestId = "req1",
      deviceId = Some("device1"),
      userId = None,
      appId = None,
      ip = None,
      country = Some("US"),
      platform = None,
      screenId = None,
      city = Some("New York"),
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    val requestMatchingOnlyOne = DeliveryRequest(
      requestId = "req2",
      deviceId = Some("device2"),
      userId = None,
      appId = None,
      ip = None,
      country = Some("US"),
      platform = None,
      screenId = None,
      city = Some("Los Angeles"),  // Doesn't match
      area = None,
      venueType = None,
      screenTags = List.empty,
      timezone = None,
      timestamp = Instant.now()
    )

    TargetingService.matches(ad, requestMatchingBoth) should be(true)
    TargetingService.matches(ad, requestMatchingOnlyOne) should be(false)
  }
}

