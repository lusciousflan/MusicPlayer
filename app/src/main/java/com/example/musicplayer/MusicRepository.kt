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
        include: List<String>,
        exclude: List<String>
    ) {

        dao.insertPlaylist(
            PlaylistEntity(
                name = name,
                includeTags = include.joinToString(","),
                excludeTags = exclude.joinToString(",")
            )
        )
    }

    suspend fun getPlaylistSongs(
        playlist: PlaylistEntity
    ): List<AudioWithTags> {

        val all = dao.getAllAudioWithTags()

        val include =
            playlist.includeTags
                .split(",")
                .filter { it.isNotBlank() }

        val exclude =
            playlist.excludeTags
                .split(",")
                .filter { it.isNotBlank() }

        return all.filter { audio ->

            val tags = audio.tags.map { it.name }

            include.all { it in tags } &&
            exclude.none { it in tags }
        }
    }
    suspend fun getAllPlaylists(): List<PlaylistEntity> {
        return dao.getAllPlaylists()
    }

}