package com.example.musicplayer.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.playback.startMusicServiceWithAudioList
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.data.local.albumArtUri
import com.example.musicplayer.data.local.queryAudioFiles
import com.example.musicplayer.ui.adapter.AudioAdapter
import com.example.musicplayer.ui.showAudioSortDialog
import com.example.musicplayer.ui.MiniPlayerController

class AlbumSongsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)
        MiniPlayerController(this)
        val albumId = intent.getLongExtra("albumId", -1)
        title = intent.getStringExtra("albumName") ?: "アルバム"
        var songs = loadSongs(albumId)
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@AlbumSongsActivity)
            adapter = AudioAdapter(songs, ::albumArtUri,
                onClick = { _, index -> startMusicServiceWithAudioList("PLAY_PLAYLIST_FROM_INDEX", songs) { putExtra("index", index) } },
                onAddToQueue = { }, onEditTag = { })
            val songsAdapter = adapter as AudioAdapter
            this@AlbumSongsActivity.findViewById<android.view.View>(R.id.sortButton).setOnClickListener {
                showAudioSortDialog(this@AlbumSongsActivity, songs, songsAdapter) { songs = it }
            }
        }
    }
    private fun loadSongs(albumId: Long): List<AudioFile> = queryAudioFiles(
        this,
        "${MediaStore.Audio.Media.ALBUM_ID} = ?",
        arrayOf(albumId.toString())
    )
    companion object {
        fun intent(context: Context, id: Long, name: String) = Intent(context, AlbumSongsActivity::class.java).apply { putExtra("albumId", id); putExtra("albumName", name) }
    }
}
