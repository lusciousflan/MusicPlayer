package com.example.musicplayer.playback

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class NowPlayingTest {

    private val received = mutableListOf<Long>()
    private val listener: (Long) -> Unit = { received += it }

    @After
    fun tearDown() {
        NowPlaying.removeListener(listener)
        NowPlaying.update(NowPlaying.NONE)
    }

    @Test
    fun `再生中の曲が変わると監視している側に通知される`() {
        NowPlaying.addListener(listener)

        NowPlaying.update(1)
        NowPlaying.update(2)

        assertEquals(listOf(1L, 2L), received)
        assertEquals(2L, NowPlaying.audioId)
    }

    @Test
    fun `同じ曲のままなら通知しない`() {
        NowPlaying.update(1)
        NowPlaying.addListener(listener)

        NowPlaying.update(1)

        assertEquals(emptyList<Long>(), received)
    }

    @Test
    fun `監視をやめた後は通知されない`() {
        NowPlaying.addListener(listener)
        NowPlaying.removeListener(listener)

        NowPlaying.update(3)

        assertEquals(emptyList<Long>(), received)
    }
}
