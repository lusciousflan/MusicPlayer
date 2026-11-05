package com.example.musicplayer.domain.playlist

import com.example.musicplayer.util.normalizeTag

private const val OPERATOR_CHARS = "()&|+-!"

/**
 * プレイリスト式をトークンに分割する。
 *
 * - 演算子 ( ) & | + - ! は前後に空白がなくても区切りになる。"not" も否定演算子として扱う。
 * - それ以外の語はタグ名として扱い、[normalizeTag] で正規化する。
 * - title{...} / artist{...} / album{...} / search{...} は検索。{} の中は空白や演算子の記号も含めて
 *   そのまま検索語になる。
 *
 * @throws PlaylistSyntaxException 閉じていない { がある、または未知の検索項目名の場合
 */
fun tokenize(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var index = 0

    while (index < input.length) {
        val character = input[index]
        when {
            character.isWhitespace() -> index++

            character in OPERATOR_CHARS -> {
                tokens += when (character) {
                    '(' -> Token.LParen
                    ')' -> Token.RParen
                    '&' -> Token.And
                    '|' -> Token.Or
                    '+' -> Token.Plus
                    '-' -> Token.Minus
                    else -> Token.Not
                }
                index++
            }

            else -> {
                val start = index
                while (index < input.length &&
                    !input[index].isWhitespace() &&
                    input[index] !in OPERATOR_CHARS &&
                    input[index] != '{'
                ) {
                    index++
                }
                val word = input.substring(start, index)

                if (word.isEmpty()) {
                    // 項目名のない "{" など
                    throw PlaylistSyntaxException("Unexpected character: ${input[index]}")
                } else if (index < input.length && input[index] == '{') {
                    val field = SearchField.fromKeyword(word.lowercase())
                        ?: throw PlaylistSyntaxException("Unknown search field: $word")
                    val close = input.indexOf('}', startIndex = index + 1)
                    if (close < 0) throw PlaylistSyntaxException("Missing closing brace: $word{")
                    tokens += Token.Search(field, input.substring(index + 1, close).trim())
                    index = close + 1
                } else if (word == "not") {
                    tokens += Token.Not
                } else {
                    tokens += Token.Tag(normalizeTag(word))
                }
            }
        }
    }

    return tokens
}
