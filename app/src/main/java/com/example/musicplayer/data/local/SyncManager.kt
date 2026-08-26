package com.example.musicplayer.data.local

import android.content.Context

suspend fun syncMediaStore(context: Context, dao: AudioDao) {

    val mediaList = loadAudioFromMediaStore(context)
    val dbIds = dao.getAllIds()
    val mediaIds = mediaList.map { it.id }.toHashSet()

    mediaList.forEach { audio ->
        if (audio.id in dbIds) {
            dao.updateAudioMetadata(audio.id, audio.title, audio.artist, audio.uri, audio.albumId)
        } else {
            dao.insertAudio(audio.copy(addedAt = System.currentTimeMillis()))
        }
    }

    dbIds.asSequence()
        .filter { it !in mediaIds }
        .forEach { id -> dao.deleteAudio(id) }
}
