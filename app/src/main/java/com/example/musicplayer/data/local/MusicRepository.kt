package com.example.musicplayer.data.local

import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.util.normalizeTag
class MusicRepository(private val dao: AudioDao) {

    var allAudio: List<AudioFile> = emptyList()

    suspend fun addTag(audioId: Long, tag: String) {
        val normalized = normalizeTag(tag)

        dao.insertTag(TagEntity(normalized))
        dao.insertCrossRef(AudioTagCrossRef(audioId, normalized))
    }

    suspend fun removeTag(audioId: Long, tag: String) {
        dao.removeTag(audioId, tag)
    }

    suspend fun deleteTag(tag: String) {
        dao.removeTagFromAllAudio(tag)
        dao.deleteTag(tag)
    }

    suspend fun getTags(audioId: Long): List<String> {
        return dao.getTagsForAudio(audioId)
    }

    suspend fun getNote(audioId: Long): String = dao.getAudioNote(audioId).orEmpty()

    suspend fun updateNote(audioId: Long, note: String) {
        dao.updateAudioNote(audioId, note)
    }

    suspend fun getAllTags(): List<TagEntity> {
        return dao.getAllTags()
    }

    suspend fun getAudioByTag(tag: String): List<AudioEntity> {
        return dao.getAudioByTag(tag)
    }

    suspend fun getAudioWithoutTags(): List<AudioEntity> {
        return dao.getAudioWithoutTags()
    }

    suspend fun getRecentlyAddedAudio(): List<AudioEntity> {
        return dao.getRecentlyAddedAudio()
    }

    suspend fun createPlaylist(
        name: String,
        expression: String
    ) {
        dao.insertPlaylist(
            PlaylistEntity(
                name = name,
                expression = expression
            )
        )
    }

    suspend fun getAllPlaylists(): List<PlaylistEntity> {
        return dao.getAllPlaylists()
    }

    suspend fun getPlaylistById(
        id: Long
    ): PlaylistEntity {
        return dao.getPlaylistById(id)
    }

    suspend fun updatePlaylist(id: Long, name: String, expression: String) {
        dao.updatePlaylist(id, name, expression)
    }

    suspend fun getAllAudioWithTags():
        List<AudioWithTags> {
        return dao.getAllAudioWithTags()
    }

}
