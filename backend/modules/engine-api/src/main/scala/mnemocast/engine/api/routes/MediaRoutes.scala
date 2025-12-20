package mnemocast.engine.api.routes

import java.io.File

import scala.concurrent.{ExecutionContext, Future}

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.model.headers.`Access-Control-Allow-Headers`
import org.apache.pekko.http.scaladsl.model.headers.`Access-Control-Allow-Methods`
import org.apache.pekko.http.scaladsl.model.headers.`Access-Control-Allow-Origin`
import org.apache.pekko.http.scaladsl.model.{ContentType, HttpEntity, HttpResponse, HttpMethods, MediaTypes, StatusCodes}
import org.apache.pekko.http.scaladsl.server.{Directives, ExceptionHandler, RejectionHandler, Route}
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.stream.scaladsl.{FileIO, Sink}
import org.apache.pekko.stream.{ActorMaterializer, Materializer}
import org.apache.pekko.util.ByteString

import mnemocast.engine.api.json.JsonSupport
import mnemocast.engine.api.middleware.AuthMiddleware
import mnemocast.engine.domain.model.MediaUploadResponse
import mnemocast.engine.infra.services.{AuthService, MediaValidator}
import mnemocast.engine.infra.storage.MediaStorage

/**
  * Media file upload and serving routes.
  * 
  * POST /api/v1/creatives/upload - Upload a media file (protected)
  * GET /api/v1/media/creatives/{filename} - Serve a media file (public)
  */
