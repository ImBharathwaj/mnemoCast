package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, DriverManager, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant
import java.util.Properties

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

/**
  * PostgreSQL database client with connection pooling.
  */
class PostgresClient(
  host: String = "localhost",
  port: Int = 5432,
  database: String = "mnemocast",
  username: String = "postgres",
  password: String = "root"
) {

  private val jdbcUrl = s"jdbc:postgresql://$host:$port/$database"

  // HikariCP connection pool configuration
  private val config = new HikariConfig()
  config.setJdbcUrl(jdbcUrl)
  config.setUsername(username)
  config.setPassword(password)
  config.setMaximumPoolSize(10)
  config.setMinimumIdle(2)
  config.setConnectionTimeout(30000)
  config.setIdleTimeout(600000)
  config.setMaxLifetime(1800000)
  // Test connection on creation
  config.setConnectionTestQuery("SELECT 1")

  private val dataSource = try {
    new HikariDataSource(config)
  } catch {
    case ex: Exception =>
      throw new RuntimeException(s"Failed to initialize Postgres connection pool: ${ex.getMessage}", ex)
  }

  /**
    * Execute a query with a connection from the pool.
    */
  def withConnection[A](f: Connection => A): A = {
    val conn = dataSource.getConnection
    try {
      f(conn)
    } finally {
      conn.close()
    }
  }

  /**
    * Execute a query asynchronously.
    */
  def withConnectionAsync[A](f: Connection => A)(implicit ec: ExecutionContext): Future[A] = {
    Future {
      withConnection(f)
    }
  }

  /**
    * Execute a transaction.
    */
  def withTransaction[A](f: Connection => A)(implicit ec: ExecutionContext): Future[A] = {
    Future {
      val conn = dataSource.getConnection
      try {
        conn.setAutoCommit(false)
        try {
          val result = f(conn)
          conn.commit()
          result
        } catch {
          case e: Exception =>
            conn.rollback()
            throw e
        }
      } finally {
        conn.setAutoCommit(true)
        conn.close()
      }
    }
  }

  /**
    * Close the connection pool.
    */
  def close(): Unit = {
    dataSource.close()
  }
}

