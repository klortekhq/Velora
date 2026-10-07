package com.klortek.velora.jellyfin

import java.net.URI

/** Validation shared by login and persisted Jellyfin configuration. */
internal object ServerUrlValidator {
    fun isValid(value: String): Boolean {
        val candidate = value.trim()
        if (candidate.isEmpty() || candidate.any { it.isISOControl() || it.isWhitespace() }) return false

        val uri = runCatching { URI(candidate) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        if (uri.userInfo != null || uri.query != null || uri.fragment != null) return false
        if (uri.host.isNullOrBlank()) return false
        return uri.port == -1 || uri.port in 1..65535
    }
}
