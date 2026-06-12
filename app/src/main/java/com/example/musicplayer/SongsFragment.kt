package com.example.musicplayer

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SongsFragment : Fragment(R.layout.fragment_songs) {

    private lateinit var adapter: AudioAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val audioList = (requireActivity() as MainActivity).getAudioFiles()

        adapter = AudioAdapter(
            audioList,
            getAlbumArtUri = { albumId ->
                ContentUris.withAppendedId(
                    Uri.parse(
                        "content://media/external/audio/albumart"
                    ),
                    albumId
                )
            },
            onClick = { audio, position ->
                val intent = Intent(
                        requireContext(),
                        MusicService::class.java
                    )
                intent.action = "PLAY"
                intent.putExtra("audio", audio)
                requireContext().startService(intent)
                adapter.setCurrentPlaying(position)
            },

            onAddToQueue = { audio ->
                val intent = Intent(
                        requireContext(),
                        MusicService::class.java
                    )
                intent.action = "ADD_TO_QUEUE"
                intent.putExtra("audio", audio)
                requireContext().startService(intent)
            },

            onEditTag = { audio ->
                (requireActivity() as MainActivity)
                    .showTagDialog(audio)
            }
        )
        recyclerView.adapter = adapter
    }
}