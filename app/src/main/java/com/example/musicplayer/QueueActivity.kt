package com.example.musicplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.net.Uri
import android.content.Intent
import android.content.ContentUris


class QueueActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AudioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)

        recyclerView = findViewById(R.id.queueRecyclerView)

        adapter = AudioAdapter(
            MusicService.playQueue,

            getAlbumArtUri = { albumId: Long ->
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
            },

            onClick = { audio: AudioFile, position: Int ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = "PLAY"
                intent.putExtra("audio", audio)
                startService(intent)
            },

            onAddToQueue = { } // ここは不要なので空
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}