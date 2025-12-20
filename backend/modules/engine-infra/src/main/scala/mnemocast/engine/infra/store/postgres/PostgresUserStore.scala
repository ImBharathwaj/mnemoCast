package mnemocast.engine.infra.store.postgres

import java.sql.{Connection, PreparedStatement, ResultSet, Timestamp}
import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.User
import mnemocast.engine.infra.store.UserStore

/**
  * PostgreSQL implementation of UserStore.
  * Stores users persistently in Postgres with authentication data.
  */
class PostgresUserStore(
  client: PostgresClient
)(implicit ec: ExecutionContext) extends UserStore {
  
  override def create(user: User): Future[User] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement(
        """INSERT INTO users (id, email, username, password_hash, full_name, role, is_active, email_verified, created_at, updated_at)
           VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"""
      )
      stmt.setObject(1, user.id)
      stmt.setString(2, user.email)
      stmt.setString(3, user.username)
      stmt.setString(4, user.passwordHash)
      stmt.setString(5, user.fullName.orNull)
      stmt.setString(6, user.role)
      stmt.setBoolean(7, user.isActive)
      stmt.setBoolean(8, user.emailVerified)
      stmt.setTimestamp(9, Timestamp.from(user.createdAt))
      stmt.setTimestamp(10, Timestamp.from(user.updatedAt))
      stmt.executeUpdate()
      stmt.close()
      user
    }
  }
  
  override def getById(id: UUID): Future[Option[User]] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement("SELECT * FROM users WHERE id = ?")
      stmt.setObject(1, id)
      val rs = stmt.executeQuery()
      val result = if (rs.next()) Some(rowToUser(rs)) else None
      rs.close()
      stmt.close()
      result
    }
  }
  
  override def getByEmail(email: String): Future[Option[User]] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ?")
      stmt.setString(1, email)
      val rs = stmt.executeQuery()
      val result = if (rs.next()) Some(rowToUser(rs)) else None
      rs.close()
      stmt.close()
      result
    }
  }
  
  override def getByUsername(username: String): Future[Option[User]] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement("SELECT * FROM users WHERE username = ?")
      stmt.setString(1, username)
      val rs = stmt.executeQuery()
      val result = if (rs.next()) Some(rowToUser(rs)) else None
      rs.close()
      stmt.close()
      result
    }
  }
  
  override def update(user: User): Future[User] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement(
        """UPDATE users SET
           email = ?, username = ?, password_hash = ?, full_name = ?, role = ?,
           is_active = ?, email_verified = ?, last_login = ?, updated_at = ?
           WHERE id = ?"""
      )
      stmt.setString(1, user.email)
      stmt.setString(2, user.username)
      stmt.setString(3, user.passwordHash)
      stmt.setString(4, user.fullName.orNull)
      stmt.setString(5, user.role)
      stmt.setBoolean(6, user.isActive)
      stmt.setBoolean(7, user.emailVerified)
      stmt.setTimestamp(8, user.lastLogin.map(Timestamp.from).orNull)
      stmt.setTimestamp(9, Timestamp.from(user.updatedAt))
      stmt.setObject(10, user.id)
      stmt.executeUpdate()
      stmt.close()
      user
    }
  }
  
  override def updateLastLogin(userId: UUID): Future[Unit] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement(
        """UPDATE users SET last_login = ?, updated_at = ? WHERE id = ?"""
      )
      val now = Instant.now()
      stmt.setTimestamp(1, Timestamp.from(now))
      stmt.setTimestamp(2, Timestamp.from(now))
      stmt.setObject(3, userId)
      stmt.executeUpdate()
      stmt.close()
    }
  }
  
  private def rowToUser(rs: ResultSet): User = {
    User(
      id = rs.getObject("id", classOf[UUID]),
      email = rs.getString("email"),
      username = rs.getString("username"),
      passwordHash = rs.getString("password_hash"),
      fullName = Option(rs.getString("full_name")),
      role = rs.getString("role"),
      isActive = rs.getBoolean("is_active"),
      emailVerified = rs.getBoolean("email_verified"),
      lastLogin = Option(rs.getTimestamp("last_login")).map(_.toInstant),
      createdAt = rs.getTimestamp("created_at").toInstant,
      updatedAt = rs.getTimestamp("updated_at").toInstant
    )
  }
}

