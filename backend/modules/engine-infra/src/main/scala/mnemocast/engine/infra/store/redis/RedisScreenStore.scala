package mnemocast.engine.infra.store.redis

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.Screen
import mnemocast.engine.infra.store.ScreenStore

class RedisScreenStore(
  client: RedisClient
)(implicit ec: ExecutionContext) extends ScreenStore {

  private def screenKey(id: String): String = s"screens:$id"
  private val screensIndexKey: String = "screens:index"

  override def upsert(screen: Screen): Future[Unit] = Future {
    client.withJedis { jedis =>
      val json = screen.asJson.noSpaces
      jedis.set(screenKey(screen.id), json)
      jedis.sadd(screensIndexKey, screen.id)
      ()
    }
  }

  override def getById(id: String): Future[Option[Screen]] = Future {
    client.withJedis { jedis =>
      val value = jedis.get(screenKey(id))
      if (value == null) None
      else decode[Screen](value).toOption
    }
  }

  override def listAll(): Future[List[Screen]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val ids = jedis.smembers(screensIndexKey).asScala.toList
      if (ids.isEmpty) Nil
      else {
        val keys = ids.map(screenKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Screen](json).toOption.toList
        }
      }
    }
  }

  override def delete(id: String): Future[Unit] = Future {
    client.withJedis { jedis =>
      jedis.del(screenKey(id))
      jedis.srem(screensIndexKey, id)
      ()
    }
  }

  override def updateLastSeen(id: String): Future[Unit] = Future {
    client.withJedis { jedis =>
      val value = jedis.get(screenKey(id))
      if (value != null) {
        decode[Screen](value).foreach { screen =>
          val updated = screen.copy(
            lastSeen = Some(Instant.now()),
            isOnline = true,
            updatedAt = Instant.now()
          )
          val json = updated.asJson.noSpaces
          jedis.set(screenKey(id), json)
        }
      }
      ()
    }
  }
}

