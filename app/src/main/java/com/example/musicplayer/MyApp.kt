package com.example.musicplayer

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