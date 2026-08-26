package com.example.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_volume")
data class AudioVolumeEntity(
    @PrimaryKey val audioId: Long,
    val gainDbHundredths: Int = 0
)
