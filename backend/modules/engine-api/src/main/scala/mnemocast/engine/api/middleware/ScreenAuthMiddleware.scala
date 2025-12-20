package mnemocast.engine.api.middleware

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import mnemocast.engine.domain.model.Screen
import mnemocast.engine.infra.store.ScreenStore

/**
  * Authentication middleware for screen clients.
  * 
  * Screens authenticate using:
  * - X-Screen-Id header: The screen ID
  * - X-Screen-Passkey header: The authentication passkey
  * 
  * This is separate from user authentication and is designed for device/IoT authentication.
  */
object ScreenAuthMiddleware {
  
  /**
    * Require screen authentication - extracts Screen from headers.
    * Rejects with AuthorizationFailedRejection if screen ID or passkey is invalid.
    */
  def authenticate(screenStore: ScreenStore)(implicit ec: ExecutionContext): Directive1[Screen] = {
    extractRequest.flatMap { request =>
      // Try both lowercase and case-insensitive header matching
      val screenIdOpt = request.headers.find(h => h.is("x-screen-id") || h.lowercaseName() == "x-screen-id").map(_.value())
      val passkeyOpt = request.headers.find(h => h.is("x-screen-passkey") || h.lowercaseName() == "x-screen-passkey").map(_.value())
      
      val timestamp = java.time.LocalDateTime.now()
      
      (screenIdOpt, passkeyOpt) match {
        case (Some(screenId), Some(passkey)) =>
          println(s"[$timestamp] [ScreenAuth] Attempting authentication for screen: $screenId")
          onComplete(screenStore.getById(screenId)).flatMap {
            case scala.util.Success(Some(screen)) if screen.passkey == passkey =>
              println(s"[$timestamp] [ScreenAuth] Authentication successful for screen: $screenId (${screen.name})")
              provide(screen)
            case scala.util.Success(Some(screen)) =>
              // Screen exists but passkey doesn't match
              println(s"[$timestamp] [ScreenAuth] Authentication failed: Passkey mismatch for screen: $screenId")
              println(s"[$timestamp] [ScreenAuth] Expected passkey length: ${screen.passkey.length}, Provided passkey length: ${passkey.length}")
              reject(AuthorizationFailedRejection)
            case scala.util.Success(None) =>
              // Screen not found
              println(s"[$timestamp] [ScreenAuth] Authentication failed: Screen not found: $screenId")
              reject(AuthorizationFailedRejection)
            case scala.util.Failure(ex) =>
              // Error fetching screen
              println(s"[$timestamp] [ScreenAuth] Authentication failed: Error fetching screen $screenId: ${ex.getMessage}")
              ex.printStackTrace()
              reject(AuthorizationFailedRejection)
          }
        case (None, _) =>
          // Missing screen ID header
          println(s"[$timestamp] [ScreenAuth] Authentication failed: Missing X-Screen-Id header")
          println(s"[$timestamp] [ScreenAuth] Available headers: ${request.headers.map(_.name()).mkString(", ")}")
          reject(AuthorizationFailedRejection)
        case (_, None) =>
          // Missing passkey header
          println(s"[$timestamp] [ScreenAuth] Authentication failed: Missing X-Screen-Passkey header")
          println(s"[$timestamp] [ScreenAuth] Available headers: ${request.headers.map(_.name()).mkString(", ")}")
          reject(AuthorizationFailedRejection)
      }
    }
  }
  
  /**
    * Optional screen authentication - extracts Screen if headers are present and valid.
    * Returns None if headers are missing or invalid.
    */
  def optionalAuth(screenStore: ScreenStore)(implicit ec: ExecutionContext): Directive1[Option[Screen]] = {
    extractRequest.flatMap { request =>
      val screenIdOpt = request.headers.find(_.is("x-screen-id")).map(_.value())
      val passkeyOpt = request.headers.find(_.is("x-screen-passkey")).map(_.value())
      
      (screenIdOpt, passkeyOpt) match {
        case (Some(screenId), Some(passkey)) =>
          onComplete(screenStore.getById(screenId)).flatMap {
            case scala.util.Success(Some(screen)) if screen.passkey == passkey =>
              provide(Some(screen))
            case _ =>
              provide(None)
          }
        case _ =>
          provide(None)
      }
    }
  }
}

