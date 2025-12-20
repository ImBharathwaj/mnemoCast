package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

import mnemocast.engine.domain.model.{CreateUserRequest, LoginRequest, AuthResponse, UserInfo, User}
import mnemocast.engine.infra.store.UserStore

/**
  * Service for user authentication and authorization.
  */
class AuthService(
  userStore: UserStore
)(implicit ec: ExecutionContext) {
  
  /**
    * Register a new user.
    * 
    * @param request User registration request
    * @return Either error message or AuthResponse with token
    */
  def register(request: CreateUserRequest): Future[Either[String, AuthResponse]] = {
    // Validate email format (basic)
    if (!isValidEmail(request.email)) {
      return Future.successful(Left("Invalid email format"))
    }
    
    // Validate password length
    if (request.password.length < 8) {
      return Future.successful(Left("Password must be at least 8 characters"))
    }
    
    // Check if user exists by email
    userStore.getByEmail(request.email).flatMap {
      case Some(_) => Future.successful(Left("Email already registered"))
      case None =>
        // Check if username is taken
        userStore.getByUsername(request.username).flatMap {
          case Some(_) => Future.successful(Left("Username already taken"))
          case None =>
            // Create new user
            val user = User(
              id = UUID.randomUUID(),
              email = request.email.toLowerCase.trim,
              username = request.username.trim,
              passwordHash = PasswordService.hashPassword(request.password),
              fullName = request.fullName.map(_.trim),
              role = "user",
              isActive = true,
              emailVerified = false,
              lastLogin = None,
              createdAt = Instant.now(),
              updatedAt = Instant.now()
            )
            userStore.create(user).map { createdUser =>
              val token = JwtService.generateAccessToken(createdUser.id, createdUser.email, createdUser.role)
              Right(AuthResponse(
                token = token,
                refreshToken = Some(JwtService.generateRefreshToken(createdUser.id)),
                user = UserInfo(
                  id = createdUser.id.toString,
                  email = createdUser.email,
                  username = createdUser.username,
                  fullName = createdUser.fullName,
                  role = createdUser.role
                )
              ))
            }
        }
    }
  }
  
  /**
    * Login a user (can use email or username).
    * 
    * @param request Login request
    * @return Either error message or AuthResponse with token
    */
  def login(request: LoginRequest): Future[Either[String, AuthResponse]] = {
    val identifier = request.email.trim.toLowerCase
    
    // Try email first, then username
    userStore.getByEmail(identifier).flatMap {
      case Some(user) => authenticateUser(user, request.password)
      case None =>
        userStore.getByUsername(identifier).flatMap {
          case Some(user) => authenticateUser(user, request.password)
          case None => Future.successful(Left("Invalid email/username or password"))
        }
    }
  }
  
  /**
    * Authenticate a user with password.
    */
  private def authenticateUser(user: User, password: String): Future[Either[String, AuthResponse]] = {
    if (!user.isActive) {
      Future.successful(Left("Account is inactive"))
    } else if (!PasswordService.verifyPassword(password, user.passwordHash)) {
      Future.successful(Left("Invalid email/username or password"))
    } else {
      userStore.updateLastLogin(user.id).map { _ =>
        val token = JwtService.generateAccessToken(user.id, user.email, user.role)
        Right(AuthResponse(
          token = token,
          refreshToken = Some(JwtService.generateRefreshToken(user.id)),
          user = UserInfo(
            id = user.id.toString,
            email = user.email,
            username = user.username,
            fullName = user.fullName,
            role = user.role
          )
        ))
      }
    }
  }
  
  /**
    * Validate a JWT token and return the user.
    * 
    * @param token JWT token
    * @return Some(user) if valid, None otherwise
    */
  def validateToken(token: String): Future[Option[User]] = {
    JwtService.validateToken(token) match {
      case Some(claim) =>
        JwtService.extractUserId(claim) match {
          case Some(userId) =>
            userStore.getById(userId).map(_.filter(_.isActive))
          case None => Future.successful(None)
        }
      case None => Future.successful(None)
    }
  }
  
  /**
    * Get current user by token.
    * 
    * @param token JWT token
    * @return Some(UserInfo) if valid, None otherwise
    */
  def getCurrentUser(token: String): Future[Option[UserInfo]] = {
    validateToken(token).map(_.map { user =>
      UserInfo(
        id = user.id.toString,
        email = user.email,
        username = user.username,
        fullName = user.fullName,
        role = user.role
      )
    })
  }
  
  /**
    * Basic email validation.
    */
  private def isValidEmail(email: String): Boolean = {
    email.contains("@") && email.contains(".") && email.length > 5
  }
}

