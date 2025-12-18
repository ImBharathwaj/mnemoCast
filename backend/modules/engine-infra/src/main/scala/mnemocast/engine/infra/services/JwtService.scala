package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID

import io.circe.parser._
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

/**
  * Service for JWT token generation and validation.
  */
object JwtService {
  
  private val secretKey = sys.env.getOrElse("JWT_SECRET", "change-this-secret-key-in-production")
  private val algorithm = JwtAlgorithm.HS256
  private val accessTokenExpiry = sys.env.get("JWT_ACCESS_TOKEN_EXPIRY").map(_.toInt).getOrElse(3600) // 1 hour
  private val refreshTokenExpiry = sys.env.get("JWT_REFRESH_TOKEN_EXPIRY").map(_.toInt).getOrElse(604800) // 7 days
  
  /**
    * Generate an access token for a user.
    * 
    * @param userId User ID
    * @param email User email
    * @param role User role
    * @return JWT access token
    */
  def generateAccessToken(userId: UUID, email: String, role: String): String = {
    val claim = JwtClaim(
      content = s"""{"userId":"${userId.toString}","email":"$email","role":"$role"}""",
      expiration = Some(Instant.now().plusSeconds(accessTokenExpiry).getEpochSecond),
      issuedAt = Some(Instant.now().getEpochSecond)
    )
    Jwt.encode(claim, secretKey, algorithm)
  }
  
  /**
    * Generate a refresh token for a user.
    * 
    * @param userId User ID
    * @return JWT refresh token
    */
  def generateRefreshToken(userId: UUID): String = {
    val claim = JwtClaim(
      content = s"""{"userId":"${userId.toString}","type":"refresh"}""",
      expiration = Some(Instant.now().plusSeconds(refreshTokenExpiry).getEpochSecond),
      issuedAt = Some(Instant.now().getEpochSecond)
    )
    Jwt.encode(claim, secretKey, algorithm)
  }
  
  /**
    * Validate a JWT token.
    * 
    * @param token JWT token to validate
    * @return Some(claim) if valid, None otherwise
    */
  def validateToken(token: String): Option[JwtClaim] = {
    try {
      Jwt.decode(token, secretKey, Seq(algorithm)).toOption
    } catch {
      case _: Exception => None
    }
  }
  
  /**
    * Extract user ID from a JWT claim.
    * 
    * @param claim JWT claim
    * @return Some(userId) if found, None otherwise
    */
  def extractUserId(claim: JwtClaim): Option[UUID] = {
    parse(claim.content).toOption.flatMap { json =>
      json.hcursor.get[String]("userId").toOption.flatMap { id =>
        scala.util.Try(UUID.fromString(id)).toOption
      }
    }
  }
  
  /**
    * Extract email from a JWT claim.
    * 
    * @param claim JWT claim
    * @return Some(email) if found, None otherwise
    */
  def extractEmail(claim: JwtClaim): Option[String] = {
    parse(claim.content).toOption.flatMap(_.hcursor.get[String]("email").toOption)
  }
  
  /**
    * Extract role from a JWT claim.
    * 
    * @param claim JWT claim
    * @return Some(role) if found, None otherwise
    */
  def extractRole(claim: JwtClaim): Option[String] = {
    parse(claim.content).toOption.flatMap(_.hcursor.get[String]("role").toOption)
  }
}

