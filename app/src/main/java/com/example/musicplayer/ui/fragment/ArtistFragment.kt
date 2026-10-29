package com.example.musicplayer.ui.fragment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.data.local.queryAudioFiles
import com.example.musicplayer.ui.adapter.ArtistAdapter

class ArtistFragment : Fragment(R.layout.fragment_artist) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 楽曲一覧と同じ条件（除外ディレクトリなど）で絞り込んだ曲から、アーティストを集める。
        val artists = queryAudioFiles(requireContext())
            .map { it.artist }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        view.findViewById<RecyclerView>(R.id.artistRecycler).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = ArtistAdapter(artists)
        }
    }
}
