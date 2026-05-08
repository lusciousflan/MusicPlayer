package com.example.musicplayer

import android.content.Context
import android.provider.MediaStore
import android.content.ContentUris

fun loadAudioFromMediaStore(context: Context): List<AudioEntity> {

    val list = mutableListOf<AudioEntity>()

    val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM_ID
    )

    val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    val cursor = context.contentResolver.query(
        uri,
        projection,
        selection,
        null,
        null
    )

    cursor?.use {
        val idIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val titleIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val artistIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val albumIdIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

        while (it.moveToNext()) {
            val id = it.getLong(idIndex)
            val title = it.getString(titleIndex)
            val artist = it.getString(artistIndex)
            val contentUri = ContentUris.withAppendedId(
                uri,
                id
            ).toString()
            val albumId = it.getLong(albumIdIndex)

            list.add(AudioEntity(id, title, artist, contentUri, albumId))
        }
    }

    return list
}