package mnemocast.engine.domain.model

import java.time.Instant
import java.util.UUID

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * User represents an authenticated user in the system.
  *
  * @param id Unique identifier for the user
  * @param email User's email address (unique)
  * @param username User's username (unique)
  * @param passwordHash Hashed password (bcrypt)
  * @param fullName Optional full name
  * @param role User role: 'user', 'admin', 'advertiser'
  * @param isActive Whether the account is active
  * @param emailVerified Whether the email has been verified
  * @param lastLogin Timestamp of last login
  * @param createdAt When the user was created
  * @param updatedAt Last update timestamp
  */
final case class User(
  id: UUID,
  email: String,
  username: String,
  passwordHash: String,
  fullName: Option[String] = None,
  role: String = "user",
  isActive: Boolean = true,
  emailVerified: Boolean = false,
  lastLogin: Option[Instant] = None,
  createdAt: Instant,
  updatedAt: Instant
)

object User {
  implicit val userCodec: Codec[User] = deriveCodec[User]
}

/**
  * Request to create a new user (registration).
  */
final case class CreateUserRequest(
  email: String,
  username: String,
  password: String,
  fullName: Option[String] = None
)

object CreateUserRequest {
  implicit val createUserRequestCodec: Codec[CreateUserRequest] = deriveCodec[CreateUserRequest]
}

/**
  * Request to login (can use email or username).
  */
final case class LoginRequest(
  email: String,  // Can be email or username
  password: String
)

object LoginRequest {
  implicit val loginRequestCodec: Codec[LoginRequest] = deriveCodec[LoginRequest]
}

/**
  * Response after successful authentication.
  */
final case class AuthResponse(
  token: String,
  refreshToken: Option[String] = None,
  user: UserInfo
)

object AuthResponse {
  implicit val authResponseCodec: Codec[AuthResponse] = deriveCodec[AuthResponse]
}

/**
  * Public user information (without sensitive data).
  */
final case class UserInfo(
  id: String,
  email: String,
  username: String,
  fullName: Option[String],
  role: String
)

object UserInfo {
  implicit val userInfoCodec: Codec[UserInfo] = deriveCodec[UserInfo]
}

