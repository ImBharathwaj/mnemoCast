package mnemocast.engine.infra.store

import java.util.UUID
import scala.concurrent.Future

import mnemocast.engine.domain.model.User

/**
  * Abstraction over user storage.
  * Implementations can be in-memory, Redis, Postgres, etc.
  */
trait UserStore {
  
  /** Create a new user. */
  def create(user: User): Future[User]
  
  /** Fetch a user by ID. */
  def getById(id: UUID): Future[Option[User]]
  
  /** Fetch a user by email. */
  def getByEmail(email: String): Future[Option[User]]
  
  /** Fetch a user by username. */
  def getByUsername(username: String): Future[Option[User]]
  
  /** Update an existing user. */
  def update(user: User): Future[User]
  
  /** Update the last login timestamp for a user. */
  def updateLastLogin(userId: UUID): Future[Unit]
}

