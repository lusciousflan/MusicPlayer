package com.example.musicplayer.ui.fragment

import com.example.musicplayer.R
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.data.local.queryAudioFiles
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.activity.MainActivity
import com.example.musicplayer.ui.adapter.AudioAdapter
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import com.example.musicplayer.ui.showAudioSortDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.musicplayer.MyApp

class SongsFragment : Fragment(R.layout.fragment_songs) {

    private val grouping: String?
        get() = arguments?.getString("grouping")

    private lateinit var adapter: AudioAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // MediaStoreから返された現在の順番を反転して表示する
        val originalAudioList = queryAudioFiles(requireContext())
            .let { songs ->
                when (grouping) {
                    "album" -> songs.sortedWith(compareBy({ it.albumId }, { it.title }))
                    "artist" -> songs.sortedWith(compareBy({ it.artist.lowercase() }, { it.title }))
                    else -> songs.reversed()
                }
            }

        var audioList = originalAudioList

        adapter = AudioAdapter(
            audioList,
            getAlbumArtUri = ::albumArtUri,
            onClick = { audio, position ->
                val intent = Intent(
                        requireContext(),
                        MusicService::class.java
                    )
                intent.action = "PLAY"
                intent.putExtra("audio", audio)
                requireContext().startService(intent)
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
            },
            onEditMemo = { audio ->
                (requireActivity() as MainActivity).showNoteDialog(audio)
            },
            onExcludeDirectory = { audio ->
                (requireActivity() as MainActivity).excludeDirectoryFor(audio)
            },
            onSelectionChanged = { count ->
                view.findViewById<Button>(R.id.bulkTagButton).text =
                    if (count == 0) "🏷" else "🏷 ${count}"
            }
        )
        recyclerView.adapter = adapter

        // MediaStoreの表示情報に、DBで管理している追加日時・再生回数を補完する。
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val metadata = (requireContext().applicationContext as MyApp).database
                .audioDao().getAllAudioWithTags().associateBy { it.audio.id }
            val enriched = audioList.map { audio ->
                val stored = metadata[audio.id]?.audio
                if (stored == null) audio else audio.copy(
                    // 起動直後の同期前で DB の追加日時が未記録(0)なら、MediaStore の値のままにする
                    addedAt = stored.addedAt.takeIf { it > 0 } ?: audio.addedAt,
                    playCount = stored.playCount
                )
            }
            launch(Dispatchers.Main) {
                audioList = enriched
                adapter.updateList(enriched)
            }
        }

        view.findViewById<Button>(R.id.sortButton).setOnClickListener {
            showAudioSortDialog(requireContext(), audioList, adapter) { audioList = it }
        }

        val bulkTagButton = view.findViewById<Button>(R.id.bulkTagButton)
        bulkTagButton.setOnClickListener {
            if (!adapter.isSelectionMode()) {
                adapter.startSelection()
            } else {
                val selected = adapter.getSelectedAudios()
                if (selected.isNotEmpty()) {
                    (requireActivity() as MainActivity).showBulkTagDialog(selected) {
                        adapter.stopSelection()
                        bulkTagButton.text = "🏷"
                    }
                }
            }
        }

    }
}
