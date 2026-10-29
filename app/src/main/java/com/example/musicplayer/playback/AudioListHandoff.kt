package com.example.musicplayer.playback

import android.content.Context
import android.content.Intent
import com.example.musicplayer.model.AudioFile
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 画面から MusicService へ曲リストを渡すための受け渡し場所。
 *
 * Intent に曲リストを直接載せると、1000曲程度で Android のプロセス間通信の上限(約1MB)を超え、
 * 命令が MusicService に届かないまま捨てられる。MusicService は画面と同じプロセスで動くため、
 * リスト本体はここに置き、Intent には受け取り用のキーだけを載せる。
 */
object AudioListHandoff {
    const val EXTRA_KEY = "audioListKey"

    private val pending = ConcurrentHashMap<Long, List<AudioFile>>()
    private val nextKey = AtomicLong()

    /** リストを預けて、受け取り用のキーを返す。 */
    fun put(audios: List<AudioFile>): Long {
        val key = nextKey.incrementAndGet()
        pending[key] = audios.toList()
        return key
    }

    /** キーに対応するリストを取り出す。取り出したリストは受け渡し場所から消える。 */
    fun take(key: Long): List<AudioFile>? = pending.remove(key)

    /** Intent に載せたキーから、預けられたリストを取り出す。 */
    fun take(intent: Intent): List<AudioFile>? =
        intent.getLongExtra(EXTRA_KEY, -1L).takeIf { it >= 0 }?.let(::take)
}

/**
 * 曲リスト付きの命令を MusicService に送る。
 * @param configure リスト以外に Intent に載せる値（再生開始位置など）を設定する。
 */
fun Context.startMusicServiceWithAudioList(
    action: String,
    audios: List<AudioFile>,
    configure: Intent.() -> Unit = {}
) {
    startService(
        Intent(this, MusicService::class.java)
            .setAction(action)
            .putExtra(AudioListHandoff.EXTRA_KEY, AudioListHandoff.put(audios))
            .apply(configure)
    )
}
