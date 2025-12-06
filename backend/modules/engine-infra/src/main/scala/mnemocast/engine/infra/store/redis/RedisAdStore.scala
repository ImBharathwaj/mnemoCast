package mnemocast.engine.infra.store.redis

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.Ad
import mnemocast.engine.infra.store.AdStore

class RedisAdStore(
  client: RedisClient
)(implicit ec: ExecutionContext) extends AdStore {

  private def adKey(id: String): String = s"ads:$id"
  private val adsIndexKey: String       = "ads:index"

  override def upsert(ad: Ad): Future[Unit] = Future {
    client.withJedis { jedis =>
      val json = ad.asJson.noSpaces
      jedis.set(adKey(ad.id), json)
      jedis.sadd(adsIndexKey, ad.id)
      ()
    }
  }

  override def getById(id: String): Future[Option[Ad]] = Future {
    client.withJedis { jedis =>
      val value = jedis.get(adKey(id))
      if (value == null) None
      else decode[Ad](value).toOption
    }
  }

  override def listActive(): Future[List[Ad]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val ids = jedis.smembers(adsIndexKey).asScala.toList
      if (ids.isEmpty) Nil
      else {
        val keys   = ids.map(adKey)
        val values = jedis.mget(keys: _*).asScala.toList
        val ads    = values.flatMap { json =>
          if (json == null) Nil else decode[Ad](json).toOption.toList
        }
        ads.filter(_.isActive)
      }
    }
  }

  override def delete(id: String): Future[Unit] = Future {
    client.withJedis { jedis =>
      jedis.del(adKey(id))
      jedis.srem(adsIndexKey, id)
      ()
    }
  }
}
