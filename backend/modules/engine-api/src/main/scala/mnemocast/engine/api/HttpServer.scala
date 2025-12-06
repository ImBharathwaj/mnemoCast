package mnemocast.engine.api

import scala.concurrent.ExecutionContext
import scala.io.StdIn

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.routes.AdRoutes
import mnemocast.engine.infra.services.AdDeliveryService
import mnemocast.engine.infra.store.redis.{RedisAdStore, RedisClient, RedisEventStore}
import mnemocast.engine.infra.store.{AdStore, EventStore}

object HttpServer extends App {

  implicit val system: ActorSystem  = ActorSystem("mnemocast-engine")
  implicit val ec: ExecutionContext = system.dispatcher

  // --- Infra wiring (manual DI) ---

  private val redisClient = new RedisClient("localhost", 6379)

  private val adStore: AdStore =
    new RedisAdStore(redisClient)

  private val eventStore: EventStore =
    new RedisEventStore(redisClient)

  private val adDeliveryService =
    new AdDeliveryService(adStore, eventStore)

  private val adRoutes = new AdRoutes(adDeliveryService)

  private val allRoutes: Route = adRoutes.routes

  // --- HTTP binding ---

  private val interface = "0.0.0.0"
  private val port      = 8080

  private val bindingF = Http().newServerAt(interface, port).bind(allRoutes)

  println(s"✅ Mnemocast Engine API running at http://$interface:$port/")
  println("Press ENTER to stop...")

  StdIn.readLine()

  bindingF
    .flatMap(_.unbind())
    .onComplete { _ =>
      system.terminate()
    }
}
