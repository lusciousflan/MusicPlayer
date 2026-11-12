package com.example.musicplayer.playback

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressWatchersTest {

    private val received = mutableListOf<Boolean>()
    private val listener: (Boolean) -> Unit = { received += it }

    @After
    fun tearDown() {
        ProgressWatchers.removeListener(listener)
        while (ProgressWatchers.isWatched) ProgressWatchers.unwatch()
    }

    @Test
    fun `見ている画面がなくなったときとできたときだけ通知する`() {
        ProgressWatchers.addListener(listener)

        ProgressWatchers.watch()
        ProgressWatchers.watch()
        ProgressWatchers.unwatch()
        ProgressWatchers.unwatch()

        assertEquals(listOf(true, false), received)
        assertFalse(ProgressWatchers.isWatched)
    }

    @Test
    fun `画面を切り替えるときに一時的に0になっても、最後は見ている状態になる`() {
        ProgressWatchers.watch()
        ProgressWatchers.unwatch()
        ProgressWatchers.watch()

        assertTrue(ProgressWatchers.isWatched)
    }

    @Test
    fun `見ていないのに unwatch しても負の数にならない`() {
        ProgressWatchers.unwatch()
        ProgressWatchers.watch()

        assertTrue(ProgressWatchers.isWatched)
    }

    @Test
    fun `次の秒に変わった直後に送る`() {
        assertEquals(1_020L, ProgressWatchers.nextUpdateDelayMs(0))
        assertEquals(520L, ProgressWatchers.nextUpdateDelayMs(12_500))
        assertEquals(21L, ProgressWatchers.nextUpdateDelayMs(12_999))
    }
}
