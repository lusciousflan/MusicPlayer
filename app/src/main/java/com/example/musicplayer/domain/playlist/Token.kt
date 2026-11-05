package com.example.musicplayer.domain.playlist

sealed class Token {

    data class Tag(
        val name: String
    ) : Token()

    /** title{...} などの検索。[query] を含む曲に一致する。 */
    data class Search(
        val field: SearchField,
        val query: String
    ) : Token()

    data object Plus : Token()
    data object Minus : Token()
    data object Not : Token()
    data object And : Token()
    data object Or : Token()

    data object LParen : Token()
    data object RParen : Token()
}

/** プレイリスト式の検索で調べる項目。[keyword] は式に書く名前（例: artist{...}）。 */
enum class SearchField(val keyword: String) {
    TITLE("title"),
    ARTIST("artist"),
    ALBUM("album"),

    /** 曲名・アーティスト名・アルバム名のどれか */
    ANY("search");

    companion object {
        fun fromKeyword(keyword: String): SearchField? = entries.firstOrNull { it.keyword == keyword }
    }
}
