package com.example.musicplayer.model

import android.net.Uri
import java.io.Serializable

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: String,
    val albumId: Long,
    val albumName: String = "",
    val tags: MutableSet<String> = mutableSetOf(),
    val addedAt: Long = 0L,
    val playCount: Long = 0L
) : Serializable
