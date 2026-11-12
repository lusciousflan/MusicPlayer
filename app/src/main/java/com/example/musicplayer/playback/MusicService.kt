package com.example.musicplayer.playback

import com.example.musicplayer.model.AudioFile
import android.app.*
import android.content.Intent
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.session.MediaSessionService
import androidx.media3.session.MediaSession
import android.content.Context
import android.content.SharedPreferences
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.media.audiofx.LoudnessEnhancer
import com.example.musicplayer.MyApp
import com.example.musicplayer.data.local.AudioVolumeEntity
import com.example.musicplayer.data.local.AudioWithTags
import com.example.musicplayer.data.local.albumArtUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    private var albumArtCacheId: Long? = null
    private var albumArtCache: Bitmap? = null
    private var albumArtLoadingId: Long? = null
    private var sleepTimerRunnable: Runnable? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var queueStore: QueueStore
    // 保存したキューを読み込んでいる間は、空のキューで保存内容を上書きしないように保存を止める
    private var isRestoringQueue = false
    // キューの復元で曲が切り替わったときは、再生回数を数えない
    private var suppressPlayCount = false
    // キューの曲の並びが変わり、曲 ID の一覧を保存し直す必要があるか
    private var queueItemsDirty = false
    private val saveQueueRunnable = Runnable { saveQueue() }
    private lateinit var settingsPreferences: SharedPreferences
    // SharedPreferences は登録したリスナーを弱参照でしか持たないため、フィールドで保持する
    private val settingsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == PlaybackSettings.KEY_PAUSE_FOR_OTHER_APPS) applyAudioFocusSetting()
    }
    private val progressWatchersListener: (Boolean) -> Unit = { watched ->
        if (watched && player.isPlaying) startProgressUpdates() else if (!watched) stopProgressUpdates()
    }

    companion object {
        // 通知のアルバムアートを読み込むときの最大の一辺(px)。これより大きい画像は縮小する。
        private const val ALBUM_ART_MAX_SIZE = 512

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

        settingsPreferences = getSharedPreferences("music_settings", MODE_PRIVATE)
        player = ExoPlayer.Builder(this)
            // イヤホンを抜いたら、スピーカーで鳴り続けないよう一時停止する
            .setHandleAudioBecomingNoisy(true)
            .build()
        applyAudioFocusSetting()
        settingsPreferences.registerOnSharedPreferenceChangeListener(settingsListener)
        // UI に表示する初期値と ExoPlayer の実際のリピート状態を一致させる。
        player.repeatMode = Player.REPEAT_MODE_ALL
        isRepeatAll = player.repeatMode == Player.REPEAT_MODE_ALL
        MusicService.player = player
        mediaSession = MediaSession.Builder(this, player).build()
        queueStore = QueueStore(this)
        player.addListener(
            object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                    val intent = Intent("PLAYING_STATE_CHANGED")
                    intent.putExtra("isPlaying", playing)
                    sendBroadcast(intent)
                    updateNotification()
                    if (playing) {
                        startProgressUpdates()
                    } else {
                        // 止まった位置を画面に反映してから、定期送信をやめる
                        stopProgressUpdates()
                        sendProgress()
                        // 一時停止したまま終了されても同じ位置から再開できるように保存する
                        saveQueue()
                    }
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) {
                        applyGainFor(audioMap[player.currentMediaItem?.mediaId])
                        // 一時停止中に曲を切り替えた場合も、曲の長さが分かった時点で画面に反映する
                        sendProgress()
                    }
                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val audio = audioMap[mediaItem?.mediaId]
                    currentTitle = audio?.title ?: ""
                    currentArtist = audio?.artist ?: ""
                    currentAlbumId = audio?.albumId ?: -1
                    applyGainFor(audio)
                    if (audio != null && !suppressPlayCount) {
                        serviceScope.launch(Dispatchers.IO) {
                            (application as MyApp).database.audioDao().incrementPlayCount(audio.id)
                        }
                    }
                    sendNowPlaying()
                    sendProgress()
                    updateNotification()
                    scheduleQueueSave()
                }

                override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                    if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                        queueItemsDirty = true
                        scheduleQueueSave()
                    }
                }

                override fun onRepeatModeChanged(repeatMode: Int) {
                    isRepeatAll = repeatMode == Player.REPEAT_MODE_ALL
                    sendBroadcast(
                        Intent("REPEAT_STATE_CHANGED")
                            .putExtra("isRepeatAll", isRepeatAll)
                    )
                }
            }
        )
        ProgressWatchers.addListener(progressWatchersListener)
        restoreSavedQueue()
    }

    /**
     * 「他のアプリが音を鳴らしたら一時停止する」設定を反映する。
     * オンなら音声フォーカスを扱い、他のアプリの再生や通話に合わせて一時停止・音量を下げる。
     * オフなら音声フォーカスを扱わず、他のアプリの音と重なっても再生を続ける。
     */
    private fun applyAudioFocusSetting() {
        player.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                .build(),
            /* handleAudioFocus = */ PlaybackSettings.pauseForOtherApps(settingsPreferences)
        )
    }

    /**
     * 前回終了したときのキューを、再生はせずに読み込んでおく。
     * 読み込み中に別の曲の再生が始まった場合は、そちらを優先して復元しない。
     */
    private fun restoreSavedQueue() {
        val saved = queueStore.load() ?: return
        isRestoringQueue = true
        serviceScope.launch {
            val restored = try {
                val audiosById = withContext(Dispatchers.IO) {
                    (application as MyApp).database.audioDao().getAllAudioWithTags()
                }.associate { it.audio.id to it.toAudioFile() }
                restoreQueue(saved, audiosById)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w("MusicService", "Failed to restore queue", e)
                null
            } finally {
                isRestoringQueue = false
            }
            if (restored == null || player.mediaItemCount > 0) return@launch

            restored.audios.forEach { audio -> audioMap[audio.id.toString()] = audio }
            isShuffle = saved.isShuffle
            suppressPlayCount = true
            try {
                player.setMediaItems(
                    restored.audios.map { it.toMediaItem() },
                    restored.currentIndex,
                    restored.positionMs
                )
            } finally {
                suppressPlayCount = false
            }
            // 読み込んだ一覧は保存済みのものと同じなので、書き直さない
            queueItemsDirty = false
            // 曲の長さと再生位置を画面に出せるよう、再生はせずに準備だけしておく
            player.prepare()
            sendBroadcast(Intent("SHUFFLE_STATE_CHANGED").putExtra("isShuffle", isShuffle))
            sendNowPlaying()
        }
    }

    /** キューの編集が続いたときに何度も書き込まないよう、少し待ってからまとめて保存する。 */
    private fun scheduleQueueSave() {
        handler.removeCallbacks(saveQueueRunnable)
        handler.postDelayed(saveQueueRunnable, 1_000)
    }

    private fun saveQueue(sync: Boolean = false) {
        handler.removeCallbacks(saveQueueRunnable)
        if (isRestoringQueue) return
        // 曲 ID の一覧は、キューの並びが変わったときだけ作って保存する
        val ids = if (queueItemsDirty) {
            (0 until player.mediaItemCount).mapNotNull {
                player.getMediaItemAt(it).mediaId.toLongOrNull()
            }
        } else {
            emptyList()
        }
        queueStore.save(
            SavedQueue(
                audioIds = ids,
                currentIndex = player.currentMediaItemIndex,
                positionMs = player.currentPosition,
                isShuffle = isShuffle
            ),
            includeItems = queueItemsDirty,
            sync = sync
        )
        queueItemsDirty = false
    }

    private fun AudioWithTags.toAudioFile() = AudioFile(
        id = audio.id,
        title = audio.title,
        artist = audio.artist,
        uri = audio.uri,
        albumId = audio.albumId,
        albumName = audio.albumName,
        tags = tags.mapTo(mutableSetOf()) { it.name },
        addedAt = audio.addedAt,
        playCount = audio.playCount
    )

    private fun applyGainFor(audio: AudioFile?) {
        if (audio == null) return
        serviceScope.launch {
            val gain = (application as MyApp).database.audioDao()
                .getAudioGainDbHundredths(audio.id) ?: 0
            // A previous asynchronous lookup must not overwrite the newly
            // selected track's value after a fast track transition.
            if (player.currentMediaItem?.mediaId != audio.id.toString()) return@launch
            setLoudnessGain(gain)
            sendBroadcast(Intent("AUDIO_GAIN_CHANGED").apply {
                putExtra("audioId", audio.id)
                putExtra("gainDbHundredths", gain)
            })
        }
    }

    private fun sendCurrentGain() {
        val audioId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        serviceScope.launch {
            val gain = (application as MyApp).database.audioDao()
                .getAudioGainDbHundredths(audioId) ?: 0
            if (player.currentMediaItem?.mediaId != audioId.toString()) return@launch
            sendBroadcast(Intent("AUDIO_GAIN_CHANGED").apply {
                putExtra("audioId", audioId)
                putExtra("gainDbHundredths", gain)
            })
        }
    }

    /**
     * 音量補正を設定する。補正しない曲(0)では、音声にエフェクト処理をかけないよう無効にする。
     */
    private fun setLoudnessGain(gain: Int) {
        if (gain <= 0) {
            loudnessEnhancer?.setEnabled(false)
            return
        }
        ensureLoudnessEnhancer()
        loudnessEnhancer?.apply {
            setTargetGain(gain)
            setEnabled(true)
        }
    }

    private fun ensureLoudnessEnhancer() {
        if (loudnessEnhancer != null) return
        val sessionId = player.audioSessionId
        if (sessionId == 0) return
        try {
            loudnessEnhancer = LoudnessEnhancer(sessionId)
        } catch (e: RuntimeException) {
            // Some device audio implementations don't provide this effect.
            Log.w("MusicService", "LoudnessEnhancer is not available", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(
            "MusicService",
            "onStartCommand action=${intent?.action}"
        )

        when (intent?.action) {
            "SET_SLEEP_TIMER" -> {
                sleepTimerRunnable?.let(handler::removeCallbacks)
                sleepTimerRunnable = null
                val minutes = intent.getIntExtra("minutes", 0)
                if (minutes > 0) {
                    sleepTimerRunnable = Runnable {
                        player.pause()
                        sleepTimerRunnable = null
                    }.also { handler.postDelayed(it, minutes * 60_000L) }
                }
            }
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
                    enterForegroundPlayback()
                }
            }
            "TOGGLE_PLAY" -> {
                // 通話中などで音声フォーカスを一時的に失っている間は、isPlaying は false でも再生待ちになっている。
                // そのとき押されたら、フォーカスが戻っても自動で再開しないよう一時停止として扱う。
                val wantsToPlay = player.playWhenReady && player.playbackState != Player.STATE_ENDED
                if (wantsToPlay) {
                    player.pause()
                    isPlaying = false
                } else {
                    prepareIfNeeded()
                    player.play()
                    isPlaying = true
                    enterForegroundPlayback()
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
                val audio = intent.getSerializableExtra("audio") as? AudioFile
                if (audio != null) {
                    val alreadyExists = (0 until player.mediaItemCount).any { index ->
                        player.getMediaItemAt(index).mediaId == audio.id.toString()
                    }
                    if (!alreadyExists) {
                        if (isShuffle) {
                            mixShuffledQueueIntoExisting(listOf(audio))
                        } else {
                            addToQueue(audio)
                        }
                    } else {
                        Log.d("MusicService", "Already in queue")
                    }
                }
            }
            "ADD_LIST_TO_QUEUE" -> {
                val audios = AudioListHandoff.take(intent)

                if (audios != null) {
                    if (isShuffle) {
                        mixShuffledQueueIntoExisting(audios)
                    } else {
                        addListToQueueAfterCurrent(audios)
                    }
                }
            }
            "PLAY_LIST_SHUFFLED" -> {
                val audios = AudioListHandoff.take(intent)

                isShuffle = true
                val shuffleIntent = Intent("SHUFFLE_STATE_CHANGED")
                shuffleIntent.putExtra("isShuffle", isShuffle)
                sendBroadcast(shuffleIntent)

                playAudioList(audios?.shuffled().orEmpty(), 0)
            }
            "PLAY_PLAYLIST_FROM_INDEX" -> {
                val audios = AudioListHandoff.take(intent)
                val index = intent.getIntExtra("index", -1)

                if (audios.isNullOrEmpty() || index !in audios.indices) return START_STICKY

                Toast.makeText(this, "再生します", Toast.LENGTH_SHORT).show()

                val orderedAudios = if (isShuffle) {
                    val selected = audios[index]
                    listOf(selected) + audios
                        .filterIndexed { itemIndex, _ -> itemIndex != index }
                        .shuffled()
                } else {
                    audios
                }
                val playbackIndex = if (isShuffle) 0 else index
                playAudioList(orderedAudios, playbackIndex)
            }
            "NEXT" -> playNext()
            "PREV" -> playPrev()
            "SEEK" -> {
                val position = intent.getIntExtra("position", 0)
                player.seekTo(position.toLong())
                sendProgress()
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
                isShuffle = !isShuffle
                if (isShuffle) {
                    shuffleCurrentQueue()
                }
                scheduleQueueSave()
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
                sendCurrentGain()
                sendBroadcast(Intent("PLAYING_STATE_CHANGED").putExtra("isPlaying", player.isPlaying))
                isRepeatAll = player.repeatMode == Player.REPEAT_MODE_ALL
                sendBroadcast(Intent("REPEAT_STATE_CHANGED").putExtra("isRepeatAll", isRepeatAll))
                sendBroadcast(Intent("SHUFFLE_STATE_CHANGED").putExtra("isShuffle", isShuffle))
                // 一時停止中は再生位置を定期送信していないため、開いた画面のシークバー用に1回送る
                sendProgress()
            }
            "PLAY_INDEX" -> {
                val index = intent.getIntExtra("index", 0)
                prepareIfNeeded()
                player.seekTo(index, 0)
                player.play()
                enterForegroundPlayback()
            }
            "SET_AUDIO_GAIN" -> {
                val audioId = intent.getLongExtra("audioId", -1L)
                val gain = intent.getIntExtra("gainDbHundredths", 0).coerceIn(0, 1200)
                if (audioId >= 0) {
                    serviceScope.launch(Dispatchers.IO) {
                        (application as MyApp).database.audioDao()
                            .upsertAudioVolume(AudioVolumeEntity(audioId, gain))
                        launch(Dispatchers.Main) {
                            if (player.currentMediaItem?.mediaId == audioId.toString()) {
                                setLoudnessGain(gain)
                            }
                        }
                    }
                }
            }
        }

        return START_STICKY
    }

    /**
     * 何も再生していない状態でキューに曲を追加した場合など、ExoPlayer がまだ再生準備されていなければ準備する。
     * 準備されていないまま play() しても音は鳴らない。
     */
    private fun prepareIfNeeded() {
        if (player.playbackState == Player.STATE_IDLE && player.mediaItemCount > 0) {
            player.prepare()
        }
    }

    private fun enterForegroundPlayback() {
        startForeground(1, createNotification())
        startProgressUpdates()
    }

    private fun shuffleCurrentQueue() {
        val count = player.mediaItemCount
        if (count == 0) return
        val currentIndex = player.currentMediaItemIndex

        // シャッフルを切り替えても、再生中の曲と再生位置は維持する。
        // 現在曲を先頭に固定し、それ以外の曲だけをシャッフルする。
        // 現在曲そのものには触れず、前後の曲だけをまとめて入れ替えるので再生は途切れない。
        // （1曲ずつ移動すると、曲数が多いときに画面が応答しなくなるほど重くなる）
        val others = (0 until count)
            .filter { it != currentIndex }
            .map { player.getMediaItemAt(it) }
            .shuffled()
        player.replaceMediaItems(currentIndex + 1, count, others)
        if (currentIndex > 0) {
            player.removeMediaItems(0, currentIndex)
        }

        sendNowPlaying()
    }

    private fun playAudioList(audios: List<AudioFile>, startIndex: Int) {
        if (audios.isEmpty() || startIndex !in audios.indices) return

        audios.forEach { audio -> audioMap[audio.id.toString()] = audio }
        // 1曲ずつ追加するとキュー全体の再構築と通知が曲数分発生して重いため、まとめて差し替える。
        player.setMediaItems(audios.map { it.toMediaItem() }, startIndex, 0)

        player.prepare()
        player.play()
        enterForegroundPlayback()
        sendNowPlaying()
    }

    // キューの編集は、曲数が多くても ExoPlayer への操作が1回で済むようにまとめて行う。
    // 1曲ずつ追加すると、そのたびにキュー全体の再構築と MediaSession への通知が発生して重くなる。

    private fun mixShuffledQueueIntoExisting(newItems: List<AudioFile>) {
        val audios = audiosNotInQueue(queuedMediaIds(), newItems)
        if (audios.isEmpty()) return
        audios.forEach { audio -> audioMap[audio.id.toString()] = audio }

        // 再生中の曲より後ろ(再生予定の曲)に、新しい曲をランダムな位置で混ぜる。
        // 再生中の曲には触れないので、再生は途切れない。
        val anchorIndex = getInsertionIndexAfterCurrent()
        val upcoming = (anchorIndex until player.mediaItemCount).map { player.getMediaItemAt(it) }
        val mixed = mixRandomly(upcoming, audios.map { it.toMediaItem() })
        player.replaceMediaItems(anchorIndex, player.mediaItemCount, mixed)
    }

    private fun addListToQueueAfterCurrent(newItems: List<AudioFile>) {
        val audios = audiosNotInQueue(queuedMediaIds(), newItems)
        if (audios.isEmpty()) return
        audios.forEach { audio -> audioMap[audio.id.toString()] = audio }

        player.addMediaItems(getInsertionIndexAfterCurrent(), audios.map { it.toMediaItem() })
    }

    private fun queuedMediaIds(): Set<String> =
        (0 until player.mediaItemCount).mapTo(HashSet()) { player.getMediaItemAt(it).mediaId }

    private fun getInsertionIndexAfterCurrent(): Int {
        val current = player.currentMediaItemIndex
        return if (current >= 0 && player.mediaItemCount > 0) {
            current + 1
        } else {
            player.mediaItemCount
        }
    }

    fun addToQueue(audio: AudioFile) {
        audioMap[audio.id.toString()] = audio
        val insertIndex = getInsertionIndexAfterCurrent()
        player.addMediaItem(insertIndex, audio.toMediaItem())
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
            .setLargeIcon(albumArtFor(currentAlbumId))
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

    /**
     * 再生中で、再生位置を表示している画面があるときだけ、秒が変わるごとに再生位置を画面へ送る。
     * 一時停止中や画面オフ中は送らない（電池の消費を抑えるため）。再生が止まるか画面が隠れるとループも止まる。
     */
    private fun startProgressUpdates() {
        stopProgressUpdates()
        // 画面オフ中など、再生位置を表示している画面がなければ送らない
        if (!ProgressWatchers.isWatched) return
        progressRunnable = object : Runnable {
            override fun run() {
                sendProgress()
                if (player.isPlaying && ProgressWatchers.isWatched) {
                    handler.postDelayed(this, ProgressWatchers.nextUpdateDelayMs(player.currentPosition))
                } else {
                    progressRunnable = null
                }
            }
        }.also(handler::post)
    }

    private fun stopProgressUpdates() {
        progressRunnable?.let(handler::removeCallbacks)
        progressRunnable = null
    }

    private fun sendProgress() {
        if (!player.isPlaying && player.playbackState == Player.STATE_IDLE) return
        val intent = Intent("MUSIC_PROGRESS")
        intent.putExtra("current", player.currentPosition.toInt())
        intent.putExtra("duration", player.duration.toInt())
        sendBroadcast(intent)
    }

    /**
     * 通知に表示するアルバムアート。
     * 画像の読み込みは重いため、通知を更新するたびにメインスレッドで読み込まず、バックグラウンドで1回だけ読んで保持する。
     * 読み込みが終わるまではアートなしで表示し、読み込み後に通知を更新する。
     */
    private fun albumArtFor(albumId: Long): Bitmap? {
        if (albumId < 0) return null
        if (albumArtCacheId == albumId) return albumArtCache
        if (albumArtLoadingId != albumId) {
            albumArtLoadingId = albumId
            serviceScope.launch {
                val bitmap = withContext(Dispatchers.IO) { loadAlbumArt(albumId) }
                if (albumArtLoadingId != albumId) return@launch
                albumArtLoadingId = null
                albumArtCacheId = albumId
                albumArtCache = bitmap
                if (currentAlbumId == albumId) updateNotification()
            }
        }
        return null
    }

    /** アルバムアートを通知用の大きさまで縮小して読み込む。画像がなければ null。 */
    private fun loadAlbumArt(albumId: Long): Bitmap? {
        return try {
            val uri = albumArtUri(albumId)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sampleSize = 1
            while (bounds.outWidth / (sampleSize * 2) >= ALBUM_ART_MAX_SIZE &&
                bounds.outHeight / (sampleSize * 2) >= ALBUM_ART_MAX_SIZE
            ) {
                sampleSize *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        } catch (e: Exception) {
            null
        }
    }

    private fun sendNowPlaying() {
        val audioId = player.currentMediaItem?.mediaId?.toLongOrNull() ?: NowPlaying.NONE
        NowPlaying.update(audioId)
        val intent = Intent("NOW_PLAYING")
        intent.putExtra("title", currentTitle)
        intent.putExtra("artist", currentArtist)
        intent.putExtra("albumId", currentAlbumId)
        intent.putExtra("audioId", audioId)
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
        ProgressWatchers.removeListener(progressWatchersListener)
        if (this::settingsPreferences.isInitialized) {
            settingsPreferences.unregisterOnSharedPreferenceChangeListener(settingsListener)
        }
        stopProgressUpdates()
        try {
            if (this::player.isInitialized) {
                saveQueue(sync = true)
                player.release()
            }
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            Log.w("MusicService", "Error releasing player", e)
        }
        serviceScope.cancel()
        mediaSession?.run {
            release()
            mediaSession = null
        }
        MusicService.player = null
        NowPlaying.update(NowPlaying.NONE)
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.d("MusicService", "onTaskRemoved")
        // この後プロセスごと終了されることがあるため、ここで確実に保存しておく
        saveQueue(sync = true)

        if (player.isPlaying) {
            startForeground(1, createNotification())
            return
        }

        super.onTaskRemoved(rootIntent)
    }
}
