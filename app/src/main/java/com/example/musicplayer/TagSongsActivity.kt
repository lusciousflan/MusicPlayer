package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.ContentUris

class TagSongsActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_songs)

        recyclerView = findViewById(R.id.recyclerView)
        val tag = intent.getStringExtra("tag") ?: return
        title = tag
        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch {

            val songs = repository.getAudioByTag(tag)
            val audioFiles = songs.map {
                AudioFile(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    uri = it.uri,
                    albumId = it.albumId
                )
            }

            recyclerView.layoutManager = LinearLayoutManager(this@TagSongsActivity)
            recyclerView.adapter = AudioAdapter(
                list = audioFiles,
                getAlbumArtUri = { albumId ->
                    ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )
                },
                onClick = { _, _ -> },
                onAddToQueue = { },
                onEditTag = { }
            )
        }
    }
}