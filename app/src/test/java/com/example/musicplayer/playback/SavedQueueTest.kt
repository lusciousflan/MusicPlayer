package com.example.musicplayer.playback

import com.example.musicplayer.model.AudioFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SavedQueueTest {

    private fun audio(id: Long) = AudioFile(id = id, title = "曲$id", artist = "", uri = "", albumId = 0)

    private fun library(vararg ids: Long) = ids.associateWith { audio(it) }

    private fun saved(ids: List<Long>, index: Int, position: Long = 0L) =
        SavedQueue(ids, index, position, isShuffle = false)

    @Test
    fun `全曲が残っていれば保存したときの並びと位置で復元される`() {
        val result = restoreQueue(saved(listOf(3, 1, 2), index = 1, position = 42_000), library(1, 2, 3))!!

        assertEquals(listOf(3L, 1L, 2L), result.audios.map { it.id })
        assertEquals(1, result.currentIndex)
        assertEquals(42_000L, result.positionMs)
    }

    @Test
    fun `削除された曲は飛ばし、再生中の曲の位置はずれない`() {
        val result = restoreQueue(saved(listOf(1, 2, 3, 4), index = 2, position = 5_000), library(1, 3, 4))!!

        assertEquals(listOf(1L, 3L, 4L), result.audios.map { it.id })
        assertEquals(3L, result.audios[result.currentIndex].id)
        assertEquals(5_000L, result.positionMs)
    }

    @Test
    fun `再生中の曲が削除されていたら次の曲の先頭から再開する`() {
        val result = restoreQueue(saved(listOf(1, 2, 3), index = 1, position = 5_000), library(1, 3))!!

        assertEquals(3L, result.audios[result.currentIndex].id)
        assertEquals(0L, result.positionMs)
    }

    @Test
    fun `再生中の曲より後ろが残っていなければ最後に残った曲から再開する`() {
        val result = restoreQueue(saved(listOf(1, 2, 3), index = 2, position = 5_000), library(1, 2))!!

        assertEquals(2L, result.audios[result.currentIndex].id)
        assertEquals(0L, result.positionMs)
    }

    @Test
    fun `残っている曲がなければ復元しない`() {
        assertNull(restoreQueue(saved(listOf(1, 2), index = 0), library(5)))
    }

    @Test
    fun `曲IDの文字列化と読み込みで元に戻る`() {
        val ids = listOf(10L, 2L, 9_999_999_999L)

        assertEquals(ids, decodeAudioIds(encodeAudioIds(ids)))
        assertEquals(emptyList<Long>(), decodeAudioIds(""))
    }
}
