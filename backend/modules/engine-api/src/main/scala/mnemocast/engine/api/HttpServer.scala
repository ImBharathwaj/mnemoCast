package mnemocast.engine.api

import scala.concurrent.ExecutionContext
import scala.io.StdIn

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.model.headers.{
  `Access-Control-Allow-Headers`,
  `Access-Control-Allow-Methods`,
  `Access-Control-Allow-Origin`,
  `Access-Control-Request-Method`
}
import org.apache.pekko.http.scaladsl.model.{HttpMethods, StatusCodes}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{Directive0, Route}

// Rate limiting and request ID middleware can be enabled when fully tested
// import mnemocast.engine.api.middleware.{RateLimiter, RequestId}
import mnemocast.engine.api.routes.{AdRoutes, AdminAdRoutes, AnalyticsRoutes, CampaignRoutes, CreativeRoutes, DocsRoutes, EventRoutes, HealthRoutes, MediaRoutes, MetricsRoutes, PlaylistRoutes, ScreenRoutes}
import mnemocast.engine.infra.services.{AdDeliveryService, AnalyticsService, BudgetService, CampaignBudgetService, CampaignPlaylistService, FrequencyCapService, HealthService, MediaValidator, MetricsService, PlaylistService}
import mnemocast.engine.infra.storage.{LocalFileStorage, MediaStorage, MinIOStorage}
import mnemocast.engine.infra.store.redis.{RedisAdStore, RedisCampaignStore, RedisClient, RedisCreativeStore, RedisDecisionStore, RedisEventStore, RedisScreenStore}
import mnemocast.engine.infra.store.postgres.{PostgresAdStore, PostgresCampaignStore, PostgresClient, PostgresCreativeStore, PostgresEventStore, PostgresScreenStore}
import mnemocast.engine.infra.store.{AdStore, CampaignStore, CreativeStore, DecisionStore, EventStore, HybridAdStore, HybridCampaignStore, HybridCreativeStore, HybridEventStore, HybridScreenStore, ScreenStore}

object HttpServer extends App {

  implicit val system: ActorSystem  = ActorSystem("mnemocast-engine")
  implicit val ec: ExecutionContext = system.dispatcher
  
  // Track server start time for uptime calculation
  private val serverStartTime = java.time.Instant.now()

  // --- Infra wiring (manual DI) ---
  // Storage strategy: Use environment variable STORAGE_STRATEGY
  // Options: "redis", "postgres", "hybrid" (default: "redis" for easy startup)
  val storageStrategy = sys.env.getOrElse("STORAGE_STRATEGY", "redis")
  println(s"📦 Storage Strategy: $storageStrategy")
  if (storageStrategy == "redis") {
    println("⚠️  NOTE: Using Redis-only mode. Data will NOT be stored in Postgres.")
    println("⚠️  To store in Postgres, set STORAGE_STRATEGY=hybrid or STORAGE_STRATEGY=postgres")
  }

  private val redisClient = new RedisClient("localhost", 6379)
  private val redisAdStore = new RedisAdStore(redisClient)
  private val redisEventStore = new RedisEventStore(redisClient)
  private val redisScreenStore = new RedisScreenStore(redisClient)
  private val redisCampaignStore = new RedisCampaignStore(redisClient)
  private val redisCreativeStore = new RedisCreativeStore(redisClient)
  private val redisDecisionStore = new RedisDecisionStore(redisClient)

  // Postgres client (only initialized if needed, with error handling)
  private val postgresClientOpt: Option[PostgresClient] = 
    if (storageStrategy == "postgres" || storageStrategy == "hybrid") {
      try {
        Some(new PostgresClient(
          host = sys.env.getOrElse("POSTGRES_HOST", "localhost"),
          port = sys.env.getOrElse("POSTGRES_PORT", "5432").toInt,
          database = sys.env.getOrElse("POSTGRES_DB", "mnemocast"),
          username = sys.env.getOrElse("POSTGRES_USER", "postgres"),
          password = sys.env.getOrElse("POSTGRES_PASSWORD", "root")
        ))
      } catch {
        case ex: Exception =>
          println(s"⚠️  Warning: Failed to initialize Postgres client: ${ex.getMessage}")
          println(s"⚠️  Falling back to Redis-only mode. Set STORAGE_STRATEGY=redis to suppress this warning.")
          None
      }
    } else {
      None
    }

