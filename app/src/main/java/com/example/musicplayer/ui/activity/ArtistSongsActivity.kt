package com.example.musicplayer.ui.activity

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.R
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.model.isVisibleAudioTitle
import com.example.musicplayer.data.local.isExcludedPath
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.adapter.AudioAdapter

class ArtistSongsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_playlist_songs)
        val artistName = intent.getStringExtra("artist") ?: return; title = artistName
        val songs = mutableListOf<AudioFile>(); val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = mutableListOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID).apply { add(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA) }
        contentResolver.query(uri, projection.toTypedArray(),
            "${MediaStore.Audio.Media.ARTIST} = ?", arrayOf(artistName), "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE")?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE); val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID); val path = c.getColumnIndexOrThrow(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) { val t = c.getString(title); if (isVisibleAudioTitle(t) && !isExcludedPath(this, c.getString(path).orEmpty())) { val songId = c.getLong(id); songs += AudioFile(songId, t, c.getString(artist), ContentUris.withAppendedId(uri, songId).toString(), c.getLong(album)) } }
        }
        findViewById<RecyclerView>(R.id.recyclerView).apply { layoutManager = LinearLayoutManager(this@ArtistSongsActivity); adapter = AudioAdapter(songs, { id -> AlbumSongsActivity.albumArtUri(id) }, { _, index -> startService(Intent(this@ArtistSongsActivity, MusicService::class.java).apply { action = "PLAY_PLAYLIST_FROM_INDEX"; putExtra("audioList", ArrayList(songs)); putExtra("index", index) }) }, { }, { }) }
    }
}
