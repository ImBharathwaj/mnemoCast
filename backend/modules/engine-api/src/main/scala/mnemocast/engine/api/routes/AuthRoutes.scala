package mnemocast.engine.api.routes

import scala.concurrent.ExecutionContext

import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.AuthMiddleware
import mnemocast.engine.domain.model.{CreateUserRequest, LoginRequest}
import mnemocast.engine.infra.services.AuthService

/**
  * Authentication routes for user registration and login.
  */
class AuthRoutes(
  authService: AuthService
)(implicit ec: ExecutionContext) extends JsonSupport {
  
  val routes: Route =
    pathPrefix("api" / "v1" / "auth") {
      path("register") {
        post {
          entity(as[CreateUserRequest]) { request =>
            onComplete(authService.register(request)) {
              case scala.util.Success(Right(response)) =>
                complete(StatusCodes.Created, response)
              case scala.util.Success(Left(error)) =>
                complete(StatusCodes.BadRequest, Map("error" -> error))
              case scala.util.Failure(ex) =>
                complete(StatusCodes.InternalServerError, Map("error" -> ex.getMessage))
            }
          }
        }
      } ~
      path("login") {
        post {
          entity(as[LoginRequest]) { request =>
            onComplete(authService.login(request)) {
              case scala.util.Success(Right(response)) =>
                complete(response)
              case scala.util.Success(Left(error)) =>
                complete(StatusCodes.Unauthorized, Map("error" -> error))
              case scala.util.Failure(ex) =>
                complete(StatusCodes.InternalServerError, Map("error" -> ex.getMessage))
            }
          }
        }
      } ~
      path("me") {
        get {
          AuthMiddleware.authenticate(authService).apply { user =>
            complete(Map(
              "id" -> user.id.toString,
              "email" -> user.email,
              "username" -> user.username,
              "fullName" -> user.fullName.getOrElse(""),
              "role" -> user.role
            ))
          }
        }
      }
    }
}

