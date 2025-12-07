package mnemocast.engine.infra.services

import java.time.{Instant, LocalDate, LocalDateTime, ZoneId}

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.Ad
import mnemocast.engine.infra.store.EventStore

/**
  * Budget checking service.
  *
  * Validates whether an ad can be served based on budget constraints:
  * - maxPlays: Total maximum plays (global)
  * - dailyLimit: Maximum plays per day
  * - hourlyLimit: Maximum plays per hour
  */
class BudgetService(
  eventStore: EventStore
)(implicit ec: ExecutionContext) {

  /**
    * Checks if an ad is within budget limits.
    *
    * @param ad The ad to check
    * @return Future[Boolean] - true if ad is within budget, false if budget exhausted
    */
  def isWithinBudget(ad: Ad): Future[Boolean] = {
    // Check all budget constraints
    val checks = List(
      checkMaxPlays(ad),
      checkDailyLimit(ad),
      checkHourlyLimit(ad)
    )

    // All checks must pass (AND logic)
    Future.sequence(checks).map(_.forall(identity))
  }

  /**
    * Checks if total plays (maxPlays) budget is not exhausted.
    */
  private def checkMaxPlays(ad: Ad): Future[Boolean] = {
    ad.maxPlays match {
      case None => Future.successful(true) // No limit
      case Some(max) =>
        eventStore.countTotalImpressionsByAdId(ad.id).map { count =>
          count < max
        }
    }
  }

  /**
    * Checks if daily limit is not exhausted.
    */
  private def checkDailyLimit(ad: Ad): Future[Boolean] = {
    ad.dailyLimit match {
      case None => Future.successful(true) // No limit
      case Some(max) =>
        val startOfDay = getStartOfDay()
        eventStore.countImpressionsByAdId(ad.id, startOfDay).map { count =>
          count < max
        }
    }
  }

  /**
    * Checks if hourly limit is not exhausted.
    */
  private def checkHourlyLimit(ad: Ad): Future[Boolean] = {
    ad.hourlyLimit match {
      case None => Future.successful(true) // No limit
      case Some(max) =>
        val startOfHour = getStartOfHour()
        eventStore.countImpressionsByAdId(ad.id, startOfHour).map { count =>
          count < max
        }
    }
  }

  /**
    * Gets the start of the current day (UTC).
    */
  private def getStartOfDay(): Instant = {
    val now = Instant.now()
    val localDate = LocalDate.ofInstant(now, ZoneId.of("UTC"))
    localDate.atStartOfDay(ZoneId.of("UTC")).toInstant
  }

  /**
    * Gets the start of the current hour (UTC).
    */
  private def getStartOfHour(): Instant = {
    val now = Instant.now()
    val localDateTime = LocalDateTime.ofInstant(now, ZoneId.of("UTC"))
    localDateTime.withMinute(0).withSecond(0).withNano(0)
      .atZone(ZoneId.of("UTC")).toInstant
  }
}

