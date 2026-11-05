package com.example.musicplayer.domain.playlist

import com.example.musicplayer.data.local.AudioEntity
import com.example.musicplayer.data.local.AudioWithTags
import com.example.musicplayer.data.local.TagEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PlaylistEvaluatorTest {

    private fun audio(id: Long, vararg tags: String) = AudioWithTags(
        AudioEntity(id = id, title = "曲$id", artist = "", uri = "", albumId = 0),
        tags.map { TagEntity(it) }
    )

    // 1: rock / 2: pop / 3: rock+pop / 4: jazz / 5: タグなし
    private val library = listOf(
        audio(1, "rock"),
        audio(2, "pop"),
        audio(3, "rock", "pop"),
        audio(4, "jazz"),
        audio(5)
    )

    /** 式を評価し、該当した曲の ID を昇順で返す。 */
    private fun evaluate(expression: String): List<Long> =
        PlaylistEvaluator(tokenize(expression), library).evaluate()
            .map { it.audio.id }
            .sorted()

    @Test
    fun `タグ1つで、そのタグが付いた曲だけが選ばれる`() {
        assertEquals(listOf(1L, 3L), evaluate("rock"))
    }

    @Test
    fun `大文字で書いても小文字のタグに一致する`() {
        assertEquals(listOf(1L, 3L), evaluate("ROCK"))
    }

    @Test
    fun `存在しないタグは0曲になる`() {
        assertEquals(emptyList<Long>(), evaluate("metal"))
    }

    @Test
    fun `プラスとパイプは和集合`() {
        assertEquals(listOf(1L, 2L, 3L), evaluate("rock + pop"))
        assertEquals(listOf(1L, 2L, 3L), evaluate("rock | pop"))
    }

    @Test
    fun `アンドは積集合`() {
        assertEquals(listOf(3L), evaluate("rock & pop"))
    }

    @Test
    fun `マイナスは差集合`() {
        assertEquals(listOf(1L), evaluate("rock - pop"))
    }

    @Test
    fun `否定はライブラリ全体からの補集合でタグなしの曲も含む`() {
        assertEquals(listOf(2L, 4L, 5L), evaluate("!rock"))
        assertEquals(listOf(2L, 4L, 5L), evaluate("not rock"))
    }

    @Test
    fun `アンドはプラスとマイナスより優先される`() {
        // jazz + (rock & pop)
        assertEquals(listOf(3L, 4L), evaluate("jazz + rock & pop"))
        // rock - (rock & pop)
        assertEquals(listOf(1L), evaluate("rock - rock & pop"))
    }

    @Test
    fun `プラスとマイナスは左から順に評価される`() {
        // (rock - pop) + pop
        assertEquals(listOf(1L, 2L, 3L), evaluate("rock - pop + pop"))
        // (rock + pop) - pop
        assertEquals(listOf(1L), evaluate("rock + pop - pop"))
    }

    @Test
    fun `括弧で優先順位を変えられる`() {
        // (jazz + rock) & pop
        assertEquals(listOf(3L), evaluate("(jazz + rock) & pop"))
        // !(rock | pop)
        assertEquals(listOf(4L, 5L), evaluate("!(rock | pop)"))
    }

    @Test
    fun `否定は直後の要素だけにかかる`() {
        // (!rock) & pop
        assertEquals(listOf(2L), evaluate("!rock & pop"))
    }

    @Test
    fun `空の式は構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("") }
    }

    @Test
    fun `演算子の後ろにタグがないと構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("rock &") }
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("rock +") }
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("!") }
    }

    @Test
    fun `閉じ括弧がないと構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("(rock + pop") }
    }

    @Test
    fun `演算子から始まる式は構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("& rock") }
        assertThrows(PlaylistSyntaxException::class.java) { evaluate(") rock") }
    }

    @Test
    fun `演算子なしでタグが続くと構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("rock pop") }
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("(rock) pop") }
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("!rock pop") }
    }

    @Test
    fun `対応する開き括弧のない閉じ括弧は構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("rock )") }
        assertThrows(PlaylistSyntaxException::class.java) { evaluate("(rock + pop))") }
    }

    @Test
    fun `曲が空のライブラリでも構文チェックとして評価できる`() {
        // プレイリスト作成画面は、曲リストを空にして式の構文だけを検査している。
        assertEquals(
            emptySet<AudioWithTags>(),
            PlaylistEvaluator(tokenize("(rock + pop) & !jazz"), emptyList()).evaluate()
        )
    }

    // ---- 検索 ----

    private fun song(id: Long, title: String, artist: String, album: String, vararg tags: String) = AudioWithTags(
        AudioEntity(id = id, title = title, artist = artist, uri = "", albumId = 0, albumName = album),
        tags.map { TagEntity(it) }
    )

    // 10: A のソロ曲 / 11: A と B の共作 / 12: B のソロ曲 / 13: C の曲
    private val searchLibrary = listOf(
        song(10, "ハロー", "A", "First", "pop"),
        song(11, "コラボ曲", "A×B", "Collab"),
        song(12, "Night Drive", "B", "Second", "pop"),
        song(13, "恋の歌", "C", "ラブソング集")
    )

    private fun search(expression: String): List<Long> =
        PlaylistEvaluator(tokenize(expression), searchLibrary).evaluate()
            .map { it.audio.id }
            .sorted()

    @Test
    fun `アーティスト検索は部分一致で共作の曲はどちらの名前でも含まれる`() {
        assertEquals(listOf(10L, 11L), search("artist{A}"))
        assertEquals(listOf(11L, 12L), search("artist{B}"))
    }

    @Test
    fun `曲名とアルバム名で検索できる`() {
        assertEquals(listOf(13L), search("title{恋}"))
        assertEquals(listOf(11L), search("album{collab}"))
    }

    @Test
    fun `searchは曲名とアーティストとアルバムのどれかに一致する曲`() {
        // 曲名「Night Drive」、アルバム「First」の両方に i が含まれる
        assertEquals(listOf(10L, 12L), search("search{i}"))
    }

    @Test
    fun `検索は大文字小文字とひらがなカタカナを区別しない`() {
        assertEquals(listOf(12L), search("title{NIGHT}"))
        assertEquals(listOf(10L), search("title{はろー}"))
        assertEquals(listOf(13L), search("album{らぶ}"))
    }

    @Test
    fun `検索とタグを組み合わせられる`() {
        assertEquals(listOf(12L), search("pop & artist{B}"))
        assertEquals(listOf(10L), search("artist{A} - artist{B}"))
    }

    @Test
    fun `空の検索は構文エラー`() {
        assertThrows(PlaylistSyntaxException::class.java) { search("artist{}") }
        assertThrows(PlaylistSyntaxException::class.java) { search("artist{  }") }
    }
}
