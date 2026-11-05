package com.example.musicplayer.ui.view

import android.content.Context
import android.util.AttributeSet
import android.widget.MultiAutoCompleteTextView

/**
 * プレイリスト式の入力欄。タグ候補や検索式の候補(title{} など)を、カーソル位置の語に対して出す。
 *
 * - 候補を選ぶと、入力欄全体ではなくカーソル位置の語だけを置き換える（[ExpressionTokenizer]）。
 * - 検索式の候補を選んだときは、続けて検索語を打てるようカーソルを {} の中に置く。
 * - {} の中を入力している間は、検索語の入力の邪魔にならないよう候補を出さない。
 */
class ExpressionEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.autoCompleteTextViewStyle
) : MultiAutoCompleteTextView(context, attrs, defStyleAttr) {

    init {
        setTokenizer(ExpressionTokenizer())
    }

    override fun replaceText(text: CharSequence) {
        super.replaceText(text)
        if (text.endsWith("{}")) {
            setSelection(selectionEnd - 1)
        }
    }

    override fun enoughToFilter(): Boolean =
        !isInsideBraces(getText(), selectionEnd) && super.enoughToFilter()

    companion object {
        /** [cursor] が、閉じていない { の後ろ（検索語の入力中）にあるか */
        fun isInsideBraces(text: CharSequence, cursor: Int): Boolean {
            if (cursor <= 0) return false
            val before = text.subSequence(0, cursor.coerceAtMost(text.length))
            return before.lastIndexOf('{') > before.lastIndexOf('}')
        }
    }
}
