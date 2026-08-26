package com.example.musicplayer.ui.activity

import com.example.musicplayer.R
import com.example.musicplayer.playback.MusicService
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.Button
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.content.IntentFilter
import android.widget.ImageView
import android.net.Uri
import android.content.ContentUris
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.widget.SeekBar

class PlayerActivity : AppCompatActivity() {

    private lateinit var title: TextView
    private lateinit var playPause: Button
    lateinit var albumArt: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var timeText: TextView
    private lateinit var gainSeekBar: SeekBar
    private lateinit var gainText: TextView
    private var currentAudioId: Long = -1L

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                "NOW_PLAYING" -> {
                    title.text = intent.getStringExtra("title")
                    currentAudioId = intent.getLongExtra("audioId", -1L)
                    val albumId = intent.getLongExtra("albumId", -1)
                    if (albumId != -1L) {
                        Glide.with(this@PlayerActivity)
                            .load(getAlbumArtUri(albumId))
                            .placeholder(R.drawable.default_art)   // 読み込み中
                            .error(R.drawable.default_art)         // 読み込み失敗
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(albumArt)
                    } else {
                        albumArt.setImageResource(R.drawable.default_art) // fallback
                    }
                }

                "AUDIO_GAIN_CHANGED" -> {
                    val gain = intent.getIntExtra("gainDbHundredths", 0)
                    gainText.text = "音量補正 ${gain / 100f} dB"
                    gainSeekBar.progress = gain
                }

                "PLAY_STATE_CHANGED" -> {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    playPause.text = if (isPlaying) "⏸" else "▶"
                }
            }
        }
    }

    private val progressReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val current = intent?.getIntExtra("current", 0) ?: 0
            val duration = intent?.getIntExtra("duration", 0) ?: 0
            seekBar.max = duration
            seekBar.progress = current
            timeText.text = "${formatTime(current)} / ${formatTime(duration)}"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        title = findViewById(R.id.playerTitle)
        playPause = findViewById(R.id.playerPlayPause)
        albumArt = findViewById(R.id.playerAlbumArt)
        seekBar = findViewById(R.id.playerSeekBar)
        timeText = findViewById(R.id.playerTimeText)
        gainSeekBar = findViewById(R.id.playerGainSeekBar)
        gainText = findViewById(R.id.playerGainText)
        gainSeekBar.max = 1200
        gainSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser || currentAudioId < 0) return
                gainText.text = "音量補正 ${progress / 100f} dB"
                startService(Intent(this@PlayerActivity, MusicService::class.java).apply {
                    action = "SET_AUDIO_GAIN"
                    putExtra("audioId", currentAudioId)
                    putExtra("gainDbHundredths", progress)
                })
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })

        playPause.setOnClickListener {
            val intent = Intent(this, MusicService::class.java)
            intent.action = "TOGGLE_PLAY"
            startService(intent)
        }

        findViewById<Button>(R.id.playerNext).setOnClickListener {
            startService(Intent(this, MusicService::class.java).setAction("NEXT"))
        }

        findViewById<Button>(R.id.playerPrev).setOnClickListener {
            startService(Intent(this, MusicService::class.java).setAction("PREV"))
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val intent = Intent(this@PlayerActivity, MusicService::class.java)
                    intent.action = "SEEK"
                    intent.putExtra("position", progress)
                    startService(intent)
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter("NOW_PLAYING"))
        registerReceiver(receiver, IntentFilter("PLAY_STATE_CHANGED"))
        registerReceiver(progressReceiver, IntentFilter("MUSIC_PROGRESS"))
        registerReceiver(receiver, IntentFilter("AUDIO_GAIN_CHANGED"))

        startService(Intent(this, MusicService::class.java).apply {
            action = "REQUEST_STATE"
        })

    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
        unregisterReceiver(progressReceiver)
    }

    private fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
