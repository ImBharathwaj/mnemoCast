package mnemocast.engine.infra.services

import org.mindrot.jbcrypt.BCrypt

/**
  * Service for password hashing and verification using bcrypt.
  */
object PasswordService {
  
  /**
    * Hash a password using bcrypt with cost factor 12.
    * 
    * @param password Plain text password
    * @return Hashed password
    */
  def hashPassword(password: String): String = {
    BCrypt.hashpw(password, BCrypt.gensalt(12))
  }
  
  /**
    * Verify a password against a hash.
    * 
    * @param password Plain text password to verify
    * @param hash Stored password hash
    * @return true if password matches, false otherwise
    */
  def verifyPassword(password: String, hash: String): Boolean = {
    try {
      BCrypt.checkpw(password, hash)
    } catch {
      case _: Exception => false
    }
  }
}

