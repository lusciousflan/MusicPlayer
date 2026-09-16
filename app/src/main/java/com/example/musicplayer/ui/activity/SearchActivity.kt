package com.example.musicplayer.ui.activity

import android.content.ContentUris
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.ensureActive
import com.example.musicplayer.R
import com.example.musicplayer.model.AudioFile
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.adapter.AudioAdapter
import com.example.musicplayer.ui.adapter.ArtistAdapter
import com.example.musicplayer.ui.adapter.AlbumAdapter
import com.example.musicplayer.ui.adapter.AlbumItem
import com.example.musicplayer.ui.MiniPlayerController
import com.example.musicplayer.model.isVisibleAudioTitle
import com.example.musicplayer.data.local.isExcludedPath

class SearchActivity : AppCompatActivity() {
    private lateinit var songsAdapter: AudioAdapter
    private lateinit var artistNamesAdapter: ArtistAdapter
    private lateinit var albumsAdapter: AlbumAdapter
    private var songs: List<AudioFile> = emptyList()
    private var albums: List<AlbumItem> = emptyList()
    private var searchJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        MiniPlayerController(this)
        title = "検索"
        songsAdapter = createAdapter(emptyList())
        artistNamesAdapter = ArtistAdapter(emptyList())
        albumsAdapter = AlbumAdapter(emptyList())
        findViewById<RecyclerView>(R.id.searchSongs).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = songsAdapter
        }
        findViewById<RecyclerView>(R.id.searchArtists).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = artistNamesAdapter
        }
        findViewById<RecyclerView>(R.id.searchAlbums).apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = albumsAdapter
        }

        lifecycleScope.launch {
            songs = withContext(Dispatchers.IO) { loadSongs() }
            albums = songs.map { AlbumItem(it.albumId, it.albumName) }
                .distinctBy { it.id }
            val currentQuery = findViewById<EditText>(R.id.searchInput).text.toString().trim()
            if (currentQuery.isNotEmpty()) scheduleSearch(currentQuery)
        }

        findViewById<EditText>(R.id.searchInput).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                scheduleSearch(s?.toString().orEmpty().trim())
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    private fun scheduleSearch(query: String) {
        searchJob?.cancel()
        searchJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.Default) {
                searchSongs(query)
            }
            ensureActive()
            songsAdapter.updateList(result.first)
            artistNamesAdapter.updateList(result.second)
            albumsAdapter.updateList(result.third)
            val visibility = if (query.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
            findViewById<android.view.View>(R.id.searchSongsLabel).visibility = visibility
            findViewById<android.view.View>(R.id.searchArtistsLabel).visibility = visibility
            findViewById<android.view.View>(R.id.searchAlbumsLabel).visibility = visibility
            findViewById<android.view.View>(R.id.searchSongsEmpty).visibility =
                if (query.isNotBlank() && result.first.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            findViewById<android.view.View>(R.id.searchArtistsEmpty).visibility =
                if (query.isNotBlank() && result.second.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
            findViewById<android.view.View>(R.id.searchAlbumsEmpty).visibility =
                if (query.isNotBlank() && result.third.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        }
    }

    private fun searchSongs(query: String): Triple<List<AudioFile>, List<String>, List<AlbumItem>> {
        if (query.isBlank()) {
            return Triple(emptyList(), emptyList(), emptyList())
        }
        fun score(audio: AudioFile): Int = when {
            audio.title.equals(query, true) -> 0
            audio.title.startsWith(query, true) -> 1
            else -> 2
        }
        val matched = songs.filter {
            it.title.contains(query, true)
        }.sortedWith(compareBy(::score, { it.title.lowercase() }))
        val matchedArtists = songs
            .filter { it.artist.contains(query, true) }
            .map { it.artist }
            .distinct()
            .sortedBy { it.lowercase() }
        val matchedAlbums = albums.filter { it.name.contains(query, true) }
        return Triple(matched, matchedArtists, matchedAlbums)
    }

    private fun createAdapter(initial: List<AudioFile>) = AudioAdapter(
        initial,
        getAlbumArtUri = { albumId -> ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId) },
        onClick = { audio, _ -> startService(Intent(this, MusicService::class.java).apply { action = "PLAY"; putExtra("audio", audio) }) },
        onAddToQueue = { audio -> startService(Intent(this, MusicService::class.java).apply { action = "ADD_TO_QUEUE"; putExtra("audio", audio) }) },
        onEditTag = { }
    )

    private fun loadSongs(): List<AudioFile> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val result = mutableListOf<AudioFile>()
        val projection = mutableListOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.ALBUM
        ).apply {
            add(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)
        }
        contentResolver.query(
            collection,
            projection.toTypedArray(),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'",
            null, null
        )?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val title = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artist = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val album = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val albumName = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val path = cursor.getColumnIndexOrThrow(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.RELATIVE_PATH else MediaStore.Audio.Media.DATA)
            while (cursor.moveToNext()) {
                val songTitle = cursor.getString(title)
                if (!isVisibleAudioTitle(songTitle)) continue
                if (isExcludedPath(this, cursor.getString(path).orEmpty())) continue
                val songId = cursor.getLong(id)
                result += AudioFile(songId, songTitle, cursor.getString(artist),
                    ContentUris.withAppendedId(collection, songId).toString(), cursor.getLong(album),
                    albumName = cursor.getString(albumName).orEmpty())
            }
        }
        return result
    }
}
