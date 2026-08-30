package com.klortek.velora.offline

import java.io.InputStream
import java.security.MessageDigest

/**
 * Verifies managed offline media without assuming a particular transfer
 * backend. The stream is owned by the caller and is deliberately not closed
 * here so the verifier can be used with ContentResolver and file streams.
 */
internal object OfflineIntegrityVerifier {
    private const val BUFFER_SIZE = 64 * 1024

    fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            if (read == 0) continue
            digest.update(buffer, 0, read)
        }
        return digest.digest().joinToString(separator = "") { byte -> "%02x".format(byte) }
    }

    fun matches(input: InputStream, expectedSha256: String): Boolean =
        sha256(input) == expectedSha256.trim().lowercase()
}
