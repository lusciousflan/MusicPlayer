package com.example.musicplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextSearchTest {

    @Test
    fun `英字は小文字にカタカナはひらがなにそろえる`() {
        assertEquals("よあそび yoasobi", normalizeForSearch("ヨアソビ YOASOBI"))
    }

    @Test
    fun `長音や漢字などはそのまま残す`() {
        assertEquals("らぶそんぐ集", normalizeForSearch("ラブソング集"))
    }

    @Test
    fun `表記ゆれを吸収して部分一致を判定する`() {
        assertTrue(containsForSearch("ヨルシカ - 盗作", "よるしか"))
        assertTrue(containsForSearch("Night Drive", "DRIVE"))
        assertFalse(containsForSearch("Night Drive", "day"))
    }
}