  private val postgresAdStoreOpt = postgresClientOpt.map(new PostgresAdStore(_))
  private val postgresEventStoreOpt = postgresClientOpt.map(new PostgresEventStore(_))
  private val postgresScreenStoreOpt = postgresClientOpt.map(new PostgresScreenStore(_))
  private val postgresCampaignStoreOpt = postgresClientOpt.map(new PostgresCampaignStore(_))
  private val postgresCreativeStoreOpt = postgresClientOpt.map(new PostgresCreativeStore(_))

  // Choose storage based on strategy (with fallback to Redis if Postgres fails)
  private val adStore: AdStore = storageStrategy match {
    case "redis" => 
      redisAdStore
    case "postgres" => 
      postgresAdStoreOpt.getOrElse {
        println("⚠️  Postgres unavailable, falling back to Redis")
        redisAdStore
      }
    case "hybrid" => 
      postgresAdStoreOpt match {
        case Some(pgStore) => new HybridAdStore(pgStore, redisAdStore)
        case None =>
          println("⚠️  Postgres unavailable for hybrid mode, using Redis-only")
          redisAdStore
      }
    case _ => 
      println(s"⚠️  Unknown storage strategy: $storageStrategy, using Redis")
      redisAdStore
  }

  private val eventStore: EventStore = storageStrategy match {
    case "redis" => 
      redisEventStore
    case "postgres" => 
      postgresEventStoreOpt.getOrElse {
        println("⚠️  Postgres unavailable, falling back to Redis")
        redisEventStore
      }
    case "hybrid" => 
      postgresEventStoreOpt match {
        case Some(pgStore) => new HybridEventStore(pgStore, redisEventStore)
        case None =>
          println("⚠️  Postgres unavailable for hybrid mode, using Redis-only")
          redisEventStore
      }
    case _ => 
      println(s"⚠️  Unknown storage strategy: $storageStrategy, using Redis")
      redisEventStore
  }

  // Screen store (using Postgres when available, with Redis cache in hybrid mode)
  private val screenStore: ScreenStore = storageStrategy match {
    case "redis" => 
      redisScreenStore
    case "postgres" => 
      postgresScreenStoreOpt.getOrElse {
        println("⚠️  Postgres unavailable, falling back to Redis")
        redisScreenStore
      }
    case "hybrid" => 
      postgresScreenStoreOpt match {
        case Some(pgStore) => new HybridScreenStore(pgStore, redisScreenStore)
        case None =>
          println("⚠️  Postgres unavailable for hybrid mode, using Redis-only")
          redisScreenStore
      }
    case _ => 
      println(s"⚠️  Unknown storage strategy: $storageStrategy, using Redis")
      redisScreenStore
  }

  private val budgetService = new BudgetService(eventStore)
  private val frequencyCapService = new FrequencyCapService(eventStore)
  private val analyticsService = new AnalyticsService(adStore, eventStore, Some(campaignStore), Some(creativeStore), Some(screenStore))

  private val adDeliveryService =
    new AdDeliveryService(adStore, eventStore, budgetService, frequencyCapService, Some(screenStore))

  private val playlistService =
    new PlaylistService(adStore, budgetService, frequencyCapService, Some(screenStore))

  private val campaignBudgetService = new CampaignBudgetService(eventStore)
  private val decisionStore: DecisionStore = redisDecisionStore // For MVP, use Redis only

  // Campaign and Creative stores (using same strategy as other stores)
  private val campaignStore: CampaignStore = storageStrategy match {
    case "redis" => redisCampaignStore
    case "postgres" => postgresCampaignStoreOpt.getOrElse(redisCampaignStore)
    case "hybrid" => postgresCampaignStoreOpt match {
      case Some(pgStore) => new HybridCampaignStore(pgStore, redisCampaignStore)
      case None => redisCampaignStore
    }
    case _ => redisCampaignStore
  }

