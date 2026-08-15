package com.example.musicplayer.domain.playlist

sealed class Token {

    data class Tag(
        val name: String
    ) : Token()

    object Plus : Token()
    object Minus : Token()
    object Not : Token()
    object And : Token()
    object Or : Token()

    object LParen : Token()
    object RParen : Token()
}
