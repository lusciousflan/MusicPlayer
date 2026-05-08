package com.example.musicplayer

import androidx.room.Entity

@Entity(
    primaryKeys = ["audioId", "tagName"]
)
data class AudioTagCrossRef(
    val audioId: Long,
    val tagName: String
)