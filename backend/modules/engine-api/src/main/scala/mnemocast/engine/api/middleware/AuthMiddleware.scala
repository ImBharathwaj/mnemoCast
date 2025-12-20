package mnemocast.engine.api.middleware

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import mnemocast.engine.domain.model.User
import mnemocast.engine.infra.services.AuthService

/**
  * Authentication middleware for protecting routes.
  */
object AuthMiddleware {
  
  /**
    * Require authentication - extracts User from JWT token.
    * Rejects with AuthorizationFailedRejection if token is invalid or missing.
    */
  def authenticate(authService: AuthService)(implicit ec: ExecutionContext): Directive1[User] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7) // Remove "Bearer " prefix
        onComplete(authService.validateToken(token)).flatMap {
          case scala.util.Success(Some(user)) => provide(user)
          case _ => reject(AuthorizationFailedRejection)
        }
      case _ => reject(AuthorizationFailedRejection)
    }
  }
  
  /**
    * Optional authentication - extracts User if token is present and valid.
    * Returns None if token is missing or invalid.
    */
  def optionalAuth(authService: AuthService)(implicit ec: ExecutionContext): Directive1[Option[User]] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) if authHeader.startsWith("Bearer ") =>
        val token = authHeader.substring(7)
        onComplete(authService.validateToken(token)).flatMap {
          case scala.util.Success(Some(user)) => provide(Some(user))
          case _ => provide(None)
        }
      case _ => provide(None)
    }
  }
  
  /**
    * Require a specific role (or admin).
    */
  def requireRole(role: String)(user: User): Boolean = {
    user.role == role || user.role == "admin"
  }
}

