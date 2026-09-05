package com.klortek.velora.security

/** Removes credentials that may be present in legacy Jellyfin/media URLs before logging. */
object SensitiveDataRedactor {
    private val credentialQueryParameter = Regex(
        "(?i)([?&](?:api_key|access_token|token|apikey|secret|password|authorization|x-emby-token)=)[^&]*"
    )
    private val credentialAssignment = Regex(
        "(?i)(\\b(?:token|password|authorization|x-emby-token)\\s*[:=]\\s*(?:Bearer\\s+)?[\\\"']?)[^\\s,;\\\"']+([\\\"']?)"
    )

    fun url(value: String?): String = value
        ?.replace(credentialQueryParameter, "$1<redacted>")
        ?.replace(credentialAssignment, "$1<redacted>$2")
        ?: "<null>"

    fun message(error: Throwable?): String = url(error?.message ?: error?.javaClass?.simpleName)

    /** Local media paths and filenames are not useful in production logs. */
    fun localPath(value: String?): String = if (value.isNullOrBlank()) "<null>" else "<local-path>"

    fun localFileName(value: String?): String = if (value.isNullOrBlank()) "<null>" else "<local-file>"
}
