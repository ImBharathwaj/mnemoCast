package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.{Screen, ScreenLocation}
import mnemocast.engine.infra.store.ScreenStore

/**
  * PostgreSQL implementation of ScreenStore.
  * Stores screens persistently in Postgres with location, tags, and metadata.
  */
class PostgresScreenStore(
  client: PostgresClient
)(implicit ec: ExecutionContext) extends ScreenStore {

  override def upsert(screen: Screen): Future[Unit] = {
    client.withTransaction { conn =>
      // Upsert screen
      val screenStmt = conn.prepareStatement("""
        INSERT INTO screens (
          id, name, country, city, area, venue_type, timezone,
          is_online, last_seen, classification, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        ON CONFLICT (id) DO UPDATE SET
          name = EXCLUDED.name,
          country = EXCLUDED.country,
          city = EXCLUDED.city,
          area = EXCLUDED.area,
          venue_type = EXCLUDED.venue_type,
          timezone = EXCLUDED.timezone,
          is_online = EXCLUDED.is_online,
          last_seen = EXCLUDED.last_seen,
          classification = EXCLUDED.classification,
          updated_at = EXCLUDED.updated_at
      """)

      screenStmt.setString(1, screen.id)
      screenStmt.setString(2, screen.name)
      screenStmt.setString(3, screen.location.country.orNull)
      screenStmt.setString(4, screen.location.city.orNull)
      screenStmt.setString(5, screen.location.area.orNull)
      screenStmt.setString(6, screen.location.venueType.orNull)
      screenStmt.setString(7, screen.location.timezone.orNull)
      screenStmt.setBoolean(8, screen.isOnline)
      screenStmt.setTimestamp(9, screen.lastSeen.map(Timestamp.from).orNull)
      screenStmt.setInt(10, screen.classification)
      screenStmt.setTimestamp(11, Timestamp.from(screen.createdAt))
      screenStmt.setTimestamp(12, Timestamp.from(screen.updatedAt))
      screenStmt.executeUpdate()
      screenStmt.close()

      // Delete existing tags
      val deleteTagsStmt = conn.prepareStatement("DELETE FROM screen_tags WHERE screen_id = ?")
      deleteTagsStmt.setString(1, screen.id)
      deleteTagsStmt.executeUpdate()
      deleteTagsStmt.close()

      // Insert new tags
      if (screen.tags.nonEmpty) {
        val tagStmt = conn.prepareStatement("INSERT INTO screen_tags (screen_id, tag) VALUES (?, ?)")
        screen.tags.foreach { tag =>
          tagStmt.setString(1, screen.id)
          tagStmt.setString(2, tag)
          tagStmt.addBatch()
        }
        tagStmt.executeBatch()
        tagStmt.close()
      }

      // Delete existing metadata
      val deleteMetaStmt = conn.prepareStatement("DELETE FROM screen_metadata WHERE screen_id = ?")
      deleteMetaStmt.setString(1, screen.id)
      deleteMetaStmt.executeUpdate()
      deleteMetaStmt.close()

      // Insert new metadata
      if (screen.metadata.nonEmpty) {
        val metaStmt = conn.prepareStatement("INSERT INTO screen_metadata (screen_id, metadata_key, metadata_value) VALUES (?, ?, ?)")
        screen.metadata.foreach { case (key, value) =>
          metaStmt.setString(1, screen.id)
          metaStmt.setString(2, key)
          metaStmt.setString(3, value)
          metaStmt.addBatch()
        }
        metaStmt.executeBatch()
        metaStmt.close()
      }
    }
  }

  override def getById(id: String): Future[Option[Screen]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, name, country, city, area, venue_type, timezone,
               is_online, last_seen, classification, created_at, updated_at
        FROM screens WHERE id = ?
      """)
      stmt.setString(1, id)
      val rs = stmt.executeQuery()

      if (rs.next()) {
        val screen = rowToScreen(rs)
        stmt.close()

        // Load tags and metadata
        val tags = loadTags(conn, id)
        val metadata = loadMetadata(conn, id)
        Some(screen.copy(tags = tags, metadata = metadata))
      } else {
        stmt.close()
        None
      }
    }
  }

  override def listAll(): Future[List[Screen]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT id, name, country, city, area, venue_type, timezone,
               is_online, last_seen, classification, created_at, updated_at
        FROM screens
        ORDER BY created_at DESC
      """)
      val rs = stmt.executeQuery()
      val screens = Iterator.continually(rs).takeWhile(_.next()).map(rowToScreen).toList
      stmt.close()

      // Load tags and metadata for all screens
      screens.map { screen =>
        val tags = loadTags(conn, screen.id)
        val metadata = loadMetadata(conn, screen.id)
        screen.copy(tags = tags, metadata = metadata)
      }
    }
  }

  override def delete(id: String): Future[Unit] = {
    client.withTransaction { conn =>
      // Delete tags and metadata first (cascade will handle it, but explicit is cleaner)
      val deleteTagsStmt = conn.prepareStatement("DELETE FROM screen_tags WHERE screen_id = ?")
      deleteTagsStmt.setString(1, id)
      deleteTagsStmt.executeUpdate()
      deleteTagsStmt.close()

      val deleteMetaStmt = conn.prepareStatement("DELETE FROM screen_metadata WHERE screen_id = ?")
      deleteMetaStmt.setString(1, id)
      deleteMetaStmt.executeUpdate()
      deleteMetaStmt.close()

      // Delete screen
      val stmt = conn.prepareStatement("DELETE FROM screens WHERE id = ?")
      stmt.setString(1, id)
      stmt.executeUpdate()
      stmt.close()
    }
  }

  override def updateLastSeen(id: String): Future[Unit] = {
    client.withTransaction { conn =>
      val stmt = conn.prepareStatement("""
        UPDATE screens
        SET last_seen = ?, is_online = true, updated_at = ?
        WHERE id = ?
      """)
      val now = Instant.now()
      stmt.setTimestamp(1, Timestamp.from(now))
      stmt.setTimestamp(2, Timestamp.from(now))
      stmt.setString(3, id)
      stmt.executeUpdate()
      stmt.close()
    }
  }

  private def rowToScreen(rs: ResultSet): Screen = {
    val location = ScreenLocation(
      country = Option(rs.getString("country")),
      city = Option(rs.getString("city")),
      area = Option(rs.getString("area")),
      venueType = Option(rs.getString("venue_type")),
      timezone = Option(rs.getString("timezone"))
    )

    Screen(
      id = rs.getString("id"),
      name = rs.getString("name"),
      location = location,
      tags = Nil, // Loaded separately
      metadata = Map.empty, // Loaded separately
      classification = rs.getInt("classification"),
      isOnline = rs.getBoolean("is_online"),
      lastSeen = Option(rs.getTimestamp("last_seen")).map(_.toInstant),
      createdAt = rs.getTimestamp("created_at").toInstant,
      updatedAt = rs.getTimestamp("updated_at").toInstant
    )
  }

  private def loadTags(conn: Connection, screenId: String): List[String] = {
    val stmt = conn.prepareStatement("SELECT tag FROM screen_tags WHERE screen_id = ? ORDER BY tag")
    stmt.setString(1, screenId)
    val rs = stmt.executeQuery()
    val tags = Iterator.continually(rs).takeWhile(_.next()).map(_.getString("tag")).toList
    stmt.close()
    tags
  }

  private def loadMetadata(conn: Connection, screenId: String): Map[String, String] = {
    val stmt = conn.prepareStatement("SELECT metadata_key, metadata_value FROM screen_metadata WHERE screen_id = ?")
    stmt.setString(1, screenId)
    val rs = stmt.executeQuery()
    val metadata = Iterator.continually(rs).takeWhile(_.next()).map { rs =>
      rs.getString("metadata_key") -> rs.getString("metadata_value")
    }.toMap
    stmt.close()
    metadata
  }
}

