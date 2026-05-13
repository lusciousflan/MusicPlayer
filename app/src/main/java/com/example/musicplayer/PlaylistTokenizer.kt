package com.example.musicplayer

fun tokenize(input: String): List<Token> {

    val normalized =

        input
            .replace("(", " ( ")
            .replace(")", " ) ")
            .replace("&", " & ")
            .replace("|", " | ")
            .replace("+", " + ")
            .replace("-", " - ")
            .replace("!", " ! ")

    val parts =
        normalized
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

    return parts.map { part ->

        when (part) {

            "+" -> Token.Plus
            "-" -> Token.Minus
            "&" -> Token.And
            "|" -> Token.Or
            "!" -> Token.Not
            "not" -> Token.Not

            "(" -> Token.LParen
            ")" -> Token.RParen

            else -> Token.Tag(
                normalizeTag(part)
            )
        }
    }
}