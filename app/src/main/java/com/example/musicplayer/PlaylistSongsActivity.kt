package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.net.Uri
import android.content.ContentUris
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import android.content.Intent


class PlaylistSongsActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView
    private var currentSongs: List<AudioFile> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)

        recyclerView = findViewById(R.id.recyclerView)

        val playlistId = intent.getLongExtra("playlistId", -1L)
        if (playlistId == -1L) return

        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch {

            val playlist = repository.getPlaylistById(playlistId)

            title = playlist.name

            val allAudio = repository.getAllAudioWithTags()

            val tokens = tokenize(playlist.expression)

            try {

                val evaluator = PlaylistEvaluator(
                        tokens,
                        allAudio
                    )

                val result = evaluator.evaluate()

                val audioFiles = result.map {
                    AudioFile(
                        id = it.audio.id,
                        title = it.audio.title,
                        artist = it.audio.artist,
                        uri = it.audio.uri,
                        albumId = it.audio.albumId
                    )
                }
                currentSongs = audioFiles

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

            } catch (e: PlaylistSyntaxException) {

                Toast.makeText(
                    this@PlaylistSongsActivity,
                    e.message,
                    Toast.LENGTH_LONG
                ).show()
            }

            recyclerView.layoutManager = LinearLayoutManager(this@PlaylistSongsActivity)

        }
    }

    override fun onCreateOptionsMenu(
        menu: Menu
    ): Boolean {

        menuInflater.inflate(
            R.menu.menu_playlist_songs,
            menu
        )

        return true
    }

    override fun onOptionsItemSelected(
        item: MenuItem
    ): Boolean {

        when (item.itemId) {

            R.id.action_add_queue -> {

                val ids = currentSongs
                    .map { it.id }
                    .toLongArray()

                val intent = Intent(
                        this,
                        MusicService::class.java
                    )
                intent.action = "ADD_LIST_TO_QUEUE"
                intent.putExtra(
                    "audioList",
                    ArrayList(currentSongs)
                )
                startService(intent)

                Toast.makeText(
                    this,
                    "キューに追加しました",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}