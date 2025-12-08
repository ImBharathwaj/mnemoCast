package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant

import scala.concurrent.{ExecutionContext, Future}

import io.circe.parser.decode
import io.circe.syntax._

import mnemocast.engine.domain.model.DeliveryEvent
import mnemocast.engine.infra.store.EventStore

/**
  * PostgreSQL implementation of EventStore.
  * Stores events persistently in Postgres for analytics and historical queries.
  */
class PostgresEventStore(
  client: PostgresClient
)(implicit ec: ExecutionContext) extends EventStore {

  override def append(event: DeliveryEvent): Future[Unit] = {
    client.withTransaction { conn =>
      // Insert event
      val eventStmt = conn.prepareStatement("""
        INSERT INTO delivery_events (event_id, request_id, ad_id, event_type, occurred_at)
        VALUES (?, ?, ?, ?, ?)
        ON CONFLICT (event_id) DO NOTHING
      """)
      eventStmt.setString(1, event.eventId)
      eventStmt.setString(2, event.requestId)
      eventStmt.setString(3, event.adId)
      eventStmt.setString(4, event.eventType)
      eventStmt.setTimestamp(5, Timestamp.from(event.occurredAt))
      eventStmt.executeUpdate()
      eventStmt.close()

      // Get the inserted event's database ID
      val getIdStmt = conn.prepareStatement("SELECT id FROM delivery_events WHERE event_id = ?")
      getIdStmt.setString(1, event.eventId)
      val rs = getIdStmt.executeQuery()
      if (rs.next()) {
        val dbEventId = rs.getObject("id", classOf[java.util.UUID])
        getIdStmt.close()

        // Insert metadata
        if (event.metadata.nonEmpty) {
          val metaStmt = conn.prepareStatement("""
            INSERT INTO event_metadata (event_id, metadata_key, metadata_value)
            VALUES (?, ?, ?)
            ON CONFLICT (event_id, metadata_key) DO UPDATE SET metadata_value = EXCLUDED.metadata_value
          """)
          event.metadata.foreach { case (key, value) =>
            metaStmt.setObject(1, dbEventId)
            metaStmt.setString(2, key)
            metaStmt.setString(3, value)
            metaStmt.addBatch()
          }
          metaStmt.executeBatch()
          metaStmt.close()
        }
      } else {
        getIdStmt.close()
      }
    }
  }

  override def findByAdId(adId: String, limit: Int): Future[List[DeliveryEvent]] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT e.event_id, e.request_id, e.ad_id, e.event_type, e.occurred_at
        FROM delivery_events e
        WHERE e.ad_id = ?
        ORDER BY e.occurred_at DESC
        LIMIT ?
      """)
      stmt.setString(1, adId)
      stmt.setInt(2, limit)
      val rs = stmt.executeQuery()
      val events = Iterator.continually(rs).takeWhile(_.next()).map(rowToEvent(conn, _)).toList
      stmt.close()
      events
    }
  }

  override def countImpressionsByAdId(adId: String, since: Instant): Future[Int] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT COUNT(*) FROM delivery_events
        WHERE ad_id = ? AND event_type = 'impression' AND occurred_at >= ?
      """)
      stmt.setString(1, adId)
      stmt.setTimestamp(2, Timestamp.from(since))
      val rs = stmt.executeQuery()
      val count = if (rs.next()) rs.getInt(1) else 0
      stmt.close()
      count
    }
  }

  override def countImpressionsByAdIdAndDevice(adId: String, deviceId: String, since: Instant): Future[Int] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT COUNT(DISTINCT e.id)
        FROM delivery_events e
        INNER JOIN event_metadata m ON e.id = m.event_id
        WHERE e.ad_id = ? 
          AND e.event_type = 'impression'
          AND e.occurred_at >= ?
          AND m.metadata_key = 'deviceId'
          AND m.metadata_value = ?
      """)
      stmt.setString(1, adId)
      stmt.setTimestamp(2, Timestamp.from(since))
      stmt.setString(3, deviceId)
      val rs = stmt.executeQuery()
      val count = if (rs.next()) rs.getInt(1) else 0
      stmt.close()
      count
    }
  }

  override def countImpressionsByAdIdAndUser(adId: String, userId: String, since: Instant): Future[Int] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT COUNT(DISTINCT e.id)
        FROM delivery_events e
        INNER JOIN event_metadata m ON e.id = m.event_id
        WHERE e.ad_id = ? 
          AND e.event_type = 'impression'
          AND e.occurred_at >= ?
          AND m.metadata_key = 'userId'
          AND m.metadata_value = ?
      """)
      stmt.setString(1, adId)
      stmt.setTimestamp(2, Timestamp.from(since))
      stmt.setString(3, userId)
      val rs = stmt.executeQuery()
      val count = if (rs.next()) rs.getInt(1) else 0
      stmt.close()
      count
    }
  }

  override def countTotalImpressionsByAdId(adId: String): Future[Int] = {
    client.withConnectionAsync { conn =>
      val stmt = conn.prepareStatement("""
        SELECT COUNT(*) FROM delivery_events
        WHERE ad_id = ? AND event_type = 'impression'
      """)
      stmt.setString(1, adId)
      val rs = stmt.executeQuery()
      val count = if (rs.next()) rs.getInt(1) else 0
      stmt.close()
      count
    }
  }

  private def rowToEvent(conn: Connection, rs: ResultSet): DeliveryEvent = {
    val eventId = rs.getString("event_id")
    val dbEventId = {
      val getIdStmt = conn.prepareStatement("SELECT id FROM delivery_events WHERE event_id = ?")
      getIdStmt.setString(1, eventId)
      val getIdRs = getIdStmt.executeQuery()
      val id = if (getIdRs.next()) Some(getIdRs.getObject("id", classOf[java.util.UUID])) else None
      getIdStmt.close()
      id
    }

    // Load metadata
    val metadata = dbEventId match {
      case Some(id) =>
        val metaStmt = conn.prepareStatement("SELECT metadata_key, metadata_value FROM event_metadata WHERE event_id = ?")
        metaStmt.setObject(1, id)
        val metaRs = metaStmt.executeQuery()
        val meta = Iterator.continually(metaRs).takeWhile(_.next()).map { rs =>
          rs.getString("metadata_key") -> rs.getString("metadata_value")
        }.toMap
        metaStmt.close()
        meta
      case None => Map.empty[String, String]
    }

    DeliveryEvent(
      eventId = eventId,
      requestId = rs.getString("request_id"),
      adId = rs.getString("ad_id"),
      eventType = rs.getString("event_type"),
      occurredAt = rs.getTimestamp("occurred_at").toInstant,
      metadata = metadata
    )
  }
}

