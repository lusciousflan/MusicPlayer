package com.example.musicplayer

import androidx.room.Embedded
import androidx.room.Relation
import androidx.room.Junction

data class AudioWithTags(
    @Embedded val audio: AudioEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "name",
        associateBy = Junction(
            value = AudioTagCrossRef::class,
            parentColumn = "audioId",
            entityColumn = "tagName"
        )
    )
    val tags: List<TagEntity>
)