package com.klortek.velora.jellyfin

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Small Android Keystore-backed store for credentials that must not be kept in plaintext. */
internal class SecureCredentialStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        "jellyfin_secure_credentials",
        Context.MODE_PRIVATE
    )

    private val keyAlias = "velora_jellyfin_credentials"

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
            init(256)
            generateKey()
        }
    }

    fun read(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        return runCatching {
            val bytes = Base64.decode(encoded, Base64.NO_WRAP)
            val ivLength = bytes[0].toInt()
            val iv = bytes.copyOfRange(1, ivLength + 1)
            val ciphertext = bytes.copyOfRange(ivLength + 1, bytes.size)
            Cipher.getInstance("AES/GCM/NoPadding").run {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
                String(doFinal(ciphertext), StandardCharsets.UTF_8)
            }
        }.getOrNull()
    }

    fun write(name: String, value: String) {
        if (value.isEmpty()) {
            preferences.edit().remove(name).apply()
            return
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = cipher.iv
        val payload = byteArrayOf(iv.size.toByte()) + iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        preferences.edit().putString(name, Base64.encodeToString(payload, Base64.NO_WRAP)).apply()
    }

    fun remove(name: String) = preferences.edit().remove(name).apply()
}
