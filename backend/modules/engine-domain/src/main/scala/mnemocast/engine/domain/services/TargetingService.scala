package mnemocast.engine.domain.services

import mnemocast.engine.domain.model.{Ad, DeliveryRequest, TargetingRule}

/**
  * Pure targeting logic service.
  *
  * Evaluates whether an ad matches a delivery request based on targeting rules.
  */
object TargetingService {

  /**
    * Determines if an ad matches the given delivery request.
    *
    * Rules:
    * - If ad has no targeting rules → matches all requests (default: show everywhere)
    * - All targeting rules must pass (AND logic)
    * - Rule evaluation extracts request field by key and compares using operator
    *
    * @param ad      The ad to evaluate
    * @param request The delivery request to match against
    * @return true if the ad matches the request, false otherwise
    */
  def matches(ad: Ad, request: DeliveryRequest): Boolean = {
    // No targeting rules = match all requests
    if (ad.targetingRules.isEmpty) {
      return true
    }

    // All rules must pass (AND logic)
    ad.targetingRules.forall { rule =>
      evaluateRule(rule, request)
    }
  }

  /**
    * Evaluates a single targeting rule against a delivery request.
    *
    * @param rule    The targeting rule to evaluate
    * @param request The delivery request
    * @return true if the rule matches, false otherwise
    */
  private def evaluateRule(rule: TargetingRule, request: DeliveryRequest): Boolean = {
    // Extract the request field value by key
    val requestValueOpt = extractRequestValue(rule.key, request)

    // If the field doesn't exist in the request, the rule fails
    requestValueOpt match {
      case None => false
      case Some(requestValue) =>
        rule.operator.toLowerCase match {
          case "eq" => evaluateEq(rule.value, requestValue)
          case "in" => evaluateIn(rule.value, requestValue)
          case "daypart" | "timeband" => evaluateTimeBand(rule.value, request)
          case _    => false // Unknown operator = rule fails
        }
    }
  }

  /**
    * Extracts the request field value by key name.
    *
    * Supported keys:
    * - "country" → request.country
    * - "platform" → request.platform
    * - "deviceId" → request.deviceId
    * - "userId" → request.userId
    * - "appId" → request.appId
    * - "screenId" → request.screenId (OOH)
    * - "city" → request.city (OOH)
    * - "area" → request.area (OOH)
    * - "venueType" → request.venueType (OOH)
    * - "venuetype" → request.venueType (OOH, alternate)
    * - "timezone" → request.timezone (OOH)
    *
    * @param key     The field key (case-insensitive)
    * @param request The delivery request
    * @return Some(value) if the field exists and has a value, None otherwise
    */
  private def extractRequestValue(key: String, request: DeliveryRequest): Option[String] = {
    val normalizedKey = key.toLowerCase.trim

    normalizedKey match {
      case "country"  => request.country
      case "platform" => request.platform
      case "deviceid" => request.deviceId
      case "userid"   => request.userId
      case "appid"    => request.appId
      case "screenid" => request.screenId
      case "city"     => request.city
      case "area"     => request.area
      case "venuetype" | "venue_type" => request.venueType
      case "timezone" => request.timezone
      case _          => None // Unknown key
    }
  }

  /**
    * Evaluates "eq" (equals) operator.
    *
    * @param ruleValue    The value from the targeting rule
    * @param requestValue The value from the delivery request
    * @return true if values match (case-insensitive), false otherwise
    */
  private def evaluateEq(ruleValue: String, requestValue: String): Boolean = {
    ruleValue.trim.equalsIgnoreCase(requestValue.trim)
  }

  /**
    * Evaluates "in" (list membership) operator.
    *
    * Checks if requestValue exists in the comma-separated list of ruleValue.
    *
    * Example:
    * - ruleValue = "android,ios"
    * - requestValue = "android" → true
    * - requestValue = "ios" → true
    * - requestValue = "web" → false
    *
    * @param ruleValue    Comma-separated list from the targeting rule
    * @param requestValue The value from the delivery request
    * @return true if requestValue is in the list (case-insensitive), false otherwise
    */
  private def evaluateIn(ruleValue: String, requestValue: String): Boolean = {
    val allowedValues = ruleValue
      .split(",")
      .map(_.trim)
      .filter(_.nonEmpty)

    allowedValues.exists(_.equalsIgnoreCase(requestValue.trim))
  }

  /**
    * Evaluates "daypart" or "timeband" operator for time-based targeting.
    * 
    * The rule value should be in format "HH:mm-HH:mm" or "HH:mm-HH:mm,day1,day2"
    * Example: "09:00-17:00" or "09:00-17:00,monday,friday"
    */
  private def evaluateTimeBand(ruleValue: String, request: DeliveryRequest): Boolean = {
    TimeTargetingService.parseTimeBandFromRule(ruleValue) match {
      case Some(timeBand) =>
        TimeTargetingService.matches(List(timeBand), request)
      case None =>
        false // Invalid format = rule fails
    }
  }
}

