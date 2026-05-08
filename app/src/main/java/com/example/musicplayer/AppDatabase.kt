package com.example.musicplayer

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AudioEntity::class, TagEntity::class, AudioTagCrossRef::class],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
}