  private val creativeStore: CreativeStore = storageStrategy match {
    case "redis" => redisCreativeStore
    case "postgres" => postgresCreativeStoreOpt.getOrElse(redisCreativeStore)
    case "hybrid" => postgresCreativeStoreOpt match {
      case Some(pgStore) => new HybridCreativeStore(pgStore, redisCreativeStore)
      case None => redisCreativeStore
    }
    case _ => redisCreativeStore
  }

  private val campaignPlaylistService =
    new CampaignPlaylistService(campaignStore, creativeStore, campaignBudgetService, decisionStore)

  // Health and Metrics services
  private val healthService = new HealthService(redisClient, postgresClientOpt, mediaStorage, serverStartTime)
  private val metricsService = new MetricsService(campaignStore, creativeStore, screenStore, serverStartTime)

  private val adRoutes = new AdRoutes(adDeliveryService)
  private val adminAdRoutes = new AdminAdRoutes(adStore, eventStore)
  private val eventRoutes = new EventRoutes(eventStore)
  private val analyticsRoutes = new AnalyticsRoutes(analyticsService)
  private val screenRoutes = new ScreenRoutes(screenStore)
  private val playlistRoutes = new PlaylistRoutes(playlistService, campaignPlaylistService, screenStore)
  private val campaignRoutes = new CampaignRoutes(campaignStore)
  private val creativeRoutes = new CreativeRoutes(creativeStore, campaignStore)
  private val healthRoutes = new HealthRoutes(healthService)
  private val metricsRoutes = new MetricsRoutes(metricsService)

  // Media storage and upload
  // Storage type: "local" or "minio" (default: "minio" for dev)
  val mediaStorageType = sys.env.getOrElse("MEDIA_STORAGE_TYPE", "minio")
  
