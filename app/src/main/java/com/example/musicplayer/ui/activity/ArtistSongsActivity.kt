package com.example.musicplayer.ui.activity

import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.playback.startMusicServiceWithAudioList
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.data.local.queryAudioFiles
import com.example.musicplayer.ui.adapter.AudioAdapter
import com.example.musicplayer.ui.showAudioSortDialog
import com.example.musicplayer.ui.MiniPlayerController

class ArtistSongsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_playlist_songs)
        MiniPlayerController(this)
        val artistName = intent.getStringExtra("artist") ?: return; title = artistName
        var songs = queryAudioFiles(
            this,
            "${MediaStore.Audio.Media.ARTIST} = ?",
            arrayOf(artistName),
            "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE"
        )
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@ArtistSongsActivity)
            val songsAdapter = AudioAdapter(songs, ::albumArtUri, { _, index -> startMusicServiceWithAudioList("PLAY_PLAYLIST_FROM_INDEX", songs) { putExtra("index", index) } }, { }, { })
            adapter = songsAdapter
            this@ArtistSongsActivity.findViewById<android.view.View>(R.id.sortButton).setOnClickListener {
                showAudioSortDialog(this@ArtistSongsActivity, songs, songsAdapter) { songs = it }
            }
        }
    }
}
