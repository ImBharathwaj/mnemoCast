package mnemocast.engine.domain.model

import io.circe.Codec
import io.circe.generic.semiauto._

/**
  * Standardized error response format.
  */
final case class ErrorCode(
  code: String,
  httpStatus: Int
)

object ErrorCode {
  implicit val errorCodeCodec: Codec[ErrorCode] = deriveCodec[ErrorCode]
  
  val NotFound = ErrorCode("NOT_FOUND", 404)
  val BadRequest = ErrorCode("BAD_REQUEST", 400)
  val Unauthorized = ErrorCode("UNAUTHORIZED", 401)
  val Forbidden = ErrorCode("FORBIDDEN", 403)
  val Conflict = ErrorCode("CONFLICT", 409)
  val InternalServerError = ErrorCode("INTERNAL_SERVER_ERROR", 500)
  val ServiceUnavailable = ErrorCode("SERVICE_UNAVAILABLE", 503)
  val TooManyRequests = ErrorCode("TOO_MANY_REQUESTS", 429)
  val ValidationError = ErrorCode("VALIDATION_ERROR", 400)
}

/**
  * Standardized error response.
  */
final case class ErrorResponse(
  error: ErrorCode,
  message: String,
  requestId: Option[String] = None,
  timestamp: String = java.time.Instant.now().toString,
  details: Option[Map[String, String]] = None
)

object ErrorResponse {
  implicit val errorResponseCodec: Codec[ErrorResponse] = deriveCodec[ErrorResponse]
  
  def create(errorCode: ErrorCode, message: String, requestId: Option[String] = None, details: Option[Map[String, String]] = None): ErrorResponse = {
    ErrorResponse(errorCode, message, requestId, java.time.Instant.now().toString, details)
  }
}

