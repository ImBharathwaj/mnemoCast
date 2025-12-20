# 🔐 Authentication & Authorization Implementation Plan

**Purpose:** Add fully functional signup and login features to Mnemocast with secure authentication, session management, and protected routes.

---

## 📋 Overview

This plan covers implementing a complete authentication system including:
- User registration (signup)
- User login
- JWT-based authentication
- Password hashing (bcrypt)
- Protected routes (frontend & backend)
- Session management
- User roles (optional for future)

---

## 🏗️ Architecture

### Backend (Scala + Pekko HTTP)
- **JWT Authentication**: Stateless token-based authentication
- **Password Security**: bcrypt for password hashing
- **User Store**: PostgreSQL for user persistence
- **Middleware**: Authentication middleware for protected routes

### Frontend (React + TypeScript)
- **Auth Context**: Global authentication state management
- **Protected Routes**: Route guards for authenticated pages
- **Login/Signup Pages**: User authentication UI
- **Token Storage**: Secure token storage (httpOnly cookies or localStorage)

---

## 📊 Database Schema

### Users Table

```sql
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email TEXT NOT NULL UNIQUE,
    username TEXT NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    full_name TEXT,
    role TEXT NOT NULL DEFAULT 'user',  -- 'user', 'admin', 'advertiser'
    is_active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    last_login TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active) WHERE is_active = true;
```

### Refresh Tokens Table (Optional - for token refresh)

```sql
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token ON refresh_tokens(token);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

---

## 🔧 Backend Implementation

### 1. Domain Models

**File:** `backend/modules/engine-domain/src/main/scala/mnemocast/engine/domain/model/User.scala`

```scala
package mnemocast.engine.domain.model

import java.time.Instant
import java.util.UUID

final case class User(
  id: UUID,
  email: String,
  username: String,
  passwordHash: String,
  fullName: Option[String],
  role: String = "user",
  isActive: Boolean = true,
  emailVerified: Boolean = false,
  lastLogin: Option[Instant] = None,
  createdAt: Instant,
  updatedAt: Instant
)

final case class CreateUserRequest(
  email: String,
  username: String,
  password: String,
  fullName: Option[String] = None
)

final case class LoginRequest(
  email: String,  // or username
  password: String
)

final case class AuthResponse(
  token: String,
  refreshToken: Option[String] = None,
  user: UserInfo
)

final case class UserInfo(
  id: String,
  email: String,
  username: String,
  fullName: Option[String],
  role: String
)
```

### 2. Password Hashing Service

**File:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/PasswordService.scala`

```scala
package mnemocast.engine.infra.services

import org.mindrot.jbcrypt.BCrypt

object PasswordService {
  def hashPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt(12))
  }
  
  def verifyPassword(password: String, hash: String): Boolean = {
    BCrypt.checkpw(password, hash)
  }
}
```

**Dependencies:** Add to `build.sbt`:
```scala
libraryDependencies += "org.mindrot" % "jbcrypt" % "0.4"
```

### 3. JWT Service

**File:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/JwtService.scala`

```scala
package mnemocast.engine.infra.services

import java.time.Instant
import java.util.{Date, UUID}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

object JwtService {
  private val secretKey = sys.env.getOrElse("JWT_SECRET", "your-secret-key-change-in-production")
  private val algorithm = JwtAlgorithm.HS256
  private val accessTokenExpiry = 3600 // 1 hour
  private val refreshTokenExpiry = 604800 // 7 days
  
  def generateAccessToken(userId: UUID, email: String, role: String): String = {
    val claim = JwtClaim(
      content = s"""{"userId":"${userId.toString}","email":"$email","role":"$role"}""",
      expiration = Some(Instant.now().plusSeconds(accessTokenExpiry).getEpochSecond),
      issuedAt = Some(Instant.now().getEpochSecond)
    )
    Jwt.encode(claim, secretKey, algorithm)
  }
  
  def generateRefreshToken(userId: UUID): String = {
    val claim = JwtClaim(
      content = s"""{"userId":"${userId.toString}","type":"refresh"}""",
      expiration = Some(Instant.now().plusSeconds(refreshTokenExpiry).getEpochSecond),
      issuedAt = Some(Instant.now().getEpochSecond)
    )
    Jwt.encode(claim, secretKey, algorithm)
  }
  
  def validateToken(token: String): Option[JwtClaim] = {
    Jwt.decode(token, secretKey, Seq(algorithm)).toOption
  }
  
