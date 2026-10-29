package com.example.musicplayer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.musicplayer.R
import com.example.musicplayer.playback.MusicService
import com.example.musicplayer.ui.activity.PlayerActivity

/**
 * 作成・設定画面を除く画面で共有するミニプレイヤー。
 * レイアウトに `@layout/mini_player` を include した Activity の onCreate で生成する。
 */
class MiniPlayerController(private val activity: AppCompatActivity) : DefaultLifecycleObserver {
    private val root: View = activity.findViewById(R.id.miniPlayer)
    private val title: TextView = root.findViewById(R.id.miniTitle)
    private val artist: TextView = root.findViewById(R.id.miniArtist)
    private val playPause: Button = root.findViewById(R.id.miniPlayPause)
    private val seekBar: SeekBar = root.findViewById(R.id.miniSeekBar)
    private val timeText: TextView = root.findViewById(R.id.miniTimeText)
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
                "MUSIC_PROGRESS" -> {
                    val current = intent.getIntExtra("current", 0)
                    val duration = intent.getIntExtra("duration", 0)
                    seekBar.max = duration
                    seekBar.progress = current
                    timeText.text = "${formatTime(current)} / ${formatTime(duration)}"
                }
            }
        }
    }

    init {
        root.setOnClickListener { activity.startActivity(Intent(activity, PlayerActivity::class.java)) }
        playPause.setOnClickListener {
            activity.startService(Intent(activity, MusicService::class.java).setAction("TOGGLE_PLAY"))
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    activity.startService(
                        Intent(activity, MusicService::class.java)
                            .setAction("SEEK")
                            .putExtra("position", progress)
                    )
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        if (!registered) {
            // アプリ内の MusicService からの通知だけを受け取るため、外部には公開しない。
            ContextCompat.registerReceiver(
                activity,
                receiver,
                IntentFilter().apply {
                    addAction("NOW_PLAYING")
                    addAction("PLAYING_STATE_CHANGED")
                    addAction("MUSIC_PROGRESS")
                },
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
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

    private fun formatTime(ms: Int): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}
