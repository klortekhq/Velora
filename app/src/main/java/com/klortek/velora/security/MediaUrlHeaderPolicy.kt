package com.klortek.velora.security

import java.net.URI

/** Decides whether Jellyfin authentication headers are safe for a media URL. */
object MediaUrlHeaderPolicy {
    private val credentialQueryNames = setOf(
        "api_key", "apikey", "access_token", "token", "x-emby-token", "authorization"
    )

    fun isServerResource(serverUrl: String, mediaUrl: String): Boolean {
        val server = runCatching { URI(serverUrl) }.getOrNull() ?: return false
        val media = runCatching { URI(mediaUrl) }.getOrNull() ?: return false
        if (server.scheme.isNullOrBlank() || media.scheme.isNullOrBlank()) return false
        if (!server.scheme.equals(media.scheme, ignoreCase = true)) return false
        if (!server.host.equals(media.host, ignoreCase = true)) return false
        if (effectivePort(server) != effectivePort(media)) return false

        val configuredPrefix = server.path.trimEnd('/').ifEmpty { "/" }
        val candidatePath = media.path.ifEmpty { "/" }
        return configuredPrefix == "/" ||
            candidatePath == configuredPrefix ||
            candidatePath.startsWith("$configuredPrefix/")
    }

    /** Remove credentials returned in a PlaybackInfo media URL. */
    fun stripCredentialQueryParameters(mediaUrl: String): String = runCatching {
        val uri = URI(mediaUrl)
        val query = uri.rawQuery ?: return@runCatching mediaUrl
        val safeQuery = query.split('&')
            .filter { part -> part.substringBefore('=').lowercase() !in credentialQueryNames }
            .joinToString("&")
        val withoutQuery = mediaUrl.substringBefore('?').substringBefore('#')
        buildString {
            append(withoutQuery)
            if (safeQuery.isNotBlank()) append('?').append(safeQuery)
            uri.rawFragment?.let { append('#').append(it) }
        }
    }.getOrDefault(mediaUrl)

    private fun effectivePort(uri: URI): Int = when {
        uri.port != -1 -> uri.port
        uri.scheme.equals("https", ignoreCase = true) -> 443
        uri.scheme.equals("http", ignoreCase = true) -> 80
        else -> -1
    }
}
