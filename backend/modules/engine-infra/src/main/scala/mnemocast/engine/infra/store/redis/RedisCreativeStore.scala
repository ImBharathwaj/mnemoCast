package mnemocast.engine.infra.store.redis

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.Creative
import mnemocast.engine.infra.store.CreativeStore

class RedisCreativeStore(
  client: RedisClient
)(implicit ec: ExecutionContext) extends CreativeStore {

  private def creativeKey(id: String): String = s"creatives:$id"
  private val creativesIndexKey: String = "creatives:index"
  private def campaignCreativesKey(campaignId: String): String = s"campaigns:$campaignId:creatives"

  override def upsert(creative: Creative): Future[Unit] = Future {
    client.withJedis { jedis =>
      val json = creative.asJson.noSpaces
      jedis.set(creativeKey(creative.id), json)
      jedis.sadd(creativesIndexKey, creative.id)
      // Add to campaign's creative set
      jedis.sadd(campaignCreativesKey(creative.campaignId), creative.id)
      ()
    }
  }

  override def getById(id: String): Future[Option[Creative]] = Future {
    client.withJedis { jedis =>
      val value = jedis.get(creativeKey(id))
      if (value == null) None
      else decode[Creative](value).toOption
    }
  }

  override def findByCampaignId(campaignId: String): Future[List[Creative]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val creativeIds = jedis.smembers(campaignCreativesKey(campaignId)).asScala.toList
      if (creativeIds.isEmpty) Nil
      else {
        val keys = creativeIds.map(creativeKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Creative](json).toOption.toList
        }
      }
    }
  }

  override def listActive(): Future[List[Creative]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val ids = jedis.smembers(creativesIndexKey).asScala.toList
      if (ids.isEmpty) Nil
      else {
        val keys = ids.map(creativeKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Creative](json).toOption.toList
        }.filter(_.status == "active")
      }
    }
  }

  override def listAll(): Future[List[Creative]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val ids = jedis.smembers(creativesIndexKey).asScala.toList
      if (ids.isEmpty) Nil
      else {
        val keys = ids.map(creativeKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Creative](json).toOption.toList
        }
      }
    }
  }

  override def delete(id: String): Future[Unit] = Future {
    client.withJedis { jedis =>
      // Get creative to find campaign ID
      val value = jedis.get(creativeKey(id))
      if (value != null) {
        decode[Creative](value).foreach { creative =>
          // Remove from campaign's creative set
          jedis.srem(campaignCreativesKey(creative.campaignId), id)
        }
      }
      jedis.del(creativeKey(id))
      jedis.srem(creativesIndexKey, id)
      ()
    }
  }
}

