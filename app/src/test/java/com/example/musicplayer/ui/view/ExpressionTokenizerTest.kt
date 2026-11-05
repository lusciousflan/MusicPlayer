package com.example.musicplayer.ui.view

import android.content.Context
import android.widget.MultiAutoCompleteTextView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class)
class ExpressionTokenizerTest {

    private fun newEdit(text: String, cursor: Int = text.length): ExpressionEditText =
        ExpressionEditText(ApplicationProvider.getApplicationContext<Context>()).apply {
            // 画面のレイアウトと同じく、1文字入力したら候補を出す設定にする
            threshold = 1
            setText(text)
            setSelection(cursor)
        }

    /** 候補を選んだときの処理(replaceText)を呼ぶ。画面では候補一覧のタップで呼ばれる。 */
    private fun ExpressionEditText.chooseSuggestion(suggestion: String) {
        MultiAutoCompleteTextView::class.java
            .getDeclaredMethod("replaceText", CharSequence::class.java)
            .apply { isAccessible = true }
            .invoke(this, suggestion)
    }

    /** [text] の [cursor] の位置で [suggestion] を選んだ結果を返す。 */
    private fun choose(text: String, suggestion: String, cursor: Int = text.length): String =
        newEdit(text, cursor).apply { chooseSuggestion(suggestion) }.text.toString()

    @Test
    fun 最初の語は語全体が候補に置き換わる() {
        assertEquals("ボカロ", choose("ボ", "ボカロ"))
    }

    @Test
    fun 二つ目以降の候補を選んでも前の式は消えない() {
        assertEquals("ボカロ & アニソン", choose("ボカロ & あに", "アニソン"))
        assertEquals("ボカロ&アニソン", choose("ボカロ&あに", "アニソン"))
        assertEquals("(ボカロ + 原曲) - カバー", choose("(ボカロ + 原曲) - か", "カバー"))
    }

    @Test
    fun 否定の直後でも語だけが置き換わる() {
        assertEquals("ボカロ & !カバー", choose("ボカロ & !か", "カバー"))
        assertEquals("ボカロ & not カバー", choose("ボカロ & not か", "カバー"))
    }

    @Test
    fun 式の途中にカーソルがあるとき後ろの式は残る() {
        assertEquals("アニソン & 原曲", choose("あに & 原曲", "アニソン", cursor = 2))
    }

    @Test
    fun 語の範囲は演算子と括弧と空白で区切られる() {
        val tokenizer = ExpressionTokenizer()
        val text = "(ボカロ+原曲)&!カバー"

        assertEquals(5, tokenizer.findTokenStart(text, 7))
        assertEquals(7, tokenizer.findTokenEnd(text, 5))
        assertEquals(10, tokenizer.findTokenStart(text, text.length))
    }

    @Test
    fun 検索式を選ぶと波かっこの中にカーソルが入る() {
        val edit = newEdit("ボカロ & ti")

        edit.chooseSuggestion("title{}")

        assertEquals("ボカロ & title{}", edit.text.toString())
        assertEquals("ボカロ & title{".length, edit.selectionStart)
        assertEquals(edit.selectionStart, edit.selectionEnd)
    }

    @Test
    fun タグを選んだときはカーソルは語の後ろのまま() {
        val edit = newEdit("ボカロ & か")

        edit.chooseSuggestion("カバー")

        assertEquals("ボカロ & カバー".length, edit.selectionEnd)
    }

    @Test
    fun 波かっこの中を入力している間は候補を出さない() {
        assertFalse(newEdit("artist{ヨル").enoughToFilter())
        assertFalse(newEdit("ボカロ & title{").enoughToFilter())
        assertTrue(newEdit("artist{ヨルシカ} & ボ").enoughToFilter())
        assertTrue(newEdit("ボ").enoughToFilter())
    }
}
