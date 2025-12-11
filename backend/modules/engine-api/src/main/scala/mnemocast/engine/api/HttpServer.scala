package mnemocast.engine.api

import scala.concurrent.ExecutionContext
import scala.io.StdIn

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.routes.{AdRoutes, AdminAdRoutes, AnalyticsRoutes, EventRoutes}
import mnemocast.engine.infra.services.{AdDeliveryService, AnalyticsService, BudgetService, FrequencyCapService}
import mnemocast.engine.infra.store.redis.{RedisAdStore, RedisClient, RedisEventStore}
import mnemocast.engine.infra.store.postgres.{PostgresAdStore, PostgresClient, PostgresEventStore}
import mnemocast.engine.infra.store.{AdStore, EventStore, HybridAdStore, HybridEventStore}

object HttpServer extends App {

  implicit val system: ActorSystem  = ActorSystem("mnemocast-engine")
  implicit val ec: ExecutionContext = system.dispatcher

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

  private val budgetService = new BudgetService(eventStore)
  private val frequencyCapService = new FrequencyCapService(eventStore)
  private val analyticsService = new AnalyticsService(adStore, eventStore)

  private val adDeliveryService =
    new AdDeliveryService(adStore, eventStore, budgetService, frequencyCapService)

  private val adRoutes = new AdRoutes(adDeliveryService)
  private val adminAdRoutes = new AdminAdRoutes(adStore, eventStore)
  private val eventRoutes = new EventRoutes(eventStore)
  private val analyticsRoutes = new AnalyticsRoutes(analyticsService)

  private val allRoutes: Route = 
    concat(adRoutes.routes, adminAdRoutes.routes, eventRoutes.routes, analyticsRoutes.routes)

  // --- HTTP binding ---

  private val interface = "0.0.0.0"
  private val port      = 8080

  private val bindingF = Http().newServerAt(interface, port).bind(allRoutes)

  println(s"Mnemocast Engine API running at http://$interface:$port/")
  println("Press ENTER to stop...")

  StdIn.readLine()

  bindingF
    .flatMap(_.unbind())
    .onComplete { _ =>
      system.terminate()
    }
}