  def extractUserId(claim: JwtClaim): Option[UUID] = {
    import io.circe.parser._
    parse(claim.content).toOption.flatMap(_.hcursor.get[String]("userId").toOption).flatMap { id =>
      scala.util.Try(UUID.fromString(id)).toOption
    }
  }
}
```

**Dependencies:** Add to `build.sbt`:
```scala
libraryDependencies += "com.github.jwt-scala" %% "jwt-circe" % "9.4.5"
```

### 4. User Store Interface

**File:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/UserStore.scala`

```scala
package mnemocast.engine.infra.store

import java.util.UUID
import scala.concurrent.Future
import mnemocast.engine.domain.model.User

trait UserStore {
  def create(user: User): Future[User]
  def getById(id: UUID): Future[Option[User]]
  def getByEmail(email: String): Future[Option[User]]
  def getByUsername(username: String): Future[Option[User]]
  def update(user: User): Future[User]
  def updateLastLogin(userId: UUID): Future[Unit]
}
```

### 5. PostgreSQL User Store Implementation

**File:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/store/postgres/PostgresUserStore.scala`

```scala
package mnemocast.engine.infra.store.postgres

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import mnemocast.engine.domain.model.User
import mnemocast.engine.infra.store.UserStore

class PostgresUserStore(client: PostgresClient)(implicit ec: ExecutionContext) extends UserStore {
  
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
      stmt.setTimestamp(9, java.sql.Timestamp.from(user.createdAt))
      stmt.setTimestamp(10, java.sql.Timestamp.from(user.updatedAt))
      stmt.executeUpdate()
      user
    }
  }
  
  override def getByEmail(email: String): Future[Option[User]] = Future {
    client.withConnection { conn =>
      val stmt = conn.prepareStatement("SELECT * FROM users WHERE email = ?")
      stmt.setString(1, email)
      val rs = stmt.executeQuery()
      if (rs.next()) Some(rowToUser(rs)) else None
    }
  }
  
  // Similar implementations for other methods...
  
  private def rowToUser(rs: java.sql.ResultSet): User = {
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
```

### 6. Authentication Service

**File:** `backend/modules/engine-infra/src/main/scala/mnemocast/engine/infra/services/AuthService.scala`

```scala
package mnemocast.engine.infra.services

import java.time.Instant
import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}
import mnemocast.engine.domain.model.{CreateUserRequest, LoginRequest, AuthResponse, UserInfo, User}
import mnemocast.engine.infra.store.UserStore

