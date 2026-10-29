package com.example.musicplayer.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * Room の DAO とリポジトリのテスト。
 * Robolectric により PC 上で動かし、メモリ内 DB を使うため、端末やアプリ本体のデータには影響しない。
 */
@RunWith(AndroidJUnit4::class)
// MyApp は本番 DB を作るため、テストでは素の Application を使う
@Config(application = android.app.Application::class)
class MusicRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AudioDao
    private lateinit var repository: MusicRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.audioDao()
        repository = MusicRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun audio(id: Long, title: String = "曲$id", addedAt: Long = 0L) =
        AudioEntity(id = id, title = title, artist = "artist", uri = "content://audio/$id", albumId = 1, addedAt = addedAt)

    @Test
    fun タグは正規化して保存され重複しない() = runBlocking {
        dao.insertAudio(audio(1))

        repository.addTag(1, " Rock ")
        repository.addTag(1, "rock")

        assertEquals(listOf("rock"), repository.getTags(1))
        assertEquals(listOf(TagEntity("rock")), repository.getAllTags())
    }

    @Test
    fun タグで曲を取得し曲数を数えられる() = runBlocking {
        dao.insertAudio(audio(1))
        dao.insertAudio(audio(2))
        dao.insertAudio(audio(3))
        repository.addTag(1, "rock")
        repository.addTag(2, "rock")
        repository.addTag(3, "pop")

        assertEquals(setOf(1L, 2L), repository.getAudioByTag("rock").map { it.id }.toSet())
        assertEquals(2, dao.getAudioCountByTag("rock"))
        assertEquals(0, dao.getAudioCountByTag("jazz"))
    }

    @Test
    fun 曲からタグを外しても他の曲のタグとタグ自体は残る() = runBlocking {
        dao.insertAudio(audio(1))
        dao.insertAudio(audio(2))
        repository.addTag(1, "rock")
        repository.addTag(2, "rock")

        repository.removeTag(1, "rock")

        assertEquals(emptyList<String>(), repository.getTags(1))
        assertEquals(listOf("rock"), repository.getTags(2))
        assertEquals(listOf(TagEntity("rock")), repository.getAllTags())
    }

    @Test
    fun タグを削除すると全曲から外れる() = runBlocking {
        dao.insertAudio(audio(1))
        dao.insertAudio(audio(2))
        repository.addTag(1, "rock")
        repository.addTag(2, "rock")
        repository.addTag(2, "pop")

        repository.deleteTag("rock")

        assertEquals(listOf(TagEntity("pop")), repository.getAllTags())
        assertEquals(emptyList<String>(), repository.getTags(1))
        assertEquals(listOf("pop"), repository.getTags(2))
        assertEquals(0, dao.getAudioCountByTag("rock"))
    }

    @Test
    fun タグなしの曲を曲名順に取得できる() = runBlocking {
        dao.insertAudio(audio(1, title = "C"))
        dao.insertAudio(audio(2, title = "A"))
        dao.insertAudio(audio(3, title = "B"))
        repository.addTag(3, "rock")

        assertEquals(listOf("A", "C"), repository.getAudioWithoutTags().map { it.title })
    }

    @Test
    fun 最近追加した曲は追加日時の新しい順で同時刻なら曲名順() = runBlocking {
        dao.insertAudio(audio(1, title = "B", addedAt = 100))
        dao.insertAudio(audio(2, title = "A", addedAt = 100))
        dao.insertAudio(audio(3, title = "C", addedAt = 200))

        assertEquals(listOf("C", "A", "B"), repository.getRecentlyAddedAudio().map { it.title })
    }

    @Test
    fun タグ付きの全曲をタグと一緒に取得できる() = runBlocking {
        dao.insertAudio(audio(1))
        dao.insertAudio(audio(2))
        repository.addTag(1, "rock")
        repository.addTag(1, "pop")

        val byId = repository.getAllAudioWithTags().associateBy { it.audio.id }
        assertEquals(setOf("rock", "pop"), byId.getValue(1).tags.map { it.name }.toSet())
        assertEquals(emptyList<TagEntity>(), byId.getValue(2).tags)
    }

    @Test
    fun メタデータを更新してもメモと追加日時と再生回数は保持される() = runBlocking {
        // MediaStore との同期で曲名などが変わっても、アプリ独自の情報は消えてはいけない。
        dao.insertAudio(audio(1, title = "旧タイトル", addedAt = 123))
        repository.updateNote(1, "メモ")
        dao.incrementPlayCount(1)

        dao.updateAudioMetadata(1, "新タイトル", "新アーティスト", "content://audio/new", 9)

        val stored = dao.getAllAudioWithTags().single().audio
        assertEquals("新タイトル", stored.title)
        assertEquals("新アーティスト", stored.artist)
        assertEquals("content://audio/new", stored.uri)
        assertEquals(9L, stored.albumId)
        assertEquals("メモ", stored.note)
        assertEquals(123L, stored.addedAt)
        assertEquals(1L, stored.playCount)
    }

    @Test
    fun メモは未設定なら空文字で更新できる() = runBlocking {
        dao.insertAudio(audio(1))

        assertEquals("", repository.getNote(1))
        assertEquals("", repository.getNote(999))

        repository.updateNote(1, "サビが良い")
        assertEquals("サビが良い", repository.getNote(1))
    }

    @Test
    fun 再生回数は呼ぶたびに1ずつ増える() = runBlocking {
        dao.insertAudio(audio(1))

        repeat(3) { dao.incrementPlayCount(1) }

        assertEquals(3L, dao.getAllAudioWithTags().single().audio.playCount)
    }

    @Test
    fun 曲ごとの音量補正は上書き保存できる() = runBlocking {
        assertEquals(null, dao.getAudioGainDbHundredths(1))

        dao.upsertAudioVolume(AudioVolumeEntity(1, 300))
        dao.upsertAudioVolume(AudioVolumeEntity(1, 450))

        assertEquals(450, dao.getAudioGainDbHundredths(1))
    }

    @Test
    fun プレイリストを作成して編集し削除できる() = runBlocking {
        repository.createPlaylist("お気に入り", "rock & pop")
        val created = repository.getAllPlaylists().single()
        assertEquals("お気に入り", created.name)
        assertEquals("rock & pop", created.expression)

        repository.updatePlaylist(created.id, "作業用", "jazz")
        assertEquals(
            PlaylistEntity(created.id, "作業用", "jazz"),
            repository.getPlaylistById(created.id)
        )

        dao.deletePlaylist(repository.getPlaylistById(created.id))
        assertEquals(emptyList<PlaylistEntity>(), repository.getAllPlaylists())
    }
}
