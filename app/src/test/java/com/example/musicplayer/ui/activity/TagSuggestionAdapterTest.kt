package com.example.musicplayer.ui.activity

import android.os.Looper
import android.widget.Filter
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class)
class TagSuggestionAdapterTest {

    private val tags = listOf("ボカロ", "アニソン", "原曲", "カバー", "rock")

    private fun TagSuggestionAdapter.items(): List<String> = (0 until count).map(::getItem).map { it!! }

    /** 絞り込みを実行し、結果がアダプタに反映されるまで待つ。 */
    private fun TagSuggestionAdapter.filterAndWait(text: String) {
        val done = CountDownLatch(1)
        filter.filter(text, Filter.FilterListener { done.countDown() })
        val deadline = System.currentTimeMillis() + 5_000
        while (done.count > 0 && System.currentTimeMillis() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            done.await(10, TimeUnit.MILLISECONDS)
        }
        assertEquals("filter did not finish", 0L, done.count)
    }

    private fun newAdapter() = TagSuggestionAdapter(ApplicationProvider.getApplicationContext(), tags)

    @Test
    fun 入力した文字を含むタグが候補になる() {
        val adapter = newAdapter()

        adapter.filterAndWait("カバ")

        assertEquals(listOf("カバー"), adapter.items())
    }

    @Test
    fun 何度絞り込んでも候補が出続ける() {
        val adapter = newAdapter()

        adapter.filterAndWait("ボ")
        adapter.filterAndWait("アニ")
        adapter.filterAndWait("原")

        assertEquals(listOf("原曲"), adapter.items())
    }

    @Test
    fun 式の最後の演算子より後ろの文字で絞り込む() {
        val adapter = newAdapter()

        adapter.filterAndWait("ボカロ & カバ")

        assertEquals(listOf("カバー"), adapter.items())
    }

    @Test
    fun ひらがなでカタカナのタグを探せる() {
        val adapter = newAdapter()

        adapter.filterAndWait("あにそん")

        assertEquals(listOf("アニソン"), adapter.items())
    }

    @Test
    fun 演算子の直後で文字がなければ全タグが候補になる() {
        val adapter = newAdapter()
        adapter.filterAndWait("ボ")

        adapter.filterAndWait("ボカロ & ")

        assertEquals(tags, adapter.items())
    }
}