class AuthService(
  userStore: UserStore
)(implicit ec: ExecutionContext) {
  
  def register(request: CreateUserRequest): Future[Either[String, AuthResponse]] = {
    // Check if user exists
    userStore.getByEmail(request.email).flatMap {
      case Some(_) => Future.successful(Left("Email already registered"))
      case None =>
        userStore.getByUsername(request.username).flatMap {
          case Some(_) => Future.successful(Left("Username already taken"))
          case None =>
            val user = User(
              id = UUID.randomUUID(),
              email = request.email,
              username = request.username,
              passwordHash = PasswordService.hashPassword(request.password),
              fullName = request.fullName,
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
  
  def login(request: LoginRequest): Future[Either[String, AuthResponse]] = {
    // Try email first, then username
    userStore.getByEmail(request.email).flatMap {
      case Some(user) => authenticateUser(user, request.password)
      case None =>
        userStore.getByUsername(request.email).flatMap {
          case Some(user) => authenticateUser(user, request.password)
          case None => Future.successful(Left("Invalid email/username or password"))
        }
    }
  }
  
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
}
```

### 7. Authentication Middleware

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/middleware/AuthMiddleware.scala`

```scala
package mnemocast.engine.api.middleware

import scala.concurrent.{ExecutionContext, Future}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{AuthorizationFailedRejection, Directive1}
import org.apache.pekko.http.scaladsl.model.headers.Authorization
import org.apache.pekko.http.scaladsl.model.HttpHeader
import mnemocast.engine.domain.model.User
import mnemocast.engine.infra.services.AuthService

object AuthMiddleware {
  
  def authenticate(authService: AuthService)(implicit ec: ExecutionContext): Directive1[User] = {
    extractHeaderValueByName("Authorization").flatMap { authHeader =>
      val token = authHeader.replace("Bearer ", "")
      onComplete(authService.validateToken(token)).flatMap {
        case scala.util.Success(Some(user)) => provide(user)
        case _ => reject(AuthorizationFailedRejection)
      }
    }
  }
  
  def optionalAuth(authService: AuthService)(implicit ec: ExecutionContext): Directive1[Option[User]] = {
    optionalHeaderValueByName("Authorization").flatMap {
      case Some(authHeader) =>
        val token = authHeader.replace("Bearer ", "")
        onComplete(authService.validateToken(token)).flatMap {
          case scala.util.Success(Some(user)) => provide(Some(user))
          case _ => provide(None)
        }
      case None => provide(None)
    }
  }
  
  def requireRole(role: String)(user: User): Boolean = {
    user.role == role || user.role == "admin"
  }
}
```

### 8. Auth Routes

**File:** `backend/modules/engine-api/src/main/scala/mnemocast/engine/api/routes/AuthRoutes.scala`

```scala
package mnemocast.engine.api.routes

import scala.concurrent.ExecutionContext
import org.apache.pekko.http.scaladsl.model.StatusCodes
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.Route
import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.domain.model.{CreateUserRequest, LoginRequest}
import mnemocast.engine.infra.services.AuthService

class AuthRoutes(
  authService: AuthService
)(implicit ec: ExecutionContext) extends JsonSupport {
  
  val routes: Route =
    pathPrefix("auth") {
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
          // Protected route - requires authentication
          // Will be implemented with AuthMiddleware
          complete(StatusCodes.NotImplemented)
        }
      }
    }
}
```

---

## 🎨 Frontend Implementation

### 1. Auth Types

**File:** `dashboard/src/types/auth.ts`

```typescript
export interface User {
  id: string;
  email: string;
  username: string;
  fullName?: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface SignupRequest {
  email: string;
  username: string;
  password: string;
  fullName?: string;
}
```

### 2. Auth Service

**File:** `dashboard/src/services/authService.ts`

```typescript
import { API_BASE_URL } from '../config/api';
import { AuthResponse, LoginRequest, SignupRequest, User } from '../types/auth';

class AuthService {
  private getAuthToken(): string | null {
    return localStorage.getItem('authToken');
  }
  
  private setAuthToken(token: string): void {
    localStorage.setItem('authToken', token);
  }
  
  private removeAuthToken(): void {
    localStorage.removeItem('authToken');
  }
  
  async login(request: LoginRequest): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Login failed');
    }
    
    const data: AuthResponse = await response.json();
    this.setAuthToken(data.token);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    return data;
  }
  
  async signup(request: SignupRequest): Promise<AuthResponse> {
    const response = await fetch(`${API_BASE_URL}/api/v1/auth/register`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(request),
    });
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Signup failed');
    }
    
    const data: AuthResponse = await response.json();
    this.setAuthToken(data.token);
    if (data.refreshToken) {
      localStorage.setItem('refreshToken', data.refreshToken);
    }
    return data;
  }
  
  async logout(): Promise<void> {
    this.removeAuthToken();
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
  }
  
  async getCurrentUser(): Promise<User | null> {
    const token = this.getAuthToken();
    if (!token) return null;
    
    try {
      const response = await fetch(`${API_BASE_URL}/api/v1/auth/me`, {
        headers: {
          'Authorization': `Bearer ${token}`,
        },
      });
      
      if (!response.ok) {
        this.logout();
        return null;
      }
      
      const user: User = await response.json();
      localStorage.setItem('user', JSON.stringify(user));
      return user;
    } catch (error) {
      this.logout();
      return null;
    }
  }
  
  isAuthenticated(): boolean {
    return !!this.getAuthToken();
  }
  
  getAuthHeaders(): Record<string, string> {
    const token = this.getAuthToken();
    return token ? { 'Authorization': `Bearer ${token}` } : {};
  }
}

export const authService = new AuthService();
```

### 3. Auth Context

**File:** `dashboard/src/contexts/AuthContext.tsx`

```typescript
import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react';
import { User, LoginRequest, SignupRequest } from '../types/auth';
import { authService } from '../services/authService';

interface AuthContextType {
  user: User | null;
  loading: boolean;
  login: (request: LoginRequest) => Promise<void>;
  signup: (request: SignupRequest) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  
  useEffect(() => {
    const initAuth = async () => {
      const storedUser = localStorage.getItem('user');
      if (storedUser) {
        setUser(JSON.parse(storedUser));
      }
      
      // Verify token is still valid
      const currentUser = await authService.getCurrentUser();
      setUser(currentUser);
      setLoading(false);
    };
    
    initAuth();
  }, []);
  
  const login = async (request: LoginRequest) => {
    const response = await authService.login(request);
    setUser(response.user);
  };
  
  const signup = async (request: SignupRequest) => {
    const response = await authService.signup(request);
    setUser(response.user);
  };
  
  const logout = async () => {
    await authService.logout();
    setUser(null);
  };
  
  return (
    <AuthContext.Provider
      value={{
        user,
        loading,
        login,
        signup,
        logout,
        isAuthenticated: !!user,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
```

### 4. Protected Route Component

**File:** `dashboard/src/components/ProtectedRoute.tsx`

```typescript
import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

interface ProtectedRouteProps {
  children: React.ReactNode;
}

export const ProtectedRoute: React.FC<ProtectedRouteProps> = ({ children }) => {
  const { isAuthenticated, loading } = useAuth();
  
  if (loading) {
    return <div className="flex items-center justify-center min-h-screen">Loading...</div>;
  }
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }
  
  return <>{children}</>;
};
```

### 5. Login Page

**File:** `dashboard/src/pages/Login.tsx`

```typescript
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Login: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { login } = useAuth();
  const navigate = useNavigate();
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    
    try {
      await login({ email, password });
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Login failed');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Sign in to Mnemocast
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}
          <div className="rounded-md shadow-sm -space-y-px">
            <div>
              <label htmlFor="email" className="sr-only">
                Email or Username
              </label>
              <input
                id="email"
                name="email"
                type="text"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-t-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
                placeholder="Email or Username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div>
              <label htmlFor="password" className="sr-only">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="appearance-none rounded-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-b-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 focus:z-10 sm:text-sm"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
          </div>
          
          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {loading ? 'Signing in...' : 'Sign in'}
            </button>
          </div>
          
          <div className="text-center">
            <Link to="/signup" className="text-blue-600 hover:text-blue-800">
              Don't have an account? Sign up
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Login;
```

### 6. Signup Page

**File:** `dashboard/src/pages/Signup.tsx`

```typescript
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';

const Signup: React.FC = () => {
  const [formData, setFormData] = useState({
    email: '',
    username: '',
    password: '',
    confirmPassword: '',
    fullName: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const { signup } = useAuth();
  const navigate = useNavigate();
  
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match');
      return;
    }
    
    if (formData.password.length < 8) {
      setError('Password must be at least 8 characters');
      return;
    }
    
    setLoading(true);
    
    try {
      await signup({
        email: formData.email,
        username: formData.username,
        password: formData.password,
        fullName: formData.fullName || undefined,
      });
      navigate('/dashboard');
    } catch (err: any) {
      setError(err.message || 'Signup failed');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div>
          <h2 className="mt-6 text-center text-3xl font-extrabold text-gray-900">
            Create your account
          </h2>
        </div>
        <form className="mt-8 space-y-6" onSubmit={handleSubmit}>
          {error && (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
              {error}
            </div>
          )}
          <div className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700">
                Email
              </label>
              <input
                id="email"
                name="email"
                type="email"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="your@email.com"
                value={formData.email}
                onChange={handleChange}
              />
            </div>
            <div>
              <label htmlFor="username" className="block text-sm font-medium text-gray-700">
                Username
              </label>
              <input
                id="username"
                name="username"
                type="text"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="username"
                value={formData.username}
                onChange={handleChange}
              />
            </div>
            <div>
              <label htmlFor="fullName" className="block text-sm font-medium text-gray-700">
                Full Name (Optional)
              </label>
              <input
                id="fullName"
                name="fullName"
                type="text"
                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="John Doe"
                value={formData.fullName}
                onChange={handleChange}
              />
            </div>
            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="At least 8 characters"
                value={formData.password}
                onChange={handleChange}
              />
            </div>
            <div>
              <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700">
                Confirm Password
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                required
                className="mt-1 appearance-none relative block w-full px-3 py-2 border border-gray-300 placeholder-gray-500 text-gray-900 rounded-md focus:outline-none focus:ring-blue-500 focus:border-blue-500 sm:text-sm"
                placeholder="Re-enter password"
                value={formData.confirmPassword}
                onChange={handleChange}
              />
            </div>
          </div>
          
          <div>
            <button
              type="submit"
              disabled={loading}
              className="group relative w-full flex justify-center py-2 px-4 border border-transparent text-sm font-medium rounded-md text-white bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-blue-500 disabled:opacity-50"
            >
              {loading ? 'Creating account...' : 'Sign up'}
            </button>
          </div>
          
          <div className="text-center">
            <Link to="/login" className="text-blue-600 hover:text-blue-800">
              Already have an account? Sign in
            </Link>
          </div>
        </form>
      </div>
    </div>
  );
};

export default Signup;
```

### 7. Update App.tsx

**File:** `dashboard/src/App.tsx` (update)

```typescript
import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ProtectedRoute } from './components/ProtectedRoute';
import Layout from './components/Layout';
import Login from './pages/Login';
import Signup from './pages/Signup';
import Dashboard from './pages/Dashboard';
// ... other imports

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/signup" element={<Signup />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout>
                  <Dashboard />
                </Layout>
              </ProtectedRoute>
            }
          />
          {/* Other protected routes */}
        </Routes>
      </Router>
    </AuthProvider>
  );
}

export default App;
```

### 8. Update API Service

**File:** `dashboard/src/services/api.ts` (update)

```typescript
import { authService } from './authService';

// Add auth headers to all API calls
const getHeaders = (): HeadersInit => {
  const headers: HeadersInit = {
    'Content-Type': 'application/json',
  };
  
  const authHeaders = authService.getAuthHeaders();
  return { ...headers, ...authHeaders };
};

// Update all fetch calls to use getHeaders()
```

---

## 📝 Implementation Steps

### Phase 1: Database Setup
1. ✅ Add users table to `init.sql`
2. ✅ Add refresh_tokens table (optional)
3. ✅ Run migration script

### Phase 2: Backend Core
1. ✅ Add dependencies (jbcrypt, jwt-circe) to `build.sbt`
2. ✅ Create domain models (User, AuthRequest, AuthResponse)
3. ✅ Implement PasswordService
4. ✅ Implement JwtService
5. ✅ Create UserStore interface
6. ✅ Implement PostgresUserStore
7. ✅ Implement AuthService
8. ✅ Create AuthMiddleware
9. ✅ Create AuthRoutes
10. ✅ Wire up routes in HttpServer

### Phase 3: Frontend Core
1. ✅ Create auth types
2. ✅ Create authService
3. ✅ Create AuthContext
4. ✅ Create ProtectedRoute component
5. ✅ Create Login page
6. ✅ Create Signup page
7. ✅ Update App.tsx with routes
8. ✅ Update API service to include auth headers

### Phase 4: Integration
1. ✅ Protect backend routes with AuthMiddleware
2. ✅ Update frontend API calls to use auth headers
3. ✅ Add logout functionality
4. ✅ Add user profile display
5. ✅ Handle token expiration

### Phase 5: Testing & Polish
1. ✅ Test signup flow
2. ✅ Test login flow
3. ✅ Test protected routes
4. ✅ Test token expiration handling
5. ✅ Add error handling
6. ✅ Add loading states
7. ✅ Add form validation

---

## 🔒 Security Considerations

1. **Password Security**
   - Use bcrypt with cost factor 12
   - Enforce minimum password length (8 characters)
   - Consider password complexity requirements

2. **JWT Security**
   - Use strong secret key (environment variable)
   - Set appropriate token expiration times
   - Consider refresh token rotation

3. **API Security**
   - Validate all inputs
   - Rate limiting on auth endpoints
   - HTTPS in production
   - CORS configuration

4. **Frontend Security**
   - Store tokens securely (consider httpOnly cookies)
   - Clear tokens on logout
   - Handle token expiration gracefully

---

## 🚀 Environment Variables

### Backend
```bash
JWT_SECRET=your-super-secret-key-change-in-production
JWT_ACCESS_TOKEN_EXPIRY=3600  # 1 hour
JWT_REFRESH_TOKEN_EXPIRY=604800  # 7 days
```

### Frontend
```bash
REACT_APP_API_URL=http://localhost:8080  # Already exists
```

---

## 📚 Additional Features (Future)

1. **Email Verification**
   - Send verification email on signup
   - Verify email endpoint

2. **Password Reset**
   - Forgot password flow
   - Reset password with token

3. **User Roles & Permissions**
   - Admin dashboard
   - Role-based access control

4. **Session Management**
   - Active sessions list
   - Revoke sessions

5. **Two-Factor Authentication (2FA)**
   - TOTP-based 2FA
   - Backup codes

---

## ✅ Acceptance Criteria

- [ ] Users can register with email, username, and password
- [ ] Users can login with email/username and password
- [ ] JWT tokens are generated and validated correctly
- [ ] Protected routes require authentication
- [ ] Frontend redirects to login when not authenticated
- [ ] Tokens are stored securely
- [ ] Logout clears tokens and redirects to login
- [ ] Password is hashed with bcrypt
- [ ] Error messages are user-friendly
- [ ] Loading states are shown during auth operations

---

**Last Updated:** December 2024

