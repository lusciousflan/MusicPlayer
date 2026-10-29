package com.example.musicplayer.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.data.local.queryAudioFiles
import com.example.musicplayer.ui.adapter.AlbumAdapter
import com.example.musicplayer.ui.adapter.AlbumItem

class AlbumFragment : Fragment(R.layout.fragment_album) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 楽曲一覧と同じ条件（除外ディレクトリなど）で絞り込んだ曲から、アルバムを集める。
        val albums = queryAudioFiles(requireContext())
            .map { AlbumItem(it.albumId, it.albumName) }
            .distinctBy { it.id }
            .sortedBy { it.name.lowercase() }
        view.findViewById<RecyclerView>(R.id.albumRecycler).apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = AlbumAdapter(albums)
        }
    }
}
