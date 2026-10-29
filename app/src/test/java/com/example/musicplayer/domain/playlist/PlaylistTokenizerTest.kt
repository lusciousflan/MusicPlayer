package com.example.musicplayer.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaylistTokenizerTest {

    @Test
    fun `タグ名は前後の空白を除いて小文字に正規化される`() {
        assertEquals(listOf(Token.Tag("rock")), tokenize("  Rock  "))
    }

    @Test
    fun `演算子は前後に空白がなくても分割される`() {
        assertEquals(
            listOf(
                Token.LParen, Token.Tag("a"), Token.Plus, Token.Tag("b"), Token.RParen,
                Token.And, Token.Tag("c"), Token.Or, Token.Tag("d"),
                Token.Minus, Token.Not, Token.Tag("e")
            ),
            tokenize("(a+b)&c|d-!e")
        )
    }

    @Test
    fun `notはキーワードとして否定演算子になる`() {
        assertEquals(listOf(Token.Not, Token.Tag("rock")), tokenize("not rock"))
    }

    @Test
    fun `日本語のタグ名もそのまま扱える`() {
        assertEquals(listOf(Token.Tag("作業用"), Token.And, Token.Tag("ボカロ")), tokenize("作業用 & ボカロ"))
    }

    @Test
    fun `空文字や空白だけの入力はトークンなしになる`() {
        assertEquals(emptyList<Token>(), tokenize(""))
        assertEquals(emptyList<Token>(), tokenize("   "))
    }
}
