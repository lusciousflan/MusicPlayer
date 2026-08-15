package com.example.musicplayer

import com.example.musicplayer.data.local.AppDatabase
import android.app.Application
import androidx.room.Room

class MyApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "music-db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}
