package com.example.musicplayer.domain.playlist

import com.example.musicplayer.data.local.AudioWithTags
import com.example.musicplayer.util.containsForSearch
class PlaylistEvaluator(
    private val tokens: List<Token>,
    private val allAudio: List<AudioWithTags>
) {

    private var index = 0

    fun evaluate(): Set<AudioWithTags> {
        val result = parseExpression()
        // 式として解釈できたところで止まり、後ろにトークンが残っている場合（例: "rock pop" や "rock )"）は
        // 残りを黙って無視せず構文エラーにする。
        if (index < tokens.size) {
            throw PlaylistSyntaxException(
                "Unexpected token: ${tokens[index]}"
            )
        }
        return result
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

    // factor: tag, search{...}, not, (...)        
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

            is Token.Search -> {
                if (token.query.isEmpty()) {
                    throw PlaylistSyntaxException(
                        "Empty search: ${token.field.keyword}{}"
                    )
                }
                allAudio.filter { audio ->
                    searchTargets(audio, token.field).any {
                        containsForSearch(it, token.query)
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

    private fun searchTargets(audio: AudioWithTags, field: SearchField): List<String> =
        when (field) {
            SearchField.TITLE -> listOf(audio.audio.title)
            SearchField.ARTIST -> listOf(audio.audio.artist)
            SearchField.ALBUM -> listOf(audio.audio.albumName)
            SearchField.ANY -> listOf(audio.audio.title, audio.audio.artist, audio.audio.albumName)
        }
}
