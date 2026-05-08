package com.example.musicplayer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.OnConflictStrategy
import androidx.room.Transaction

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAudio(audio: AudioEntity)

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

    // @Query("DELETE FROM audio WHERE id NOT IN (:ids)")
    // suspend fun deleteMissing(ids: List<Long>)
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

    // あるタグが付いている楽曲をすべて取得
    @Transaction
    @Query("""
        SELECT * FROM audio
        INNER JOIN AudioTagCrossRef
        ON audio.id = AudioTagCrossRef.audioId
        WHERE AudioTagCrossRef.tagName = :tag
    """)
    suspend fun getAudioByTag(tag: String): List<AudioEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlist")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

}