  private val mediaStorage: MediaStorage = mediaStorageType match {
    case "minio" =>
      val minioEndpointRaw = sys.env.getOrElse("MINIO_ENDPOINT", "localhost:9000")
      val minioAccessKey = sys.env.getOrElse("MINIO_ACCESS_KEY", "minioadmin")
      val minioSecretKey = sys.env.getOrElse("MINIO_SECRET_KEY", "minioadmin")
      val minioBucket = sys.env.getOrElse("MINIO_BUCKET", "mnemocast-creatives")
      val minioUseSSL = sys.env.getOrElse("MINIO_USE_SSL", "false").toBoolean
      
      // Get server host from environment or default to localhost
      // For LAN access, set SERVER_HOST environment variable to your server's IP
      val serverHost = sys.env.getOrElse("SERVER_HOST", "localhost")
      
      // MinIO client expects endpoint without protocol, just host:port
      // Remove http:// or https:// if present
      val minioEndpoint = minioEndpointRaw
        .replaceFirst("^https?://", "")
      
      // Construct MinIO base URL for serving files
      // If MINIO_BASE_URL is explicitly set, use it
      // Otherwise, construct from SERVER_HOST (for LAN access)
      // If SERVER_HOST is localhost, use the endpoint host (may still be localhost)
      val minioBaseUrl = sys.env.get("MINIO_BASE_URL").orElse {
        // Extract host from endpoint (in case endpoint has different host than server)
        val endpointHost = {
          val cleanEndpoint = minioEndpoint.replaceFirst("^https?://", "")
          if (cleanEndpoint.contains(":")) cleanEndpoint.split(":", 2)(0) else cleanEndpoint
        }
        val finalHost = if (serverHost != "localhost") serverHost else endpointHost
        val protocol = if (minioUseSSL) "https" else "http"
        val endpointPort = if (minioEndpoint.contains(":")) {
          minioEndpoint.split(":", 2)(1).toInt
        } else {
          if (minioUseSSL) 443 else 9000
        }
        val portSuffix = if ((minioUseSSL && endpointPort == 443) || (!minioUseSSL && endpointPort == 9000)) {
          ""
        } else {
          s":$endpointPort"
        }
        Some(s"$protocol://$finalHost$portSuffix/$minioBucket")
      }
      
      println(s"📦 Media Storage: MinIO")
      println(s"   Endpoint: $minioEndpoint")
      println(s"   Bucket: $minioBucket")
      println(s"   Use SSL: $minioUseSSL")
      println(s"   Base URL: ${minioBaseUrl.getOrElse("(auto-generated from endpoint)")}")
      if (serverHost == "localhost" && !sys.env.contains("MINIO_BASE_URL")) {
        println(s"⚠️  WARNING: Using localhost for MinIO URLs. For LAN access, set SERVER_HOST environment variable.")
        println(s"⚠️  Example: export SERVER_HOST=192.168.1.100")
        println(s"⚠️  Or set MINIO_BASE_URL explicitly: export MINIO_BASE_URL=http://192.168.1.100:9000/mnemocast-creatives")
      }
      
      new MinIOStorage(
        endpoint = minioEndpoint,
        accessKey = minioAccessKey,
        secretKey = minioSecretKey,
        bucketName = minioBucket,
        useSSL = minioUseSSL,
        baseUrl = minioBaseUrl
      )
      
    case "local" =>
      val storageBasePath = sys.env.getOrElse("STORAGE_BASE_PATH", "storage/uploads")
      // Get server host from environment or default to localhost
      // For LAN access, set SERVER_HOST environment variable to your server's IP
      val serverHost = sys.env.getOrElse("SERVER_HOST", "localhost")
      val storageBaseUrl = sys.env.getOrElse("STORAGE_BASE_URL", s"http://$serverHost:8080/api/v1/media")
      
      println(s"📦 Media Storage: Local Filesystem")
      println(s"   Base Path: $storageBasePath")
      println(s"   Base URL: $storageBaseUrl")
      if (serverHost == "localhost") {
        println(s"⚠️  WARNING: Using localhost for media URLs. For LAN access, set SERVER_HOST environment variable.")
        println(s"⚠️  Example: export SERVER_HOST=192.168.1.100")
      }
      
      new LocalFileStorage(storageBasePath, storageBaseUrl)
      
    case other =>
      println(s"⚠️  Unknown media storage type: $other, falling back to local filesystem")
      val storageBasePath = sys.env.getOrElse("STORAGE_BASE_PATH", "storage/uploads")
      val serverHost = sys.env.getOrElse("SERVER_HOST", "localhost")
      val storageBaseUrl = sys.env.getOrElse("STORAGE_BASE_URL", s"http://$serverHost:8080/api/v1/media")
      if (serverHost == "localhost") {
        println(s"⚠️  WARNING: Using localhost for media URLs. For LAN access, set SERVER_HOST environment variable.")
      }
      new LocalFileStorage(storageBasePath, storageBaseUrl)
  }
  
  private val mediaValidator = new MediaValidator(
    maxImageSizeBytes = sys.env.getOrElse("MAX_IMAGE_SIZE_MB", "10").toLong * 1024 * 1024,
    maxVideoSizeBytes = sys.env.getOrElse("MAX_VIDEO_SIZE_MB", "500").toLong * 1024 * 1024
  )
  private val mediaRoutes = new MediaRoutes(mediaStorage, mediaValidator)

  // Add logging and metrics tracking directive to all routes
  private def logRequestResponse: Directive0 = {
    extractRequest.flatMap { request =>
      val startTime = System.currentTimeMillis()
      val timestamp = java.time.LocalDateTime.now()
      val method = request.method.value
      val path = request.uri.path.toString()
      val query = if (request.uri.query().isEmpty) "" else s"?${request.uri.query()}"
      
      val requestId = request.headers.find(_.is("x-request-id")).map(_.value()).getOrElse("unknown")
      val ip = request.headers.find(_.is("x-forwarded-for"))
        .map(_.value())
        .orElse(request.headers.find(_.is("remote-address")).map(_.value()))
        .getOrElse("unknown")
      
      // Structured logging (can be enhanced to JSON format)
      println(s"[$timestamp] [INFO] [RequestID:$requestId] [IP:$ip] ===> $method $path$query")
      
      mapResponse { response =>
        val endTime = System.currentTimeMillis()
        val responseTimeMs = endTime - startTime
        val status = response.status.intValue()
        val isError = status >= 400
        val logLevel = if (isError) "ERROR" else if (responseTimeMs > 1000) "WARN" else "INFO"
        
        // Record metrics (exclude metrics endpoint itself to avoid recursion)
        if (!path.contains("/api/v1/metrics")) {
          metricsService.recordRequest(path, method, responseTimeMs, isError)
        }
        
        println(s"[$timestamp] [$logLevel] [RequestID:$requestId] [IP:$ip] <=== $method $path$query -> $status (${responseTimeMs}ms)")
        response
      }
    }
  }

