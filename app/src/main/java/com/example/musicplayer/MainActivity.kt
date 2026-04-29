package com.example.musicplayer

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.content.Context


class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var audioList: List<AudioFile> = emptyList()
    private var isPlaying = false
    private var currentAudio: AudioFile? = null
    private lateinit var playPauseButton: Button
    private lateinit var nextButton: Button
    private lateinit var prevButton: Button
    private var currentIndex = -1
    private lateinit var seekBar: SeekBar
    private val handler = android.os.Handler()
    private lateinit var timeText: TextView
    private lateinit var adapter: AudioAdapter
    private lateinit var repeatButton: Button

    // シークバーの状態を更新
    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val current = intent?.getIntExtra("current", 0) ?: 0
            val duration = intent?.getIntExtra("duration", 0) ?: 0

            seekBar.max = duration
            seekBar.progress = current

            timeText.text = "${formatTime(current)} / ${formatTime(duration)}"
        }
    }

    // 次の曲を再生するレシーバー
    private val nextReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            playNext()
        }
    }
    // 前の曲を再生するレシーバー
    private val prevReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            playPrev()
        }
    }

    // 画面関連処理
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (checkPermission()) {
            setupRecycler()
        } else {
            requestPermission()
        }
    
        playPauseButton = findViewById(R.id.playPauseButton)
        nextButton = findViewById(R.id.nextButton)
        prevButton = findViewById(R.id.prevButton)
        seekBar = findViewById(R.id.seekBar)
        timeText = findViewById(R.id.timeText)
        val queueButton = findViewById<Button>(R.id.queueButton)
        repeatButton = findViewById(R.id.repeatButton)


        playPauseButton.setOnClickListener {

            if (currentAudio == null) {
                Toast.makeText(this, "曲を選択してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, MusicService::class.java)

            if (isPlaying) {
                // 一時停止
                intent.action = "PAUSE"
                playPauseButton.text = "再生"
                isPlaying = false
            } else {
                // 再開
                intent.action = "RESUME"
                playPauseButton.text = "停止"
                isPlaying = true
            }

            startService(intent)
        }
        nextButton.setOnClickListener {
            playNext()
        }
        prevButton.setOnClickListener {
            playPrev()
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intent = Intent(this@MainActivity, MusicService::class.java)
                    intent.action = "SEEK"
                    intent.putExtra("position", progress)
                    startService(intent)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        queueButton.setOnClickListener {
            val intent = Intent(this, QueueActivity::class.java)
            startActivity(intent)
        }
        repeatButton.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            intent.action = "TOGGLE_REPEAT"
            startService(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(progressReceiver, IntentFilter("MUSIC_PROGRESS"))
        registerReceiver(nextReceiver, IntentFilter("MUSIC_NEXT"))
        registerReceiver(prevReceiver, IntentFilter("MUSIC_PREV"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(progressReceiver)
        unregisterReceiver(nextReceiver)
        unregisterReceiver(prevReceiver)
    }

    private fun setupRecycler() {
        audioList = getAudioFiles()

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = AudioAdapter(
            audioList,
            getAlbumArtUri = { albumId ->
                ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"),
                    albumId
                )
            },
            onClick = { audio, position ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = "PLAY"
                intent.putExtra("audio", audio)
                // Toast.makeText(this, "タップされました", Toast.LENGTH_SHORT).show()

                startService(intent)

                currentIndex = position
                adapter.setCurrentPlaying(position)
            },
            onAddToQueue = { audio ->
                val intent = Intent(this, MusicService::class.java)
                intent.action = "ADD_TO_QUEUE"
                intent.putExtra("audio", audio)
                startService(intent)
            }
            )
        recyclerView.adapter = adapter
    }

    private fun getAudioFiles(): List<AudioFile> {
        val list = mutableListOf<AudioFile>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val cursor = contentResolver.query(
            collection,
            arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM_ID
            ),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.MIME_TYPE} LIKE 'audio/%'",
            null,
            null
        )

        cursor?.use {
            val idCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)

            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val title = it.getString(titleCol)
                val artist = it.getString(artistCol)
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val albumId = it.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))

                list.add(AudioFile(id, title, artist, uri, albumId))
            }
        }
        return list
    }

    private fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }

    private fun playNext() {

        if (audioList.isEmpty()) return

        currentIndex = (currentIndex + 1) % audioList.size
        val audio = audioList[currentIndex]

        playAudio(audio)
    }

    private fun playPrev() {

        if (audioList.isEmpty()) return

        if (currentIndex <= 0) {
            currentIndex = audioList.size - 1
        } else {
            currentIndex--
        }

        val audio = audioList[currentIndex]

        playAudio(audio)
    }

    private fun playAudio(audio: AudioFile) {
        val intent = Intent(this, MusicService::class.java)
        intent.action = "PLAY"
        intent.putExtra("uri", audio.uri)
        intent.putExtra("title", audio.title)
        intent.putExtra("artist", audio.artist)
        intent.putExtra("albumId", audio.albumId)
        startService(intent)

        isPlaying = true
        playPauseButton.text = "停止"
        adapter.setCurrentPlaying(currentIndex)

    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    private fun checkPermission(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        return ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        val permission = if (Build.VERSION.SDK_INT >= 33) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        ActivityCompat.requestPermissions(this, arrayOf(permission), 1)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1 &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            setupRecycler()
        } else {
            Toast.makeText(this, "権限が必要です", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}