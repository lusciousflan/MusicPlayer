package com.example.musicplayer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Delete
import androidx.room.Query
import androidx.room.OnConflictStrategy
import androidx.room.Transaction
import androidx.room.RewriteQueriesToDropUnusedColumns

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAudioVolume(volume: AudioVolumeEntity)

    @Query("SELECT gainDbHundredths FROM audio_volume WHERE audioId = :audioId")
    suspend fun getAudioGainDbHundredths(audioId: Long): Int?

    @Query("SELECT note FROM audio WHERE id = :audioId")
    suspend fun getAudioNote(audioId: Long): String?

    @Query("UPDATE audio SET note = :note WHERE id = :audioId")
    suspend fun updateAudioNote(audioId: Long, note: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audio: AudioEntity)

    @Query("UPDATE audio SET title = :title, artist = :artist, uri = :uri, albumId = :albumId WHERE id = :id")
    suspend fun updateAudioMetadata(id: Long, title: String, artist: String, uri: String, albumId: Long)

    @Query("DELETE FROM audio WHERE id = :id")
    suspend fun deleteAudio(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCrossRef(crossRef: AudioTagCrossRef)

    @Transaction
    @Query("SELECT * FROM audio")
    suspend fun getAllAudioWithTags(): List<AudioWithTags>

    @Query("SELECT id FROM audio")
    suspend fun getAllIds(): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(audioList: List<AudioEntity>)

    @Query("DELETE FROM audio")
    suspend fun deleteAll()

    // 全タグ取得
    @Query("SELECT * FROM tag ORDER BY name")
    suspend fun getAllTags(): List<TagEntity>

    // ある楽曲についている全タグを取得
    @Query("""
        SELECT tag.name FROM tag
        INNER JOIN AudioTagCrossRef
        ON tag.name = AudioTagCrossRef.tagName
        WHERE AudioTagCrossRef.audioId = :audioId
    """)
    suspend fun getTagsForAudio(audioId: Long): List<String>

    // ある楽曲からタグ削除
    @Query("""
        DELETE FROM AudioTagCrossRef
        WHERE audioId = :audioId AND tagName = :tag
    """)
    suspend fun removeTag(audioId: Long, tag: String)

    @Query("DELETE FROM AudioTagCrossRef WHERE tagName = :tag")
    suspend fun removeTagFromAllAudio(tag: String)

    @Query("DELETE FROM tag WHERE name = :tag")
    suspend fun deleteTag(tag: String)

    // あるタグが付いている楽曲をすべて取得
    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("""
        SELECT * FROM audio
        INNER JOIN AudioTagCrossRef
        ON audio.id = AudioTagCrossRef.audioId
        WHERE AudioTagCrossRef.tagName = :tag
    """)
    suspend fun getAudioByTag(tag: String): List<AudioEntity>

    @Query("""
        SELECT audio.* FROM audio
        LEFT JOIN AudioTagCrossRef ON audio.id = AudioTagCrossRef.audioId
        WHERE AudioTagCrossRef.audioId IS NULL
        ORDER BY audio.title
    """)
    suspend fun getAudioWithoutTags(): List<AudioEntity>

    @Query("SELECT * FROM audio ORDER BY addedAt DESC, title")
    suspend fun getRecentlyAddedAudio(): List<AudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("UPDATE playlist SET name = :name, expression = :expression WHERE id = :id")
    suspend fun updatePlaylist(id: Long, name: String, expression: String)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlist WHERE id = :id")
    suspend fun getPlaylistById(
        id: Long
    ): PlaylistEntity

    // 全タグ名を取得
    @Query("SELECT name FROM tag")
    suspend fun getAllTagNames(): List<String>

}
