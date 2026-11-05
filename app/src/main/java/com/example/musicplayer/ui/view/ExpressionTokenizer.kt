package com.example.musicplayer.ui.view

import android.widget.MultiAutoCompleteTextView

/**
 * プレイリスト式の入力欄で、タグ候補を選んだときに置き換える範囲を決める。
 *
 * 演算子・括弧・空白で区切られた、カーソル位置の語だけを候補で置き換える。
 * （入力欄全体を置き換えると、すでに書いた式が消えてしまう）
 */
class ExpressionTokenizer : MultiAutoCompleteTextView.Tokenizer {

    override fun findTokenStart(text: CharSequence, cursor: Int): Int {
        var start = cursor
        while (start > 0 && !isSeparator(text[start - 1])) start--
        return start
    }

    override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
        var end = cursor
        while (end < text.length && !isSeparator(text[end])) end++
        return end
    }

    override fun terminateToken(text: CharSequence): CharSequence = text

    private fun isSeparator(character: Char): Boolean =
        character.isWhitespace() || character in SEPARATORS

    private companion object {
        const val SEPARATORS = "()&|+-!{}"
    }
}
