package com.example.musicplayer.ui.activity

import android.content.ContentUris
import android.content.Context
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
import com.example.musicplayer.ui.adapter.AudioAdapter

class AlbumSongsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_songs)
        val albumId = intent.getLongExtra("albumId", -1)
        title = intent.getStringExtra("albumName") ?: "アルバム"
        val songs = loadSongs(albumId)
        findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = LinearLayoutManager(this@AlbumSongsActivity)
            adapter = AudioAdapter(songs, { id -> albumArtUri(id) },
                onClick = { audio, index -> startService(Intent(this@AlbumSongsActivity, com.example.musicplayer.playback.MusicService::class.java).apply { action = "PLAY_PLAYLIST_FROM_INDEX"; putExtra("audioList", ArrayList(songs)); putExtra("index", index) }) },
                onAddToQueue = { }, onEditTag = { })
        }
    }
    private fun loadSongs(albumId: Long): List<AudioFile> {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val result = mutableListOf<AudioFile>()
        val projection = mutableListOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID).apply {
            add(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)
        }
        contentResolver.query(uri, projection.toTypedArray(),
            "${MediaStore.Audio.Media.ALBUM_ID} = ?", arrayOf(albumId.toString()), null)?.use { c ->
            val id = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID); val title = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST); val album = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val path = c.getColumnIndexOrThrow(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) { val t = c.getString(title); if (isVisibleAudioTitle(t) && !isExcludedPath(this, c.getString(path).orEmpty())) { val songId = c.getLong(id); result += AudioFile(songId, t, c.getString(artist), ContentUris.withAppendedId(uri, songId).toString(), c.getLong(album)) } }
        }
        return result
    }
    companion object {
        fun albumArtUri(id: Long): Uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), id)
        fun intent(context: Context, id: Long, name: String) = Intent(context, AlbumSongsActivity::class.java).apply { putExtra("albumId", id); putExtra("albumName", name) }
    }
}
