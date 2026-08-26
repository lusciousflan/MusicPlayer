package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.MusicRepository
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.ui.adapter.AudioAdapter
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
