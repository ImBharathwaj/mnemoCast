package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.{Campaign, TargetingRule}
import mnemocast.engine.infra.store.CampaignStore

/**
  * PostgreSQL implementation of CampaignStore.
  * Stores campaigns persistently in Postgres with targeting rules.
  */
class PostgresCampaignStore(
  client: PostgresClient
)(implicit ec: ExecutionContext) extends CampaignStore {

  override def upsert(campaign: Campaign): Future[Unit] = {
    client.withTransaction { conn =>
      // Upsert campaign
      val campaignStmt = conn.prepareStatement("""
        INSERT INTO campaigns (
          id, name, advertiser_id, status, start_date, end_date,
          total_budget, target_playouts, priority, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          name = EXCLUDED.name,
          advertiser_id = EXCLUDED.advertiser_id,
          status = EXCLUDED.status,
          start_date = EXCLUDED.start_date,
          end_date = EXCLUDED.end_date,
          total_budget = EXCLUDED.total_budget,
          target_playouts = EXCLUDED.target_playouts,
          priority = EXCLUDED.priority,
          updated_at = EXCLUDED.updated_at
      """)

      campaignStmt.setString(1, campaign.id)
      campaignStmt.setString(2, campaign.name)
      campaignStmt.setString(3, campaign.advertiserId)
      campaignStmt.setString(4, campaign.status)
      campaignStmt.setTimestamp(5, Timestamp.from(campaign.startDate))
      campaignStmt.setTimestamp(6, Timestamp.from(campaign.endDate))
      campaignStmt.setObject(7, campaign.totalBudget.map(_.asInstanceOf[Object]).orNull, java.sql.Types.BIGINT)
      campaignStmt.setObject(8, campaign.targetPlayouts.map(_.asInstanceOf[Object]).orNull, java.sql.Types.BIGINT)
      campaignStmt.setInt(9, campaign.priority)
      campaignStmt.setTimestamp(10, Timestamp.from(campaign.createdAt))
      campaignStmt.setTimestamp(11, Timestamp.from(campaign.updatedAt))
      campaignStmt.executeUpdate()
      campaignStmt.close()

      // Delete existing targeting rules for this campaign
      val deleteStmt = conn.prepareStatement("DELETE FROM targeting_rules WHERE campaign_id = ?")
      deleteStmt.setString(1, campaign.id)
      deleteStmt.executeUpdate()
      deleteStmt.close()

      // Insert new targeting rules
      if (campaign.targetingRules.nonEmpty) {
        val ruleStmt = conn.prepareStatement("""
          INSERT INTO targeting_rules (campaign_id, rule_key, operator, rule_value)
          VALUES (?, ?, ?, ?)
        """)
        campaign.targetingRules.foreach { rule =>
          ruleStmt.setString(1, campaign.id)
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

  override def getById(id: String): Future[Option[Campaign]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, name, advertiser_id, status, start_date, end_date,
               total_budget, target_playouts, priority, created_at, updated_at
        FROM campaigns WHERE id = ?
      """)
      stmt.setString(1, id)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        val campaign = rowToCampaign(rs)
        stmt.close()

        // Load targeting rules
        val rules = loadTargetingRules(conn, id)
        Some(campaign.copy(targetingRules = rules))
      } else {
        stmt.close()
        None
      }
    }
  }

  override def listActive(): Future[List[Campaign]] = {
    client.withConnectionAsync { conn =>
      val now = Instant.now()
      val stmt = conn.prepareStatement("""
        SELECT id, name, advertiser_id, status, start_date, end_date,
               total_budget, target_playouts, priority, created_at, updated_at
        FROM campaigns
        WHERE status = 'active'
          AND start_date <= ?
          AND end_date >= ?
        ORDER BY priority DESC, created_at DESC
      """)
      stmt.setTimestamp(1, Timestamp.from(now))
      stmt.setTimestamp(2, Timestamp.from(now))
      val rs = stmt.executeQuery()
      val campaigns = Iterator.continually(rs).takeWhile(_.next()).map(rowToCampaign).toList
      stmt.close()

      // Load targeting rules for all campaigns
      campaigns.map { campaign =>
        val rules = loadTargetingRules(conn, campaign.id)
        campaign.copy(targetingRules = rules)
      }
    }
  }

  override def listAll(): Future[List[Campaign]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, name, advertiser_id, status, start_date, end_date,
               total_budget, target_playouts, priority, created_at, updated_at
        FROM campaigns
        ORDER BY created_at DESC
      """)
      val rs = stmt.executeQuery()
      val campaigns = Iterator.continually(rs).takeWhile(_.next()).map(rowToCampaign).toList
      stmt.close()

      // Load targeting rules for all campaigns
      campaigns.map { campaign =>
        val rules = loadTargetingRules(conn, campaign.id)
        campaign.copy(targetingRules = rules)
      }
    }
  }

  override def delete(id: String): Future[Unit] = {
    client.withTransaction { conn =>
      val stmt = conn.prepareStatement("DELETE FROM campaigns WHERE id = ?")
      stmt.setString(1, id)
      stmt.executeUpdate()
      stmt.close()
    }
  }

  private def rowToCampaign(rs: ResultSet): Campaign = {
    Campaign(
      id = rs.getString("id"),
      name = rs.getString("name"),
      advertiserId = rs.getString("advertiser_id"),
      status = rs.getString("status"),
      startDate = rs.getTimestamp("start_date").toInstant,
      endDate = rs.getTimestamp("end_date").toInstant,
      totalBudget = Option(rs.getObject("total_budget")).map(_.asInstanceOf[Long]),
      targetPlayouts = Option(rs.getObject("target_playouts")).map(_.asInstanceOf[Long]),
      targetingRules = Nil, // Loaded separately
      priority = rs.getInt("priority"),
      createdAt = rs.getTimestamp("created_at").toInstant,
      updatedAt = rs.getTimestamp("updated_at").toInstant
    )
  }

  private def loadTargetingRules(conn: Connection, campaignId: String): List[TargetingRule] = {
    val stmt = conn.prepareStatement("SELECT rule_key, operator, rule_value FROM targeting_rules WHERE campaign_id = ?")
    stmt.setString(1, campaignId)
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

