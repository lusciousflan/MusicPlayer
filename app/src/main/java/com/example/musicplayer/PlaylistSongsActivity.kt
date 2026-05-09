package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.ContentUris


class PlaylistSongsActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)

        recyclerView = findViewById(R.id.recyclerView)

        // val tag = intent.getStringExtra("tag") ?: return

        // title = tag

        val playlistId = intent.getLongExtra("playlistId", -1L)
        if (playlistId == -1L) return

        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch {

            val playlist = repository.getPlaylistById(playlistId)

            title = playlist.name

            val allAudio = repository.getAllAudioWithTags()

            // 仮実装で最初のタグのみを採用する
            val firstTag = playlist.expression
                            .split(" ")
                            .firstOrNull()
                            ?: return@launch

            val songs = repository.getAudioByTag(firstTag)

            val audioFiles = songs.map {
                AudioFile(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    uri = it.uri,
                    albumId = it.albumId
                )
            }

            recyclerView.layoutManager = LinearLayoutManager(this@PlaylistSongsActivity)

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