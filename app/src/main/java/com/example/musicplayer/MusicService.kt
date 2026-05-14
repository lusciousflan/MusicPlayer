package com.example.musicplayer

import android.app.*
import android.content.Intent
import android.media.MediaPlayer
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

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentTitle: String = "Unknown"
    private var currentArtist: String = ""
    private var isPlaying = false
    private var currentAlbumId: Long = -1
    private var currentIndex = -1
    var isRepeatAll = true
    var isShuffle = false

    companion object {
        var playQueue: MutableList<AudioFile> = mutableListOf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val uriString = intent?.getStringExtra("uri")

        when (intent?.action) {
            "PLAY" -> {
                val audio = intent.getSerializableExtra("audio") as? AudioFile
                if (audio != null) {
                    Toast.makeText(this, "再生します", Toast.LENGTH_SHORT).show()
                    // キューのリセット
                    playQueue.clear()
                    playQueue.add(audio)
                    currentIndex = 0
                    // 再生ボタンの見た目切り替えメッセージの送信
                    val intent = Intent("PLAYING_STATE_CHANGED")
                    intent.putExtra("isPlaying", isPlaying)
                    sendBroadcast(intent)
                    sendNowPlaying()
                    // 再生
                    playCurrent()
                }
            }
            "TOGGLE_PLAY" -> {
                if (isPlaying) {
                    mediaPlayer?.pause()
                    isPlaying = false
                } else {
                    mediaPlayer?.start()
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
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
            }
            "ADD_TO_QUEUE" -> {
                val audio = intent.getSerializableExtra("audio") as AudioFile
                if (audio != null) {
                    val alreadyExists = playQueue.any { it.uri == audio.uri }
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
                    val alreadyExists = playQueue.any {
                            it.uri == audio.uri
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
                mediaPlayer?.seekTo(position)
            }
            "TOGGLE_REPEAT" -> {
                isRepeatAll = !isRepeatAll
                Toast.makeText(this, 
                    if (isRepeatAll) "リピートON" else "リピートOFF",
                    Toast.LENGTH_SHORT
                ).show()
                val intent = Intent("REPEAT_STATE_CHANGED")
                intent.putExtra("isRepeatAll", isRepeatAll)
                sendBroadcast(intent)
            }
            "TOGGLE_SHUFFLE" -> {
                isShuffle = !isShuffle
                Toast.makeText(this, 
                    if (isShuffle) "シャッフルON" else "シャッフルOFF",
                    Toast.LENGTH_SHORT
                ).show()

                if (isShuffle) {
                    shuffleQueue()
                }

                val intent = Intent("SHUFFLE_STATE_CHANGED")
                intent.putExtra("isShuffle", isShuffle)
                sendBroadcast(intent)
            }
            "REQUEST_STATE" -> {
                sendNowPlaying()
            }
        }

        return START_STICKY
    }

    private fun play(uri: Uri) {
        mediaPlayer?.release()
        isPlaying = true

        mediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, uri)
            prepare()

            setOnCompletionListener {
                playNext()
            }

            setOnErrorListener { _, _, _ ->
                playNext()
                true
            }
            start()
        }

        startForeground(1, createNotification())
        startProgressUpdates()
    }

    fun addToQueue(audio: AudioFile) {
        playQueue.add(audio)

        // 初回だけ再生開始
        if (currentIndex == -1) {
            currentIndex = 0
            playCurrent()
        }
    }

    private fun shuffleQueue() {
        if (playQueue.isEmpty()) return

        // 再生中の曲を保持
        val current = playQueue.getOrNull(currentIndex) ?: return

        // 現在の曲以外を取り出す
        val others = playQueue.filterIndexed { index, _ -> index != currentIndex }.toMutableList()

        // シャッフル
        others.shuffle()

        // 新しいキュー作成（先頭に現在の曲）
        playQueue.clear()
        playQueue.add(current)
        playQueue.addAll(others)

        // インデックスをリセット
        currentIndex = 0
    }

    private fun playCurrent() {
        if (currentIndex !in playQueue.indices) {
            stopSelf()
            return
        }
        val audio = playQueue[currentIndex]
        currentTitle = audio.title
        currentAlbumId = audio.albumId
        sendNowPlaying()

        play(Uri.parse(audio.uri))
    }

    private fun playNext() {
        currentIndex++

        // キューの最後まで再生したとき
        if (currentIndex >= playQueue.size) {
            if (isRepeatAll) {
                // 先頭に戻る
                currentIndex = 0
            } else {
                // 止める
                stopSelf()
                return
            }
        }
        playCurrent()
    }

    private fun playPrev() {
        if (playQueue.isEmpty()) return

        currentIndex--

        if (currentIndex < 0) {
            currentIndex = 0
        }

        playCurrent()
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

        val icon = if (isPlaying)
            android.R.drawable.ic_media_pause
        else
            android.R.drawable.ic_media_play

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
        val notification = createNotification()
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    private fun startProgressUpdates() {
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    val intent = Intent("MUSIC_PROGRESS")
                    intent.putExtra("current", it.currentPosition)
                    intent.putExtra("duration", it.duration)
                    sendBroadcast(intent)
                }
                handler.postDelayed(this, 500)
            }
        })
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
        intent.putExtra("albumId", currentAlbumId)
        sendBroadcast(intent)
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}