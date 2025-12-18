package mnemocast.engine.infra.store.redis

import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.Campaign
import mnemocast.engine.infra.store.CampaignStore

class RedisCampaignStore(
  client: RedisClient
)(implicit ec: ExecutionContext) extends CampaignStore {

  private def campaignKey(id: String): String = s"campaigns:$id"
  private val campaignsIndexKey: String = "campaigns:index"

  override def upsert(campaign: Campaign): Future[Unit] = Future {
    client.withJedis { jedis =>
      val json = campaign.asJson.noSpaces
      jedis.set(campaignKey(campaign.id), json)
      jedis.sadd(campaignsIndexKey, campaign.id)
      ()
    }
  }

  override def getById(id: String): Future[Option[Campaign]] = Future {
    client.withJedis { jedis =>
      val value = jedis.get(campaignKey(id))
      if (value == null) None
      else decode[Campaign](value).toOption
    }
  }

  override def listActive(): Future[List[Campaign]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val ids = jedis.smembers(campaignsIndexKey).asScala.toList
      if (ids.isEmpty) Nil
      else {
        val keys = ids.map(campaignKey)
        val values = jedis.mget(keys: _*).asScala.toList
        val campaigns = values.flatMap { json =>
          if (json == null) Nil else decode[Campaign](json).toOption.toList
        }
        // Filter active campaigns and check date range
        val now = Instant.now()
        campaigns.filter { c =>
          c.status == "active" &&
          !c.startDate.isAfter(now) &&
          !c.endDate.isBefore(now)
        }
      }
    }
  }

  override def listAll(): Future[List[Campaign]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val ids = jedis.smembers(campaignsIndexKey).asScala.toList
      if (ids.isEmpty) Nil
      else {
        val keys = ids.map(campaignKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Campaign](json).toOption.toList
        }
      }
    }
  }

  override def delete(id: String): Future[Unit] = Future {
    client.withJedis { jedis =>
      jedis.del(campaignKey(id))
      jedis.srem(campaignsIndexKey, id)
      ()
    }
  }
}

