package com.example.musicplayer.ui

import com.example.musicplayer.model.AudioFile
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioSortTest {

    private fun audio(id: Long, title: String, addedAt: Long = 0, playCount: Long = 0, album: String = "") =
        AudioFile(id = id, title = title, artist = "", uri = "", albumId = 0, albumName = album, addedAt = addedAt, playCount = playCount)

    private val songs = listOf(
        audio(1, "b", addedAt = 100),
        audio(2, "a", addedAt = 100),
        audio(3, "c", addedAt = 300),
        audio(4, "a", addedAt = 200)
    )

    private fun List<AudioFile>.ids() = map { it.id }

    @Test
    fun `追加順の降順は新しい順で同じ日時なら曲名順`() {
        assertEquals(listOf(3L, 4L, 2L, 1L), sortAudio(songs, AudioSortKey.ADDED, descending = true).ids())
    }

    @Test
    fun `同じ条件で何度並べ替えても順番が変わらない`() {
        val once = sortAudio(songs, AudioSortKey.ADDED, descending = true)
        val twice = sortAudio(once, AudioSortKey.ADDED, descending = true)
        val fromShuffled = sortAudio(songs.shuffled(), AudioSortKey.ADDED, descending = true)

        assertEquals(once, twice)
        assertEquals(once, fromShuffled)
    }

    @Test
    fun `曲名が同じ曲はIDの順になる`() {
        assertEquals(listOf(2L, 4L, 1L, 3L), sortAudio(songs, AudioSortKey.TITLE, descending = false).ids())
    }

    @Test
    fun `アルバムはアルバム名の順で大文字小文字を区別しない`() {
        val list = listOf(audio(1, "x", album = "beta"), audio(2, "y", album = "Alpha"))

        assertEquals(listOf(2L, 1L), sortAudio(list, AudioSortKey.ALBUM, descending = false).ids())
    }

    @Test
    fun `再生回数の降順`() {
        val list = listOf(audio(1, "x", playCount = 3), audio(2, "y", playCount = 10), audio(3, "z", playCount = 0))

        assertEquals(listOf(2L, 1L, 3L), sortAudio(list, AudioSortKey.PLAY_COUNT, descending = true).ids())
    }
}
