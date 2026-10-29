package com.example.musicplayer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TagUtilsTest {

    @Test
    fun `前後の空白を取り除く`() {
        assertEquals("rock", normalizeTag("  rock\t"))
    }

    @Test
    fun `英字は小文字にそろえる`() {
        assertEquals("j-pop", normalizeTag("J-POP"))
    }

    @Test
    fun `途中の空白と日本語はそのまま残す`() {
        assertEquals("作業用 bgm", normalizeTag(" 作業用 BGM "))
    }
}
