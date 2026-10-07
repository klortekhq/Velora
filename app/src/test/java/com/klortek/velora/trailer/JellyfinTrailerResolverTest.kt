package com.klortek.velora.trailer

import com.klortek.velora.jellyfin.JellyfinItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JellyfinTrailerResolverTest {
    @Test
    fun localTrailerWinsOverRemoteTrailer() {
        val local = item("local")
        val remote = item("remote")
        assertEquals(local, JellyfinTrailerResolver.select(listOf(local), listOf(remote)))
    }

    @Test
    fun remoteTrailerIsFallbackWhenLocalIsEmpty() {
        val remote = item("remote")
        assertEquals(remote, JellyfinTrailerResolver.select(emptyList(), listOf(remote)))
    }

    @Test
    fun noServerTrailerReturnsNull() {
        assertNull(JellyfinTrailerResolver.select(emptyList(), emptyList()))
    }

    private fun item(id: String) = JellyfinItem(Id = id, Name = id, Type = "Video")
}
