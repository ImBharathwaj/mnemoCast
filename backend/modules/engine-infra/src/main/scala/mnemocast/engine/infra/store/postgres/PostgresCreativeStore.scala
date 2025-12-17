package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.Creative
import mnemocast.engine.infra.store.CreativeStore

/**
  * PostgreSQL implementation of CreativeStore.
  * Stores creatives persistently in Postgres with metadata.
  */
class PostgresCreativeStore(
  client: PostgresClient
)(implicit ec: ExecutionContext) extends CreativeStore {

  override def upsert(creative: Creative): Future[Unit] = {
    client.withTransaction { conn =>
      // Upsert creative
      val creativeStmt = conn.prepareStatement("""
        INSERT INTO creatives (
          id, campaign_id, name, creative_type, creative_url, target_url,
          duration_seconds, status, share_of_voice, frequency_cap_per_screen,
          created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          campaign_id = EXCLUDED.campaign_id,
          name = EXCLUDED.name,
          creative_type = EXCLUDED.creative_type,
          creative_url = EXCLUDED.creative_url,
          target_url = EXCLUDED.target_url,
          duration_seconds = EXCLUDED.duration_seconds,
          status = EXCLUDED.status,
          share_of_voice = EXCLUDED.share_of_voice,
          frequency_cap_per_screen = EXCLUDED.frequency_cap_per_screen,
          updated_at = EXCLUDED.updated_at
      """)

      creativeStmt.setString(1, creative.id)
      creativeStmt.setString(2, creative.campaignId)
      creativeStmt.setString(3, creative.name)
      creativeStmt.setString(4, creative.creativeType)
      creativeStmt.setString(5, creative.creativeUrl)
      creativeStmt.setString(6, creative.targetUrl.orNull)
      creativeStmt.setInt(7, creative.durationSeconds)
      creativeStmt.setString(8, creative.status)
      creativeStmt.setObject(9, creative.shareOfVoice.map(_.asInstanceOf[Object]).orNull, java.sql.Types.NUMERIC)
      creativeStmt.setObject(10, creative.frequencyCapPerScreen.map(_.asInstanceOf[Object]).orNull, java.sql.Types.INTEGER)
      creativeStmt.setTimestamp(11, Timestamp.from(creative.createdAt))
      creativeStmt.setTimestamp(12, Timestamp.from(creative.updatedAt))
      creativeStmt.executeUpdate()
      creativeStmt.close()

      // Delete existing metadata
      val deleteMetaStmt = conn.prepareStatement("DELETE FROM creative_metadata WHERE creative_id = ?")
      deleteMetaStmt.setString(1, creative.id)
      deleteMetaStmt.executeUpdate()
      deleteMetaStmt.close()

      // Insert new metadata
      if (creative.metadata.nonEmpty) {
        val metaStmt = conn.prepareStatement("INSERT INTO creative_metadata (creative_id, metadata_key, metadata_value) VALUES (?, ?, ?)")
        creative.metadata.foreach { case (key, value) =>
          metaStmt.setString(1, creative.id)
          metaStmt.setString(2, key)
          metaStmt.setString(3, value)
          metaStmt.addBatch()
        }
        metaStmt.executeBatch()
        metaStmt.close()
      }
    }
  }

  override def getById(id: String): Future[Option[Creative]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, campaign_id, name, creative_type, creative_url, target_url,
               duration_seconds, status, share_of_voice, frequency_cap_per_screen,
               created_at, updated_at
        FROM creatives WHERE id = ?
      """)
      stmt.setString(1, id)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        val creative = rowToCreative(rs)
        stmt.close()

        // Load metadata
        val metadata = loadMetadata(conn, id)
        Some(creative.copy(metadata = metadata))
      } else {
        stmt.close()
        None
      }
    }
  }

  override def findByCampaignId(campaignId: String): Future[List[Creative]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, campaign_id, name, creative_type, creative_url, target_url,
               duration_seconds, status, share_of_voice, frequency_cap_per_screen,
               created_at, updated_at
        FROM creatives WHERE campaign_id = ?
        ORDER BY created_at ASC
      """)
      stmt.setString(1, campaignId)
      val rs = stmt.executeQuery()
      val creatives = Iterator.continually(rs).takeWhile(_.next()).map(rowToCreative).toList
      stmt.close()

      // Load metadata for all creatives
      creatives.map { creative =>
        val metadata = loadMetadata(conn, creative.id)
        creative.copy(metadata = metadata)
      }
    }
  }

  override def listActive(): Future[List[Creative]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, campaign_id, name, creative_type, creative_url, target_url,
               duration_seconds, status, share_of_voice, frequency_cap_per_screen,
               created_at, updated_at
        FROM creatives WHERE status = 'active'
        ORDER BY created_at DESC
      """)
      val rs = stmt.executeQuery()
      val creatives = Iterator.continually(rs).takeWhile(_.next()).map(rowToCreative).toList
      stmt.close()

      // Load metadata for all creatives
      creatives.map { creative =>
        val metadata = loadMetadata(conn, creative.id)
        creative.copy(metadata = metadata)
      }
    }
  }

  override def listAll(): Future[List[Creative]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, campaign_id, name, creative_type, creative_url, target_url,
               duration_seconds, status, share_of_voice, frequency_cap_per_screen,
               created_at, updated_at
        FROM creatives
        ORDER BY created_at DESC
      """)
      val rs = stmt.executeQuery()
      val creatives = Iterator.continually(rs).takeWhile(_.next()).map(rowToCreative).toList
      stmt.close()

      // Load metadata for all creatives
      creatives.map { creative =>
        val metadata = loadMetadata(conn, creative.id)
        creative.copy(metadata = metadata)
      }
    }
  }

  override def delete(id: String): Future[Unit] = {
    client.withTransaction { conn =>
      val stmt = conn.prepareStatement("DELETE FROM creatives WHERE id = ?")
      stmt.setString(1, id)
      stmt.executeUpdate()
      stmt.close()
    }
  }

  private def rowToCreative(rs: ResultSet): Creative = {
    Creative(
      id = rs.getString("id"),
      campaignId = rs.getString("campaign_id"),
      name = rs.getString("name"),
      creativeType = rs.getString("creative_type"),
      creativeUrl = rs.getString("creative_url"),
      targetUrl = Option(rs.getString("target_url")),
      durationSeconds = rs.getInt("duration_seconds"),
      status = rs.getString("status"),
      shareOfVoice = Option(rs.getObject("share_of_voice")).map(_.asInstanceOf[java.math.BigDecimal].doubleValue()),
      frequencyCapPerScreen = Option(rs.getObject("frequency_cap_per_screen")).map(_.asInstanceOf[Int]),
      metadata = Map.empty, // Loaded separately
      createdAt = rs.getTimestamp("created_at").toInstant,
      updatedAt = rs.getTimestamp("updated_at").toInstant
    )
  }

  private def loadMetadata(conn: Connection, creativeId: String): Map[String, String] = {
    val stmt = conn.prepareStatement("SELECT metadata_key, metadata_value FROM creative_metadata WHERE creative_id = ?")
    stmt.setString(1, creativeId)
    val rs = stmt.executeQuery()
    val metadata = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      rs.getString("metadata_key") -> rs.getString("metadata_value")
    }.toMap
    stmt.close()
    metadata
  }
}

