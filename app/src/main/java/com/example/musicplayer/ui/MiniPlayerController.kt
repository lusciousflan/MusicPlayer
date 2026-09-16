package com.example.musicplayer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.musicplayer.R
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.activity.PlayerActivity

/** 作成・設定画面を除く画面で共有するミニプレイヤー。 */
class MiniPlayerController(private val activity: AppCompatActivity) : DefaultLifecycleObserver {
    private val root: View = activity.findViewById(R.id.miniPlayer)
    private val title: TextView = root.findViewById(R.id.miniTitle)
    private val artist: TextView = root.findViewById(R.id.miniArtist)
    private val playPause: Button = root.findViewById(R.id.miniPlayPause)
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                "NOW_PLAYING" -> {
                    title.text = intent.getStringExtra("title").orEmpty()
                    artist.text = intent.getStringExtra("artist").orEmpty()
                }
                "PLAYING_STATE_CHANGED" -> {
                    playPause.text = if (intent.getBooleanExtra("isPlaying", false)) "⏸" else "▶"
                }
            }
        }
    }

    init {
        root.setOnClickListener { activity.startActivity(Intent(activity, PlayerActivity::class.java)) }
        playPause.setOnClickListener {
            activity.startService(Intent(activity, MusicService::class.java).setAction("TOGGLE_PLAY"))
        }
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!registered) {
            activity.registerReceiver(receiver, IntentFilter().apply {
                addAction("NOW_PLAYING")
                addAction("PLAYING_STATE_CHANGED")
            })
            registered = true
        }
        activity.startService(Intent(activity, MusicService::class.java).setAction("REQUEST_STATE"))
    }

    override fun onPause(owner: LifecycleOwner) {
        if (registered) {
            activity.unregisterReceiver(receiver)
            registered = false
        }
    }
}
