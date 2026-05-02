package com.example.musicplayer

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



class PlayerActivity : AppCompatActivity() {

    private lateinit var title: TextView
    private lateinit var playPause: Button
    lateinit var albumArt: ImageView

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {

                "NOW_PLAYING" -> {
                    title.text = intent.getStringExtra("title")
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

                "PLAY_STATE_CHANGED" -> {
                    val isPlaying = intent.getBooleanExtra("isPlaying", false)
                    playPause.text = if (isPlaying) "⏸" else "▶"
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        title = findViewById(R.id.playerTitle)
        playPause = findViewById(R.id.playerPlayPause)
        albumArt = findViewById(R.id.playerAlbumArt)

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
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(receiver, IntentFilter("NOW_PLAYING"))
        registerReceiver(receiver, IntentFilter("PLAY_STATE_CHANGED"))
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    private fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }
}