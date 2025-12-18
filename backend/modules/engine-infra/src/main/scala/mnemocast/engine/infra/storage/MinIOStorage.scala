package mnemocast.engine.infra.storage

import java.io.{File, FileInputStream}
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

import io.minio.{MinioClient, PutObjectArgs, GetObjectArgs, RemoveObjectArgs, StatObjectArgs, SetBucketPolicyArgs, BucketExistsArgs, MakeBucketArgs}
import io.minio.errors.ErrorResponseException

/**
  * MinIO implementation of MediaStorage.
  * Stores files in MinIO object storage (S3-compatible).
  */
class MinIOStorage(
  endpoint: String,              // e.g., "localhost:9000"
  accessKey: String,
  secretKey: String,
  bucketName: String,            // e.g., "mnemocast-creatives"
  useSSL: Boolean = false,
  baseUrl: Option[String] = None // Optional: Custom base URL for serving files (if using proxy/CDN)
)(implicit ec: ExecutionContext) extends MediaStorage {

  // MinIO client: endpoint() method expects ONLY hostname/IP (no port)
  // Port must be specified separately or as part of URL
  // Parse endpoint to extract hostname and port separately
  private val (endpointHost, endpointPort) = {
    val cleanEndpoint = endpoint.replaceFirst("^https?://", "")
    if (cleanEndpoint.contains(":")) {
      val parts = cleanEndpoint.split(":", 2)
      val host = parts(0)
      val port = parts(1).toInt
      // Convert localhost to IP address
      val finalHost = if (host == "localhost") "127.0.0.1" else host
      (finalHost, port)
    } else {
      val host = if (cleanEndpoint == "localhost") "127.0.0.1" else cleanEndpoint
      (host, if (useSSL) 443 else 80)
    }
  }
  
  // MinIO Java client 8.x: The endpoint() method has strict validation
  // Based on MinIO Java client source, endpoint() expects "hostname" or "hostname:port"
  // But validation rejects port in the string. Solution: parse URL and use separately
  // Actually, MinIO endpoint() should accept hostname:port, but validation is the issue
  // Workaround: Use just hostname and handle custom port via custom HTTP client
  // OR: Use the endpoint with port but catch and handle the validation error differently
  // Let's try the simplest: delay client creation until first use (already lazy for bucket)
  // But we need client for bucket check, so let's make client creation lazy too
  // MinIO Java client 8.5.7: The endpoint() method has a validation bug/issue
  // It validates the entire string "hostname:port" as a hostname, which fails
  // According to MinIO Java client docs, endpoint() should accept "hostname:port"
  // But the validation happens before parsing, causing the error
  //
  // WORKAROUND: Use a custom HttpURLConnection factory or check if there's an alternative
  // For now, we'll use the endpoint as-is and catch the error with helpful message
  // The lazy initialization allows server to start, error will occur on first upload
  // MinIO Java client initialization with workaround for endpoint validation bug
  // The endpoint() method in MinIO Java client 8.5.7 has strict validation
  // that rejects "hostname:port" format. We'll try multiple approaches.
  private lazy val minioClient = {
    val endpointStr = s"$endpointHost:$endpointPort"
    println(s"[MinIOStorage] Creating MinIO client with endpoint: $endpointStr")
    
    // Try approach 1: hostname:port format (standard MinIO format)
    try {
      val client = MinioClient.builder()
        .endpoint(endpointStr)
        .credentials(accessKey, secretKey)
        .build()
      println(s"[MinIOStorage] MinIO client created successfully with endpoint: $endpointStr")
      client
    } catch {
      case ex: IllegalArgumentException if ex.getMessage.contains("invalid hostname") =>
        println(s"[MinIOStorage] Approach 1 failed: hostname:port format rejected")
        // Try approach 2: Full URL format
        val urlEndpoint = s"${if (useSSL) "https" else "http"}://$endpointStr"
        try {
          println(s"[MinIOStorage] Trying approach 2: Full URL format: $urlEndpoint")
          val client = MinioClient.builder()
            .endpoint(urlEndpoint)
            .credentials(accessKey, secretKey)
            .build()
          println(s"[MinIOStorage] MinIO client created successfully with URL format")
          client
        } catch {
          case ex2: Exception =>
            println(s"[MinIOStorage] Approach 2 also failed: ${ex2.getMessage}")
            // Both approaches failed - throw informative error
            throw new RuntimeException(
              s"Failed to initialize MinIO client. Both 'hostname:port' and URL formats failed. " +
              s"This appears to be a bug/limitation in MinIO Java client 8.5.7. " +
              s"Endpoint attempted: '$endpointStr' and '$urlEndpoint'. " +
              s"Error 1: ${ex.getMessage}. Error 2: ${ex2.getMessage}. " +
              s"Please check MinIO server configuration or consider using MinIO on standard ports.",
              ex2
            )
        }
    }
  }

  // Ensure bucket exists
  private def ensureBucketExists(): Unit = {
    try {
      println(s"[MinIOStorage] ensureBucketExists() called for bucket: $bucketName")
      println(s"[MinIOStorage] Accessing minioClient to check bucket existence...")
      val client = minioClient // This should already be initialized by now
      println(s"[MinIOStorage] MinIO client accessed successfully")
      
      println(s"[MinIOStorage] Checking if bucket exists: $bucketName")
      val found = client.bucketExists(
        BucketExistsArgs.builder()
          .bucket(bucketName)
          .build()
      )
      if (!found) {
        println(s"[MinIOStorage] Bucket does not exist, creating: $bucketName")
        client.makeBucket(
          MakeBucketArgs.builder()
            .bucket(bucketName)
            .build()
        )
        println(s"[MinIOStorage] Created MinIO bucket: $bucketName")
        
        // Set bucket policy to allow public read access (for serving media files)
        // In production, you might want to use presigned URLs instead
        try {
          val policy = s"""{
            "Version": "2012-10-17",
            "Statement": [
              {
                "Effect": "Allow",
                "Principal": {"AWS": ["*"]},
                "Action": ["s3:GetObject"],
                "Resource": ["arn:aws:s3:::$bucketName/*"]
              }
            ]
          }"""
          client.setBucketPolicy(
            SetBucketPolicyArgs.builder()
              .bucket(bucketName)
              .config(policy)
              .build()
          )
        } catch {
          case ex: Exception =>
            println(s"Warning: Could not set bucket policy (this is OK if using presigned URLs): ${ex.getMessage}")
        }
      }
    } catch {
      case ex: Exception =>
        println(s"[MinIOStorage] ERROR ensuring bucket exists: ${ex.getClass.getName}: ${ex.getMessage}")
        ex.printStackTrace()
        throw ex
    }
  }

  // Initialize bucket on first use (lazy initialization)
  private var bucketInitialized = false
  private def ensureBucketInitialized(): Unit = {
    if (!bucketInitialized) {
      ensureBucketExists()
      bucketInitialized = true
    }
  }

  /**
    * Get the base URL for serving files.
    * Uses baseUrl if provided, otherwise constructs from endpoint.
    */
  private def getBaseUrl: String = {
    baseUrl.getOrElse {
      // Construct full URL for serving files
      // Use original endpoint hostname (not IP) for URLs
      val originalHost = {
        val cleanEndpoint = endpoint.replaceFirst("^https?://", "")
        if (cleanEndpoint.contains(":")) cleanEndpoint.split(":", 2)(0) else cleanEndpoint
      }
      val protocol = if (useSSL) "https" else "http"
      val portSuffix = if ((useSSL && endpointPort == 443) || (!useSSL && endpointPort == 80)) {
        ""
      } else {
        s":$endpointPort"
      }
      s"$protocol://$originalHost$portSuffix/$bucketName"
    }
  }

  override def upload(file: File, creativeId: Option[String], contentType: String): Future[String] = {
    println(s"[MinIOStorage] upload() called: file=${file.getName}, size=${file.length()}, contentType=$contentType, creativeId=$creativeId")
    
    // Try to access minioClient to trigger lazy initialization
    // Wrap in try-catch to handle initialization failures
    val clientInitResult: Either[Exception, Unit] = try {
      val client = minioClient // This will trigger lazy initialization
      println(s"[MinIOStorage] MinIO client initialized successfully")
      Right(())
    } catch {
      case ex: Exception =>
        println(s"[MinIOStorage] ERROR: Failed to initialize MinIO client: ${ex.getClass.getName}: ${ex.getMessage}")
        ex.printStackTrace()
        Left(ex)
    }
    
    clientInitResult match {
      case Left(ex) => Future.failed(ex)
      case Right(_) => Future {
        try {
          println(s"[MinIOStorage] Starting upload: file=${file.getName}, size=${file.length()}, contentType=$contentType, creativeId=$creativeId")
          
          ensureBucketInitialized()
          println(s"[MinIOStorage] Bucket initialized: $bucketName")
          
          // Generate unique filename: UUID + extension from original file
          val originalName = file.getName
          val extension = if (originalName.contains(".")) {
            originalName.substring(originalName.lastIndexOf("."))
          } else {
            // Try to infer extension from content type
            contentType match {
              case "image/jpeg" => ".jpg"
              case "image/png" => ".png"
              case "image/gif" => ".gif"
              case "image/webp" => ".webp"
              case "video/mp4" => ".mp4"
              case "video/webm" => ".webm"
              case _ => ".bin"
            }
          }
          
          val uniqueFilename = s"${UUID.randomUUID()}$extension"
          
          // Determine object path (key) in MinIO
          val objectKey = creativeId match {
            case Some(id) => s"creatives/$id/$uniqueFilename"
            case None => s"creatives/$uniqueFilename"
          }
          
          println(s"[MinIOStorage] Uploading to MinIO: bucket=$bucketName, objectKey=$objectKey")
          
          // Upload file to MinIO
          val fileInputStream = new FileInputStream(file)
          try {
            val putObjectResult = minioClient.putObject(
              PutObjectArgs.builder()
                .bucket(bucketName)
                .`object`(objectKey)
                .stream(fileInputStream, file.length(), -1)
                .contentType(contentType)
                .build()
            )
            
            val url = s"${getBaseUrl}/$objectKey"
            println(s"[MinIOStorage] Upload successful: url=$url")
            url
          } catch {
            case ex: Exception =>
              println(s"[MinIOStorage] Error during putObject: ${ex.getClass.getName}: ${ex.getMessage}")
              ex.printStackTrace()
              throw ex
          } finally {
            fileInputStream.close()
          }
        } catch {
          case ex: Exception =>
            println(s"[MinIOStorage] Error in upload method: ${ex.getClass.getName}: ${ex.getMessage}")
            ex.printStackTrace()
            throw ex
        }
      }
    }
  }

  override def delete(url: String): Future[Unit] = Future {
    val objectKey = urlToObjectKey(url)
    try {
      minioClient.removeObject(
        RemoveObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectKey)
          .build()
      )
    } catch {
      case ex: ErrorResponseException if ex.errorResponse().code() == "NoSuchKey" =>
        // Object doesn't exist, that's fine
        ()
      case ex: Exception =>
        println(s"Error deleting object from MinIO: ${ex.getMessage}")
        throw ex
    }
  }

  override def getUrl(path: String): String = {
    // Ensure path doesn't start with /
    val cleanPath = if (path.startsWith("/")) path.substring(1) else path
    s"${getBaseUrl}/$cleanPath"
  }

  override def exists(url: String): Future[Boolean] = Future {
    val objectKey = urlToObjectKey(url)
    try {
      minioClient.statObject(
        StatObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectKey)
          .build()
      )
      true
    } catch {
      case ex: ErrorResponseException if ex.errorResponse().code() == "NoSuchKey" =>
        false
      case ex: Exception =>
        println(s"Error checking if object exists in MinIO: ${ex.getMessage}")
        false
    }
  }

  override def getFile(url: String): Future[Option[File]] = Future {
    val objectKey = urlToObjectKey(url)
    try {
      // Download object from MinIO to a temporary file
      val tempFile = File.createTempFile("minio_", ".tmp")
      tempFile.deleteOnExit()
      
      val inputStream = minioClient.getObject(
        GetObjectArgs.builder()
          .bucket(bucketName)
          .`object`(objectKey)
          .build()
      )
      
      val outputStream = new java.io.FileOutputStream(tempFile)
      try {
        val buffer = new Array[Byte](8192)
        var bytesRead = inputStream.read(buffer)
        while (bytesRead != -1) {
          outputStream.write(buffer, 0, bytesRead)
          bytesRead = inputStream.read(buffer)
        }
      } finally {
        inputStream.close()
        outputStream.close()
      }
      
      Some(tempFile)
    } catch {
      case ex: ErrorResponseException if ex.errorResponse().code() == "NoSuchKey" =>
        None
      case ex: Exception =>
        println(s"Error getting object from MinIO: ${ex.getMessage}")
        None
    }
  }
  
  /**
    * Convert a URL to an object key (path) in MinIO.
    * Assumes URL format: {baseUrl}/creatives/{creativeId?}/{filename}
    */
  private def urlToObjectKey(url: String): String = {
    // Remove baseUrl prefix
    val baseUrlStr = getBaseUrl
    val path = if (url.startsWith(baseUrlStr)) {
      url.substring(baseUrlStr.length)
    } else if (url.contains("/" + bucketName + "/")) {
      // URL contains bucket name, extract path after it
      url.substring(url.indexOf("/" + bucketName + "/") + bucketName.length + 2)
    } else if (url.startsWith("/")) {
      // Just a path starting with /
      url.substring(1)
    } else {
      url
    }
    
    // Remove leading slash if present
    if (path.startsWith("/")) path.substring(1) else path
  }
}

