package com.klortek.velora.security

/** Removes credentials that may be present in legacy Jellyfin/media URLs before logging. */
object SensitiveDataRedactor {
    private val credentialQueryParameter = Regex(
        "(?i)([?&](?:api_key|access_token|token|apikey|secret|password)=)[^&]*"
    )

    fun url(value: String?): String = value
        ?.replace(credentialQueryParameter, "$1<redacted>")
        ?: "<null>"
}
