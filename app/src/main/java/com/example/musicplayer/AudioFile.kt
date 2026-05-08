package com.example.musicplayer

import android.net.Uri
import java.io.Serializable

data class AudioFile(
    val id: Long,
    val title: String,
    val artist: String,
    val uri: String,
    val albumId: Long,
    val tags: MutableSet<String> = mutableSetOf()
) : Serializable