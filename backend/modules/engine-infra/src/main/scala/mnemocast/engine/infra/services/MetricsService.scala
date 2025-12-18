package mnemocast.engine.infra.services

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util.concurrent.atomic.{AtomicLong, AtomicReference}
import java.util.concurrent.ConcurrentHashMap

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.jdk.CollectionConverters._

import mnemocast.engine.domain.model.{EndpointMetrics, SystemMetrics}
import mnemocast.engine.infra.store.{CampaignStore, CreativeStore, ScreenStore}

/**
  * Service for tracking and reporting system metrics.
  */
class MetricsService(
  campaignStore: CampaignStore,
  creativeStore: CreativeStore,
  screenStore: ScreenStore,
  startTime: Instant = Instant.now()
)(implicit ec: ExecutionContext) {

  // Request tracking
  private val totalRequests = new AtomicLong(0L)
  private val totalErrors = new AtomicLong(0L)
  
  // Response time tracking per endpoint
  private val endpointStats = new ConcurrentHashMap[String, EndpointStats]()
  
  case class EndpointStats(
    var requestCount: Long = 0L,
    var errorCount: Long = 0L,
    var responseTimes: mutable.ArrayBuffer[Long] = mutable.ArrayBuffer.empty
  )

  /**
    * Records a request for metrics tracking.
    */
  def recordRequest(endpoint: String, method: String, responseTimeMs: Long, isError: Boolean = false): Unit = {
    totalRequests.incrementAndGet()
    if (isError) {
      totalErrors.incrementAndGet()
    }

    val key = s"$method:$endpoint"
    val stats = endpointStats.computeIfAbsent(key, _ => EndpointStats())
    
    stats.synchronized {
      stats.requestCount += 1
      if (isError) {
        stats.errorCount += 1
      }
      stats.responseTimes += responseTimeMs
      
      // Keep only last 1000 response times for performance
      if (stats.responseTimes.length > 1000) {
        stats.responseTimes.remove(0)
      }
    }
  }

  /**
    * Calculates percentile from sorted list.
    */
  private def percentile(sorted: Seq[Long], p: Double): Long = {
    if (sorted.isEmpty) return 0L
    val index = Math.ceil(sorted.length * p / 100.0).toInt - 1
    sorted(Math.max(0, Math.min(index, sorted.length - 1)))
  }

  /**
    * Gets current system metrics.
    */
  def getMetrics(): Future[SystemMetrics] = {
    val timestamp = LocalDateTime.now(ZoneOffset.UTC).toString
    val uptimeSeconds = Instant.now().getEpochSecond - startTime.getEpochSecond
    
    val totalReqs = totalRequests.get()
    val totalErrs = totalErrors.get()
    val errorRate = if (totalReqs > 0) totalErrs.toDouble / totalReqs else 0.0

    // Calculate endpoint metrics
    val endpointMetricsList = endpointStats.entrySet().asScala.map { entry =>
      val (key, stats) = (entry.getKey, entry.getValue)
      val parts = key.split(":", 2)
      val method = if (parts.length > 0) parts(0) else "UNKNOWN"
      val endpoint = if (parts.length > 1) parts(1) else key
      
      stats.synchronized {
        val responseTimesSeq: Seq[Long] = stats.responseTimes.toSeq
        val sortedTimes = responseTimesSeq.sorted
        val avgTime = if (sortedTimes.nonEmpty) {
          sortedTimes.sum.toDouble / sortedTimes.length
        } else 0.0
        val p95 = if (sortedTimes.nonEmpty) Some(percentile(sortedTimes, 95)) else None
        val p99 = if (sortedTimes.nonEmpty) Some(percentile(sortedTimes, 99)) else None
        
        EndpointMetrics(
          endpoint = endpoint,
          method = method,
          requestCount = stats.requestCount,
          errorCount = stats.errorCount,
          avgResponseTimeMs = avgTime,
          p95ResponseTimeMs = p95,
          p99ResponseTimeMs = p99
        )
      }
    }.toList

    // Calculate overall response time stats
    val allResponseTimes: Seq[Long] = endpointStats.values().asScala.flatMap { stats =>
      stats.synchronized {
        stats.responseTimes.toSeq
      }
    }.toSeq
    val avgResponseTime = if (allResponseTimes.nonEmpty) {
      allResponseTimes.sum.toDouble / allResponseTimes.length
    } else 0.0
    val sortedAllTimes = allResponseTimes.sorted
    val p95Overall = if (sortedAllTimes.nonEmpty) Some(percentile(sortedAllTimes, 95)) else None
    val p99Overall = if (sortedAllTimes.nonEmpty) Some(percentile(sortedAllTimes, 99)) else None

    // Get active counts
    for {
      campaigns <- campaignStore.listAll()
      creatives <- creativeStore.listAll()
      screens <- screenStore.listAll()
    } yield {
      val activeCampaigns = campaigns.count(_.status == "active")
      val activeCreatives = creatives.count(_.status == "active")
      val activeScreens = screens.count(_.isOnline)

      // Get memory usage
      val runtime = Runtime.getRuntime
      val memoryUsageMB = Some((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024))

      SystemMetrics(
        timestamp = timestamp,
        uptimeSeconds = uptimeSeconds,
        totalRequests = totalReqs,
        totalErrors = totalErrs,
        errorRate = errorRate,
        avgResponseTimeMs = avgResponseTime,
        p95ResponseTimeMs = p95Overall,
        p99ResponseTimeMs = p99Overall,
        endpoints = endpointMetricsList,
        activeCampaigns = activeCampaigns,
        activeCreatives = activeCreatives,
        activeScreens = activeScreens,
        memoryUsageMB = memoryUsageMB
      )
    }
  }
}

