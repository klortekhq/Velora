package com.klortek.velora.jellyfin

import android.content.Context
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.math.BigInteger
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.util.Calendar
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.GCMParameterSpec
import javax.security.auth.x500.X500Principal

/** Small Android Keystore-backed store for credentials that must not be kept in plaintext. */
internal class SecureCredentialStore(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        "jellyfin_secure_credentials",
        Context.MODE_PRIVATE
    )

    private val keyAlias = "velora_jellyfin_credentials"
    private val legacyWrappingAlias = "velora_jellyfin_credentials_wrap"
    private val wrappedKeyName = "wrapped_aes_key"

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return KeyGenerator.getInstance("AES", "AndroidKeyStore").run {
                init(
                    android.security.keystore.KeyGenParameterSpec.Builder(
                        keyAlias,
                        android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                            android.security.keystore.KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setRandomizedEncryptionRequired(true)
                        .build()
                )
                generateKey()
            }
        }
        return legacyAesKey(keyStore)
    }

    @Suppress("DEPRECATION")
    private fun legacyAesKey(keyStore: KeyStore): SecretKey {
        if (!keyStore.containsAlias(legacyWrappingAlias)) {
            val start = Calendar.getInstance()
            val end = Calendar.getInstance().apply { add(Calendar.YEAR, 25) }
            val generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
            generator.initialize(
                KeyPairGeneratorSpec.Builder(appContext)
                    .setAlias(legacyWrappingAlias)
                    .setSubject(X500Principal("CN=Velora Jellyfin credentials"))
                    .setSerialNumber(BigInteger.ONE)
                    .setStartDate(start.time)
                    .setEndDate(end.time)
                    .build()
            )
            generator.generateKeyPair()
        }
        val encoded = preferences.getString(wrappedKeyName, null)
        if (encoded != null) {
            val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
            val privateKey = keyStore.getKey(legacyWrappingAlias, null)
            val raw = Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
                init(Cipher.DECRYPT_MODE, privateKey)
                doFinal(encrypted)
            }
            return SecretKeySpec(raw, "AES")
        }
        val generated = KeyGenerator.getInstance("AES").run { init(256); generateKey() }
        val publicKey = keyStore.getCertificate(legacyWrappingAlias).publicKey
        val encrypted = Cipher.getInstance("RSA/ECB/PKCS1Padding").run {
            init(Cipher.ENCRYPT_MODE, publicKey)
            doFinal(generated.encoded)
        }
        preferences.edit().putString(wrappedKeyName, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
        return generated
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
