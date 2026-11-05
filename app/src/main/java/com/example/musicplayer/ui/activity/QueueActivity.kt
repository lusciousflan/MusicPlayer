package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.ui.AudioItemActions
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
        val actions = AudioItemActions(this)

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
            onAddToQueue = actions::addToQueue,
            onEditTag = { audio -> actions.editTags(audio) },
            onEditMemo = actions::editNote,
            onExcludeDirectory = actions::excludeDirectory
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
}
