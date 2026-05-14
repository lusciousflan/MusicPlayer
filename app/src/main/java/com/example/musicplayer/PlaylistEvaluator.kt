package com.example.musicplayer

class PlaylistEvaluator(
    private val tokens: List<Token>,
    private val allAudio: List<AudioWithTags>
) {

    private var index = 0

    fun evaluate(): Set<AudioWithTags> {
        return parseExpression()
    }

    // low priority: | + -
    private fun parseExpression(): Set<AudioWithTags> {
        var current = parseTerm()

        while (index < tokens.size) {
            when (tokens[index]) {

                Token.Plus,
                Token.Or -> {
                    index++
                    current = current union parseTerm()
                }
                Token.Minus -> {
                    index++
                    current = current subtract parseTerm()
                }
                else -> return current
            }
        }

        return current
    }

    // high priority: &        
    private fun parseTerm(): Set<AudioWithTags> {
        var current = parseFactor()

        while (index < tokens.size) {
            when (tokens[index]) {

                Token.And -> {
                    index++
                    current = current intersect parseFactor()
                }
                else -> return current
            }
        }

        return current
    }

    // factor: tag, not, (...)        
    private fun parseFactor(): Set<AudioWithTags> {

        if (index >= tokens.size) {
            throw PlaylistSyntaxException(
                "Unexpected end of expression"
            )
        }

        return when (val token = tokens[index++]) {

            is Token.Tag -> {
                allAudio.filter { audio ->
                    audio.tags.any {
                        it.name == token.name
                    }
                }.toSet()
            }

            Token.Not -> {
                val inner = parseFactor()
                allAudio.toSet() subtract inner
            }

            Token.LParen -> {
                val inner = parseExpression()
                if (
                    index < tokens.size &&
                    tokens[index] == Token.RParen
                ) {
                    index++
                } else {
                    throw PlaylistSyntaxException(
                        "Missing closing parenthesis"
                    )
                }
                inner
            }

            else -> {
                throw PlaylistSyntaxException(
                    "Unexpected token: $token"
                )
            }
        }
    }
}