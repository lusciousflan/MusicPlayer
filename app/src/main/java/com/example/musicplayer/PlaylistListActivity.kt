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
import androidx.appcompat.app.AlertDialog


class PlaylistListActivity : AppCompatActivity() {

    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var dao: AudioDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_list)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        dao = (application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        loadLibrary()

    }

    private fun showDeletePlaylistDialog(
        playlist: PlaylistEntity
    ) {

        AlertDialog.Builder(this)
            .setTitle("プレイリスト削除")
            .setMessage(
                "「${playlist.name}」を削除しますか？"
            )
            .setPositiveButton("削除") { _, _ ->
                lifecycleScope.launch {
                    dao.deletePlaylist(playlist)
                    loadLibrary()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun loadLibrary() {

        lifecycleScope.launch {

            val playlists = repository.getAllPlaylists()
            val tags = repository.getAllTags()

            val items = mutableListOf<LibraryItem>()

            items.add(LibraryItem.Header("プレイリスト"))

            items.add(LibraryItem.CreatePlaylist)

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
                },

                onPlaylistLongClick = { playlist ->
                    showDeletePlaylistDialog(
                        playlist
                    )
                }
            )
        }
    }
}