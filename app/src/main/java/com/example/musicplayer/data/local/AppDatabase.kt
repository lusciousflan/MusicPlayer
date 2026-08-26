package com.example.musicplayer.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [AudioEntity::class, AudioVolumeEntity::class, TagEntity::class, AudioTagCrossRef::class, PlaylistEntity::class],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioDao(): AudioDao
}
