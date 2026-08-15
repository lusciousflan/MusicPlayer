package com.example.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio")
data class AudioEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val uri: String,
    val albumId: Long
)
