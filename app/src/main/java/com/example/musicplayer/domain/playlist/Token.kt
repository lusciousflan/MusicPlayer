package com.example.musicplayer.domain.playlist

sealed class Token {

    data class Tag(
        val name: String
    ) : Token()

    data object Plus : Token()
    data object Minus : Token()
    data object Not : Token()
    data object And : Token()
    data object Or : Token()

    data object LParen : Token()
    data object RParen : Token()
}
