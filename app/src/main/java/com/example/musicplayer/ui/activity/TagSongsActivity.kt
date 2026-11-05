package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.ui.AudioItemActions
import com.example.musicplayer.playback.startMusicServiceWithAudioList
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.ui.adapter.AudioAdapter
import com.example.musicplayer.ui.showAudioSortDialog
import com.example.musicplayer.ui.MiniPlayerController
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class TagSongsActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView
    private val actions by lazy { AudioItemActions(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_songs)
        MiniPlayerController(this)

        recyclerView = findViewById(R.id.recyclerView)
        val isUntagged = intent.getBooleanExtra("untagged", false)
        val isRecentlyAdded = intent.getBooleanExtra("recentlyAdded", false)
        val tag = intent.getStringExtra("tag")
        if (!isUntagged && !isRecentlyAdded && tag == null) return
        title = when {
            isUntagged -> "タグなし"
            isRecentlyAdded -> "最近追加した曲"
            else -> tag!!
        }
        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch {

            val songs = if (isUntagged) {
                repository.getAudioWithoutTags()
            } else if (isRecentlyAdded) {
                repository.getRecentlyAddedAudio()
            } else {
                repository.getAudioByTag(tag!!)
            }
            var audioFiles = songs.map {
                AudioFile(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    uri = it.uri,
                    albumId = it.albumId,
                    albumName = it.albumName,
                    addedAt = it.addedAt,
                    playCount = it.playCount
                )
            }

            recyclerView.layoutManager = LinearLayoutManager(this@TagSongsActivity)
            recyclerView.adapter = AudioAdapter(
                list = audioFiles,
                getAlbumArtUri = ::albumArtUri,
                // プレイリスト画面と同じく、表示中の並び順のまま、タップした曲から再生する
                onClick = { _, position ->
                    startMusicServiceWithAudioList("PLAY_PLAYLIST_FROM_INDEX", audioFiles) {
                        putExtra("index", position)
                    }
                },
                onAddToQueue = actions::addToQueue,
                // タグ編集でこの一覧の条件に合わなくなった曲は一覧から外す
                onEditTag = { audio ->
                    actions.editTagsInList(
                        audio,
                        recyclerView.adapter as AudioAdapter,
                        stillMatches = { audioId ->
                            val tags = repository.getTags(audioId)
                            when {
                                isUntagged -> tags.isEmpty()
                                isRecentlyAdded -> true
                                else -> tag in tags
                            }
                        },
                        onListChanged = { audioFiles = it }
                    )
                },
                onEditMemo = actions::editNote,
                onExcludeDirectory = actions::excludeDirectory
            )
            val songsAdapter = recyclerView.adapter as AudioAdapter
            findViewById<android.view.View>(R.id.sortButton).setOnClickListener {
                showAudioSortDialog(this@TagSongsActivity, audioFiles, songsAdapter) { audioFiles = it }
            }
        }
    }
}
