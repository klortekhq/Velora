package com.klortek.velora.preview

/** Pure lifecycle policy for a future persistent preview player. */
data class PreviewRequest(val itemId: String, val sourceUrl: String)

sealed interface PreviewDecision {
    data class Schedule(val request: PreviewRequest, val token: Long) : PreviewDecision
    data class Start(val request: PreviewRequest, val token: Long) : PreviewDecision
    data class Stop(val token: Long) : PreviewDecision
    data object Ignore : PreviewDecision
}

class PreviewPlaybackPolicy(private val dwellMs: Long = DEFAULT_DWELL_MS) {
    private var token = 0L
    private var scheduled: PreviewRequest? = null
    private var active: PreviewRequest? = null

    fun focusChanged(request: PreviewRequest?, nowMs: Long, focusedSinceMs: Long): PreviewDecision {
        if (request == null || request.sourceUrl.isBlank()) {
            scheduled = null
            return active?.let { active = null; PreviewDecision.Stop(token) } ?: PreviewDecision.Ignore
        }
        if (active?.itemId == request.itemId) return PreviewDecision.Ignore
        if (scheduled?.itemId != request.itemId) {
            token += 1
            scheduled = request
        }
        return if (nowMs - focusedSinceMs >= dwellMs && scheduled == request) {
            scheduled = null
            active = request
            PreviewDecision.Start(request, token)
        } else {
            PreviewDecision.Schedule(request, token)
        }
    }

    fun stop(): PreviewDecision =
        if (active != null || scheduled != null) {
            active = null
            scheduled = null
            token += 1
            PreviewDecision.Stop(token)
        } else PreviewDecision.Ignore

    companion object { const val DEFAULT_DWELL_MS = 450L }
}
