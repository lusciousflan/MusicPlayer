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

@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class)
class AudioSyncTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AudioDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        dao = db.audioDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun media(id: Long, title: String = "曲$id", addedAt: Long = 1_000L) =
        AudioEntity(id = id, title = title, artist = "artist", uri = "content://audio/$id", albumId = 1, addedAt = addedAt)

    private val now = 9_999_000L

    // ---- 変更内容の計算 ----

    @Test
    fun 新しい曲はMediaStoreの追加日時で追加される() {
        val plan = planAudioSync(emptyList(), listOf(media(1, addedAt = 5_000)), now)

        assertEquals(listOf(media(1, addedAt = 5_000)), plan.inserts)
    }

    @Test
    fun MediaStoreに追加日時がない新しい曲は現在時刻で追加される() {
        val plan = planAudioSync(emptyList(), listOf(media(1, addedAt = 0)), now)

        assertEquals(now, plan.inserts.single().addedAt)
    }

    @Test
    fun 変化のない曲は書き込まない() {
        val stored = listOf(media(1), media(2))

        val plan = planAudioSync(stored, listOf(media(1), media(2)), now)

        assertEquals(AudioSyncPlan(emptyList(), emptyList(), emptyList()), plan)
    }

    @Test
    fun 既存の曲はメタデータだけ更新しアプリ独自の情報を保持する() {
        val stored = media(1, title = "旧", addedAt = 3_000).copy(note = "メモ", playCount = 7)

        val plan = planAudioSync(listOf(stored), listOf(media(1, title = "新", addedAt = 5_000)), now)

        assertEquals(listOf(stored.copy(title = "新")), plan.updates)
    }

    @Test
    fun 追加日時が未記録の既存曲はMediaStoreの追加日時で補う() {
        val stored = media(1, addedAt = 0).copy(playCount = 3)

        val plan = planAudioSync(listOf(stored), listOf(media(1, addedAt = 5_000)), now)

        assertEquals(listOf(stored.copy(addedAt = 5_000)), plan.updates)
    }

    @Test
    fun MediaStoreから消えた曲は削除する() {
        val plan = planAudioSync(listOf(media(1), media(2)), listOf(media(2)), now)

        assertEquals(listOf(1L), plan.deletedIds)
    }

    // ---- DB への反映 ----

    @Test
    fun 同期結果がDBに反映される() = runBlocking {
        dao.insertAudio(media(1, title = "旧", addedAt = 0).copy(note = "メモ"))
        dao.insertAudio(media(2))
        dao.incrementPlayCount(1)

        syncAudio(db, listOf(media(1, title = "新", addedAt = 5_000), media(3, addedAt = 7_000)), now)

        val byId = dao.getAllAudio().associateBy { it.id }
        assertEquals(setOf(1L, 3L), byId.keys)
        assertEquals(media(1, title = "新", addedAt = 5_000).copy(note = "メモ", playCount = 1), byId[1])
        assertEquals(media(3, addedAt = 7_000), byId[3])
    }

    @Test
    fun 同期で曲を削除してもタグの紐付けは残る() = runBlocking {
        // SDカードの抜き差しなどで一時的に曲が消えても、戻ってきたときにタグが復活するようにしている。
        dao.insertAudio(media(1))
        MusicRepository(dao).addTag(1, "rock")

        syncAudio(db, emptyList(), now)

        assertEquals(emptyList<AudioEntity>(), dao.getAllAudio())
        assertEquals(1, dao.getAudioCountByTag("rock"))
    }

    @Test
    fun 数千曲でもまとめて同期できる() = runBlocking {
        val songs = (1L..3000L).map { media(it) }

        syncAudio(db, songs, now)
        syncAudio(db, songs.map { it.copy(title = it.title + "!") }, now)

        assertEquals(3000, dao.getAllAudio().count { it.title.endsWith("!") })
    }
}
