package com.example.musicplayer.playback

import com.example.musicplayer.model.AudioFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueueEditingTest {

    private fun audio(id: Long) = AudioFile(id = id, title = "曲$id", artist = "", uri = "", albumId = 0)

    @Test
    fun `キューにある曲は追加対象から除かれる`() {
        val result = audiosNotInQueue(setOf("1", "3"), listOf(audio(1), audio(2), audio(3), audio(4)))

        assertEquals(listOf(2L, 4L), result.map { it.id })
    }

    @Test
    fun `追加リスト内で重複する曲は最初の1曲だけ残る`() {
        val result = audiosNotInQueue(emptySet(), listOf(audio(1), audio(2), audio(1)))

        assertEquals(listOf(1L, 2L), result.map { it.id })
    }

    @Test
    fun `追加する曲の並び順は保たれる`() {
        val result = audiosNotInQueue(emptySet(), listOf(audio(5), audio(3), audio(9)))

        assertEquals(listOf(5L, 3L, 9L), result.map { it.id })
    }

    @Test
    fun `混ぜ込んだ結果には全ての曲がちょうど1回ずつ含まれる`() {
        val upcoming = (1..50).toList()
        val newItems = (101..180).toList()

        val mixed = mixRandomly(upcoming, newItems, Random(0))

        assertEquals((upcoming + newItems).sorted(), mixed.sorted())
    }

    @Test
    fun `再生予定の曲の順番は混ぜ込んだ後も変わらない`() {
        val upcoming = (1..50).toList()

        repeat(20) { seed ->
            val mixed = mixRandomly(upcoming, (101..180).toList(), Random(seed))
            assertEquals(upcoming, mixed.filter { it in upcoming })
        }
    }

    @Test
    fun `新しい曲は末尾にまとめて追加されず全体に散らばる`() {
        val upcoming = (1..100).toList()
        val newItems = (1001..1100).toList()

        val mixed = mixRandomly(upcoming, newItems, Random(42))

        // 前半にも新しい曲が入っていること（末尾に固まっていないこと）を確かめる。
        assertTrue(mixed.take(100).any { it in newItems })
    }

    @Test
    fun `再生予定の曲がなければ新しい曲だけになる`() {
        val mixed = mixRandomly(emptyList(), listOf(1, 2, 3), Random(0))

        assertEquals(listOf(1, 2, 3), mixed.sorted())
    }

    @Test
    fun `数千曲でも混ぜ込みが短時間で終わる`() {
        val start = System.nanoTime()
        mixRandomly((1..5000).toList(), (10001..15000).toList(), Random(0))
        val elapsedMs = (System.nanoTime() - start) / 1_000_000

        assertTrue("elapsed=${elapsedMs}ms", elapsedMs < 1000)
    }
}
