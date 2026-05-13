package com.example.musicplayer

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AudioEntity::class, TagEntity::class, AudioTagCrossRef::class, PlaylistEntity::class],
    version = 5
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
}