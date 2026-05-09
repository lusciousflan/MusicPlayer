package com.example.musicplayer

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist")
data class PlaylistEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    val expression: String
)

// 仮実装
fun parseTemporaryExpression(
    expression: String
): Set<String> {

    return expression
        .split(" ")
        .firstOrNull()
        ?.let { setOf(it) }
        ?: emptySet()
}