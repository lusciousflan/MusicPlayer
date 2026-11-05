package com.example.musicplayer.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.musicplayer.MyApp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** DB バージョン 9 のデータが、最新版へのマイグレーション後もそのまま読めることを確かめる。 */
@RunWith(AndroidJUnit4::class)
@Config(application = android.app.Application::class)
class MigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    /** バージョン 9 のテーブル定義（Room が生成していたもの）でデータを作る。 */
    private fun createVersion9Database() {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }, null)
        db.execSQL("CREATE TABLE IF NOT EXISTS `audio` (`id` INTEGER NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `uri` TEXT NOT NULL, `albumId` INTEGER NOT NULL, `note` TEXT NOT NULL, `addedAt` INTEGER NOT NULL, `playCount` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `audio_volume` (`audioId` INTEGER NOT NULL, `gainDbHundredths` INTEGER NOT NULL, PRIMARY KEY(`audioId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `tag` (`name` TEXT NOT NULL, PRIMARY KEY(`name`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `AudioTagCrossRef` (`audioId` INTEGER NOT NULL, `tagName` TEXT NOT NULL, PRIMARY KEY(`audioId`, `tagName`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_AudioTagCrossRef_tagName` ON `AudioTagCrossRef` (`tagName`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `playlist` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `expression` TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '23e8f368537c1245982d5ca4d2e163ea')")

        db.execSQL("INSERT INTO audio VALUES (1, '曲', 'アーティスト', 'content://audio/1', 5, 'メモ', 123, 7)")
        db.execSQL("INSERT INTO tag VALUES ('rock')")
        db.execSQL("INSERT INTO AudioTagCrossRef VALUES (1, 'rock')")
        db.execSQL("INSERT INTO playlist (name, expression) VALUES ('ロック', 'rock')")
        db.execSQL("INSERT INTO audio_volume VALUES (1, 300)")
        db.version = 9
        db.close()
    }

    @Test
    fun バージョン9からマイグレーションしてもデータが残る() = runBlocking {
        createVersion9Database()

        val db = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MyApp.MIGRATION_9_10)
            .build()
        try {
            val dao = db.audioDao()
            val audio = dao.getAllAudio().single()
            assertEquals(
                AudioEntity(1, "曲", "アーティスト", "content://audio/1", 5, note = "メモ", addedAt = 123, playCount = 7, albumName = ""),
                audio
            )
            assertEquals(listOf("rock"), dao.getTagsForAudio(1))
            assertEquals("rock", dao.getAllPlaylists().single().expression)
            assertEquals(300, dao.getAudioGainDbHundredths(1))
        } finally {
            db.close()
        }
    }
}
