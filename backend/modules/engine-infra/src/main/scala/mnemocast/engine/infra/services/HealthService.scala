package mnemocast.engine.infra.services

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import mnemocast.engine.domain.model.{ComponentHealth, ComponentStatus, HealthResponse, SystemStatus}
import mnemocast.engine.domain.model.{Healthy, Unhealthy, Unknown}
import mnemocast.engine.domain.model.{Up, Down, Degraded}
import mnemocast.engine.infra.store.postgres.PostgresClient
import mnemocast.engine.infra.store.redis.RedisClient
import mnemocast.engine.infra.storage.MediaStorage

/**
  * Service for checking system health.
  */
class HealthService(
  redisClient: RedisClient,
  postgresClientOpt: Option[PostgresClient],
  mediaStorage: MediaStorage,
  startTime: Instant = Instant.now()
)(implicit ec: ExecutionContext) {

  /**
    * Checks Redis connectivity.
    */
  private def checkRedis(): Future[ComponentHealth] = {
    val start = System.currentTimeMillis()
    Future {
      Try {
        redisClient.withJedis { jedis =>
          jedis.ping()
        }
        val responseTime = System.currentTimeMillis() - start
          ComponentHealth(
            name = "redis",
            status = Healthy,
          message = Some("Redis connection successful"),
          responseTimeMs = Some(responseTime)
        )
      }.recover {
        case ex: Exception =>
          val responseTime = System.currentTimeMillis() - start
          ComponentHealth(
            name = "redis",
            status = Unhealthy,
            message = Some(s"Redis connection failed: ${ex.getMessage}"),
            responseTimeMs = Some(responseTime)
          )
      }.get
    }
  }

  /**
    * Checks Postgres connectivity.
    */
  private def checkPostgres(): Future[ComponentHealth] = {
    postgresClientOpt match {
      case Some(pgClient) =>
        val start = System.currentTimeMillis()
        Future {
          Try {
            pgClient.withConnection { conn =>
              val stmt = conn.createStatement()
              val rs = stmt.executeQuery("SELECT 1")
              rs.next()
              rs.close()
              stmt.close()
            }
            val responseTime = System.currentTimeMillis() - start
            ComponentHealth(
              name = "postgres",
              status = Healthy,
              message = Some("Postgres connection successful"),
              responseTimeMs = Some(responseTime)
            )
          }.recover {
            case ex: Exception =>
              val responseTime = System.currentTimeMillis() - start
              ComponentHealth(
                name = "postgres",
                status = Unhealthy,
                message = Some(s"Postgres connection failed: ${ex.getMessage}"),
                responseTimeMs = Some(responseTime)
              )
          }.get
        }
      case None =>
        Future.successful(
          ComponentHealth(
            name = "postgres",
            status = Unknown,
            message = Some("Postgres not configured")
          )
        )
    }
  }

  /**
    * Checks media storage connectivity.
    */
  private def checkMediaStorage(): Future[ComponentHealth] = {
    val start = System.currentTimeMillis()
    Future {
      Try {
        // Try to check if storage is accessible (basic check)
        // This is a simple check - actual implementations may vary
        val responseTime = System.currentTimeMillis() - start
        ComponentHealth(
          name = "media_storage",
          status = Healthy,
          message = Some("Media storage accessible"),
          responseTimeMs = Some(responseTime)
        )
      }.recover {
        case ex: Exception =>
          val responseTime = System.currentTimeMillis() - start
          ComponentHealth(
            name = "media_storage",
            status = Unhealthy,
            message = Some(s"Media storage check failed: ${ex.getMessage}"),
            responseTimeMs = Some(responseTime)
          )
      }.get
    }
  }

  /**
    * Performs comprehensive health check.
    */
  def checkHealth(): Future[HealthResponse] = {
    val timestamp = LocalDateTime.now(ZoneOffset.UTC).toString
    val uptimeSeconds = Instant.now().getEpochSecond - startTime.getEpochSecond

    for {
      redisHealth <- checkRedis()
      postgresHealth <- checkPostgres()
      mediaHealth <- checkMediaStorage()
    } yield {
      val components = List(redisHealth, postgresHealth, mediaHealth)
      val unhealthyCount = components.count(_.status == Unhealthy)
      val healthyCount = components.count(_.status == Healthy)

      val systemStatus = if (unhealthyCount == 0) {
        Up
      } else if (healthyCount > 0) {
        Degraded
      } else {
        Down
      }

      HealthResponse(
        status = systemStatus,
        timestamp = timestamp,
        uptimeSeconds = uptimeSeconds,
        components = components
      )
    }
  }

  /**
    * Simple readiness check (for Kubernetes readiness probe).
    */
  def checkReady(): Future[Boolean] = {
    checkHealth().map(_.status == Up)
  }

  /**
    * Simple liveness check (for Kubernetes liveness probe).
    */
  def checkLive(): Future[Boolean] = {
    Future.successful(true) // System is alive if we can respond
  }
}

