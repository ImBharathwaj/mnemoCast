package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.{Ad, TargetingRule}
import mnemocast.engine.infra.store.AdStore

/**
  * PostgreSQL implementation of AdStore.
  * Stores ads persistently in Postgres, with targeting rules in separate table.
  */
class PostgresAdStore(
  client: PostgresClient
)(implicit ec: ExecutionContext) extends AdStore {

  override def upsert(ad: Ad): Future[Unit] = {
    client.withTransaction { conn =>
      // Upsert ad
      val adStmt = conn.prepareStatement("""
        INSERT INTO ads (
          id, advertiser_id, creative_url, target_url, is_active,
          max_plays, daily_limit, hourly_limit,
          max_impressions_per_device, max_impressions_per_user, frequency_cap_window_hours,
          duration_seconds, weight, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          advertiser_id = EXCLUDED.advertiser_id,
          creative_url = EXCLUDED.creative_url,
          target_url = EXCLUDED.target_url,
          is_active = EXCLUDED.is_active,
          max_plays = EXCLUDED.max_plays,
          daily_limit = EXCLUDED.daily_limit,
          hourly_limit = EXCLUDED.hourly_limit,
          max_impressions_per_device = EXCLUDED.max_impressions_per_device,
          max_impressions_per_user = EXCLUDED.max_impressions_per_user,
          frequency_cap_window_hours = EXCLUDED.frequency_cap_window_hours,
          duration_seconds = EXCLUDED.duration_seconds,
          weight = EXCLUDED.weight,
          updated_at = EXCLUDED.updated_at
      """)

      adStmt.setString(1, ad.id)
      adStmt.setString(2, ad.advertiserId)
      adStmt.setString(3, ad.creativeUrl)
      adStmt.setString(4, ad.targetUrl.orNull)
      adStmt.setBoolean(5, ad.isActive)
      adStmt.setObject(6, ad.maxPlays.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setObject(7, ad.dailyLimit.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setObject(8, ad.hourlyLimit.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setObject(9, ad.maxImpressionsPerDevice.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setObject(10, ad.maxImpressionsPerUser.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setObject(11, ad.frequencyCapWindowHours.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setObject(12, ad.durationSeconds.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      adStmt.setInt(13, ad.weight)
      adStmt.setTimestamp(14, Timestamp.from(ad.createdAt))
      adStmt.setTimestamp(15, Timestamp.from(ad.updatedAt))
      adStmt.executeUpdate()
      adStmt.close()

      // Delete existing targeting rules
      val deleteStmt = conn.prepareStatement("DELETE FROM targeting_rules WHERE ad_id = ?")
      deleteStmt.setString(1, ad.id)
      deleteStmt.executeUpdate()
      deleteStmt.close()

      // Insert new targeting rules
      if (ad.targetingRules.nonEmpty) {
        val ruleStmt = conn.prepareStatement("""
          INSERT INTO targeting_rules (ad_id, rule_key, operator, rule_value)
          VALUES (?, ?, ?, ?)
        """)
        ad.targetingRules.foreach { rule =>
          ruleStmt.setString(1, ad.id)
          ruleStmt.setString(2, rule.key)
          ruleStmt.setString(3, rule.operator)
          ruleStmt.setString(4, rule.value)
          ruleStmt.addBatch()
        }
        ruleStmt.executeBatch()
        ruleStmt.close()
      }
    }
  }

  override def getById(id: String): Future[Option[Ad]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, advertiser_id, creative_url, target_url, is_active,
               max_plays, daily_limit, hourly_limit,
               max_impressions_per_device, max_impressions_per_user, frequency_cap_window_hours,
               duration_seconds, weight, created_at, updated_at
        FROM ads WHERE id = ?
      """)
      stmt.setString(1, id)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        val ad = rowToAd(rs)
        stmt.close()
        
        // Load targeting rules
        val rules = loadTargetingRules(conn, id)
        Some(ad.copy(targetingRules = rules))
      } else {
        stmt.close()
        None
      }
    }
  }

  override def listActive(): Future[List[Ad]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, advertiser_id, creative_url, target_url, is_active,
               max_plays, daily_limit, hourly_limit,
               max_impressions_per_device, max_impressions_per_user, frequency_cap_window_hours,
               duration_seconds, weight, created_at, updated_at
        FROM ads WHERE is_active = true
        ORDER BY created_at DESC
      """)
      val rs = stmt.executeQuery()
      val ads = Iterator.continually(rs).takeWhile(_.next()).map(rowToAd).toList
      stmt.close()

      // Load targeting rules for all ads
      ads.map { ad =>
        val rules = loadTargetingRules(conn, ad.id)
        ad.copy(targetingRules = rules)
      }
    }
  }

  override def listAll(): Future[List[Ad]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, advertiser_id, creative_url, target_url, is_active,
               max_plays, daily_limit, hourly_limit,
               max_impressions_per_device, max_impressions_per_user, frequency_cap_window_hours,
               duration_seconds, weight, created_at, updated_at
        FROM ads
        ORDER BY created_at DESC
      """)
      val rs = stmt.executeQuery()
      val ads = Iterator.continually(rs).takeWhile(_.next()).map(rowToAd).toList
      stmt.close()

      // Load targeting rules for all ads
      ads.map { ad =>
        val rules = loadTargetingRules(conn, ad.id)
        ad.copy(targetingRules = rules)
      }
    }
  }

  override def delete(id: String): Future[Unit] = {
    client.withTransaction { conn =>
      val stmt = conn.prepareStatement("DELETE FROM ads WHERE id = ?")
      stmt.setString(1, id)
      stmt.executeUpdate()
      stmt.close()
    }
  }

  private def rowToAd(rs: ResultSet): Ad = {
    def getNullableInt(column: String): Option[Int] = {
      val value = rs.getObject(column)
      if (value == null) None else Some(rs.getInt(column))
    }

    Ad(
      id = rs.getString("id"),
      advertiserId = rs.getString("advertiser_id"),
      creativeUrl = rs.getString("creative_url"),
      targetUrl = Option(rs.getString("target_url")),
      targetingRules = Nil, // Loaded separately
      isActive = rs.getBoolean("is_active"),
      maxPlays = getNullableInt("max_plays"),
      dailyLimit = getNullableInt("daily_limit"),
      hourlyLimit = getNullableInt("hourly_limit"),
      maxImpressionsPerDevice = getNullableInt("max_impressions_per_device"),
      maxImpressionsPerUser = getNullableInt("max_impressions_per_user"),
      frequencyCapWindowHours = getNullableInt("frequency_cap_window_hours"),
      durationSeconds = getNullableInt("duration_seconds"),
      weight = rs.getInt("weight"),
      createdAt = rs.getTimestamp("created_at").toInstant,
      updatedAt = rs.getTimestamp("updated_at").toInstant
    )
  }

  private def loadTargetingRules(conn: Connection, adId: String): List[TargetingRule] = {
    val stmt = conn.prepareStatement("SELECT rule_key, operator, rule_value FROM targeting_rules WHERE ad_id = ?")
    stmt.setString(1, adId)
    val rs = stmt.executeQuery()
    val rules = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      TargetingRule(
        key = rs.getString("rule_key"),
        operator = rs.getString("operator"),
        value = rs.getString("rule_value")
      )
    }.toList
    stmt.close()
    rules
  }
}

