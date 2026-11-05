package com.example.musicplayer.domain.playlist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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

    @Test
    fun `検索項目と波かっこの中の語が検索トークンになる`() {
        assertEquals(
            listOf(
                Token.Search(SearchField.TITLE, "恋"), Token.And,
                Token.Search(SearchField.ARTIST, "ヨルシカ"), Token.Or,
                Token.Search(SearchField.ALBUM, "盗作"), Token.Minus,
                Token.Search(SearchField.ANY, "ライブ")
            ),
            tokenize("title{恋} & artist{ヨルシカ} | album{盗作} - search{ライブ}")
        )
    }

    @Test
    fun `波かっこの中では空白や演算子の記号もそのまま検索語になる`() {
        assertEquals(
            listOf(Token.Search(SearchField.ARTIST, "A-B & C (feat. D)")),
            tokenize("artist{ A-B & C (feat. D) }")
        )
    }

    @Test
    fun `検索項目名は大文字でもよい`() {
        assertEquals(listOf(Token.Search(SearchField.ARTIST, "x")), tokenize("Artist{x}"))
    }

    @Test
    fun `検索とタグを演算子で組み合わせられる`() {
        assertEquals(
            listOf(Token.Tag("ボカロ"), Token.And, Token.Not, Token.Search(SearchField.TITLE, "remix")),
            tokenize("ボカロ&!title{remix}")
        )
    }

    @Test
    fun `未知の検索項目は構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { tokenize("genre{rock}") }
    }

    @Test
    fun `閉じていない波かっこは構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { tokenize("artist{ヨルシカ") }
    }

    @Test
    fun `項目名のない波かっこは構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { tokenize("{rock}") }
    }
}
