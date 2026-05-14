package com.example.musicplayer

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

    suspend fun getTags(audioId: Long): List<String> {
        return dao.getTagsForAudio(audioId)
    }

    suspend fun getAllTags(): List<TagEntity> {
        return dao.getAllTags()
    }

    suspend fun getAudioByTag(tag: String): List<AudioEntity> {
        return dao.getAudioByTag(tag)
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

    suspend fun getAllAudioWithTags():
        List<AudioWithTags> {
        return dao.getAllAudioWithTags()
    }

}