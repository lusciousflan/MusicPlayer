package com.example.musicplayer

import com.example.musicplayer.data.local.AppDatabase
import android.app.Application
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

class MyApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "music-db"
        )
        .addMigrations(MIGRATION_5_6)
        .build()
    }

    companion object {
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS audio_volume (audioId INTEGER NOT NULL, gainDbHundredths INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(audioId))")
            }
        }
    }
}
