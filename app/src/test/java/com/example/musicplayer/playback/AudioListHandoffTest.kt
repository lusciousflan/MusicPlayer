package com.example.musicplayer.playback

import com.example.musicplayer.model.AudioFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AudioListHandoffTest {

    private fun audio(id: Long) = AudioFile(id = id, title = "曲$id", artist = "", uri = "", albumId = 0)

    @Test
    fun `預けたリストをキーで受け取れる`() {
        val audios = (1L..3000L).map(::audio)

        val key = AudioListHandoff.put(audios)

        assertEquals(audios, AudioListHandoff.take(key))
    }

    @Test
    fun `一度受け取ったリストは二度目は受け取れない`() {
        val key = AudioListHandoff.put(listOf(audio(1)))
        AudioListHandoff.take(key)

        assertNull(AudioListHandoff.take(key))
    }

    @Test
    fun `預けるたびに別のキーになり取り違えない`() {
        val first = AudioListHandoff.put(listOf(audio(1)))
        val second = AudioListHandoff.put(listOf(audio(2)))

        assertNotEquals(first, second)
        assertEquals(listOf(audio(2)), AudioListHandoff.take(second))
        assertEquals(listOf(audio(1)), AudioListHandoff.take(first))
    }

    @Test
    fun `預けた後で元のリストを変更しても受け取る内容は変わらない`() {
        val audios = mutableListOf(audio(1))
        val key = AudioListHandoff.put(audios)

        audios.add(audio(2))

        assertEquals(listOf(audio(1)), AudioListHandoff.take(key))
    }
}
