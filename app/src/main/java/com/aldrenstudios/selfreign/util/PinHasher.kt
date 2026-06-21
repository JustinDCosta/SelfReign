package com.aldrenstudios.selfreign.util

import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Salted SHA-256 hashing for the optional app-lock PIN.
 *
 * The PIN itself is never stored. We store "salt:hash"; verification re-hashes the
 * entered PIN with the stored salt and compares. This avoids keeping the PIN in
 * plain text even though the whole store is already encrypted at rest.
 */
object PinHasher {

    /** Creates a "saltHex:hashHex" string for [pin]. */
    fun hash(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val digest = pbkdf2(pin, salt)
        return "${salt.toHex()}:${digest.toHex()}"
    }

    /** Returns true if [pin] matches the previously stored [stored] "salt:hash". */
    fun verify(pin: String, stored: String?): Boolean {
        if (stored.isNullOrBlank()) return false
        val parts = stored.split(":")
        if (parts.size != 2) return false
        val salt = runCatching { parts[0].fromHex() }.getOrNull() ?: return false
        val expected = parts[1]
        val actual = pbkdf2(pin, salt).toHex()
        // Constant-time-ish comparison.
        return MessageDigest.isEqual(actual.toByteArray(), expected.toByteArray())
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = javax.crypto.spec.PBEKeySpec(pin.toCharArray(), salt, 10000, 256)
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun String.fromHex(): ByteArray =
        chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
