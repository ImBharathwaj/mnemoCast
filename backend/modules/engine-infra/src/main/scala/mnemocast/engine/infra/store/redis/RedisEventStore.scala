package mnemocast.engine.infra.store.redis

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.DeliveryEvent
import mnemocast.engine.infra.store.EventStore

class RedisEventStore(
  client: RedisClient
)(implicit ec: ExecutionContext) extends EventStore {

  // We keep events per adId in a Redis list
  private def eventsKey(adId: String): String =
    s"events:ad:$adId"

  override def append(event: DeliveryEvent): Future[Unit] = Future {
    client.withJedis { jedis =>
      val json = event.asJson.noSpaces
      jedis.lpush(eventsKey(event.adId), json)
      ()
    }
  }

  override def findByAdId(adId: String, limit: Int): Future[List[DeliveryEvent]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val end   = math.max(0, limit - 1).toLong
      val items = jedis.lrange(eventsKey(adId), 0L, end).asScala.toList

      items.flatMap { json =>
        decode[DeliveryEvent](json).toOption.toList
      }
    }
  }
}
