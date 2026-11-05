package com.example.musicplayer.util

/**
 * 検索で表記ゆれを吸収するための正規化。英字は小文字に、カタカナはひらがなにそろえる。
 * 例: "ヨルシカ" と "よるしか"、"YOASOBI" と "yoasobi" を同じものとして比べられる。
 */
fun normalizeForSearch(text: String): String = buildString(text.length) {
    text.lowercase().forEach { character ->
        append(
            if (character in 'ァ'..'ヶ') (character.code - 0x60).toChar() else character
        )
    }
}

/** [text] が [query] を含むかを、[normalizeForSearch] で表記ゆれを吸収して判定する。 */
fun containsForSearch(text: String, query: String): Boolean =
    normalizeForSearch(text).contains(normalizeForSearch(query))
