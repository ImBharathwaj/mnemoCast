package mnemocast.engine.infra.services

import java.security.SecureRandom
import java.util.Base64

/**
  * Service for generating secure passkeys for screen authentication.
  * 
  * Generates cryptographically secure random passkeys that are:
  * - 32 bytes (256 bits) of entropy
  * - Base64 encoded for easy storage and transmission
  * - Suitable for use as API keys/device credentials
  */
object PasskeyService {
  
  private val secureRandom = new SecureRandom()
  private val passkeyLength = 32 // 32 bytes = 256 bits of entropy
  
  /**
    * Generate a new secure passkey.
    * 
    * @return Base64-encoded passkey (44 characters)
    */
  def generatePasskey(): String = {
    val bytes = new Array[Byte](passkeyLength)
    secureRandom.nextBytes(bytes)
    Base64.getEncoder.encodeToString(bytes)
  }
  
  /**
    * Validate passkey format (basic check).
    * 
    * @param passkey The passkey to validate
    * @return True if passkey appears to be valid format
    */
  def isValidFormat(passkey: String): Boolean = {
    passkey != null && passkey.length >= 32 && passkey.matches("^[A-Za-z0-9+/=]+$")
  }
}

