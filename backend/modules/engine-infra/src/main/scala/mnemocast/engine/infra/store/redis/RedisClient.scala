package mnemocast.engine.infra.store.redis

import redis.clients.jedis.{Jedis, JedisPool, JedisPoolConfig}

/**
  * Very small wrapper around JedisPool.
  * Default: localhost:6379
  */
class RedisClient(
  host: String = "localhost",
  port: Int = 6379
) {

  private val pool = new JedisPool(new JedisPoolConfig(), host, port)

  def withJedis[A](f: Jedis => A): A = {
    val jedis = pool.getResource
    try f(jedis)
    finally jedis.close()
  }
}