  // Global rejection handler to log all rejections (including JSON parsing errors)
  // Important: Rejection handlers must also include CORS headers
  implicit val rejectionHandler: org.apache.pekko.http.scaladsl.server.RejectionHandler = org.apache.pekko.http.scaladsl.server.RejectionHandler.newBuilder()
    .handle {
      case org.apache.pekko.http.scaladsl.server.MalformedRequestContentRejection(message, cause) =>
        val timestamp = java.time.LocalDateTime.now()
        println(s"[$timestamp] REJECTION: MalformedRequestContentRejection - $message")
        if (cause != null) {
          println(s"[$timestamp] REJECTION: Cause: ${cause.getClass.getName}: ${cause.getMessage}")
          cause.printStackTrace()
        }
        respondWithHeaders(
          `Access-Control-Allow-Origin`.*,
          `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
          `Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
        ) {
          complete(StatusCodes.BadRequest, s"Invalid JSON: $message")
        }
      case org.apache.pekko.http.scaladsl.server.UnsupportedRequestContentTypeRejection(supported) =>
        val timestamp = java.time.LocalDateTime.now()
        println(s"[$timestamp] REJECTION: UnsupportedRequestContentTypeRejection - Supported: ${supported.mkString(", ")}")
        respondWithHeaders(
          `Access-Control-Allow-Origin`.*,
          `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
          `Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
        ) {
          complete(StatusCodes.UnsupportedMediaType, s"Unsupported content type. Supported: ${supported.mkString(", ")}")
        }
      case rejection =>
        val timestamp = java.time.LocalDateTime.now()
        println(s"[$timestamp] REJECTION: ${rejection.getClass.getSimpleName} - $rejection")
        respondWithHeaders(
          `Access-Control-Allow-Origin`.*,
          `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
          `Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
        ) {
          complete(StatusCodes.BadRequest, s"Request rejected: ${rejection.toString}")
        }
    }
    .result()

  private val docsRoutes = new DocsRoutes()

  // Request ID tracking and rate limiting middleware can be enabled when fully tested
  // For now, using basic logging which includes request tracking
  private val allRoutes: Route = 
    logRequestResponse {
      concat(
        docsRoutes.routes,
        healthRoutes.routes,
        metricsRoutes.routes,
        adRoutes.routes,
        adminAdRoutes.routes,
        eventRoutes.routes,
        analyticsRoutes.routes,
        screenRoutes.routes,
        playlistRoutes.routes,
        campaignRoutes.routes,
        creativeRoutes.routes,
        mediaRoutes.routes
      )
    }

  // CORS support for UI dashboard - proper CORS headers for all responses
  private val routesWithCors: Route = {
    respondWithHeaders(
      `Access-Control-Allow-Origin`.*,
      `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
      `Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
    ) {
      // Handle OPTIONS preflight requests explicitly
      options {
        extractRequest { request =>
          val timestamp = java.time.LocalDateTime.now()
          val path = request.uri.path.toString()
          println(s"[$timestamp] [OPTIONS] CORS preflight for $path")
          complete(StatusCodes.OK)
        }
      } ~
      allRoutes
    }
  }

  // --- HTTP binding ---

  private val interface = "0.0.0.0"
  private val port      = 8080

  private val bindingF = Http().newServerAt(interface, port).bind(routesWithCors)

  println(s"Mnemocast Engine API running at http://$interface:$port/")
  println("Press ENTER to stop...")

  StdIn.readLine()

  bindingF
    .flatMap(_.unbind())
    .onComplete { _ =>
      system.terminate()
    }
}
