package com.example.musicplayer.ui.fragment

import com.example.musicplayer.R
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.activity.MainActivity
import com.example.musicplayer.ui.adapter.AudioAdapter
import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.text.Editable
import android.text.TextWatcher
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SongsFragment : Fragment(R.layout.fragment_songs) {

    private lateinit var adapter: AudioAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // MediaStoreから返された現在の順番を反転して表示する
        val audioList = (requireActivity() as MainActivity).getAudioFiles().reversed()

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

        view.findViewById<EditText>(R.id.searchEditText).addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    text: CharSequence?, start: Int, count: Int, after: Int
                ) = Unit

                override fun onTextChanged(
                    text: CharSequence?, start: Int, before: Int, count: Int
                ) {
                    val query = text?.toString()?.trim()?.lowercase() ?: ""
                    adapter.updateList(
                        if (query.isEmpty()) {
                            audioList
                        } else {
                            audioList.filter { audio ->
                                audio.title.lowercase().contains(query) ||
                                    audio.artist.lowercase().contains(query)
                            }
                        }
                    )
                }

                override fun afterTextChanged(text: Editable?) = Unit
            }
        )
    }
}
