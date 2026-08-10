package com.example.musicplayer

import android.app.*
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaSession
import android.content.Context
import android.net.Uri
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.content.ContentUris
import android.util.Log

class MusicService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private var mediaSession: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentTitle: String = "Unknown"
    private var currentArtist: String = ""
    private var isPlaying = false
    private var currentAlbumId: Long = -1
    var isRepeatAll = true
    var isShuffle = false
    private val audioMap = mutableMapOf<String, AudioFile>()
    private var progressRunnable: Runnable? = null

    companion object {
        var player: ExoPlayer? = null
        fun getQueue(): List<AudioFile> {
            val p = player ?: return emptyList()
            return (0 until p.mediaItemCount).mapNotNull {
                index -> p.getMediaItemAt(index).localConfiguration?.tag as? AudioFile
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this).build()
        MusicService.player = player
        mediaSession = MediaSession.Builder(this, player).build()
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    val intent = Intent("PLAYING_STATE_CHANGED")
                    intent.putExtra("isPlaying", playing)
                    sendBroadcast(intent)
                    updateNotification()
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val audio = audioMap[mediaItem?.mediaId]
                    currentTitle = audio?.title ?: ""
                    currentArtist = audio?.artist ?: ""
                    currentAlbumId = audio?.albumId ?: -1
                    sendNowPlaying()
                }
            }
        )
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            "MusicService",
            "onStartCommand action=${intent?.action}"
        )

        when (intent?.action) {
            "PLAY" -> {
                val audio = intent.getSerializableExtra("audio") as? AudioFile
                if (audio != null) {
                    Toast.makeText(this, "再生します", Toast.LENGTH_SHORT).show()
                    audioMap[audio.id.toString()] = audio
                    currentTitle = audio.title
                    currentArtist = audio.artist
                    currentAlbumId = audio.albumId
                    // キューのリセット
                    player.clearMediaItems()
                    player.setMediaItem(audio.toMediaItem())
                    // 再生ボタンの見た目切り替えメッセージの送信
                    val intent = Intent("PLAYING_STATE_CHANGED")
                    intent.putExtra("isPlaying", isPlaying)
                    sendBroadcast(intent)
                    sendNowPlaying()
                    // 再生
                    player.prepare()
                    player.play()
                    startForeground(1, createNotification())
                    startProgressUpdates()
                }
            }
            "TOGGLE_PLAY" -> {
                if (isPlaying) {
                    player.pause()
                    isPlaying = false
                } else {
                    player.play()
                    isPlaying = true
                }
                updateNotification()
                // 再生ボタンの見た目切り替えメッセージの送信
                val intent = Intent("PLAYING_STATE_CHANGED")
                intent.putExtra("isPlaying", isPlaying)
                sendBroadcast(intent)
                sendNowPlaying()
            }
            "STOP" -> {
                player.stop()
            }
            "ADD_TO_QUEUE" -> {
                val audio = intent.getSerializableExtra("audio") as AudioFile
                if (audio != null) {
                    val alreadyExists = (0 until player.mediaItemCount).any { index ->
                        player.getMediaItemAt(index).mediaId == audio.id.toString()
                        }
                    if (!alreadyExists) {
                        addToQueue(audio)
                    } else {
                        Log.d("MusicService", "Already in queue")
                    }
                }
            }
            "ADD_LIST_TO_QUEUE" -> {
                val audios = intent.getSerializableExtra(
                        "audioList"
                    ) as? ArrayList<AudioFile>

                audios?.forEach { audio ->
                    val alreadyExists = (0 until player.mediaItemCount).any { 
                        index -> player.getMediaItemAt(index).mediaId == audio.id.toString()
                        }
                    if (!alreadyExists) {
                        addToQueue(audio)
                    }
                }
            }
            "NEXT" -> playNext()
            "PREV" -> playPrev()
            "SEEK" -> {
                val position = intent.getIntExtra("position", 0)
                player.seekTo(position.toLong())
            }
            "TOGGLE_REPEAT" -> {
                player.repeatMode =
                    if (player.repeatMode == Player.REPEAT_MODE_ALL) Player.REPEAT_MODE_OFF
                    else Player.REPEAT_MODE_ALL
                isRepeatAll = player.repeatMode == Player.REPEAT_MODE_ALL
                Toast.makeText(this, 
                    if (isRepeatAll) "リピートON" else "リピートOFF",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent("REPEAT_STATE_CHANGED")
                intent.putExtra("isRepeatAll", isRepeatAll)
                sendBroadcast(intent)
            }
            "TOGGLE_SHUFFLE" -> {
                player.shuffleModeEnabled = !player.shuffleModeEnabled
                isShuffle = player.shuffleModeEnabled
                Toast.makeText(this, 
                    if (isShuffle) "シャッフルON" else "シャッフルOFF",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent("SHUFFLE_STATE_CHANGED")
                intent.putExtra("isShuffle", isShuffle)
                sendBroadcast(intent)
            }
            "REQUEST_STATE" -> {
                sendNowPlaying()
                sendBroadcast(Intent("PLAYING_STATE_CHANGED").putExtra("isPlaying", player.isPlaying))
            }
            "PLAY_INDEX" -> {
                val index = intent.getIntExtra("index", 0)
                player.seekTo(index, 0)
                player.play()
            }
        }

        return START_STICKY
    }

    fun addToQueue(audio: AudioFile) {
        audioMap[audio.id.toString()] = audio
        player.addMediaItem(audio.toMediaItem())

        // 初回だけ再生開始
        if (player.mediaItemCount == 1) {
            player.prepare()
            player.play()
        }
    }

    private fun playNext() {
        player.seekToNextMediaItem()
    }

    private fun playPrev() {
        player.seekToPreviousMediaItem()
    }

    private fun createNotification(): Notification {

        val channelId = "music_channel"
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            channelId,
            "Music Player",
            NotificationManager.IMPORTANCE_LOW
        )
        manager.createNotificationChannel(channel)

        // 前へ
        val prevIntent = Intent(this, MusicService::class.java).apply {
            action = "PREV"
        }
        val prevPending = PendingIntent.getService(
            this, 1, prevIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 次へ
        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = "NEXT"
        }
        val nextPending = PendingIntent.getService(
            this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 再生 or 一時停止
        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = "TOGGLE_PLAY"
        }
        val playPausePending = PendingIntent.getService(
            this, 3, playPauseIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val icon = 
            if (isPlaying) android.R.drawable.ic_media_pause
            else android.R.drawable.ic_media_play

        val text = if (isPlaying) "Pause" else "Play"

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(currentTitle)
            .setContentText(currentArtist)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setLargeIcon(getAlbumArt(currentAlbumId))
            .addAction(android.R.drawable.ic_media_previous, "Prev", prevPending)
            .addAction(icon, text, playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle())
            .build()
    }

    private fun updateNotification() {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, createNotification())
    }

    private fun startProgressUpdates() {
        progressRunnable?.let {
            handler.removeCallbacks(it)
        }
        progressRunnable = object : Runnable {
            override fun run() {
                Log.d(
                    "MusicService",
                    "progress=${player.currentPosition} duration=${player.duration}"
                )
                if (player.isPlaying || player.playbackState != Player.STATE_IDLE) {
                    val intent = Intent("MUSIC_PROGRESS")
                    intent.putExtra("current", player.currentPosition.toInt())
                    intent.putExtra("duration", player.duration.toInt())
                    sendBroadcast(intent)
                }
                handler.postDelayed(this, 500)
            }
        }
        handler.post(progressRunnable!!)
    }


    private fun getAlbumArt(albumId: Long): Bitmap? {
        return try {
            val uri = Uri.parse("content://media/external/audio/albumart")
            val artUri = ContentUris.withAppendedId(uri, albumId)
            val input = contentResolver.openInputStream(artUri)
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            null
        }
    }

    private fun sendNowPlaying() {
        val intent = Intent("NOW_PLAYING")
        intent.putExtra("title", currentTitle)
        intent.putExtra("artist", currentArtist)
        intent.putExtra("albumId", currentAlbumId)
        sendBroadcast(intent)
    }

    private fun AudioFile.toMediaItem(): MediaItem {
        return MediaItem.Builder()
            .setUri(uri)
            .setMediaId(id.toString())
            .setTag(this)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        Log.d("MusicService", "onDestroy")
        progressRunnable?.let {
            handler.removeCallbacks(it)
        }
        try {
            if (this::player.isInitialized) {
                player.release()
            }
        } catch (e: Exception) {
            Log.w("MusicService", "Error releasing player", e)
        }
        mediaSession?.run {
            release()
            mediaSession = null
        }
        MusicService.player = null
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("MusicService", "onTaskRemoved")
        super.onTaskRemoved(rootIntent)
    }
}