package mnemocast.engine.infra.storage

import java.io.File

import scala.concurrent.Future

/**
  * Abstraction for media file storage.
  * Implementations can use local filesystem, S3, MinIO, etc.
  */
trait MediaStorage {
  
  /**
    * Upload a file and return the URL where it can be accessed.
    * 
    * @param file The file to upload
    * @param creativeId Optional creative ID for organizing files
    * @param contentType MIME type of the file
    * @return Future containing the full URL to access the file
    */
  def upload(file: File, creativeId: Option[String], contentType: String): Future[String]
  
  /**
    * Delete a file by its URL.
    * 
    * @param url The URL of the file to delete
    * @return Future that completes when deletion is done
    */
  def delete(url: String): Future[Unit]
  
  /**
    * Get the full URL for a stored file path.
    * 
    * @param path The storage path of the file
    * @return Full URL to access the file
    */
  def getUrl(path: String): String
  
  /**
    * Check if a file exists at the given URL.
    * 
    * @param url The URL of the file
    * @return Future containing true if file exists, false otherwise
    */
  def exists(url: String): Future[Boolean]
  
  /**
    * Get a File object for the given URL (for serving).
    * 
    * @param url The URL of the file
    * @return Future containing Some(File) if exists, None otherwise
    */
  def getFile(url: String): Future[Option[File]]
}

