package mnemocast.engine.api

import org.apache.pekko.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCode}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.{Directive0, Route}
import org.apache.pekko.http.scaladsl.server.directives.{LogEntry, LoggingMagnet}
import org.apache.pekko.http.scaladsl.model.DateTime

/**
  * Request logging utilities for API endpoints.
  */
object RequestLogging {

  /**
    * Logs incoming requests with method, path, and timestamp.
    */
  def logRequest: Directive0 = {
    extractRequest.flatMap { request =>
      val timestamp = DateTime.now.toIsoDateTimeString()
      val method = request.method.value
      val path = request.uri.path.toString()
      val query = if (request.uri.query().isEmpty) "" else s"?${request.uri.query()}"
      
      println(s"[$timestamp] $method $path$query")
      
      pass
    }
  }

  /**
    * Logs request with body (for POST/PUT requests).
    */
  def logRequestWithBody(body: String): Unit = {
    val timestamp = DateTime.now.toIsoDateTimeString()
    println(s"[$timestamp] Request body: $body")
  }

  /**
    * Logs response status.
    */
  def logResponse(method: String, path: String, status: StatusCode): Unit = {
    val timestamp = DateTime.now.toIsoDateTimeString()
    println(s"[$timestamp] $method $path -> ${status.intValue()} ${status.reason()}")
  }

  /**
    * Logs errors with stack trace.
    */
  def logError(method: String, path: String, error: Throwable): Unit = {
    val timestamp = DateTime.now.toIsoDateTimeString()
    println(s"[$timestamp] ERROR $method $path: ${error.getMessage}")
    error.printStackTrace()
  }
}

