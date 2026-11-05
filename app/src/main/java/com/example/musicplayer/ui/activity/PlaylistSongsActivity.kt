package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.ui.AudioItemActions
import com.example.musicplayer.playback.startMusicServiceWithAudioList
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import com.example.musicplayer.domain.playlist.*
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.ui.adapter.AudioAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.Toast
import android.view.Menu
import android.view.MenuItem
import com.example.musicplayer.ui.showAudioSortDialog
import com.example.musicplayer.ui.MiniPlayerController

class PlaylistSongsActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView
    private var currentSongs: List<AudioFile> = emptyList()
    private val actions by lazy { AudioItemActions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)
        MiniPlayerController(this)

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
                        albumId = it.audio.albumId,
                        addedAt = it.audio.addedAt,
                        playCount = it.audio.playCount
                    )
                }
                currentSongs = audioFiles

                recyclerView.adapter = AudioAdapter(
                    list = audioFiles,
                    getAlbumArtUri = ::albumArtUri,
                    onClick = { _, position ->
                        startMusicServiceWithAudioList("PLAY_PLAYLIST_FROM_INDEX", currentSongs) {
                            putExtra("index", position)
                        }
                    },
                    onAddToQueue = actions::addToQueue,
                    // タグ編集でプレイリストの条件に合わなくなった曲は一覧から外す
                    onEditTag = { audio ->
                        actions.editTagsInList(
                            audio,
                            recyclerView.adapter as AudioAdapter,
                            stillMatches = { audioId -> playlistContains(playlist.expression, audioId) },
                            onListChanged = { currentSongs = it }
                        )
                    },
                    onEditMemo = actions::editNote,
                    onExcludeDirectory = actions::excludeDirectory
                )
                val songsAdapter = recyclerView.adapter as AudioAdapter
                findViewById<android.view.View>(R.id.sortButton).setOnClickListener {
                    showAudioSortDialog(this@PlaylistSongsActivity, currentSongs, songsAdapter) {
                        currentSongs = it
                    }
                }

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

    private suspend fun playlistContains(expression: String, audioId: Long): Boolean =
        try {
            PlaylistEvaluator(tokenize(expression), repository.getAllAudioWithTags())
                .evaluate()
                .any { it.audio.id == audioId }
        } catch (_: PlaylistSyntaxException) {
            true
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

                startMusicServiceWithAudioList("ADD_LIST_TO_QUEUE", currentSongs)

                Toast.makeText(
                    this,
                    "キューに追加しました",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }

            R.id.action_add_queue_shuffle -> {
                startMusicServiceWithAudioList("PLAY_LIST_SHUFFLED", currentSongs)

                Toast.makeText(
                    this,
                    "シャッフルして再生します",
                    Toast.LENGTH_SHORT
                ).show()

                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }
}
