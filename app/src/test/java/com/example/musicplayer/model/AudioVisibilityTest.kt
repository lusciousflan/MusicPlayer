package com.example.musicplayer.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioVisibilityTest {

    @Test
    fun `自動生成ファイルの接頭辞で始まる曲は非表示`() {
        assertFalse(isVisibleAudioTitle("AutoBackup_20260101"))
        assertFalse(isVisibleAudioTitle("AutoPlaylist_favorites"))
    }

    @Test
    fun `通常の曲は表示する`() {
        assertTrue(isVisibleAudioTitle("Song"))
        assertTrue(isVisibleAudioTitle(""))
    }

    @Test
    fun `接頭辞が途中にあるだけなら表示する`() {
        assertTrue(isVisibleAudioTitle("My AutoBackup_ song"))
    }
}
