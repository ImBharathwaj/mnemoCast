package mnemocast.engine.infra.services

import java.io.File

import scala.concurrent.{ExecutionContext, Future}

/**
  * Validates media files (images/videos) before upload.
  */
class MediaValidator(
  maxImageSizeBytes: Long = 10 * 1024 * 1024, // 10MB default
  maxVideoSizeBytes: Long = 500 * 1024 * 1024, // 500MB default
  allowedImageTypes: Set[String] = Set("image/jpeg", "image/png", "image/gif", "image/webp"),
  allowedVideoTypes: Set[String] = Set("video/mp4", "video/webm")
)(implicit ec: ExecutionContext) {

  case class ValidationResult(
    isValid: Boolean,
    errors: List[String],
    metadata: Option[MediaMetadata]
  )

  case class MediaMetadata(
    contentType: String,
    size: Long,
    width: Option[Int] = None, // For images/videos - to be extracted later
    height: Option[Int] = None, // For images/videos - to be extracted later
    duration: Option[Int] = None // For videos - to be extracted later
  )

  /**
    * Validate a file for upload.
    * 
    * @param file The file to validate
    * @param contentType The declared MIME type
    * @return ValidationResult with validation status and metadata
    */
  def validateFile(file: File, contentType: String): Future[ValidationResult] = Future {
    val errors = scala.collection.mutable.ListBuffer[String]()
    
    // Check file exists
    if (!file.exists() || !file.isFile) {
      ValidationResult(
        isValid = false,
        errors = List("File does not exist or is not a regular file"),
        metadata = None
      )
    } else {
    
    // Check file size
    val fileSize = file.length()
    
    // Determine max size based on content type
    val maxSize = if (allowedImageTypes.contains(contentType)) {
      maxImageSizeBytes
    } else if (allowedVideoTypes.contains(contentType)) {
      maxVideoSizeBytes
    } else {
      // Unknown type, use smaller limit
      maxImageSizeBytes
    }
    
    if (fileSize > maxSize) {
      val maxSizeMB = maxSize / (1024 * 1024)
      val fileSizeMB = fileSize / (1024 * 1024)
      errors += s"File size ($fileSizeMB MB) exceeds maximum allowed size ($maxSizeMB MB)"
    }
    
    if (fileSize == 0) {
      errors += "File is empty"
    }
    
    // Validate content type
    val isAllowed = allowedImageTypes.contains(contentType) || allowedVideoTypes.contains(contentType)
    if (!isAllowed) {
      errors += s"Content type '$contentType' is not allowed. Allowed types: ${(allowedImageTypes ++ allowedVideoTypes).mkString(", ")}"
    }
    
    // Basic content validation (check file extension matches content type)
    val fileName = file.getName.toLowerCase
    val hasValidExtension = contentType match {
      case "image/jpeg" => fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")
      case "image/png" => fileName.endsWith(".png")
      case "image/gif" => fileName.endsWith(".gif")
      case "image/webp" => fileName.endsWith(".webp")
      case "video/mp4" => fileName.endsWith(".mp4")
      case "video/webm" => fileName.endsWith(".webm")
      case _ => false
    }
    
    if (!hasValidExtension && isAllowed) {
      // Warning, not error - extension mismatch is suspicious but not blocking
      // errors += s"File extension does not match content type '$contentType'"
    }
    
    // Create metadata
    val metadata = if (errors.isEmpty) {
      Some(MediaMetadata(
        contentType = contentType,
        size = fileSize
        // width, height, duration will be extracted later if needed
      ))
    } else {
      None
    }
    
      ValidationResult(
        isValid = errors.isEmpty,
        errors = errors.toList,
        metadata = metadata
      )
    }
  }
}
