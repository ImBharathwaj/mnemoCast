package mnemocast.engine.infra

import java.time.Instant

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext}
import scala.util.control.NonFatal

import mnemocast.engine.domain.model.{Ad, TargetingRule}
import mnemocast.engine.infra.store.redis.{RedisAdStore, RedisClient}
import mnemocast.engine.infra.store.AdStore

object SeedDemoAds extends App {

  implicit val ec: ExecutionContext =
    ExecutionContext.global

  val client: RedisClient = new RedisClient("localhost", 6379)
  val adStore: AdStore    = new RedisAdStore(client)

  val now = Instant.now()

  val demoAd = Ad(
    id             = "ad-1",
    advertiserId   = "adv-123",
    creativeUrl    = "https://example.com/creative1.jpg",
    targetUrl      = Some("https://example.com/landing"),
    targetingRules = List(
      TargetingRule("country", "eq", "IN"),
      TargetingRule("platform", "in", "android,ios")
    ),
    isActive       = true,
    createdAt      = now,
    updatedAt      = now
  )

  try {
    println("Seeding demo ad into Redis...")
    val f = adStore.upsert(demoAd)
    Await.result(f, 5.seconds)
    println("✅ Seeded demo ad with id='ad-1'")
  } catch {
    case NonFatal(e) =>
      println(s"❌ Failed to seed demo ad: ${e.getMessage}")
  }
}
