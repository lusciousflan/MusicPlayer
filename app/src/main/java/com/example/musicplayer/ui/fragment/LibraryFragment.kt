package com.example.musicplayer.ui.fragment

import com.example.musicplayer.R
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.*
import com.example.musicplayer.model.LibraryItem
import com.example.musicplayer.ui.activity.*
import com.example.musicplayer.ui.adapter.LibraryAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.launch

class LibraryFragment : Fragment(R.layout.fragment_library) {
    private lateinit var repository: MusicRepository
    private lateinit var recyclerView: RecyclerView
    private lateinit var dao: AudioDao

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        dao = (requireActivity().application as MyApp).database.audioDao()
        repository = MusicRepository(dao)

        // loadLibrary()
    }

    fun reloadLibrary() {
        viewLifecycleOwner.lifecycleScope.launch {
            val playlists = repository.getAllPlaylists()
            val tags = repository.getAllTags()
            val items = mutableListOf<LibraryItem>()

            items.add(LibraryItem.RecentlyAdded)
            items.add(LibraryItem.Untagged)

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
                        requireContext(),
                        PlaylistSongsActivity::class.java
                    )
                    intent.putExtra("playlistId", playlist.id)
                    startActivity(intent)
                },

                onTagClick = { tag ->
                    val intent = Intent(
                        requireContext(),
                        TagSongsActivity::class.java
                    )
                    intent.putExtra("tag", tag.name)
                    startActivity(intent)
                },

                onUntaggedClick = {
                    startActivity(Intent(requireContext(), TagSongsActivity::class.java).apply {
                        putExtra("untagged", true)
                    })
                },

                onPlaylistLongClick = { playlist ->
                    showDeletePlaylistDialog(
                        playlist
                    )
                }
            )
        }
    }
    
    private fun showDeletePlaylistDialog(playlist: PlaylistEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("プレイリスト削除")
            .setMessage(
                "「${playlist.name}」を削除しますか？"
            )
            .setNeutralButton("編集") { _, _ ->
                startActivity(Intent(requireContext(), CreatePlaylistActivity::class.java).apply {
                    putExtra("playlistId", playlist.id)
                })
            }
            .setPositiveButton("削除") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    dao.deletePlaylist(playlist)
                    reloadLibrary()
                }
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        reloadLibrary()
    }

}
