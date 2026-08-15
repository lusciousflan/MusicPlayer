package com.example.musicplayer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class PlaylistEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val expression: String
)