class MediaRoutes(
  mediaStorage: MediaStorage,
  mediaValidator: MediaValidator,
  authServiceOpt: Option[AuthService] = None
)(implicit ec: ExecutionContext, system: ActorSystem) extends JsonSupport {
  
  // Helper to protect routes with authentication
  private def requireAuth(route: Route): Route = {
    authServiceOpt match {
      case Some(authService) => AuthMiddleware.authenticate(authService).apply { _ => route }
      case None => route // If auth is not available, allow access (backward compatibility)
    }
  }

  implicit val materializer: Materializer = ActorMaterializer()

  // Helper to get content type from filename
  private def getContentType(filename: String): ContentType = {
    val lowerName = filename.toLowerCase
    if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
      ContentType(MediaTypes.`image/jpeg`)
    } else if (lowerName.endsWith(".png")) {
      ContentType(MediaTypes.`image/png`)
    } else if (lowerName.endsWith(".gif")) {
      ContentType(MediaTypes.`image/gif`)
    } else if (lowerName.endsWith(".webp")) {
      ContentType(MediaTypes.`image/webp`)
    } else if (lowerName.endsWith(".mp4")) {
      ContentType(MediaTypes.`video/mp4`)
    } else if (lowerName.endsWith(".webm")) {
      ContentType(MediaTypes.`video/webm`)
    } else {
      ContentType(MediaTypes.`application/octet-stream`)
    }
  }

  val routes: Route =
    concat(
      // File upload endpoint (protected)
      pathPrefix("api" / "v1" / "creatives") {
        path("upload") {
          post {
            requireAuth {
              extractRequest { httpRequest =>
              val timestamp = java.time.LocalDateTime.now()
              println(s"[$timestamp] [POST /api/v1/creatives/upload] Request received")
              println(s"[$timestamp] [POST /api/v1/creatives/upload] Content-Type: ${httpRequest.entity.contentType}")
              
              handleRejections(RejectionHandler.newBuilder()
                .handleAll[org.apache.pekko.http.scaladsl.server.Rejection] { rejections =>
                  val rejectTimestamp = java.time.LocalDateTime.now()
                  println(s"[$rejectTimestamp] [POST /api/v1/creatives/upload] REJECTIONS: ${rejections.size} rejection(s)")
                  rejections.foreach { rejection =>
                    println(s"[$rejectTimestamp] [POST /api/v1/creatives/upload] REJECTION: ${rejection.getClass.getSimpleName} - $rejection")
                  }
                  respondWithHeaders(
                    `Access-Control-Allow-Origin`.*,
                    `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
                    `Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
                  ) {
                    complete(StatusCodes.BadRequest, s"Request rejected: ${rejections.mkString(", ")}")
                  }
                }
                .result()) {
                handleExceptions(ExceptionHandler {
                  case ex: Exception =>
                    val errorTimestamp = java.time.LocalDateTime.now()
                    println(s"[$errorTimestamp] [POST /api/v1/creatives/upload] EXCEPTION: ${ex.getClass.getName}: ${ex.getMessage}")
                    ex.printStackTrace()
                    respondWithHeaders(
                      `Access-Control-Allow-Origin`.*,
                      `Access-Control-Allow-Methods`(HttpMethods.GET, HttpMethods.POST, HttpMethods.PUT, HttpMethods.DELETE, HttpMethods.OPTIONS),
                      `Access-Control-Allow-Headers`("Content-Type", "Authorization", "X-Requested-With")
                    ) {
                      complete(StatusCodes.InternalServerError, s"Error: ${ex.getMessage}")
                    }
                }) {
                  // Handle multipart form data - extract all parts
                  entity(as[org.apache.pekko.http.scaladsl.model.Multipart.FormData]) { formData =>
                println(s"[$timestamp] [POST /api/v1/creatives/upload] Multipart form data received, processing parts...")
                // Process all parts to extract file and form fields
                val resultFuture = formData.parts
                  .mapAsync(1) { part =>
                    part.name match {
                      case "file" =>
                        // Handle file part
                        val contentType = part.entity.contentType.mediaType.toString()
                        println(s"[$timestamp] [POST /api/v1/creatives/upload] Processing file part, content type: $contentType")
                        
                        // Create temporary file
                        val tempFile = File.createTempFile("upload_", ".tmp")
                        tempFile.deleteOnExit()
                        
                        // Write file to temp location
                        part.entity.dataBytes
                          .runWith(FileIO.toPath(tempFile.toPath))
                          .map(_ => {
                            println(s"[$timestamp] [POST /api/v1/creatives/upload] File written to temp: ${tempFile.getAbsolutePath}, size: ${tempFile.length()}")
                            ("file", (tempFile, contentType))
                          })
                          
                      case "campaignId" | "creativeId" =>
                        // Handle text field
                        part.entity.dataBytes
                          .runFold(ByteString.empty)(_ ++ _)
                          .map(_.utf8String)
                          .map(value => (part.name, value))
                          
                      case _ =>
                        Future.successful((part.name, ()))
                    }
                  }
                  .runWith(Sink.seq)
                  .flatMap { parts =>
                    println(s"[$timestamp] [POST /api/v1/creatives/upload] Processed ${parts.length} parts")
                    
                    // Extract values from parts
                    val filePart = parts.collectFirst { case ("file", (f: File, ct: String)) => (f, ct) }
                    val campaignId = parts.collectFirst { case ("campaignId", id: String) => id }
                    val creativeId = parts.collectFirst { case ("creativeId", id: String) => id }
                    
                    filePart match {
                      case Some((tempFile, contentType)) =>
                        println(s"[$timestamp] [POST /api/v1/creatives/upload] File extracted, validating...")
                        
                        // Validate file
                        mediaValidator.validateFile(tempFile, contentType).flatMap { validationResult =>
                          if (!validationResult.isValid) {
                            tempFile.delete()
                            println(s"[$timestamp] [POST /api/v1/creatives/upload] Validation failed: ${validationResult.errors.mkString(", ")}")
                            Future.failed(new IllegalArgumentException(s"File validation failed: ${validationResult.errors.mkString(", ")}"))
                          } else {
                            println(s"[$timestamp] [POST /api/v1/creatives/upload] File validated, uploading to storage...")
                            // Upload to storage
                            mediaStorage.upload(tempFile, creativeId, contentType).map { url =>
                              // Clean up temp file
                              tempFile.delete()
                              
                              // Extract path from URL for response
                              val path = if (url.contains("/media/")) {
                                url.substring(url.indexOf("/media/") + "/media/".length)
                              } else {
                                url
                              }
                              
                              println(s"[$timestamp] [POST /api/v1/creatives/upload] Upload successful, URL: $url")
                              
                              MediaUploadResponse(
                                url = url,
                                path = path,
                                contentType = contentType,
                                size = validationResult.metadata.map(_.size).getOrElse(tempFile.length()),
                                duration = validationResult.metadata.flatMap(_.duration)
                              )
                            }
                          }
                        }
                      case None =>
                        println(s"[$timestamp] [POST /api/v1/creatives/upload] No file part found in request")
                        Future.failed(new IllegalArgumentException("No file provided in request"))
                    }
                  }

                onComplete(resultFuture) {
                  case scala.util.Success(response) =>
                    println(s"[$timestamp] [POST /api/v1/creatives/upload] Request completed successfully: ${response.url}")
                    complete(response)
                  case scala.util.Failure(ex: IllegalArgumentException) =>
                    println(s"[$timestamp] [POST /api/v1/creatives/upload] Validation/Request error: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.BadRequest, ex.getMessage)
                  case scala.util.Failure(ex) =>
                    println(s"[$timestamp] [POST /api/v1/creatives/upload] Unexpected error: ${ex.getClass.getName}: ${ex.getMessage}")
                    ex.printStackTrace()
                    complete(StatusCodes.InternalServerError, s"Failed to upload file: ${ex.getMessage}")
                }
                  }
                }
              }
            }
            }
          }
        }
      },
      // File serving endpoint
      pathPrefix("api" / "v1" / "media" / "creatives") {
        path(Segment) { filename =>
          get {
            val timestamp = java.time.LocalDateTime.now()
            println(s"[$timestamp] [GET /api/v1/media/creatives/$filename] Request received")
            
            // Construct URL to look up file
            val url = mediaStorage.getUrl(s"creatives/$filename")
            println(s"[$timestamp] [GET /api/v1/media/creatives/$filename] Looking up file at URL: $url")
            
            onComplete(mediaStorage.getFile(url)) {
              case scala.util.Success(Some(file)) =>
                println(s"[$timestamp] [GET /api/v1/media/creatives/$filename] File found: ${file.getAbsolutePath}")
                
                val contentType = getContentType(file.getName)
                
                // Serve file with appropriate content type and caching headers
                val response = HttpResponse(
                  entity = HttpEntity.fromPath(contentType, file.toPath),
                  headers = List(
                    org.apache.pekko.http.scaladsl.model.headers.`Cache-Control`(
                      org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.`public`,
                      org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.`max-age`(3600)
                    )
                  )
                )
                complete(response)
                
              case scala.util.Success(None) =>
                println(s"[$timestamp] [GET /api/v1/media/creatives/$filename] File not found")
                complete(StatusCodes.NotFound, s"File not found: $filename")
                
              case scala.util.Failure(ex) =>
                println(s"[$timestamp] [GET /api/v1/media/creatives/$filename] Error: ${ex.getMessage}")
                ex.printStackTrace()
                complete(StatusCodes.InternalServerError, s"Error serving file: ${ex.getMessage}")
            }
          }
        } ~
        // Support nested paths (for creativeId-organized files)
        path(Remaining) { filePath =>
          get {
            val timestamp = java.time.LocalDateTime.now()
            println(s"[$timestamp] [GET /api/v1/media/creatives/$filePath] Request received")
            
            val url = mediaStorage.getUrl(s"creatives/$filePath")
            println(s"[$timestamp] [GET /api/v1/media/creatives/$filePath] Looking up file at URL: $url")
            
            onComplete(mediaStorage.getFile(url)) {
              case scala.util.Success(Some(file)) =>
                val contentType = getContentType(file.getName)
                
                val response = HttpResponse(
                  entity = HttpEntity.fromPath(contentType, file.toPath),
                  headers = List(
                    org.apache.pekko.http.scaladsl.model.headers.`Cache-Control`(
                      org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.`public`,
                      org.apache.pekko.http.scaladsl.model.headers.CacheDirectives.`max-age`(3600)
                    )
                  )
                )
                complete(response)
                
              case scala.util.Success(None) =>
                complete(StatusCodes.NotFound, s"File not found: $filePath")
                
              case scala.util.Failure(ex) =>
                ex.printStackTrace()
                complete(StatusCodes.InternalServerError, s"Error serving file: ${ex.getMessage}")
            }
          }
        }
      }
    )
}
