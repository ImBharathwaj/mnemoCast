package mnemocast.engine.infra.store.redis

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.Decision
import mnemocast.engine.infra.store.DecisionStore

class RedisDecisionStore(
  client: RedisClient
)(implicit ec: ExecutionContext) extends DecisionStore {

  private def decisionKey(decisionId: String): String = s"decisions:$decisionId"
  private val decisionsIndexKey: String = "decisions:index"
  private def screenDecisionsKey(screenId: String): String = s"decisions:screen:$screenId"
  private val recentDecisionsKey: String = "decisions:recent"

  override def append(decision: Decision): Future[Unit] = Future {
    client.withJedis { jedis =>
      val json = decision.asJson.noSpaces
      jedis.set(decisionKey(decision.decisionId), json)
      jedis.sadd(decisionsIndexKey, decision.decisionId)
      
      // Add to screen's decision list
      jedis.lpush(screenDecisionsKey(decision.screenId), decision.decisionId)
      
      // Add to recent decisions list (keep last 1000)
      jedis.lpush(recentDecisionsKey, decision.decisionId)
      jedis.ltrim(recentDecisionsKey, 0, 999)
      
      ()
    }
  }

  override def findByScreenId(screenId: String, limit: Int): Future[List[Decision]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val end = math.max(0, limit - 1).toLong
      val decisionIds = jedis.lrange(screenDecisionsKey(screenId), 0L, end).asScala.toList
      
      if (decisionIds.isEmpty) Nil
      else {
        val keys = decisionIds.map(decisionKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Decision](json).toOption.toList
        }
      }
    }
  }

  override def getById(decisionId: String): Future[Option[Decision]] = Future {
    client.withJedis { jedis =>
      val value = jedis.get(decisionKey(decisionId))
      if (value == null) None
      else decode[Decision](value).toOption
    }
  }

  override def listRecent(limit: Int): Future[List[Decision]] = Future {
    client.withJedis { jedis =>
      import scala.jdk.CollectionConverters._

      val end = math.max(0, limit - 1).toLong
      val decisionIds = jedis.lrange(recentDecisionsKey, 0L, end).asScala.toList
      
      if (decisionIds.isEmpty) Nil
      else {
        val keys = decisionIds.map(decisionKey)
        val values = jedis.mget(keys: _*).asScala.toList
        values.flatMap { json =>
          if (json == null) Nil else decode[Decision](json).toOption.toList
        }
      }
    }
  }
}

