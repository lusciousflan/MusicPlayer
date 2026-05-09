package com.example.musicplayer

class MusicRepository(private val dao: AudioDao) {

    var allAudio: List<AudioFile> = emptyList()

    fun applyRule(list: List<AudioWithTags>, rule: PlaylistRule): List<AudioWithTags> {
        return list.filter { audio ->

            val tagNames = audio.tags.map { it.name }

            val includeOK = rule.includeTags.all { it in tagNames }
            val excludeOK = rule.excludeTags.none { it in tagNames }

            includeOK && excludeOK
        }
    }

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

    fun normalizeTag(tag: String): String {
        return tag.trim().lowercase()
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

    suspend fun getPlaylistSongs(
        playlist: PlaylistEntity
    ): List<AudioWithTags> {

        val all = dao.getAllAudioWithTags()

        // 仮実装で最初のタグのみ採用する
        val firstTag = parseTemporaryExpression(playlist.expression)

        return all.filter { audio ->

            val tags = audio.tags.map { it.name }
            firstTag.all { it in tags }

            // include.all { it in tags } &&
            // exclude.none { it in tags }
        }
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