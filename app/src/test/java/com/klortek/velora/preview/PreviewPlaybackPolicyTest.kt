package com.klortek.velora.preview

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewPlaybackPolicyTest {
    private val first = PreviewRequest("one", "https://media/one")
    private val second = PreviewRequest("two", "https://media/two")

    @Test
    fun waitsForDwellBeforeStarting() {
        val policy = PreviewPlaybackPolicy(450)
        assertTrue(policy.focusChanged(first, 100, 100) is PreviewDecision.Schedule)
        assertEquals(first, (policy.focusChanged(first, 550, 100) as PreviewDecision.Start).request)
    }

    @Test
    fun focusChangeInvalidatesPreviousRequest() {
        val policy = PreviewPlaybackPolicy(450)
        val old = policy.focusChanged(first, 100, 100) as PreviewDecision.Schedule
        val current = policy.focusChanged(second, 200, 200) as PreviewDecision.Schedule
        assertTrue(current.token > old.token)
        assertEquals(second, (policy.focusChanged(second, 700, 200) as PreviewDecision.Start).request)
    }

    @Test
    fun missingFocusStopsActivePreview() {
        val policy = PreviewPlaybackPolicy(0)
        policy.focusChanged(first, 100, 100)
        assertTrue(policy.focusChanged(null, 101, 101) is PreviewDecision.Stop)
    }
}
