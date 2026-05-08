package com.example.musicplayer

import androidx.appcompat.app.AppCompatActivity
// import android.widget.ListView
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.widget.ArrayAdapter
import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager


class PlaylistListActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_list)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        lifecycleScope.launch {

            val playlists = repository.getAllPlaylists()
            val tags = repository.getAllTags()

            val items = mutableListOf<LibraryItem>()

            items.add(LibraryItem.Header("プレイリスト"))

            playlists.forEach {
                items.add(LibraryItem.Playlist(it))
            }

            items.add(LibraryItem.Header("タグ"))

            tags.forEach {
                items.add(LibraryItem.Tag(it))
            }

            recyclerView.adapter = LibraryAdapter(
                items,

                onPlaylistClick = { playlist ->

                    val intent = Intent(
                        this@PlaylistListActivity,
                        PlaylistSongsActivity::class.java
                    )

                    intent.putExtra("playlistId", playlist.id)

                    startActivity(intent)
                },

                onTagClick = { tag ->

                    val intent = Intent(
                        this@PlaylistListActivity,
                        TagSongsActivity::class.java
                    )

                    intent.putExtra("tag", tag.name)

                    startActivity(intent)
                }
            )
        }
    }
}