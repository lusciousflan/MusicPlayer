package com.example.musicplayer

import androidx.room.Entity
import androidx.room.Index

@Entity(
    primaryKeys = ["audioId", "tagName"],
    indices = [Index("tagName")]
)
data class AudioTagCrossRef(
    val audioId: Long,
    val tagName: String
)