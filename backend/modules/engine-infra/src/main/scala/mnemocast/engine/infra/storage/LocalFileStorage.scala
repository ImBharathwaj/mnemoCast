package mnemocast.engine.infra.storage

import java.io.{File, FileInputStream, FileOutputStream}
import java.nio.file.{Files, Paths, StandardCopyOption}
import java.util.UUID

import scala.concurrent.{ExecutionContext, Future}

/**
  * Local filesystem implementation of MediaStorage.
  * Stores files in a local directory structure.
  */
class LocalFileStorage(
  basePath: String,
  baseUrl: String // e.g., "http://localhost:8080/api/v1/media"
)(implicit ec: ExecutionContext) extends MediaStorage {

  private val storageDir = new File(basePath)
  private val creativesDir = new File(storageDir, "creatives")
  
  // Ensure directories exist
  if (!creativesDir.exists()) {
    creativesDir.mkdirs()
  }

  override def upload(file: File, creativeId: Option[String], contentType: String): Future[String] = Future {
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
    
    // Determine storage path
    val targetDir = creativeId match {
      case Some(id) =>
        val dir = new File(creativesDir, id)
        if (!dir.exists()) dir.mkdirs()
        dir
      case None =>
        creativesDir
    }
    
    val targetFile = new File(targetDir, uniqueFilename)
    
    // Copy file to storage location
    Files.copy(
      file.toPath,
      targetFile.toPath,
      StandardCopyOption.REPLACE_EXISTING
    )
    
    // Generate relative path for URL construction
    val relativePath = s"creatives/${if (creativeId.isDefined) s"${creativeId.get}/" else ""}$uniqueFilename"
    getUrl(relativePath)
  }

  override def delete(url: String): Future[Unit] = Future {
    val file = urlToFile(url)
    if (file.exists() && file.isFile) {
      file.delete()
      // Also delete parent directory if empty (for creativeId-organized files)
      val parent = file.getParentFile
      if (parent != null && parent.exists() && parent.listFiles().isEmpty) {
        parent.delete()
      }
    }
  }

  override def getUrl(path: String): String = {
    // Ensure path doesn't start with /
    val cleanPath = if (path.startsWith("/")) path.substring(1) else path
    s"$baseUrl/$cleanPath"
  }

  override def exists(url: String): Future[Boolean] = Future {
    val file = urlToFile(url)
    file.exists() && file.isFile
  }

  override def getFile(url: String): Future[Option[File]] = Future {
    val file = urlToFile(url)
    if (file.exists() && file.isFile) {
      Some(file)
    } else {
      None
    }
  }
  
  /**
    * Convert a URL to a File object.
    * Assumes URL format: {baseUrl}/creatives/{creativeId?}/{filename}
    */
  private def urlToFile(url: String): File = {
    // Remove baseUrl prefix
    val path = if (url.startsWith(baseUrl)) {
      url.substring(baseUrl.length)
    } else if (url.startsWith("/api/v1/media/")) {
      url.substring("/api/v1/media/".length)
    } else {
      url
    }
    
    // Ensure path doesn't start with /
    val cleanPath = if (path.startsWith("/")) path.substring(1) else path
    
    new File(storageDir, cleanPath)
  }
}

