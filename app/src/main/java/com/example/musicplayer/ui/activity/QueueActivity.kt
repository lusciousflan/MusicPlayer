package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.adapter.AudioAdapter
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.content.Intent
import com.example.musicplayer.ui.MiniPlayerController

class QueueActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AudioAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_queue)
        MiniPlayerController(this)
        title = "キュー"

        recyclerView = findViewById(R.id.queueRecyclerView)

        val queueSongs = MusicService.getQueue()
        adapter = AudioAdapter(
            queueSongs,
            getAlbumArtUri = ::albumArtUri,
            onClick = { _, position ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = "PLAY_INDEX"
                intent.putExtra("index", position)
                startService(intent)
            },
            onAddToQueue = { }, // ここは不要なので空
            onEditTag = { } // Queue画面でもタグの編集がしたくなったらここを編集
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}
